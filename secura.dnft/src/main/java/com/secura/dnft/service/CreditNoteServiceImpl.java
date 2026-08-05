package com.secura.dnft.service;

import java.math.BigDecimal;
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
		Optional<CreditNoteEntity> existingEntityOpt = creditNoteRepository.findByApartmentIdAndFlatId(apartmentId, flatId);

		CreditNoteEntity entity;
		if (existingEntityOpt.isPresent()) {
			LOGGER.info("issueCreditNote :: Existing record found, appending credit note for flatId: {}", flatId);
			entity = existingEntityOpt.get();
			entity = appendCreditNoteToExisting(entity, incomingDetails);
		} else {
			LOGGER.info("issueCreditNote :: No existing record, creating new credit note for flatId: {}", flatId);
			entity = createNewCreditNoteEntity(apartmentId, flatId, incomingDetails);
		}

		creditNoteRepository.save(entity);
		LOGGER.info("issueCreditNote :: Credit note saved successfully for flatId: {}", flatId);

		response.setMessage(SuccessMessage.SUCC_MESSAGE_57);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_57);
		return response;
	}

	private CreditNoteEntity appendCreditNoteToExisting(CreditNoteEntity entity, CreditNoteDetails incomingDetails) {
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
		return entity;
	}

	private CreditNoteEntity createNewCreditNoteEntity(String apartmentId, String flatId,
			CreditNoteDetails incomingDetails) {
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
		// TODO: Implement credit note redemption logic
		return response;
	}

	@Override
	public ViewCreditNoteDetailsResponse viewCreditNoteDetails(ViewCreditNoteDetailsRequest request) throws Exception {
		LOGGER.info("viewCreditNoteDetails :: Start for flatId: {}", request != null ? request.getFlatId() : null);
		ViewCreditNoteDetailsResponse response = new ViewCreditNoteDetailsResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		// TODO: Implement credit note details fetch logic
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
