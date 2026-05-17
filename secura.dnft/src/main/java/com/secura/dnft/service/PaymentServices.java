package com.secura.dnft.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.dao.DocumentRepository;
import com.secura.dnft.dao.DueAmountDetailsRepository;
import com.secura.dnft.dao.FlatRepository;
import com.secura.dnft.dao.PaymentRepository;
import com.secura.dnft.dao.TransactionRepository;
import com.secura.dnft.entity.DocumentEntity;
import com.secura.dnft.entity.DueAmountDetailsEntity;
import com.secura.dnft.entity.DueAmountDetailsEntityId;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.entity.Transaction;
import com.secura.dnft.entity.Worklist;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.interfaceservice.PaymentInterface;
import com.secura.dnft.request.response.AddedCharges;
import com.secura.dnft.request.response.BankInstrumentTenderDetails;
import com.secura.dnft.request.response.CreateReceiptRequest;
import com.secura.dnft.request.response.CreateReceiptResponse;
import com.secura.dnft.request.response.CreatePaymentRequest;
import com.secura.dnft.request.response.CreatePaymentResponse;
import com.secura.dnft.request.response.DiscFinReceipt;
import com.secura.dnft.request.response.DueAmountDetails;
import com.secura.dnft.request.response.DuePaymentAmountDetailsRequest;
import com.secura.dnft.request.response.DuePaymentAmountDetailsResponse;
import com.secura.dnft.request.response.GetDuePaymentAmountDetailsResponse;
import com.secura.dnft.request.response.Items;
import com.secura.dnft.request.response.GetPaymentRequest;
import com.secura.dnft.request.response.GetPaymentResponse;
import com.secura.dnft.request.response.LedgerEntryRequest;
import com.secura.dnft.request.response.LedgerEntryResponse;
import com.secura.dnft.request.response.PayDueRequest;
import com.secura.dnft.request.response.PayDueResponse;
import com.secura.dnft.request.response.PaymentTenderData;
import com.secura.dnft.request.response.UpdatePaymentRequest;
import com.secura.dnft.request.response.UpdatePaymentResponse;

import jakarta.persistence.EntityNotFoundException;

@Service
public class PaymentServices implements PaymentInterface {

	// Synthetic id used only for non-persisting due preview calculation.
	private static final String DUE_PREVIEW_PAYMENT_ID = "DUE_PREVIEW";

	@Autowired
	GenericService genericService;

	@Autowired
	PaymentRepository paymentRepository;

	@Autowired
	TransactionRepository transactionRepository;

	@Autowired
	DocumentRepository documentRepository;

	@Autowired
	ReceiptServices receiptServices;

	@Autowired
	DueDetailsService dueDetailsService;

	@Autowired
	FlatRepository flatRepository;

	@Autowired
	DueAmountDetailsRepository dueAmountDetailsRepository;

	@Override
	public DuePaymentAmountDetailsResponse getDuePaymentAmountDetails(DuePaymentAmountDetailsRequest request) {
		DuePaymentAmountDetailsResponse response = new DuePaymentAmountDetailsResponse();
		response.setGenericHeader(request.getGenericHeader());
		response.setPaymentCapita(request.getPaymentCapita());

		if (request.getCollectionStartDate() == null || request.getCollectionEndDate() == null
				|| request.getTodayDate() == null) {
			return response;
		}

		BigDecimal cycleAmount = resolveCycleAmount(request.getPaymentAmount(), request.getPaymentCapita());
		BigDecimal gstPercent = parseNumeric(request.getGst());
		BigDecimal dueBaseAmount;
		if (isOnceCycle(request.getPaymentCollectionCycle())) {
			response.setDueDate(isPost(request.getPaymentCollectionMode()) ? request.getCollectionEndDate().plusDays(1)
					: request.getCollectionStartDate());
			dueBaseAmount = cycleAmount.setScale(2, RoundingMode.HALF_UP);
		} else {
			int cycleMonths = getCycleMonths(request.getPaymentCollectionCycle());
			if (cycleMonths <= 0) {
				return response;
			}
			DueWindow dueWindow = calculateDueWindow(request.getCollectionStartDate(), request.getCollectionEndDate(),
					request.getTodayDate(), request.getPaymentCollectionMode(), cycleMonths);
			response.setDueDate(dueWindow.getDueDate());
			dueBaseAmount = calculateDueBaseAmount(dueWindow.getChargePeriodStart(), cycleMonths, request.getCollectionEndDate(),
					cycleAmount);
		}
		dueBaseAmount = roundAmountByThreshold(dueBaseAmount);
		BigDecimal gstAmount = dueBaseAmount.multiply(gstPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		BigDecimal totalWithGst = roundAmountByThreshold(dueBaseAmount.add(gstAmount));

		response.setAmountExcludingGst(formatNumber(dueBaseAmount));
		response.setGstPercent(formatNumber(gstPercent));
		response.setGstAmount(formatNumber(gstAmount));
		response.setAmountIncludingGst(formatNumber(totalWithGst));
		response.setMessage(SuccessMessage.SUCC_MESSAGE_28);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_28);

		return response;
	}
	
