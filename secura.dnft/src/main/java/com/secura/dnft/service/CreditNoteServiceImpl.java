package com.secura.dnft.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.secura.dnft.dao.CreditNoteRepository;
import com.secura.dnft.entity.CreditNoteEntity;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.interfaceservice.CreditNoteInterface;
import com.secura.dnft.request.response.CreditNoteAvailableRequest;
import com.secura.dnft.request.response.CreditNoteAvailableResponse;
import com.secura.dnft.request.response.CreditNoteDetails;
import com.secura.dnft.request.response.DeleteCreditNoteRequest;
import com.secura.dnft.request.response.DeleteCreditNoteResponse;
import com.secura.dnft.request.response.IssueCreditNoteRequest;
import com.secura.dnft.request.response.IssueCreditNoteResponse;
import com.secura.dnft.request.response.ReedemCreditNoteRequest;
import com.secura.dnft.request.response.ReedemCreditNoteResponse;
import com.secura.dnft.request.response.ValidateCreditNoteRequest;
import com.secura.dnft.request.response.ValidateCreditNoteResponse;
import com.secura.dnft.request.response.ViewCreditNoteDetailsRequest;
import com.secura.dnft.request.response.ViewCreditNoteDetailsResponse;

@Service
public class CreditNoteServiceImpl implements CreditNoteInterface {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreditNoteServiceImpl.class);

	@Autowired
	private CreditNoteRepository creditNoteRepository;
	
	@Autowired
	private CreditNoteUtiltyService creditNoteUtiltyService;
	
	@Override
	public IssueCreditNoteResponse issueCreditNote(IssueCreditNoteRequest request) throws Exception {
		LOGGER.info("issueCreditNote :: Start for flatId: {}", request != null ? request.getFlatId() : null);

		IssueCreditNoteResponse response = new IssueCreditNoteResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);

		String apartmentId = extractApartmentId(request);
		String flatId = request != null ? request.getFlatId() : null;
		CreditNoteDetails incomingDetails = request != null ? request.getCreditNoteDetails() : null;
		if (incomingDetails == null) {
			LOGGER.warn("issueCreditNote :: Credit note details missing for flatId: {}", flatId);
			response.setMessage(ErrorMessage.ERR_MESSAGE_62);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_62);
			return response;
		}
		incomingDetails.setCreditNoteNo(creditNoteUtiltyService.generateCreditNoteNo(request.getFlatId()));
		String issuedBy = request.getGenericHeader() != null ? request.getGenericHeader().getProfileName() : null;
		Date issueDate = Date.valueOf(LocalDate.now());
		Optional<CreditNoteEntity> existingEntityOpt = creditNoteRepository.findByApartmentIdAndFlatId(apartmentId, flatId);

		CreditNoteEntity entity;
		if (existingEntityOpt.isPresent()) {
			LOGGER.info("issueCreditNote :: Existing record found, appending credit note for flatId: {}", flatId);
			entity = existingEntityOpt.get();
			entity = appendCreditNoteToExisting(entity, incomingDetails, issuedBy, issueDate);
		} else {
			LOGGER.info("issueCreditNote :: No existing record, creating new credit note for flatId: {}", flatId);
			entity = createNewCreditNoteEntity(apartmentId, flatId, incomingDetails, issuedBy, issueDate);
		}

		creditNoteRepository.save(entity);
		LOGGER.info("issueCreditNote :: Credit note saved successfully for flatId: {}", flatId);

		response.setMessage(SuccessMessage.SUCC_MESSAGE_57);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_57);
		return response;
	}

	private CreditNoteEntity appendCreditNoteToExisting(CreditNoteEntity entity, CreditNoteDetails incomingDetails,
			String issuedBy, Date issueDate) {
		LOGGER.info("appendCreditNoteToExisting :: Setting issuedBy={} and issueDate={} on credit note details", issuedBy, issueDate);
		incomingDetails.setCreditNoteIssuedBy(issuedBy);
		incomingDetails.setCreditNoteIssueDate(issueDate);
		List<CreditNoteDetails> detailsList = entity.getCreditNoteDetails();
		if (detailsList == null) {
			detailsList = new ArrayList<>();
		}
		detailsList.add(incomingDetails);
		entity.setCreditNoteDetails(detailsList);

		BigDecimal totalAmount = calculateTotalAmount(detailsList);
		BigDecimal usedAmount = entity.getUsedAmount() != null ? entity.getUsedAmount() : BigDecimal.ZERO;

		entity.setTotalAmount(totalAmount);
		entity.setRemainingAmount(totalAmount.subtract(usedAmount));
		LOGGER.info("appendCreditNoteToExisting :: Updated totalAmount={}, usedAmount={}, remainingAmount={}", totalAmount, usedAmount, entity.getRemainingAmount());
		return entity;
	}

	private CreditNoteEntity createNewCreditNoteEntity(String apartmentId, String flatId,
			CreditNoteDetails incomingDetails, String issuedBy, Date issueDate) {
		LOGGER.info("createNewCreditNoteEntity :: Setting issuedBy={} and issueDate={} on credit note details", issuedBy, issueDate);
		incomingDetails.setCreditNoteIssuedBy(issuedBy);
		incomingDetails.setCreditNoteIssueDate(issueDate);
		CreditNoteEntity entity = new CreditNoteEntity();
		entity.setApartmentId(apartmentId);
		entity.setFlatId(flatId);

		List<CreditNoteDetails> detailsList = new ArrayList<>();
		detailsList.add(incomingDetails);
		entity.setCreditNoteDetails(detailsList);

		BigDecimal totalAmount = incomingDetails.getCreditNoteAmount() != null
				? incomingDetails.getCreditNoteAmount()
				: BigDecimal.ZERO;
		entity.setTotalAmount(totalAmount);
		entity.setUsedAmount(BigDecimal.ZERO);
		entity.setRemainingAmount(totalAmount);
		LOGGER.info("createNewCreditNoteEntity :: New entity created with totalAmount={} for flatId={}", totalAmount, flatId);
		return entity;
	}

	private BigDecimal calculateTotalAmount(List<CreditNoteDetails> detailsList) {
		return detailsList.stream()
				.filter(d -> d.getCreditNoteAmount() != null)
				.map(CreditNoteDetails::getCreditNoteAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private String extractApartmentId(IssueCreditNoteRequest request) {
		if (request != null && request.getGenericHeader() != null) {
			return request.getGenericHeader().getApartmentId();
		}
		return null;
	}

	@Override
	public ValidateCreditNoteResponse validateCreditNote(ValidateCreditNoteRequest request) throws Exception {
		LOGGER.info("validateCreditNote :: Start for flatId: {}", request != null ? request.getFlatId() : null);
		ValidateCreditNoteResponse response = new ValidateCreditNoteResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		// TODO: Implement OTP-based credit note validation logic
		return response;
	}

	@Override
	public ReedemCreditNoteResponse reedemCreditNote(ReedemCreditNoteRequest request) throws Exception {
		LOGGER.info("reedemCreditNote :: Start for flatId: {}", request != null ? request.getFlatId() : null);
		ReedemCreditNoteResponse response = new ReedemCreditNoteResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);

		String apartmentId = request != null && request.getGenericHeader() != null
				? request.getGenericHeader().getApartmentId() : null;
		String flatId = request != null ? request.getFlatId() : null;
		BigDecimal amount = request != null ? request.getAmount() : null;

		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			LOGGER.warn("reedemCreditNote :: Invalid amount {} for flatId: {}", amount, flatId);
			response.setMessage(ErrorMessage.ERR_MESSAGE_62);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_62);
			return response;
		}

		Optional<CreditNoteEntity> entityOpt = creditNoteRepository.findByApartmentIdAndFlatId(apartmentId, flatId);
		if (!entityOpt.isPresent()) {
			LOGGER.warn("reedemCreditNote :: Credit note not found for apartmentId={}, flatId={}", apartmentId, flatId);
			response.setMessage(ErrorMessage.ERR_MESSAGE_61);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_61);
			return response;
		}

		CreditNoteEntity entity = entityOpt.get();
		BigDecimal usedAmount = entity.getUsedAmount() != null ? entity.getUsedAmount() : BigDecimal.ZERO;
		BigDecimal remainingAmount = entity.getRemainingAmount() != null ? entity.getRemainingAmount() : BigDecimal.ZERO;

		BigDecimal newUsedAmount = usedAmount.add(amount);
		BigDecimal newRemainingAmount = remainingAmount.subtract(amount);

		LOGGER.info("reedemCreditNote :: Redeeming amount={} for flatId={}. usedAmount: {} -> {}, remainingAmount: {} -> {}",
				amount, flatId, usedAmount, newUsedAmount, remainingAmount, newRemainingAmount);

		entity.setUsedAmount(newUsedAmount);
		entity.setRemainingAmount(newRemainingAmount);
		creditNoteRepository.save(entity);

		LOGGER.info("reedemCreditNote :: Credit note redeemed and saved for flatId: {}", flatId);
		response.setBalanceAmount(newRemainingAmount);
		response.setMessage(SuccessMessage.SUCC_MESSAGE_63);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_63);
		return response;
	}

	@Override
	public ViewCreditNoteDetailsResponse viewCreditNoteDetails(ViewCreditNoteDetailsRequest request) throws Exception {
		LOGGER.info("viewCreditNoteDetails :: Start for flatId: {}", request != null ? request.getFlatId() : null);
		ViewCreditNoteDetailsResponse response = new ViewCreditNoteDetailsResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);

		String apartmentId = request != null && request.getGenericHeader() != null
				? request.getGenericHeader().getApartmentId() : null;
		String flatId = request != null ? request.getFlatId() : null;
		response.setFlatId(flatId);

		Optional<CreditNoteEntity> entityOpt = creditNoteRepository.findByApartmentIdAndFlatId(apartmentId, flatId);
		if (!entityOpt.isPresent()) {
			LOGGER.warn("viewCreditNoteDetails :: Credit note not found for apartmentId={}, flatId={}", apartmentId, flatId);
			response.setMessage(ErrorMessage.ERR_MESSAGE_61);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_61);
			return response;
		}

		CreditNoteEntity entity = entityOpt.get();
		LOGGER.info("viewCreditNoteDetails :: Credit note found for flatId={}, totalAmount={}, usedAmount={}, remainingAmount={}",
				flatId, entity.getTotalAmount(), entity.getUsedAmount(), entity.getRemainingAmount());

		response.setCreditNoteDetails(entity.getCreditNoteDetails());
		response.setTotalAmount(entity.getTotalAmount());
		response.setUsedAmount(entity.getUsedAmount());
		response.setRemainingAmount(entity.getRemainingAmount());
		response.setMessage(SuccessMessage.SUCC_MESSAGE_58);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_58);
		return response;
	}

	@Override
	public DeleteCreditNoteResponse deleteCreditNote(DeleteCreditNoteRequest request) throws Exception {
		LOGGER.info("deleteCreditNote :: Start for flatId: {}, creditNoteNo: {}",
				request != null ? request.getFlatId() : null,
				request != null ? request.getCreditNoteNo() : null);
		DeleteCreditNoteResponse response = new DeleteCreditNoteResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		// TODO: Implement credit note deletion logic
		return response;
	}

	@Override
	public CreditNoteAvailableResponse creditNoteAvailable(CreditNoteAvailableRequest request) throws Exception {
		LOGGER.info("creditNoteAvailable :: Start for flatId: {}", request != null ? request.getFlatId() : null);
		CreditNoteAvailableResponse response = new CreditNoteAvailableResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);

		String apartmentId = request != null && request.getGenericHeader() != null
				? request.getGenericHeader().getApartmentId() : null;
		String flatId = request != null ? request.getFlatId() : null;

		Optional<CreditNoteEntity> entityOpt = creditNoteRepository.findByApartmentIdAndFlatId(apartmentId, flatId);
		if (entityOpt.isPresent()) {
			CreditNoteEntity entity = entityOpt.get();
			BigDecimal remaining = entity.getRemainingAmount();
			if (remaining != null && remaining.compareTo(BigDecimal.ZERO) > 0) {
				LOGGER.info("creditNoteAvailable :: Credit note available for flatId: {}", flatId);
				response.setMessage(SuccessMessage.SUCC_MESSAGE_60);
				response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_60);
			} else {
				LOGGER.warn("creditNoteAvailable :: No remaining balance for flatId: {}", flatId);
				response.setMessage(ErrorMessage.ERR_MESSAGE_64);
				response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_64);
			}
		} else {
			LOGGER.warn("creditNoteAvailable :: No credit note found for flatId: {}", flatId);
			response.setMessage(ErrorMessage.ERR_MESSAGE_61);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_61);
		}
		return response;
	}
}
