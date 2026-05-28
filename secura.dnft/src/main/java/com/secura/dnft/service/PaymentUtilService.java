package com.secura.dnft.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.dao.DueAmountDetailsRepository;
import com.secura.dnft.dao.FlatRepository;
import com.secura.dnft.dao.PaymentRepository;
import com.secura.dnft.dao.TransactionRepository;
import com.secura.dnft.entity.DueAmountDetailsEntity;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.entity.Transaction;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.GetPaymentUtilDetailsRequest;
import com.secura.dnft.request.response.GetPaymentUtilDetailsResponse;

@Service
public class PaymentUtilService {

	@Autowired
	PaymentRepository paymentRepository;

	@Autowired
	TransactionRepository transactionRepository;

	@Autowired
	DueAmountDetailsRepository dueAmountDetailsRepository;

	@Autowired
	FlatRepository flatRepository;

	@Autowired
	GenericService genericService;

	public GetPaymentUtilDetailsResponse getPaymentDetails(GetPaymentUtilDetailsRequest request) {
		GetPaymentUtilDetailsResponse response = new GetPaymentUtilDetailsResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		setZeroTotals(response);

		String apartmentId = request != null && request.getGenericHeader() != null
				? trimValue(request.getGenericHeader().getApartmentId())
				: null;
		if (!hasText(apartmentId)) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_05);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_05);
			return response;
		}

		String paymentId = request != null ? trimValue(request.getPaymentId()) : null;
		if (!hasText(paymentId)) {
			response.setMessage(SuccessMessage.SUCC_MESSAGE_38);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_38);
			return response;
		}

		List<PaymentEntity> paymentEntities = paymentRepository.findByPaymentIdAndAprmtId(paymentId, apartmentId);
		if (paymentEntities == null || paymentEntities.isEmpty()) {
			response.setMessage(SuccessMessage.SUCC_MESSAGE_38);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_38);
			return response;
		}

		String flatId = request != null ? trimValue(request.getFlatId()) : null;
		PaymentEntity paymentEntity = selectPaymentEntity(paymentEntities);
		String flatArea = resolveFlatArea(flatId);
		BigDecimal totalCollection = sumTransactions(fetchTransactions(apartmentId, paymentId, flatId,
				SecuraConstants.TRANSACTION_STATUS_SUCCESS));
		BigDecimal totalPending = sumTransactions(fetchTransactions(apartmentId, paymentId, flatId,
				SecuraConstants.TRANSACTION_STATUS_PENDING));
		BigDecimal expectedCollection = hasText(flatId)
				? resolveExpectedCollection(paymentId, paymentEntity != null ? paymentEntity.getPaymentCapita() : null, flatId,
						flatArea)
				: BigDecimal.ZERO;

		response.setPaymentEntity(paymentEntity);
		response.setExpectedCollection(formatAmount(expectedCollection));
		response.setTotalCollection(formatAmount(totalCollection));
		response.setTotalPendingTransactionAmount(formatAmount(totalPending));
		response.setCollectionPercentage(formatPercentage(totalCollection, expectedCollection));
		response.setMessage(SuccessMessage.SUCC_MESSAGE_37);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_37);
		return response;
	}

	private void setZeroTotals(GetPaymentUtilDetailsResponse response) {
		response.setExpectedCollection(BigDecimal.ZERO.toPlainString());
		response.setTotalCollection(BigDecimal.ZERO.toPlainString());
		response.setTotalPendingTransactionAmount(BigDecimal.ZERO.toPlainString());
		response.setCollectionPercentage(BigDecimal.ZERO.toPlainString());
	}

	private List<Transaction> fetchTransactions(String apartmentId, String paymentId, String flatId, String status) {
		if (hasText(flatId)) {
			return transactionRepository.findByAprmntIdAndPymntIdAndFlatIdAndTrnsStatus(apartmentId, paymentId, flatId, status);
		}
		return transactionRepository.findByAprmntIdAndPymntIdAndTrnsStatus(apartmentId, paymentId, status);
	}

	private BigDecimal sumTransactions(List<Transaction> transactions) {
		if (transactions == null || transactions.isEmpty()) {
			return BigDecimal.ZERO;
		}
		return transactions.stream().filter(Objects::nonNull).map(Transaction::getTrnsAmt).map(this::parseBigDecimal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private BigDecimal resolveExpectedCollection(String paymentId, String paymentCapita, String flatId, String flatArea) {
		List<DueAmountDetailsEntity> dues = dueAmountDetailsRepository.findByPaymentId(paymentId);
		if (dues == null || dues.isEmpty()) {
			return BigDecimal.ZERO;
		}
		List<DueAmountDetailsEntity> selectedCycleDues = selectHighestPriorityDues(dues, paymentCapita, flatArea);
		if (selectedCycleDues.isEmpty()) {
			return BigDecimal.ZERO;
		}
		if (isPerSqftCapita(paymentCapita)) {
			return selectedCycleDues.stream().map(this::resolveDueAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		}
		List<DueAmountDetailsEntity> applicableDues = selectedCycleDues.stream().filter(Objects::nonNull)
				.filter(due -> appliesToFlat(due, flatId)).toList();
		if (applicableDues.isEmpty()) {
			return BigDecimal.ZERO;
		}
		return applicableDues.stream().map(this::resolveDueAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private PaymentEntity selectPaymentEntity(List<PaymentEntity> paymentEntities) {
		return paymentEntities.stream().filter(Objects::nonNull)
				.max(Comparator.comparingInt(entity -> resolveCyclePriority(entity.getPaymentCollectionCycle()))).orElse(null);
	}

	private BigDecimal resolveDueAmount(DueAmountDetailsEntity due) {
		BigDecimal totalAmount = parseBigDecimal(due.getTotalAmount());
		if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
			return totalAmount;
		}
		return parseBigDecimal(due.getAmount());
	}

	private List<DueAmountDetailsEntity> selectHighestPriorityDues(List<DueAmountDetailsEntity> dues, String paymentCapita,
			String flatArea) {
		List<DueAmountDetailsEntity> normalizedDues = dues.stream().filter(Objects::nonNull).toList();
		if (normalizedDues.isEmpty()) {
			return List.of();
		}
		List<DueAmountDetailsEntity> duesForSelection = isPerSqftCapita(paymentCapita)
				? normalizedDues.stream().filter(due -> matchesFlatArea(due, flatArea)).toList()
				: normalizedDues;
		if (duesForSelection.isEmpty()) {
			return List.of();
		}
		int highestPriority = duesForSelection.stream().map(DueAmountDetailsEntity::getCollectionCycle)
				.mapToInt(this::resolveCyclePriority).max().orElse(0);
		if (highestPriority <= 0) {
			return List.of();
		}
		return duesForSelection.stream().filter(due -> resolveCyclePriority(due.getCollectionCycle()) == highestPriority).toList();
	}

	private boolean appliesToFlat(DueAmountDetailsEntity due, String flatId) {
		if (due == null || !hasText(flatId)) {
			return false;
		}
		return parseStringList(due.getApplicableFlats()).stream().anyMatch(flat -> flat.equalsIgnoreCase(flatId));
	}

	private String resolveFlatArea(String flatId) {
		if (!hasText(flatId)) {
			return null;
		}
		Optional<Flat> flat = flatRepository.findById(flatId);
		return flat.map(Flat::getFlatArea).map(this::trimValue).orElse(null);
	}

	private boolean isPerSqftCapita(String paymentCapita) {
		return normalizeCapita(paymentCapita).equals("PERSQFT");
	}

	private String normalizeCapita(String paymentCapita) {
		if (!hasText(paymentCapita)) {
			return "";
		}
		return paymentCapita.trim().toUpperCase(Locale.ENGLISH).replaceAll("[\\s_-]", "");
	}

	private boolean matchesFlatArea(DueAmountDetailsEntity due, String flatArea) {
		if (due == null) {
			return false;
		}
		String dueFlatArea = trimValue(due.getFlatArea());
		if (!hasText(dueFlatArea) || "ALL".equalsIgnoreCase(dueFlatArea)) {
			return true;
		}
		if (!hasText(flatArea)) {
			return false;
		}
		return dueFlatArea.equalsIgnoreCase(flatArea.trim());
	}

	private List<String> parseStringList(String value) {
		if (!hasText(value)) {
			return new ArrayList<>();
		}
		try {
			List<String> values = genericService.fromJson(value, new TypeReference<List<String>>() {
			});
			if (values == null) {
				return new ArrayList<>();
			}
			return values.stream().filter(this::hasText).map(this::trimValue).toList();
		} catch (RuntimeException exception) {
			return new ArrayList<>();
		}
	}

	private String formatPercentage(BigDecimal totalCollection, BigDecimal expectedCollection) {
		if (totalCollection == null || expectedCollection == null || expectedCollection.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO.toPlainString();
		}
		BigDecimal percentage = totalCollection.multiply(BigDecimal.valueOf(100))
				.divide(expectedCollection, 2, RoundingMode.HALF_UP);
		return formatAmount(percentage);
	}

	private String formatAmount(BigDecimal amount) {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO.toPlainString();
		}
		return amount.stripTrailingZeros().toPlainString();
	}

	private BigDecimal parseBigDecimal(String value) {
		if (!hasText(value)) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(value.trim());
		} catch (NumberFormatException exception) {
			return BigDecimal.ZERO;
		}
	}

	private int resolveCyclePriority(String cycle) {
		switch (normalizeCycle(cycle)) {
		case "YEARLY":
			return 4;
		case "HALF_YEARLY":
			return 3;
		case "QUARTERLY":
			return 2;
		case "MONTHLY":
			return 1;
		default:
			return 0;
		}
	}

	private String normalizeCycle(String cycle) {
		if (!hasText(cycle)) {
			return "";
		}
		String normalized = cycle.trim().toUpperCase(Locale.ENGLISH).replace('-', '_').replace(' ', '_');
		if ("HALFYEARLY".equals(normalized)) {
			return "HALF_YEARLY";
		}
		if ("QUATERLY".equals(normalized)) {
			return "QUARTERLY";
		}
		if ("MONTLY".equals(normalized)) {
			return "MONTHLY";
		}
		return normalized;
	}

	private boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	private String trimValue(String value) {
		return hasText(value) ? value.trim() : null;
	}
}