	@Override
	public GetDuePaymentAmountDetailsResponse getDuePaymentAmountDetails(CreatePaymentRequest request) {
		GetDuePaymentAmountDetailsResponse response = new GetDuePaymentAmountDetailsResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		Map<String, DuePaymentAmountDetailsResponse> duePaymentAmountDetailsMap = new LinkedHashMap<>();
		List<String> paymentCollectionCycles = resolvePaymentCollectionCycles(request);
		LocalDate collectionStartDate = request != null && request.getCollectionStartDate() != null
				? request.getCollectionStartDate().toLocalDate()
				: null;
		LocalDate collectionEndDate = request != null && request.getCollectionEndDate() != null
				? request.getCollectionEndDate().toLocalDate()
				: null;
		LocalDate todayDate = request != null ? request.getTodayDate() : null;
		if (todayDate == null) {
			todayDate = LocalDate.now();
		}
		Map<String, List<Map<String, DueAmountDetails>>> dueAmountDetailsEntityMap = new LinkedHashMap<>();
		if (collectionStartDate != null && collectionEndDate != null) {
			dueAmountDetailsEntityMap = dueDetailsService.previewDuesForPayment(
					buildPreviewPaymentEntities(request, paymentCollectionCycles), request != null ? request.getGenericHeader() : null);
		}
		for (String cycle : paymentCollectionCycles) {
			DuePaymentAmountDetailsRequest dueRequest = new DuePaymentAmountDetailsRequest();
			dueRequest.setGenericHeader(request != null ? request.getGenericHeader() : null);
			dueRequest.setPaymentAmount(request != null ? request.getPaymentAmount() : null);
			dueRequest.setGst(request != null ? request.getGst() : null);
			dueRequest.setCollectionStartDate(collectionStartDate);
			dueRequest.setCollectionEndDate(collectionEndDate);
			dueRequest.setPaymentCollectionCycle(cycle);
			dueRequest.setPaymentCollectionMode(request != null ? request.getPaymentCollectionMode() : null);
			dueRequest.setPaymentCapita(request != null ? request.getPaymentCapita() : null);
			dueRequest.setTodayDate(todayDate);
			duePaymentAmountDetailsMap.put(cycle, getDuePaymentAmountDetails(dueRequest));
		}
		response.setDuePaymentAmountDetailsMap(duePaymentAmountDetailsMap);
		response.setDueAmountDetailsEntityMap(dueAmountDetailsEntityMap == null ? new LinkedHashMap<>() : dueAmountDetailsEntityMap);
		response.setMessage(SuccessMessage.SUCC_MESSAGE_28);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_28);
		return response;
	}

	private List<PaymentEntity> buildPreviewPaymentEntities(CreatePaymentRequest request, List<String> paymentCollectionCycles) {
		if (request == null || paymentCollectionCycles == null || paymentCollectionCycles.isEmpty()) {
			return List.of();
		}
		LocalDate collectionStartDate = request.getCollectionStartDate() != null ? request.getCollectionStartDate().toLocalDate()
				: null;
		LocalDate collectionEndDate = request.getCollectionEndDate() != null ? request.getCollectionEndDate().toLocalDate() : null;
		String allowedPaymentModes = serializeAllowedPaymentModes(request.getAllowedPaymentModes());
		String addedCharges = serializeAddedCharges(request.getAddedCharges());
		String discFin = serializeDiscFin(request);
		String apartmentId = request.getGenericHeader() != null ? request.getGenericHeader().getApartmentId() : null;
		List<PaymentEntity> paymentEntityList = new ArrayList<>();
		for (String paymentCollectionCycle : paymentCollectionCycles) {
			PaymentEntity entity = new PaymentEntity();
			entity.setPaymentId(DUE_PREVIEW_PAYMENT_ID);
			entity.setPaymentName(request.getPaymentName());
			entity.setPaymentCapita(request.getPaymentCapita());
			entity.setPaymentAmount(request.getPaymentAmount());
			entity.setGst(request.getGst());
			entity.setCollectionStartDate(collectionStartDate);
			entity.setCollectionEndDate(collectionEndDate);
			entity.setPaymentCollectionCycle(paymentCollectionCycle);
			entity.setPaymentCollectionMode(request.getPaymentCollectionMode());
			entity.setAllowedPaymentModes(allowedPaymentModes);
			entity.setAddedCharges(addedCharges);
			entity.setDiscFin(discFin);
			entity.setPaymentType(request.getPaymentType());
			entity.setAprmtId(apartmentId);
			entity.setCauseId(request.getCause());
			paymentEntityList.add(entity);
		}
		return paymentEntityList;
	}

	private Set<String> parseApplicableFlatNos(List<String> applicableFor) {
		Set<String> flatNos = new LinkedHashSet<>();
		for (String normalizedValue : normalizeApplicableFlatNos(applicableFor)) {
			if ("ALL".equalsIgnoreCase(normalizedValue)) {
				return new LinkedHashSet<>();
			}
			flatNos.add(normalizedValue);
		}
		return flatNos;
	}

	private boolean isApplicableForAll(List<String> applicableFor) {
		for (String value : normalizeApplicableFlatNos(applicableFor)) {
			if ("ALL".equalsIgnoreCase(value)) {
				return true;
			}
		}
		return false;
	}

	private List<String> normalizeApplicableFlatNos(List<String> applicableFor) {
		if (applicableFor == null || applicableFor.isEmpty()) {
			return List.of();
		}
		return applicableFor.stream().filter(value -> value != null).flatMap(value -> Arrays.stream(value.split(",")))
				.map(String::trim)
				.filter(value -> !value.isBlank())
				.collect(Collectors.toList());
	}

	private String normalizeFlatNoForMatch(String flatNo) {
		if (flatNo == null) {
			return null;
		}
		String normalized = flatNo.trim();
		return normalized.isBlank() ? null : normalized.toUpperCase();
	}

	private String serializeApplicableFor(List<String> applicableFor) {
		if (isApplicableForAll(applicableFor)) {
			return "ALL";
		}
		Set<String> normalizedFlatNos = parseApplicableFlatNos(applicableFor);
		if (normalizedFlatNos.isEmpty()) {
			return null;
		}
		return genericService.toJson(new ArrayList<>(normalizedFlatNos));
	}

	private DueWindow calculateDueWindow(LocalDate start, LocalDate end, LocalDate today, String mode,
			int cycleMonths) {
		LocalDate periodStart = start;
		LocalDate lastPeriodStart = start;
		while (!periodStart.isAfter(end)) {
			lastPeriodStart = periodStart;
			LocalDate naturalPeriodEnd = periodStart.plusMonths(cycleMonths).minusDays(1);
			LocalDate periodEnd = naturalPeriodEnd.isAfter(end) ? end : naturalPeriodEnd;
			if (!today.isBefore(periodStart) && !today.isAfter(periodEnd)) {
				if (isPost(mode)) {
					return new DueWindow(periodEnd.plusDays(1), periodStart);
				}
				return new DueWindow(periodStart, periodStart);
			}
			periodStart = periodStart.plusMonths(cycleMonths);
		}
		if (isPost(mode)) {
			if (today.isAfter(end)) {
				return new DueWindow(end.plusDays(1), lastPeriodStart);
			}
			return new DueWindow(start.plusMonths(cycleMonths), start);
		}
		return new DueWindow(start, start);
	}

	private BigDecimal calculateDueBaseAmount(LocalDate dueDate, int cycleMonths, LocalDate collectionEndDate,
			BigDecimal cycleAmount) {
		LocalDate naturalCycleEnd = dueDate.plusMonths(cycleMonths).minusDays(1);
		if (!naturalCycleEnd.isAfter(collectionEndDate)) {
			return cycleAmount.setScale(2, RoundingMode.HALF_UP);
		}

		long totalCycleDays = ChronoUnit.DAYS.between(dueDate, naturalCycleEnd.plusDays(1));
		long activeCycleDays = ChronoUnit.DAYS.between(dueDate, collectionEndDate.plusDays(1));
		if (activeCycleDays < 0) {
			activeCycleDays = 0;
		}
		if (activeCycleDays > totalCycleDays) {
			activeCycleDays = totalCycleDays;
		}
		if (totalCycleDays == 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}

		return cycleAmount.multiply(BigDecimal.valueOf(activeCycleDays)).divide(BigDecimal.valueOf(totalCycleDays), 2,
				RoundingMode.HALF_UP);
	}

	private BigDecimal roundAmountByThreshold(BigDecimal amount) {
		if (amount == null) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		BigDecimal normalized = amount.stripTrailingZeros();
		BigDecimal decimalPart = normalized.remainder(BigDecimal.ONE).abs();
		RoundingMode roundingMode = decimalPart.compareTo(BigDecimal.valueOf(0.5)) > 0 ? RoundingMode.UP
				: RoundingMode.DOWN;
		return normalized.setScale(0, roundingMode).setScale(2, RoundingMode.HALF_UP);
	}

	private int getCycleMonths(String cycle) {
		if (cycle == null) {
			return 0;
		}
		String normalized = cycle.trim().toLowerCase();
		if (normalized.equals("monthly")) {
			return 1;
		}
		if (normalized.equals("quarterly") || normalized.equals("quaterly")) {
			return 3;
		}
		if (normalized.equals("halfyearly") || normalized.equals("half yearly")) {
			return 6;
		}
		if (normalized.equals("yearly")) {
			return 12;
		}
		return 0;
	}

	private boolean isOnceCycle(String cycle) {
		return cycle != null && cycle.trim().equalsIgnoreCase("once");
	}

	private boolean isPost(String mode) {
		return mode != null && mode.trim().equalsIgnoreCase("post");
	}

	private BigDecimal parseNumeric(String input) {
		if (input == null || input.trim().isEmpty()) {
			return BigDecimal.ZERO;
		}
		String normalized = input.replace("%", "").replace(",", "").trim();
		try {
			return new BigDecimal(normalized);
		} catch (NumberFormatException e) {
			return BigDecimal.ZERO;
		}
	}

	private BigDecimal resolveCycleAmount(String paymentAmount, String paymentCapita) {
		BigDecimal amount = parseNumeric(paymentAmount);
		if (amount.compareTo(BigDecimal.ZERO) > 0) {
			return amount;
		}
		return parseNumeric(paymentCapita);
	}

	private String formatNumber(BigDecimal value) {
		BigDecimal normalized = value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
		return normalized.toPlainString();
	}

	private static final class DueWindow {
		private final LocalDate dueDate;
		private final LocalDate chargePeriodStart;

		private DueWindow(LocalDate dueDate, LocalDate chargePeriodStart) {
			this.dueDate = dueDate;
			this.chargePeriodStart = chargePeriodStart;
		}

		private LocalDate getDueDate() {
			return dueDate;
		}

		private LocalDate getChargePeriodStart() {
			return chargePeriodStart;
		}
	}

	@Override
	public UpdatePaymentResponse updatePayment(UpdatePaymentRequest request) throws Exception {
		UpdatePaymentResponse response = new UpdatePaymentResponse();
		response.setGenericHeader(request.getGenericHeader());
		return response;
	}

	@Override
	public GetPaymentResponse getPayments(GetPaymentRequest request) throws Exception {
		GetPaymentResponse response = new GetPaymentResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		String apartmentId = request != null && request.getGenericHeader() != null
				? request.getGenericHeader().getApartmentId()
				: null;
		if (apartmentId == null || apartmentId.isBlank()) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_05);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_05);
			response.setPaymentList(new ArrayList<>());
			return response;
		}
		List<PaymentEntity> paymentList = new ArrayList<>();
		if (request != null && request.getPaymentId() != null && !request.getPaymentId().isBlank()) {
			paymentList = paymentRepository.findByPaymentIdAndAprmtId(request.getPaymentId(), apartmentId);
		} else {
			paymentList = paymentRepository.findByAprmtId(apartmentId);
		}
		response.setPaymentList(paymentList);
		if (paymentList.isEmpty()) {
			response.setMessage(SuccessMessage.SUCC_MESSAGE_38);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_38);
		} else {
			response.setMessage(SuccessMessage.SUCC_MESSAGE_37);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_37);
		}
		return response;
	}

	@Override
	public CreatePaymentResponse createPayment(CreatePaymentRequest request) throws Exception {
		CreatePaymentResponse response = new CreatePaymentResponse();
		response.setGenericHeader(request.getGenericHeader());
		String paymentId = getPaymentId(request.getPaymentType());
		List<String> paymentCollectionCycles = resolvePaymentCollectionCycles(request);
		LocalDate collectionStartDate = request.getCollectionStartDate() != null ? request.getCollectionStartDate().toLocalDate() : null;
		LocalDate collectionEndDate = request.getCollectionEndDate() != null ? request.getCollectionEndDate().toLocalDate() : null;
		String applicableFor = serializeApplicableFor(request.getApplicableFor());
		String allowedPaymentModes = serializeAllowedPaymentModes(request.getAllowedPaymentModes());
		String addedCharges = serializeAddedCharges(request.getAddedCharges());
		String discFin = serializeDiscFin(request);
		for (String paymentCollectionCycle : paymentCollectionCycles) {
			PaymentEntity entity = new PaymentEntity();
			entity.setPaymentId(paymentId);
			entity.setPaymentName(request.getPaymentName());
			entity.setShortDetails(request.getShortDetails());
			entity.setPaymentCapita(request.getPaymentCapita());
			entity.setPaymentAmount(request.getPaymentAmount());
			entity.setGst(request.getGst());
			entity.setCurrency(SecuraConstants.PAYMENT_CURRENCY);
			entity.setCollectionStartDate(collectionStartDate);
			entity.setCollectionEndDate(collectionEndDate);
			entity.setPaymentCollectionCycle(paymentCollectionCycle);
			entity.setPaymentCollectionMode(request.getPaymentCollectionMode());
			entity.setApplicableFor(applicableFor);
			entity.setAllowedPaymentModes(allowedPaymentModes);
			entity.setAddedCharges(addedCharges);
			entity.setDiscFin(discFin);
			entity.setPaymentType(request.getPaymentType());
			entity.setBankAccountId(request.getBankAccountId());
			entity.setAprmtId(request.getGenericHeader() != null ? request.getGenericHeader().getApartmentId() : null);
			entity.setStatus(SecuraConstants.PAYMENT_STATUS_ACTIVE);
			entity.setCreatUsrId(request.getGenericHeader() != null ? request.getGenericHeader().getUserId() : null);
			entity.setCauseId(request.getCause());
			entity.setPartialPaymentAllowed(request != null && request.isPartialPaymentAllowed());
			paymentRepository.save(entity);
		}
		dueDetailsService.calculateDuesForPayment(paymentId, request.getGenericHeader());
		response.setMessage(SuccessMessage.SUCC_MESSAGE_23);
		response.setMessage_code(SuccessMessageCode.SUCC_MESSAGE_23);
		return response;
	}

	@Override
	public PayDueResponse payDues(PayDueRequest request) throws Exception {
		PayDueResponse response = new PayDueResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		String flatId = request != null && request.getGenericHeader() != null ? request.getGenericHeader().getFlatNo() : null;
		String flatArea = resolveFlatArea(flatId);
		Transaction transaction = buildTransaction(request, flatArea);
		if (SecuraConstants.TRANSACTION_STATUS_SUCCESS.equalsIgnoreCase(transaction.getTrnsStatus())) {
			DueAmountDetailsEntity dueEntity = resolveDueAmountDetailsEntity(request, flatArea);
			CreateReceiptResponse receiptResponse = receiptServices
					.createReceipt(buildReceiptRequest(request, transaction.getTrnscId(), dueEntity));
			response.setReceipt(receiptResponse != null ? receiptResponse.getReceipt() : null);
			response.setReceiptNumber(receiptResponse != null ? receiptResponse.getReceiptNumber() : null);
			transaction.setReceiptNumber(receiptResponse != null ? receiptResponse.getReceiptNumber() : null);
		}
		transactionRepository.save(transaction);
		response.setMessage(SuccessMessage.SUCC_MESSAGE_33);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_33);
		response.setTransactionId(transaction.getTrnscId());
		response.setReceiptNumber(transaction.getReceiptNumber());
		return response;
	}

	@Override
	public LedgerEntryResponse ledgerEntry(LedgerEntryRequest request) throws Exception {
		LedgerEntryResponse response = new LedgerEntryResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		List<PaymentTenderData> paymentTenderDataList = normalizePaymentTenderDataList(
				request != null ? request.getTrnsTenderList() : null);
		if (paymentTenderDataList.isEmpty()) {
			throw new IllegalArgumentException("At least one tender is required");
		}
		LocalDateTime currentTimestamp = LocalDateTime.now();
		List<String> documentIdList = saveLedgerDocuments(request, currentTimestamp);
		LocalDateTime transactionDate = resolveLedgerTransactionDate(request, currentTimestamp);
		Transaction transaction = buildLedgerTransaction(request, paymentTenderDataList, transactionDate, currentTimestamp,
				documentIdList);
		if (shouldCreateLedgerReceipt(request)) {
			CreateReceiptResponse receiptResponse = receiptServices
					.createReceipt(buildLedgerReceiptRequest(request, transaction.getTrnscId(), paymentTenderDataList));
			transaction.setReceiptNumber(receiptResponse != null ? receiptResponse.getReceiptNumber() : null);
			transactionRepository.save(transaction);
			response.setReceipt(receiptResponse != null ? receiptResponse.getReceipt() : null);
		} else {
			transactionRepository.save(transaction);
		}
		response.setMessage(SuccessMessage.SUCC_MESSAGE_40);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_40);
		return response;
	}

	public String getPaymentId(String paymentType) {
		StringBuffer paymentId = new StringBuffer();
		paymentId.append(SecuraConstants.PAYMENT_ID_PREFIX);
		paymentId.append(paymentType);
		paymentId.append(1000 + ThreadLocalRandom.current().nextInt(9000));
		return paymentId.toString().toUpperCase();
	}

	private String serializeAddedCharges(List<AddedCharges> addedCharges) {
		if (addedCharges == null || addedCharges.isEmpty()) {
			return null;
		}
		return genericService.toJson(addedCharges);
	}

	private String serializeAllowedPaymentModes(List<String> allowedPaymentModes) {
		if (allowedPaymentModes == null || allowedPaymentModes.isEmpty()) {
			return null;
		}
		List<String> normalizedModes = allowedPaymentModes.stream()
				.filter(mode -> mode != null)
				.map(String::trim)
				.filter(mode -> !mode.isEmpty())
				.collect(Collectors.toList());
		if (normalizedModes.isEmpty()) {
			return null;
		}
		return genericService.toJson(normalizedModes);
	}

	private String serializeDiscFin(CreatePaymentRequest request) {
		if (request == null) {
			return null;
		}
		List<Map<String, String>> discFin = new ArrayList<>();
		if (hasText(request.getDiscountCode())) {
			Map<String, String> discount = new LinkedHashMap<>();
			discount.put("DISTFIN_TYPE", "DISCOUNT");
			discount.put("code", request.getDiscountCode());
			discount.put("Status", SecuraConstants.DISC_FIN_STATUS_ACTIVE);
			discFin.add(discount);
		}
		if (hasText(request.getFineCode())) {
			Map<String, String> fine = new LinkedHashMap<>();
			fine.put("DISTFIN_TYPE", "FINE");
			fine.put("code", request.getFineCode());
			fine.put("Status", SecuraConstants.DISC_FIN_STATUS_ACTIVE);
			discFin.add(fine);
		}
		if (discFin.isEmpty()) {
			return null;
		}
		return genericService.toJson(discFin);
	}

	private String normalizePaymentCollectionCycle(String paymentCollectionCycle) {
		if (paymentCollectionCycle == null || paymentCollectionCycle.isBlank()) {
			return paymentCollectionCycle;
		}
		String normalized = paymentCollectionCycle.toLowerCase().replaceAll("[\\s_-]", "");
		if ("monthly".equals(normalized)) {
			return SecuraConstants.PAYMENT_CYCLE_MONTHLY;
		}
		if ("quarterly".equals(normalized) || "quaterly".equals(normalized)) {
			return SecuraConstants.PAYMENT_CYCLE_QUATERLY;
		}
		if ("halfyearly".equals(normalized)) {
			return SecuraConstants.PAYMENT_CYCLE_HALF_YEARLY;
		}
		if ("yearly".equals(normalized)) {
			return SecuraConstants.PAYMENT_CYCLE_YEARLY;
		}
		if ("once".equals(normalized)) {
			return SecuraConstants.PAYMENT_CYCLE_ONCE;
		}
		return paymentCollectionCycle.toUpperCase().trim();
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private List<String> resolvePaymentCollectionCycles(CreatePaymentRequest request) {
		List<String> paymentCollectionCycleList = request != null ? request.getPaymentCollectionCycleList() : null;
		if (paymentCollectionCycleList == null || paymentCollectionCycleList.isEmpty()) {
			String paymentCollectionCycle = normalizePaymentCollectionCycle(request != null ? request.getPaymentCollectionCycle() : null);
			return paymentCollectionCycle == null ? List.of() : List.of(paymentCollectionCycle);
		}
		return paymentCollectionCycleList.stream().map(this::normalizePaymentCollectionCycle).filter(Objects::nonNull).toList();
	}

	private Transaction buildTransaction(PayDueRequest request, String flatArea) {
		PaymentEntity paymentEntity = paymentRepository.findFirstByPaymentId(request.getPaymentId())
				.orElseThrow(() -> new EntityNotFoundException(ErrorMessage.ERR_MESSAGE_33));
		LocalDateTime currentTimestamp = LocalDateTime.now();
		String dueId = request.getDueId();
		String paymentCycle = request.getPaymentCycle();
		LocalDate dueDate = request.getDueDate();
		List<PaymentTenderData> paymentTenderDataList = buildPaymentTenderDataList(request);
		String primaryTender = resolvePrimaryTender(paymentTenderDataList);
		Transaction transaction = new Transaction();
		transaction.setAprmntId(request.getGenericHeader() != null ? request.getGenericHeader().getApartmentId() : null);
		transaction.setTrnscId(createTransactionId(primaryTender, request.getAmount(), request.getPaymentId(),
				currentTimestamp.toLocalDate()));
		transaction.setTrnsDate(currentTimestamp);
		transaction.setTrnsBy(request.getGenericHeader() != null ? request.getGenericHeader().getUserId() : null);
		transaction.setTrnsTender(paymentTenderDataList == null ? null : genericService.toJson(paymentTenderDataList));
		transaction.setTrnsType(SecuraConstants.TRANSACTION_TYPE_CREDIT);
		transaction.setTrnsShrtDesc("");
		transaction.setTrnsFiles(genericService.toJson(request.getFiles() != null ? request.getFiles() : List.of()));
		transaction.setTrnsBnkAccnt(paymentEntity.getBankAccountId());
		transaction.setTrnsAmt(request.getAmount());
		transaction.setTrnsCurrency(SecuraConstants.PAYMENT_CURRENCY);
		transaction.setPymntId(request.getPaymentId());
		transaction.setTrnsStatus(resolveTransactionStatus(primaryTender, request.getTransactionStatus()));
		transaction.setNoOfPerson(request.getNoOfPersons());
		transaction.setThirdPartyTrnsRef(request.getThirdPartyTransactionId());
		transaction.setThirdPartyName(SecuraConstants.TRANSACTION_THIRD_PARTY_RAZOR_PAY);
		transaction.setDueDetails(createFlatPendingDueId(dueId, paymentCycle, flatArea, dueDate, request.getPaymentId()));
		transaction.setCause(resolveTransactionCause(paymentEntity));
		transaction.setBankInstrumentTenderDetails(serializeBankInstrumentTenderDetails(
				request.getBankInstrumentTenderDetails()));
		transaction.setCreatTs(currentTimestamp);
		transaction.setCreatUsrId(request.getGenericHeader() != null ? request.getGenericHeader().getUserId() : null);
		transaction.setFlatId(request.getGenericHeader() != null ? request.getGenericHeader().getFlatNo() : null);
		transaction.setLstUpdtTs(null);
		transaction.setLstUpdtUsrId(null);
		if (requiresWorklist(paymentTenderDataList)) {
			Worklist worklist = genericService.createWorklist(SecuraConstants.WORKLIST_TYPE_TRANSACTION,
					request.getGenericHeader() != null ? request.getGenericHeader().getUserId() : null,
					request.getGenericHeader() != null ? request.getGenericHeader().getApartmentId() : null,
					transaction.getTrnscId());
			genericService.createWorklistAssignmentFlow(worklist.getWorklistTaskId(), List.of("admin"));
			transaction.setWorkListId(worklist.getWorklistTaskId());
		}
		return transaction;
	}

	private List<PaymentTenderData> normalizePaymentTenderDataList(List<PaymentTenderData> paymentTenderDataList) {
		if (paymentTenderDataList == null || paymentTenderDataList.isEmpty()) {
			return List.of();
		}
		return paymentTenderDataList.stream().filter(tender -> tender != null && hasText(tender.getTenderName()))
				.map(tender -> {
					PaymentTenderData data = new PaymentTenderData();
					data.setTenderName(trimValue(tender.getTenderName()));
					data.setAmountPaid(trimValue(tender.getAmountPaid()));
					return data;
				})
				.collect(Collectors.toList());
	}

	private List<String> saveLedgerDocuments(LedgerEntryRequest request, LocalDateTime currentTimestamp) {
		if (request == null || request.getSupportedFileList() == null || request.getSupportedFileList().isEmpty()) {
			return null;
		}
		List<DocumentEntity> documentList = request.getSupportedFileList().stream().filter(document -> document != null)
				.map(document -> buildLedgerDocument(request, document, currentTimestamp)).collect(Collectors.toList());
		if (documentList.isEmpty()) {
			return null;
		}
		documentRepository.saveAll(documentList);
		return documentList.stream().map(DocumentEntity::getDocumentId).collect(Collectors.toList());
	}

	private DocumentEntity buildLedgerDocument(LedgerEntryRequest request, DocumentEntity document,
			LocalDateTime currentTimestamp) {
		DocumentEntity documentEntity = new DocumentEntity();
		documentEntity
				.setAprmtId(request != null && request.getGenericHeader() != null ? request.getGenericHeader().getApartmentId() : null);
		String documentType = resolveLedgerDocumentType(document);
		documentEntity.setDocumentId(genericService.createDocumentId(documentType, SecuraConstants.LEDGER_DOC_FOR));
		documentEntity.setDocumentType(documentType);
		documentEntity.setDocumentName(document != null ? document.getDocumentName() : null);
		documentEntity.setDocumentData(document != null ? document.getDocumentData() : null);
		documentEntity.setCreatTs(currentTimestamp);
		documentEntity
				.setCreatUsrId(request != null && request.getGenericHeader() != null ? request.getGenericHeader().getUserId() : null);
		documentEntity.setLstUpdtTs(null);
		documentEntity.setLstUpdtUsrId(null);
		return documentEntity;
	}

	private String resolveLedgerDocumentType(DocumentEntity document) {
		String documentType = trimValue(document != null ? document.getDocumentType() : null);
		return hasText(documentType) ? documentType : "DOC";
	}

	private LocalDateTime resolveLedgerTransactionDate(LedgerEntryRequest request, LocalDateTime currentTimestamp) {
		if (request != null && request.getTrnsDate() != null) {
			return request.getTrnsDate().toLocalDate().atStartOfDay();
		}
		return currentTimestamp;
	}

	private Transaction buildLedgerTransaction(LedgerEntryRequest request, List<PaymentTenderData> paymentTenderDataList,
			LocalDateTime transactionDate, LocalDateTime currentTimestamp, List<String> documentIdList) {
		Transaction transaction = new Transaction();
		transaction.setAprmntId(request != null && request.getGenericHeader() != null ? request.getGenericHeader().getApartmentId() : null);
		transaction.setTrnscId(createTransactionId(resolvePrimaryTender(paymentTenderDataList),
				request != null ? request.getTrnsAmt() : null,
				request != null ? request.getLedgerfor() : null, transactionDate.toLocalDate()));
		transaction.setTrnsDate(transactionDate);
		transaction.setTrnsBy(request != null && request.getGenericHeader() != null ? request.getGenericHeader().getUserId() : null);
		transaction.setTrnsTender(genericService.toJson(paymentTenderDataList));
		transaction.setTrnsType(trimValue(request != null ? request.getTrnsType() : null));
		transaction.setTrnsShrtDesc(request != null ? request.getTrnsShrtDesc() : null);
		transaction.setTrnsFiles(genericService.toJson(documentIdList));
		transaction.setTrnsBnkAccnt(request != null ? request.getTrnsBnkAccnt() : null);
		transaction.setTrnsAmt(request != null ? request.getTrnsAmt() : null);
		transaction.setTrnsCurrency(SecuraConstants.PAYMENT_CURRENCY);
		transaction.setTrnsStatus(request != null ? request.getTrnsStatus() : null);
		transaction.setCause(request != null ? request.getCause() : null);
		transaction.setBankInstrumentTenderDetails(serializeBankInstrumentTenderDetails(
				request != null ? request.getBankInstrumentTenderDetails() : null));
		if (request != null && request.getFlatId() != null) {
			transaction.setFlatId(request.getFlatId());
		}
		transaction.setCreatTs(currentTimestamp);
		transaction.setCreatUsrId(request != null && request.getGenericHeader() != null ? request.getGenericHeader().getUserId() : null);
		return transaction;
	}

	private String serializeBankInstrumentTenderDetails(List<BankInstrumentTenderDetails> bankInstrumentTenderDetails) {
		if (bankInstrumentTenderDetails == null || bankInstrumentTenderDetails.isEmpty()) {
			return null;
		}
		return genericService.toJson(bankInstrumentTenderDetails);
	}

	private boolean shouldCreateLedgerReceipt(LedgerEntryRequest request) {
		return request != null && request.isRequiredReceiptFlag()
				&& SecuraConstants.TRANSACTION_TYPE_CREDIT.equalsIgnoreCase(trimValue(request.getTrnsType()));
	}

	private CreateReceiptRequest buildLedgerReceiptRequest(LedgerEntryRequest request, String transactionId,
			List<PaymentTenderData> paymentTenderDataList) {
		CreateReceiptRequest receiptRequest = new CreateReceiptRequest();
		receiptRequest.setGenericHeader(request != null ? request.getGenericHeader() : null);
		receiptRequest.setItems(List.of(buildLedgerReceiptItem(request)));
		receiptRequest.setAddedCharges(List.of());
		receiptRequest.setDiscFinReceipt(null);
		receiptRequest.setReceiptType("Ledger Entry");
		receiptRequest.setPerheadFlag(false);
		receiptRequest.setRemarks(null);
		receiptRequest.setUnitPriceRequired(false);
		receiptRequest.setTotalAmount(request != null ? request.getTrnsAmt() : null);
		receiptRequest.setTransactionId(transactionId);
		receiptRequest.setPaymentTenderDataList(paymentTenderDataList);
		return receiptRequest;
	}

	private Items buildLedgerReceiptItem(LedgerEntryRequest request) {
		Items item = new Items();
		item.setItemName(request != null ? request.getLedgerfor() : null);
		item.setAmount(request != null ? request.getTrnsAmt() : null);
		item.setType(request != null ? request.getCause() : null);
		return item;
	}

	private CreateReceiptRequest buildReceiptRequest(PayDueRequest request, String transactionId, DueAmountDetailsEntity dueEntity) {
		String itemAmount = dueEntity != null && hasText(dueEntity.getAmount()) ? dueEntity.getAmount()
				: (request != null ? request.getAmount() : null);
		String totalAmount = dueEntity != null && hasText(dueEntity.getTotalAmount()) ? dueEntity.getTotalAmount()
				: (request != null ? request.getAmount() : null);
		boolean perHead = isPerHeadCapita(dueEntity != null ? dueEntity.getPaymentCapita() : null);
		CreateReceiptRequest receiptRequest = new CreateReceiptRequest();
		receiptRequest.setGenericHeader(request != null ? request.getGenericHeader() : null);
		Items item = new Items();
		item.setItemName(buildPayDueReceiptItemName(request));
		if (perHead) {
			item.setUnitPrice(itemAmount);
			item.setQuantity(request != null ? trimValue(request.getNoOfPersons()) : null);
			item.setAmount(totalAmount);
		} else {
			item.setAmount(itemAmount);
		}
		item.setType(SecuraConstants.RECEIPT_TYPE_PAYMENT);
		receiptRequest.setItems(List.of(item));
		receiptRequest.setAddedCharges(buildReceiptAddedCharges(dueEntity));
		receiptRequest.setDiscFinReceipt(buildReceiptDiscFin(dueEntity));
		receiptRequest.setReceiptType(SecuraConstants.RECEIPT_TYPE_PAYMENT);
		receiptRequest.setPerheadFlag(perHead);
		receiptRequest.setRemarks(null);
		receiptRequest.setUnitPriceRequired(perHead);
		receiptRequest.setTotalAmount(totalAmount);
		receiptRequest.setTransactionId(transactionId);
		receiptRequest.setFlatId(request != null && request.getGenericHeader() != null ? request.getGenericHeader().getFlatNo() : null);
		receiptRequest.setCreatedBy("Auto Generated");
		receiptRequest.setPaymentTenderDataList(buildPaymentTenderDataList(request));
		return receiptRequest;
	}

	private List<PaymentTenderData> buildPaymentTenderDataList(PayDueRequest request) {
		return normalizePaymentTenderDataList(request != null ? request.getPaymentTenderDataList() : null);
	}

	private String resolveFlatArea(String flatId) {
		if (!hasText(flatId)) {
			return null;
		}
		Optional<Flat> flatOptional = flatRepository.findById(flatId);
		return flatOptional.isPresent() ? flatOptional.get().getFlatArea() : null;
	}

	private DueAmountDetailsEntity resolveDueAmountDetailsEntity(PayDueRequest request, String flatArea) {
		if (request == null) {
			return null;
		}
		String dueId = request.getDueId();
		String paymentCycle = request.getPaymentCycle();
		LocalDate dueDate = request.getDueDate();
		if (!hasText(dueId) || !hasText(paymentCycle) || !hasText(flatArea) || dueDate == null) {
			return null;
		}
		return dueAmountDetailsRepository
				.findById(new DueAmountDetailsEntityId(dueId, paymentCycle, flatArea, dueDate))
				.orElse(null);
	}

	private List<AddedCharges> buildReceiptAddedCharges(DueAmountDetailsEntity dueEntity) {
		List<AddedCharges> charges = new ArrayList<>();
		if (dueEntity == null) {
			return charges;
		}
		if (hasText(dueEntity.getAddedCharges())) {
			List<AddedCharges> entityCharges = genericService.fromJson(dueEntity.getAddedCharges(),
					new TypeReference<List<AddedCharges>>() {});
			if (entityCharges != null) {
				charges.addAll(entityCharges);
			}
		}
		if (hasText(dueEntity.getGstAmount())) {
			AddedCharges gstCharge = new AddedCharges();
			gstCharge.setChargeName("GST");
			gstCharge.setChargeType("GST");
			gstCharge.setValue(dueEntity.getGstAmount());
			gstCharge.setFinalChargeValue(dueEntity.getGstAmount());
			charges.add(gstCharge);
		}
		return charges;
	}

	private DiscFinReceipt buildReceiptDiscFin(DueAmountDetailsEntity dueEntity) {
		if (dueEntity == null) {
			return null;
		}
		boolean hasDiscount = hasText(dueEntity.getDiscountedAmount());
		boolean hasFine = hasText(dueEntity.getFineAmount());
		if (!hasDiscount && !hasFine) {
			return null;
		}
		DiscFinReceipt discFin = new DiscFinReceipt();
		if (hasDiscount) {
			discFin.setDiscountCode(dueEntity.getDiscountCode());
			discFin.setDiscountPercentage(dueEntity.getDiscValue());
			discFin.setDiscountType(dueEntity.getDiscountMode());
			discFin.setDiscountAmount(dueEntity.getDiscountedAmount());
		}
		if (hasFine) {
			discFin.setFineCode(dueEntity.getFineCode());
			discFin.setFinePercentage(dueEntity.getFnValue());
			discFin.setFineType(dueEntity.getFineType());
			discFin.setFineAmount(dueEntity.getFineAmount());
			discFin.setFineCycleMode(dueEntity.getFineMode());
		}
		return discFin;
	}

	private String buildPayDueReceiptItemName(PayDueRequest request) {
		if (request == null || !hasText(request.getPaymentName())) {
			return null;
		}
		if (isOnceCycle(request.getPaymentCycle())) {
			return request.getPaymentName();
		}
		if (request.getDueStartDate() == null || request.getDueEndDate() == null) {
			return null;
		}
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d-MMM-yyyy", Locale.ENGLISH);
		return request.getPaymentName() + " (Period: " + request.getDueStartDate().format(dateFormatter) + " to "
				+ request.getDueEndDate().format(dateFormatter) + ")";
	}

	private String createFlatPendingDueId(String dueId, String paymentCycle, String flatArea, LocalDate dueDate,
			String paymentId) {
		if (!hasText(dueId) || !hasText(paymentCycle) || dueDate == null) {
			return null;
		}
		String paymentCapita = paymentRepository.findFirstByPaymentId(paymentId).map(PaymentEntity::getPaymentCapita)
				.orElse(null);
		String dueFlatAreaToken = isPerSqftCapita(paymentCapita) ? trimValue(flatArea) : "ALL";
		if (!hasText(dueFlatAreaToken)) {
			return null;
		}
		return dueId + "_" + paymentCycle + "_" + dueFlatAreaToken + "_" + dueDate;
	}

	private String resolvePrimaryTender(List<PaymentTenderData> paymentTenderDataList) {
		if (paymentTenderDataList == null || paymentTenderDataList.isEmpty()) {
			return null;
		}
		return paymentTenderDataList.get(0) != null ? paymentTenderDataList.get(0).getTenderName() : null;
	}

	private String createTransactionId(String tender, String amount, String paymentId, LocalDate todayDate) {
		StringBuilder transactionId = new StringBuilder();
		transactionId.append(normalizeTransactionIdPart(tender));
		transactionId.append(normalizeTransactionIdPart(amount));
		transactionId.append(normalizeTransactionIdPart(paymentId));
		transactionId.append(todayDate != null ? todayDate.format(DateTimeFormatter.BASIC_ISO_DATE) : "");
		transactionId.append(1000 + ThreadLocalRandom.current().nextInt(9000));
		return transactionId.toString().toUpperCase();
	}

	private String normalizeTransactionIdPart(String value) {
		return value == null ? "" : value.replaceAll("\\s+", "");
	}

	private String resolveTransactionStatus(String tender, String transactionStatus) {
		if (SecuraConstants.TRANSACTION_TENDER_ONLINE.equalsIgnoreCase(trimValue(tender))
				&& SecuraConstants.TRANSACTION_STATUS_SUCCESS.equalsIgnoreCase(trimValue(transactionStatus))) {
			return SecuraConstants.TRANSACTION_STATUS_SUCCESS;
		}
		return SecuraConstants.TRANSACTION_STATUS_ON_HOLD;
	}

	private String resolveTransactionCause(PaymentEntity paymentEntity) {
		if (paymentEntity == null || !hasText(paymentEntity.getCauseId())) {
			return SecuraConstants.TRANSACTION_CAUSE_OTHERS;
		}
		String causeId = paymentEntity.getCauseId().trim();
		if (SecuraConstants.TRANSACTION_CAUSE_MAINTENANCE.equalsIgnoreCase(causeId)) {
			return SecuraConstants.TRANSACTION_CAUSE_MAINTENANCE;
		}
		if (SecuraConstants.TRANSACTION_CAUSE_EVENT.equalsIgnoreCase(causeId)) {
			return SecuraConstants.TRANSACTION_CAUSE_EVENT;
		}
		return SecuraConstants.TRANSACTION_CAUSE_OTHERS;
	}

	private boolean requiresWorklist(List<PaymentTenderData> paymentTenderDataList) {
		if (paymentTenderDataList == null || paymentTenderDataList.isEmpty()) {
			return false;
		}
		return paymentTenderDataList.stream()
				.filter(Objects::nonNull)
				.map(PaymentTenderData::getTenderName)
				.map(this::trimValue)
				.anyMatch(normalizedTender -> SecuraConstants.TRANSACTION_TENDER_CASH.equalsIgnoreCase(normalizedTender)
						|| SecuraConstants.TRANSACTION_TENDER_OFFLINE_BANK_TRANSFER.equalsIgnoreCase(normalizedTender));
	}

	private String trimValue(String value) {
		return value == null ? null : value.trim();
	}

	private boolean isPerHeadCapita(String paymentCapita) {
		if (!hasText(paymentCapita)) {
			return false;
		}
		String normalized = paymentCapita.toUpperCase(Locale.ROOT).replaceAll("[\\s_-]", "");
		return "PER_HEAD".equals(normalized);
	}

	private boolean isPerSqftCapita(String paymentCapita) {
		if (!hasText(paymentCapita)) {
			return false;
		}
		String normalized = paymentCapita.toUpperCase(Locale.ROOT).replaceAll("[\\s_-]", "");
		return normalized.contains("PERSQFT");
	}

}
