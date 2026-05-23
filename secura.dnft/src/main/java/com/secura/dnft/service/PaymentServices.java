package com.secura.dnft.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
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

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.secura.dnft.generic.bean.PaymentCauseCode;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.interfaceservice.PaymentInterface;
import com.secura.dnft.request.response.AddedCharges;
import com.secura.dnft.request.response.AddDiscfinRequest;
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
import com.secura.dnft.request.response.PaymentEntityModel;
import com.secura.dnft.request.response.PaymentTenderData;
import com.secura.dnft.request.response.UploadPastDueRequest;
import com.secura.dnft.request.response.UploadPastDueResponse;
import com.secura.dnft.request.response.UpdatePaymentRequest;
import com.secura.dnft.request.response.UpdatePaymentResponse;

import jakarta.persistence.EntityNotFoundException;

@Service
public class PaymentServices implements PaymentInterface {

	private static final Logger LOGGER = LoggerFactory.getLogger(PaymentServices.class);
	// Synthetic id used only for non-persisting due preview calculation.
	private static final String DUE_PREVIEW_PAYMENT_ID = "DUE_PREVIEW";
	private static final String TRANSACTION_ID_PREFIX = "TRN-";
	private static final DateTimeFormatter TRANSACTION_ID_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
	private static final String TRANSACTION_ID_RANDOM_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	private static final int TRANSACTION_ID_RANDOM_LENGTH = 6;
	private static final String[] PAST_DUE_UPLOAD_HEADERS = { "Flat Id", "Due From", "Due Till", "Due Name", "Due Amount",
			"Penalty Amount", "Fine Eligible", "Fine %", "Fine Type" };
	private static final DataFormatter PAST_DUE_DATA_FORMATTER = new DataFormatter();
	private static final DateTimeFormatter PAST_DUE_DATE_FORMATTER = new DateTimeFormatterBuilder().parseCaseInsensitive()
			.appendPattern("d-MMM-yyyy").toFormatter(Locale.ENGLISH);

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

	@Autowired
	WorklistService worklistService;

	@Autowired
	DiscFinServices discFinServices;

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
		List<PaymentEntity> paymentEntities = new ArrayList<>();
		if (request != null && request.getPaymentId() != null && !request.getPaymentId().isBlank()) {
			paymentEntities = paymentRepository.findByPaymentIdAndAprmtId(request.getPaymentId(), apartmentId);
		} else {
			paymentEntities = paymentRepository.findByAprmtId(apartmentId);
		}
		List<PaymentEntityModel> paymentList = buildPaymentEntityModels(paymentEntities);
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

	private List<PaymentEntityModel> buildPaymentEntityModels(List<PaymentEntity> paymentEntities) {
		if (paymentEntities == null || paymentEntities.isEmpty()) {
			return new ArrayList<>();
		}
		Map<String, List<PaymentEntity>> paymentIdToEntityMap = paymentEntities.stream().filter(Objects::nonNull)
				.collect(Collectors.groupingBy(PaymentEntity::getPaymentId, LinkedHashMap::new, Collectors.toList()));
		List<PaymentEntityModel> paymentList = new ArrayList<>();
		for (List<PaymentEntity> paymentEntityList : paymentIdToEntityMap.values()) {
			if (paymentEntityList == null || paymentEntityList.isEmpty()) {
				continue;
			}
			PaymentEntity primaryPaymentEntity = paymentEntityList.get(0);
			PaymentEntityModel paymentModel = new PaymentEntityModel();
			paymentModel.setPaymentId(primaryPaymentEntity.getPaymentId());
			paymentModel.setPaymentName(primaryPaymentEntity.getPaymentName());
			paymentModel.setShortDetails(primaryPaymentEntity.getShortDetails());
			paymentModel.setPaymentCapita(primaryPaymentEntity.getPaymentCapita());
			paymentModel.setPaymentAmount(primaryPaymentEntity.getPaymentAmount());
			paymentModel.setGst(primaryPaymentEntity.getGst());
			paymentModel.setCurrency(primaryPaymentEntity.getCurrency());
			paymentModel.setDueDate(primaryPaymentEntity.getDueDate());
			paymentModel.setCollectionStartDate(primaryPaymentEntity.getCollectionStartDate());
			paymentModel.setCollectionEndDate(primaryPaymentEntity.getCollectionEndDate());
			List<String> paymentCollectionCycleList = parsePaymentCollectionCycleList(primaryPaymentEntity.getPaymentCollectionCycle());
			boolean hasLegacyMultiRowCycles = paymentEntityList.size() > 1 && paymentCollectionCycleList.size() <= 1;
			if (paymentCollectionCycleList.isEmpty() || hasLegacyMultiRowCycles) {
				paymentCollectionCycleList = paymentEntityList.stream().map(PaymentEntity::getPaymentCollectionCycle)
						.filter(this::hasText).map(this::trimValue).distinct().collect(Collectors.toList());
			}
			paymentModel.setPaymentCollectionCycleList(paymentCollectionCycleList);
			paymentModel.setPaymentCollectionMode(primaryPaymentEntity.getPaymentCollectionMode());
			paymentModel.setPartialPaymentAllowed(primaryPaymentEntity.isPartialPaymentAllowed());
			paymentModel.setApplicableFor(primaryPaymentEntity.getApplicableFor());
			paymentModel.setAllowedPaymentModes(primaryPaymentEntity.getAllowedPaymentModes());
			paymentModel.setPaidFlats(primaryPaymentEntity.getPaidFlats());
			paymentModel.setPaymentType(primaryPaymentEntity.getPaymentType());
			paymentModel.setBankAccountId(primaryPaymentEntity.getBankAccountId());
			paymentModel.setStatus(primaryPaymentEntity.getStatus());
			paymentModel.setAprmtId(primaryPaymentEntity.getAprmtId());
			paymentModel.setCauseId(primaryPaymentEntity.getCauseId());
			paymentModel.setAddedCharges(primaryPaymentEntity.getAddedCharges());
			paymentModel.setCreatTs(primaryPaymentEntity.getCreatTs());
			paymentModel.setCreatUsrId(primaryPaymentEntity.getCreatUsrId());
			paymentModel.setLstUpdtTs(primaryPaymentEntity.getLstUpdtTs());
			paymentModel.setLstUpdtUsrId(primaryPaymentEntity.getLstUpdtUsrId());
			setDiscountAndFineCode(primaryPaymentEntity, paymentModel);
			paymentList.add(paymentModel);
		}
		return paymentList;
	}

	private void setDiscountAndFineCode(PaymentEntity paymentEntity, PaymentEntityModel paymentModel) {
		if (paymentEntity == null || paymentModel == null || !hasText(paymentEntity.getDiscFin())) {
			return;
		}
		List<Map<String, String>> discFinList = genericService.fromJson(paymentEntity.getDiscFin(),
				new TypeReference<List<Map<String, String>>>() {
				});
		if (discFinList == null || discFinList.isEmpty()) {
			return;
		}
		for (Map<String, String> discFin : discFinList) {
			if (discFin == null) {
				continue;
			}
			String discFinType = trimValue(discFin.get("DISTFIN_TYPE"));
			String code = trimValue(discFin.get("code"));
			if ("DISCOUNT".equalsIgnoreCase(discFinType) && paymentModel.getDiscountCode() == null) {
				paymentModel.setDiscountCode(code);
			}
			if ("FINE".equalsIgnoreCase(discFinType) && paymentModel.getFineCode() == null) {
				paymentModel.setFineCode(code);
			}
		}
	}

	@Override
	public CreatePaymentResponse createPayment(CreatePaymentRequest request) throws Exception {
		CreatePaymentResponse response = new CreatePaymentResponse();
		response.setGenericHeader(request.getGenericHeader());
		List<String> paymentCollectionCycles = resolvePaymentCollectionCycles(request);
		LocalDate collectionStartDate = request.getCollectionStartDate() != null ? request.getCollectionStartDate().toLocalDate() : null;
		LocalDate collectionEndDate = request.getCollectionEndDate() != null ? request.getCollectionEndDate().toLocalDate() : null;
		String paymentId = getPaymentId(request != null ? request.getCause() : null);
		String applicableFor = serializeApplicableFor(request.getApplicableFor());
		String allowedPaymentModes = serializeAllowedPaymentModes(request.getAllowedPaymentModes());
		String addedCharges = serializeAddedCharges(request.getAddedCharges());
		String discFin = serializeDiscFin(request);
		String paymentCollectionCyclesJson = serializePaymentCollectionCycles(paymentCollectionCycles);
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
		entity.setPaymentCollectionCycle(paymentCollectionCyclesJson);
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
		entity.setPartialPaymentAllowed(request.isPartialPaymentAllowed());
		paymentRepository.save(entity);
		dueDetailsService.calculateDuesForPayment(paymentId, request.getGenericHeader());
		response.setMessage(SuccessMessage.SUCC_MESSAGE_23);
		response.setMessage_code(SuccessMessageCode.SUCC_MESSAGE_23);
		return response;
	}

	@Override
	public UploadPastDueResponse uploadPastDue(UploadPastDueRequest request) throws Exception {
		UploadPastDueResponse response = new UploadPastDueResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		String apartmentId = request != null && request.getGenericHeader() != null
				? trimValue(request.getGenericHeader().getApartmentId())
				: null;
		if (!hasText(apartmentId) || request == null || !hasText(request.getFile())) {
			response.setSuccessRows(0);
			response.setFailedRows(0);
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
			return response;
		}

		List<List<String>> failedRows = new ArrayList<>();
		int successRows = 0;
		Set<String> validFlatIds = flatRepository.findByAprmntId(apartmentId).stream().filter(Objects::nonNull)
				.map(Flat::getFlatNo).filter(this::hasText).map(this::normalizeFlatNoForMatch).filter(Objects::nonNull)
				.collect(Collectors.toCollection(HashSet::new));

		try (Workbook workbook = new XSSFWorkbook(
				new ByteArrayInputStream(Base64.getDecoder().decode(stripDataUrlPrefix(request.getFile()))))) {
			Sheet sheet = workbook.getSheetAt(0);
			for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
				Row row = sheet.getRow(rowIndex);
				if (row == null || isPastDueRowBlank(row)) {
					continue;
				}
				try {
					PastDueUploadRow uploadRow = extractPastDueUploadRow(row);
					List<String> validationErrors = validatePastDueUploadRow(uploadRow, validFlatIds);
					if (!validationErrors.isEmpty()) {
						failedRows.add(buildPastDueFailedRow(uploadRow, String.join("; ", validationErrors)));
						continue;
					}
					CreatePaymentRequest createPaymentRequest = buildPastDueCreatePaymentRequest(request, uploadRow);
					createPayment(createPaymentRequest);
					successRows++;
				} catch (Exception rowException) {
					failedRows.add(buildPastDueFailedRow(row, rowException.getMessage()));
				}
			}
		} catch (Exception exception) {
			response.setSuccessRows(successRows);
			response.setFailedRows(failedRows.size());
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
			return response;
		}
		response.setSuccessRows(successRows);
		response.setFailedRows(failedRows.size());

		if (!failedRows.isEmpty()) {
			response.setFile(generatePastDueFailedRowsWorkbook(failedRows));
			response.setMessage(ErrorMessage.ERR_MESSAGE_42);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_42);
			return response;
		}
		response.setMessage(SuccessMessage.SUCC_MESSAGE_23);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_23);
		return response;
	}

	@Override
	public PayDueResponse payDues(PayDueRequest request) throws Exception {
		PayDueResponse response = new PayDueResponse();
		PaymentEntity paymentEntity = paymentRepository.findFirstByPaymentId(request.getPaymentId())
				.orElseThrow(() -> new EntityNotFoundException(ErrorMessage.ERR_MESSAGE_33));
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		String flatId = request != null && request.getGenericHeader() != null ? request.getGenericHeader().getFlatNo() : null;
		String flatArea = resolveFlatArea(flatId);
		List<PaymentTenderData> paymentTenderDataList = normalizePaymentTenderDataList(
				request != null ? request.getPaymentTenderDataList() : null);
		boolean onlinePayment = isOnlinePayment(paymentTenderDataList);
		Transaction transaction = buildTransaction(request, flatArea, paymentEntity, paymentTenderDataList);
		boolean successfulTransaction = isSuccessfulTransaction(transaction.getTrnsStatus());
		DueAmountDetailsEntity dueEntity = resolveDueAmountDetailsEntity(request, flatArea,paymentEntity);
		CreateReceiptResponse receiptResponse = receiptServices
				.createReceipt(buildReceiptRequest(request, transaction.getTrnscId(), dueEntity));
		transaction.setReceiptNumber(receiptResponse != null ? receiptResponse.getReceiptNumber() : null);
		transactionRepository.save(transaction);
		if (!onlinePayment) {
			Worklist worklist = worklistService.createTransactionReviewWorklist(transaction.getTrnscId(),
					request != null ? request.getGenericHeader() : null);
			transaction.setWorkListId(worklist.getWorklistId());
			transactionRepository.save(transaction);
		} else if (successfulTransaction) {
			response.setReceipt(receiptResponse != null ? receiptResponse.getReceipt() : null);
			response.setReceiptNumber(receiptResponse != null ? receiptResponse.getReceiptNumber() : null);
		}
		response.setMessage(SuccessMessage.SUCC_MESSAGE_33);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_33);
		response.setTransactionId(transaction.getTrnscId());
		if (onlinePayment && successfulTransaction) {
			response.setReceiptNumber(transaction.getReceiptNumber());
		}
		if (successfulTransaction) {
			removeCoveredDuesAfterSuccessfulPayment(
					request.getGenericHeader() != null ? request.getGenericHeader().getApartmentId() : null,
					request.getGenericHeader() != null ? request.getGenericHeader().getFlatNo() : null,
					request.getPaymentId(), transaction.getDueDetails());
		}
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

	public String getPaymentId(String cause) {
		String causeCode = PaymentCauseCode.getCode(cause);
		for (int attempt = 0; attempt < 10000; attempt++) {
			String candidatePaymentId = SecuraConstants.PAYMENT_ID_PREFIX + causeCode + generatePaymentIdRandomSuffix(3);
			if (paymentRepository.findFirstByPaymentId(candidatePaymentId).isEmpty()) {
				return candidatePaymentId;
			}
		}
		throw new IllegalStateException("Unable to generate unique payment id");
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

	private String serializePaymentCollectionCycles(List<String> paymentCollectionCycles) {
		if (paymentCollectionCycles == null || paymentCollectionCycles.isEmpty()) {
			return null;
		}
		List<String> normalizedCycles = paymentCollectionCycles.stream().filter(this::hasText).map(this::trimValue).distinct()
				.collect(Collectors.toList());
		if (normalizedCycles.isEmpty()) {
			return null;
		}
		return genericService.toJson(normalizedCycles);
	}

	private List<String> parsePaymentCollectionCycleList(String paymentCollectionCycles) {
		if (!hasText(paymentCollectionCycles)) {
			return List.of();
		}
		String trimmedCycles = trimValue(paymentCollectionCycles);
		if (!hasText(trimmedCycles)) {
			return List.of();
		}
		if (!trimmedCycles.startsWith("[") || !trimmedCycles.endsWith("]")) {
			return List.of(trimmedCycles);
		}
		try {
			List<String> cycleList = genericService.fromJson(trimmedCycles, new TypeReference<List<String>>() {
			});
			if (cycleList == null || cycleList.isEmpty()) {
				return List.of();
			}
			return cycleList.stream().filter(this::hasText).map(this::trimValue).distinct().collect(Collectors.toList());
		} catch (Exception exception) {
			LOGGER.warn("Failed to parse payment collection cycles JSON '{}'; falling back to raw value", trimmedCycles, exception);
			return List.of(trimmedCycles);
		}
	}

	private Transaction buildTransaction(PayDueRequest request, String flatArea, PaymentEntity paymentEntity,
			List<PaymentTenderData> paymentTenderDataList) {
//		PaymentEntity paymentEntity = paymentRepository.findFirstByPaymentId(request.getPaymentId())
//				.orElseThrow(() -> new EntityNotFoundException(ErrorMessage.ERR_MESSAGE_33));
		LocalDateTime currentTimestamp = LocalDateTime.now();
		String dueId = request.getDueId();
		String paymentCycle = request.getPaymentCycle();
		LocalDate dueDate = request.getDueDate();
		List<PaymentTenderData> resolvedTenderDataList = paymentTenderDataList != null
				? paymentTenderDataList
				: buildPaymentTenderDataList(request);
		String primaryTender = resolvePrimaryTender(resolvedTenderDataList);
		Transaction transaction = new Transaction();
		transaction.setAprmntId(request.getGenericHeader() != null ? request.getGenericHeader().getApartmentId() : null);
		transaction.setTrnscId(createTransactionId(
				request.getGenericHeader() != null ? request.getGenericHeader().getApartmentId() : null, currentTimestamp));
		transaction.setTrnsDate(currentTimestamp);
		transaction.setTrnsBy(request.getGenericHeader() != null ? request.getGenericHeader().getUserId() : null);
		transaction.setTrnsTender(
				resolvedTenderDataList == null ? null : genericService.toJson(resolvedTenderDataList));
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
		transaction.setThirdPartyName(resolveThirdPartyName(primaryTender));
		transaction.setDueDetails(createFlatPendingDueId(dueId, paymentCycle, flatArea, dueDate, request.getPaymentId(),
				paymentEntity.getPaymentCapita()));
		transaction.setCause(resolveTransactionCause(paymentEntity));
		transaction.setBankInstrumentTenderDetails(serializeBankInstrumentTenderDetails(
				request.getBankInstrumentTenderDetails()));
		transaction.setCreatTs(currentTimestamp);
		transaction.setCreatUsrId(request.getGenericHeader() != null ? request.getGenericHeader().getUserId() : null);
		transaction.setFlatId(request.getGenericHeader() != null ? request.getGenericHeader().getFlatNo() : null);
		transaction.setLstUpdtTs(null);
		transaction.setLstUpdtUsrId(null);
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
		transaction.setTrnscId(createTransactionId(
				request != null && request.getGenericHeader() != null ? request.getGenericHeader().getApartmentId() : null,
				currentTimestamp));
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
		BigDecimal perHeadMultiplier = perHead
				? resolvePerHeadMultiplier(request != null ? request.getNoOfPersons() : null)
				: BigDecimal.ONE;
		CreateReceiptRequest receiptRequest = new CreateReceiptRequest();
		receiptRequest.setGenericHeader(request != null ? request.getGenericHeader() : null);
		Items item = new Items();
		item.setItemName(buildPayDueReceiptItemName(request));
		if (perHead) {
			item.setUnitPrice(itemAmount);
			item.setQuantity(request != null ? trimValue(request.getNoOfPersons()) : null);
			item.setAmount(multiplyAmount(itemAmount, perHeadMultiplier));
		} else {
			item.setAmount(itemAmount);
		}
		item.setType(SecuraConstants.RECEIPT_TYPE_PAYMENT);
		receiptRequest.setItems(List.of(item));
		receiptRequest.setAddedCharges(buildReceiptAddedCharges(dueEntity, perHeadMultiplier));
		receiptRequest.setDiscFinReceipt(buildReceiptDiscFin(dueEntity, perHeadMultiplier));
		receiptRequest.setReceiptType(SecuraConstants.RECEIPT_TYPE_PAYMENT);
		receiptRequest.setPerheadFlag(perHead);
		receiptRequest.setRemarks(null);
		receiptRequest.setUnitPriceRequired(perHead);
		receiptRequest.setTotalAmount(perHead ? multiplyAmount(totalAmount, perHeadMultiplier) : totalAmount);
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

	private DueAmountDetailsEntity resolveDueAmountDetailsEntity(PayDueRequest request, String flatArea,PaymentEntity paymentEntity) {
		if (request == null) {
			return null;
		}
		String dueId = request.getDueId();
		String paymentCycle = request.getPaymentCycle();
		LocalDate dueDate = request.getDueDate();
		if (!hasText(dueId) || !hasText(paymentCycle) || !hasText(flatArea) || dueDate == null) {
			return null;
		}
		String dueFlatAreaToken = isPerSqftCapita(paymentEntity.getPaymentCapita()) ? trimValue(flatArea) : "ALL";
		return dueAmountDetailsRepository
				.findById(new DueAmountDetailsEntityId(dueId, paymentCycle, dueFlatAreaToken, dueDate))
				.orElse(null);
	}

	private List<AddedCharges> buildReceiptAddedCharges(DueAmountDetailsEntity dueEntity, BigDecimal multiplier) {
		List<AddedCharges> charges = new ArrayList<>();
		if (dueEntity == null) {
			return charges;
		}
		if (hasText(dueEntity.getAddedCharges())) {
			List<AddedCharges> entityCharges = genericService.fromJson(dueEntity.getAddedCharges(),
					new TypeReference<List<AddedCharges>>() {});
			if (entityCharges != null) {
				for (AddedCharges charge : entityCharges) {
					if (charge == null) {
						continue;
					}
					charge.setFinalChargeValue(multiplyAmount(charge.getFinalChargeValue(), multiplier));
					if (!"percentage".equalsIgnoreCase(trimValue(charge.getChargeType()))) {
						charge.setValue(multiplyAmount(charge.getValue(), multiplier));
					}
				}
				charges.addAll(entityCharges);
			}
		}
		if (hasText(dueEntity.getGstAmount())) {
			AddedCharges gstCharge = new AddedCharges();
			gstCharge.setChargeName("GST");
			gstCharge.setChargeType("GST");
			String gstAmount = multiplyAmount(dueEntity.getGstAmount(), multiplier);
			gstCharge.setValue(gstAmount);
			gstCharge.setFinalChargeValue(gstAmount);
			charges.add(gstCharge);
		}
		return charges;
	}

	private DiscFinReceipt buildReceiptDiscFin(DueAmountDetailsEntity dueEntity, BigDecimal multiplier) {
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
			discFin.setDiscountAmount(multiplyAmount(dueEntity.getDiscountedAmount(), multiplier));
		}
		if (hasFine) {
			discFin.setFineCode(dueEntity.getFineCode());
			discFin.setFinePercentage(dueEntity.getFnValue());
			discFin.setFineType(dueEntity.getFineType());
			discFin.setFineAmount(multiplyAmount(dueEntity.getFineAmount(), multiplier));
			discFin.setFineCycleMode(dueEntity.getFineMode());
		}
		return discFin;
	}

	private BigDecimal resolvePerHeadMultiplier(String noOfPersons) {
		BigDecimal quantity = parseNumeric(noOfPersons);
		if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ONE;
		}
		return quantity;
	}

	private String multiplyAmount(String amount, BigDecimal multiplier) {
		if (!hasText(amount) || multiplier == null || multiplier.compareTo(BigDecimal.ONE) == 0) {
			return amount;
		}
		String normalized = amount.replace(",", "").trim();
		try {
			return formatNumber(new BigDecimal(normalized).multiply(multiplier));
		} catch (NumberFormatException ex) {
			return amount;
		}
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
			String paymentId, String paymentCapita) {
		if (!hasText(dueId) || !hasText(paymentCycle) || !hasText(paymentId) || dueDate == null) {
			return null;
		}
		String dueFlatAreaToken = isPerSqftCapita(paymentCapita) ? trimValue(flatArea) : "ALL";
		if (!hasText(dueFlatAreaToken)) {
			return null;
		}
		return dueId + "_" + paymentCycle + "_" + dueFlatAreaToken + "_" + dueDate;
	}

	private void removeCoveredDuesAfterSuccessfulPayment(String apartmentId, String flatId, String paymentId,
			String paidDueId) {
		if (!hasText(apartmentId) || !hasText(flatId) || !hasText(paymentId) || !hasText(paidDueId)) {
			return;
		}
		DueAmountDetailsEntityId paidDueEntityId = parsePendingDueKeyToEntityId(paidDueId);
		if (paidDueEntityId == null) {
			return;
		}
		DueAmountDetailsEntity paidDue = dueAmountDetailsRepository.findById(paidDueEntityId).orElse(null);
		if (paidDue == null) {
			return;
		}
		LocalDate paidStartDate = paidDue.getDueStartDate() != null ? paidDue.getDueStartDate() : paidDue.getDueDate();
		LocalDate paidEndDate = paidDue.getDueEndDate() != null ? paidDue.getDueEndDate() : paidDue.getDueDate();
		if (paidStartDate == null || paidEndDate == null) {
			return;
		}
		List<DueAmountDetailsEntity> allDuesForPayment = dueAmountDetailsRepository.findByPaymentId(paymentId);
		if (allDuesForPayment == null || allDuesForPayment.isEmpty()) {
			return;
		}
		List<DueAmountDetailsEntity> coveredDues = allDuesForPayment.stream().filter(Objects::nonNull)
				.filter(due -> isCoveredDue(due, paidStartDate, paidEndDate)).collect(Collectors.toList());
		if (coveredDues.isEmpty()) {
			return;
		}
		Set<String> coveredDueKeys = coveredDues.stream().map(this::buildFlatPendingDueKey).filter(this::hasText)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		removeCoveredDueKeysFromFlat(flatId, coveredDues, coveredDueKeys);
		addFlatToCoveredDuePaidFlats(flatId, coveredDues, paidDue);
		addFlatToPaymentPaidFlatsWhenNoDuesRemain(apartmentId, flatId, paymentId);
	}

	private DueAmountDetailsEntityId parsePendingDueKeyToEntityId(String pendingDueKey) {
		if (!hasText(pendingDueKey)) {
			return null;
		}
		String normalizedPendingKey = pendingDueKey.trim();
		int firstSeparatorIndex = normalizedPendingKey.indexOf('_');
		int lastSeparatorIndex = normalizedPendingKey.lastIndexOf('_');
		if (firstSeparatorIndex <= 0 || lastSeparatorIndex <= firstSeparatorIndex) {
			return null;
		}
		int secondLastSeparatorIndex = normalizedPendingKey.lastIndexOf('_', lastSeparatorIndex - 1);
		if (secondLastSeparatorIndex <= firstSeparatorIndex) {
			return null;
		}
		String dueId = normalizedPendingKey.substring(0, firstSeparatorIndex);
		String collectionCycle = normalizedPendingKey.substring(firstSeparatorIndex + 1, secondLastSeparatorIndex);
		String flatArea = normalizedPendingKey.substring(secondLastSeparatorIndex + 1, lastSeparatorIndex);
		String dueDateValue = normalizedPendingKey.substring(lastSeparatorIndex + 1);
		if (!hasText(dueId) || !hasText(collectionCycle) || !hasText(flatArea) || !hasText(dueDateValue)) {
			return null;
		}
		try {
			return new DueAmountDetailsEntityId(dueId.trim(), collectionCycle.trim(), flatArea.trim(),
					LocalDate.parse(dueDateValue.trim()));
		} catch (Exception ex) {
			return null;
		}
	}

	private boolean isCoveredDue(DueAmountDetailsEntity due, LocalDate paidStartDate, LocalDate paidEndDate) {
		if (due == null) {
			return false;
		}
		LocalDate dueStartDate = due.getDueStartDate() != null ? due.getDueStartDate() : due.getDueDate();
		LocalDate dueEndDate = due.getDueEndDate() != null ? due.getDueEndDate() : due.getDueDate();
		if (dueStartDate == null || dueEndDate == null) {
			return false;
		}
		boolean childDueCovered = dueStartDate.compareTo(paidStartDate) >= 0 && dueEndDate.compareTo(paidEndDate) <= 0;
		boolean parentDueCovered = dueStartDate.compareTo(paidStartDate) <= 0 && dueEndDate.compareTo(paidEndDate) >= 0;
		return childDueCovered || parentDueCovered;
	}

	private String buildFlatPendingDueKey(DueAmountDetailsEntity dueEntity) {
		if (dueEntity == null || dueEntity.getDueDate() == null || !hasText(dueEntity.getDueId())
				|| !hasText(dueEntity.getCollectionCycle()) || !hasText(dueEntity.getFlatArea())) {
			return null;
		}
		return dueEntity.getDueId() + "_" + dueEntity.getCollectionCycle() + "_" + dueEntity.getFlatArea() + "_"
				+ dueEntity.getDueDate();
	}

	private void removeCoveredDueKeysFromFlat(String flatId, List<DueAmountDetailsEntity> coveredDues,
			Set<String> coveredDueKeys) {
		if (!hasText(flatId) || coveredDueKeys == null || coveredDueKeys.isEmpty() || coveredDues == null
				|| coveredDues.isEmpty()) {
			return;
		}
		Flat flat = flatRepository.findById(flatId).orElse(null);
		if (flat == null) {
			return;
		}
		List<String> pendingDueKeys = parseJsonStringList(flat.getFlatPndngPaymntLst());
		if (pendingDueKeys.isEmpty()) {
			return;
		}
		Set<String> perHeadCoveredDueKeys = coveredDues.stream().filter(Objects::nonNull)
				.filter(due -> isPerHeadCapita(due.getPaymentCapita())).map(this::buildFlatPendingDueKey).filter(this::hasText)
				.map(String::trim).collect(Collectors.toSet());
		boolean modified = pendingDueKeys.removeIf(pendingDueKey -> hasText(pendingDueKey)
				&& coveredDueKeys.contains(pendingDueKey.trim()) && !perHeadCoveredDueKeys.contains(pendingDueKey.trim()));
		if (modified) {
			flat.setFlatPndngPaymntLst(genericService.toJson(pendingDueKeys));
			flatRepository.save(flat);
		}
	}

	private void addFlatToCoveredDuePaidFlats(String flatId, List<DueAmountDetailsEntity> coveredDues,
			DueAmountDetailsEntity paidDue) {
		if (!hasText(flatId) || coveredDues == null || coveredDues.isEmpty()) {
			return;
		}
		List<DueAmountDetailsEntity> updatedDues = new ArrayList<>();
		for (DueAmountDetailsEntity due : coveredDues) {
			if (due == null) {
				continue;
			}
			boolean modified = false;
			List<String> paidFlats = parseJsonStringList(due.getPaidFlats());
			if (!containsFlatId(paidFlats, flatId)) {
				List<String> updatedPaidFlats = addFlatIdToList(paidFlats, flatId);
				if (!paidFlats.equals(updatedPaidFlats)) {
					due.setPaidFlats(genericService.toJson(updatedPaidFlats));
					modified = true;
				}
			}
			if (!isSameDueIdentity(due, paidDue)) {
				List<String> applicableFlats = parseJsonStringList(due.getApplicableFlats());
				boolean applicableRemoved = applicableFlats
						.removeIf(applicableFlat -> hasText(applicableFlat) && flatId.trim().equalsIgnoreCase(applicableFlat.trim()));
				if (applicableRemoved) {
					due.setApplicableFlats(genericService.toJson(applicableFlats));
					modified = true;
				}
			}
			if (modified) {
				updatedDues.add(due);
			}
		}
		if (!updatedDues.isEmpty()) {
			dueAmountDetailsRepository.saveAll(updatedDues);
		}
	}

	private boolean isSameDueIdentity(DueAmountDetailsEntity due, DueAmountDetailsEntity otherDue) {
		if (due == null || otherDue == null) {
			return false;
		}
		return Objects.equals(due.getDueId(), otherDue.getDueId())
				&& Objects.equals(due.getCollectionCycle(), otherDue.getCollectionCycle())
				&& Objects.equals(due.getFlatArea(), otherDue.getFlatArea())
				&& Objects.equals(due.getDueDate(), otherDue.getDueDate());
	}

	private void addFlatToPaymentPaidFlatsWhenNoDuesRemain(String apartmentId, String flatId, String paymentId) {
		if (!hasText(apartmentId) || !hasText(flatId) || !hasText(paymentId)) {
			return;
		}
		List<DueAmountDetailsEntity> duesForPayment = dueAmountDetailsRepository.findByPaymentId(paymentId);
		if (duesForPayment == null) {
			duesForPayment = List.of();
		}
		Set<String> paymentDueKeysForFlat = duesForPayment.stream().filter(Objects::nonNull)
				.filter(due -> containsFlatId(parseJsonStringList(due.getApplicableFlats()), flatId))
				.map(this::buildFlatPendingDueKey).filter(this::hasText).collect(Collectors.toSet());
		if (paymentDueKeysForFlat.isEmpty()) {
			return;
		}
		Flat flat = flatRepository.findById(flatId).orElse(null);
		if (flat == null) {
			return;
		}
		List<String> pendingDueKeys = parseJsonStringList(flat.getFlatPndngPaymntLst());
		boolean hasRemainingDueForFlat = pendingDueKeys.stream().filter(this::hasText).map(String::trim)
				.anyMatch(paymentDueKeysForFlat::contains);
		if (hasRemainingDueForFlat) {
			return;
		}
		List<PaymentEntity> paymentEntities = paymentRepository.findByPaymentIdAndAprmtId(paymentId, apartmentId);
		if (paymentEntities == null || paymentEntities.isEmpty()) {
			return;
		}
		List<PaymentEntity> updatedPayments = new ArrayList<>();
		for (PaymentEntity paymentEntity : paymentEntities) {
			if (paymentEntity == null) {
				continue;
			}
			List<String> paidFlats = parseJsonStringList(paymentEntity.getPaidFlats());
			if (containsFlatId(paidFlats, flatId)) {
				continue;
			}
			List<String> updatedPaidFlats = addFlatIdToList(paidFlats, flatId);
			if (!paidFlats.equals(updatedPaidFlats)) {
				paymentEntity.setPaidFlats(genericService.toJson(updatedPaidFlats));
				updatedPayments.add(paymentEntity);
			}
		}
		if (!updatedPayments.isEmpty()) {
			paymentRepository.saveAll(updatedPayments);
		}
	}

	private List<String> parseJsonStringList(String json) {
		if (!hasText(json)) {
			return new ArrayList<>();
		}
		try {
			List<String> values = genericService.fromJson(json, new TypeReference<List<String>>() {
			});
			if (values == null) {
				return new ArrayList<>();
			}
			return values.stream().filter(this::hasText).map(String::trim).collect(Collectors.toCollection(ArrayList::new));
		} catch (Exception ex) {
			return new ArrayList<>();
		}
	}

	private List<String> addFlatIdToList(List<String> values, String flatId) {
		if (!hasText(flatId)) {
			return values != null ? new ArrayList<>(values) : new ArrayList<>();
		}
		List<String> normalizedValues = values == null ? new ArrayList<>()
				: values.stream().filter(this::hasText).map(String::trim).distinct()
						.collect(Collectors.toCollection(ArrayList::new));
		String normalizedFlatId = flatId.trim();
		if (normalizedValues.stream().noneMatch(value -> value.equalsIgnoreCase(normalizedFlatId))) {
			normalizedValues.add(normalizedFlatId);
		}
		return normalizedValues;
	}

	private boolean containsFlatId(List<String> values, String flatId) {
		if (values == null || values.isEmpty() || !hasText(flatId)) {
			return false;
		}
		return values.stream().filter(this::hasText).map(String::trim).anyMatch(value -> value.equalsIgnoreCase(flatId.trim()));
	}

	private String resolvePrimaryTender(List<PaymentTenderData> paymentTenderDataList) {
		if (paymentTenderDataList == null || paymentTenderDataList.isEmpty()) {
			return null;
		}
		return paymentTenderDataList.get(0) != null ? paymentTenderDataList.get(0).getTenderName() : null;
	}

	private String createTransactionId(String apartmentId, LocalDateTime currentTimestamp) {
		LocalDateTime effectiveTimestamp = currentTimestamp != null ? currentTimestamp : LocalDateTime.now();
		StringBuilder transactionId = new StringBuilder();
		transactionId.append(TRANSACTION_ID_PREFIX);
		transactionId.append(normalizeTransactionIdPart(apartmentId));
		transactionId.append(effectiveTimestamp.format(TRANSACTION_ID_TIMESTAMP_FORMATTER));
		transactionId.append(generateTransactionRandomSuffix());
		return transactionId.toString().toUpperCase();
	}

	private String normalizeTransactionIdPart(String value) {
		return value == null ? "" : value.replaceAll("\\s+", "");
	}

	private String generateTransactionRandomSuffix() {
		return generateRandomSuffix(TRANSACTION_ID_RANDOM_LENGTH);
	}

	private String generatePaymentIdRandomSuffix(int length) {
		return generateRandomSuffix(length);
	}

	private String generateRandomSuffix(int length) {
		StringBuilder suffix = new StringBuilder(length);
		for (int index = 0; index < length; index++) {
			int randomIndex = ThreadLocalRandom.current().nextInt(TRANSACTION_ID_RANDOM_CHARACTERS.length());
			suffix.append(TRANSACTION_ID_RANDOM_CHARACTERS.charAt(randomIndex));
		}
		return suffix.toString();
	}

	private String resolveTransactionStatus(String tender, String transactionStatus) {
		if (SecuraConstants.TENDER_ONLINE.equalsIgnoreCase(trimValue(tender))
				&& SecuraConstants.TRANSACTION_STATUS_SUCCESS.equalsIgnoreCase(trimValue(transactionStatus))) {
			return SecuraConstants.TRANSACTION_STATUS_SUCCESS;
		}
		if (SecuraConstants.TENDER_ONLINE.equalsIgnoreCase(trimValue(tender))) {
			return trimValue(transactionStatus);
		}
		return SecuraConstants.TRANSACTION_STATUS_PENDING;
	}

	private String resolveThirdPartyName(String tender) {
		if (SecuraConstants.TENDER_ONLINE.equalsIgnoreCase(trimValue(tender))) {
			return SecuraConstants.TRANSACTION_THIRD_PARTY_RAZOR_PAY;
		}
		return "";
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

	private boolean isSuccessfulTransaction(String transactionStatus) {
		return SecuraConstants.TRANSACTION_STATUS_SUCCESS.equalsIgnoreCase(trimValue(transactionStatus));
	}

	private boolean requiresWorklist(List<PaymentTenderData> paymentTenderDataList) {
		if (paymentTenderDataList == null || paymentTenderDataList.isEmpty()) {
			return false;
		}
		return paymentTenderDataList.stream()
				.filter(Objects::nonNull)
				.map(PaymentTenderData::getTenderName)
				.map(this::trimValue)
				.anyMatch(normalizedTender -> !SecuraConstants.TENDER_ONLINE.equalsIgnoreCase(normalizedTender));
	}

	private boolean isOnlinePayment(List<PaymentTenderData> paymentTenderDataList) {
		return !requiresWorklist(paymentTenderDataList);
	}

	private PastDueUploadRow extractPastDueUploadRow(Row row) {
		PastDueUploadRow uploadRow = new PastDueUploadRow();
		uploadRow.flatId = readPastDueStringCell(row, 0);
		uploadRow.dueFromText = readPastDueStringCell(row, 1);
		uploadRow.dueTillText = readPastDueStringCell(row, 2);
		uploadRow.dueName = readPastDueStringCell(row, 3);
		uploadRow.dueAmount = readPastDueStringCell(row, 4);
		uploadRow.penaltyAmount = readPastDueStringCell(row, 5);
		uploadRow.fineEligible = readPastDueStringCell(row, 6);
		uploadRow.finePercent = readPastDueStringCell(row, 7);
		uploadRow.fineType = readPastDueStringCell(row, 8);
		uploadRow.dueFrom = parsePastDueDate(uploadRow.dueFromText, "Due From");
		uploadRow.dueTill = parsePastDueDate(uploadRow.dueTillText, "Due Till");
		return uploadRow;
	}

	private List<String> validatePastDueUploadRow(PastDueUploadRow row, Set<String> validFlatIds) {
		List<String> errors = new ArrayList<>();
		String normalizedFlatId = normalizeFlatNoForMatch(row.flatId);
		if (!hasText(row.flatId)) {
			errors.add("Flat Id is required");
		} else if (normalizedFlatId == null || !validFlatIds.contains(normalizedFlatId)) {
			errors.add("Flat Id not found for apartment");
		}
		if (row.dueFrom == null) {
			errors.add("Due From must be in d-MMM-yyyy format");
		}
		if (row.dueTill == null) {
			errors.add("Due Till must be in d-MMM-yyyy format");
		}
		if (row.dueFrom != null && row.dueTill != null && row.dueTill.isBefore(row.dueFrom)) {
			errors.add("Due Till cannot be before Due From");
		}
		if (!hasText(row.dueName)) {
			errors.add("Due Name is required");
		}
		if (!hasText(row.dueAmount)) {
			errors.add("Due Amount is required");
		} else if (!isNumeric(row.dueAmount)) {
			errors.add("Due Amount must be numeric");
		}
		if (hasText(row.penaltyAmount) && !isNumeric(row.penaltyAmount)) {
			errors.add("Penalty Amount must be numeric");
		}
		if (hasText(row.fineEligible)) {
			String normalizedFineEligible = row.fineEligible.trim().toUpperCase(Locale.ROOT);
			if (!"YES".equals(normalizedFineEligible) && !"NO".equals(normalizedFineEligible)) {
				errors.add("Fine Eligible must be Yes or No");
			}
			if ("YES".equals(normalizedFineEligible)) {
				if (!hasText(row.finePercent) || !isNumeric(row.finePercent)) {
					errors.add("Fine % must be numeric when Fine Eligible is Yes");
				}
				if (!isValidFineType(row.fineType)) {
					errors.add("Fine Type must be Simple or Cumulative");
				}
			}
		}
		if (hasText(row.finePercent) && !isNumeric(row.finePercent)) {
			errors.add("Fine % must be numeric");
		}
		if (hasText(row.fineType) && !isValidFineType(row.fineType)) {
			errors.add("Fine Type must be Simple or Cumulative");
		}
		return errors;
	}

	private CreatePaymentRequest buildPastDueCreatePaymentRequest(UploadPastDueRequest request, PastDueUploadRow row)
			throws Exception {
		CreatePaymentRequest createPaymentRequest = new CreatePaymentRequest();
		createPaymentRequest.setGenericHeader(request.getGenericHeader());
		createPaymentRequest.setPaymentName(row.dueName.trim());
		createPaymentRequest.setShortDetails(row.dueName.trim());
		createPaymentRequest.setCollectionStartDate(Date.valueOf(row.dueFrom));
		createPaymentRequest.setCollectionEndDate(Date.valueOf(row.dueTill));
		createPaymentRequest.setPaymentAmount(normalizeNumeric(row.dueAmount));
		createPaymentRequest.setGst("0");
		createPaymentRequest.setCurrency(SecuraConstants.PAYMENT_CURRENCY);
		createPaymentRequest.setPaymentCollectionCycleList(List.of(SecuraConstants.PAYMENT_CYCLE_ONCE));
		createPaymentRequest.setPaymentCollectionMode("PRE");
		createPaymentRequest.setApplicableFor(List.of(row.flatId.trim()));
		createPaymentRequest.setAllowedPaymentModes(List.of("ONLINE", "CASH", "CHEQUE"));
		createPaymentRequest.setPaymentType("MANDATORY");
		createPaymentRequest.setPartialPaymentAllowed(false);
		createPaymentRequest.setStatus(SecuraConstants.PAYMENT_STATUS_ACTIVE);
		createPaymentRequest.setPaymentCapita("PER_FLAT");
		createPaymentRequest.setCause(SecuraConstants.TRANSACTION_CAUSE_OTHERS);
		if (isFineEnabled(row)) {
			createPaymentRequest.setFineCode(createPastDueFineCode(request, row));
		}
		return createPaymentRequest;
	}

	private String createPastDueFineCode(UploadPastDueRequest request, PastDueUploadRow row) throws Exception {
		AddDiscfinRequest addDiscfinRequest = new AddDiscfinRequest();
		addDiscfinRequest.setGenericHeader(request.getGenericHeader());
		addDiscfinRequest.setDiscFnType(SecuraConstants.DISC_FN_TYPE_FINE);
		addDiscfinRequest.setDueDateAsStartDateFlag(Boolean.TRUE);
		addDiscfinRequest.setDiscFnMode(SecuraConstants.DISC_FN_MODE_PERCENTAGE);
		addDiscfinRequest.setDiscFnCumlatonCycle(SecuraConstants.DISC_FN_CYCLE_MONTHLY);
		addDiscfinRequest.setDiscFnCycleType(normalizeFineType(row.fineType));
		addDiscfinRequest.setDiscFnValue(normalizeNumeric(row.finePercent));
		addDiscfinRequest.setMinimumPaymentAmount("0");
		return discFinServices.addDiscfin(addDiscfinRequest).getDiscFnId();
	}

	private boolean isFineEnabled(PastDueUploadRow row) {
		return row != null && hasText(row.fineEligible) && "YES".equalsIgnoreCase(row.fineEligible.trim());
	}

	private String normalizeFineType(String fineType) {
		if (!isValidFineType(fineType)) {
			return SecuraConstants.DISC_FN_CYCLE_TYPE_SIMPLE;
		}
		return "CUMULATIVE".equalsIgnoreCase(fineType.trim()) ? SecuraConstants.DISC_FN_CYCLE_TYPE_CUMULATIVE
				: SecuraConstants.DISC_FN_CYCLE_TYPE_SIMPLE;
	}

	private boolean isValidFineType(String fineType) {
		if (!hasText(fineType)) {
			return false;
		}
		String normalized = fineType.trim().toUpperCase(Locale.ROOT);
		return "SIMPLE".equals(normalized) || "CUMULATIVE".equals(normalized);
	}

	private LocalDate parsePastDueDate(String value, String fieldName) {
		if (!hasText(value)) {
			return null;
		}
		try {
			return LocalDate.parse(value.trim(), PAST_DUE_DATE_FORMATTER);
		} catch (DateTimeParseException ex) {
			throw new IllegalArgumentException(fieldName + " must be in d-MMM-yyyy format");
		}
	}

	private String readPastDueStringCell(Row row, int columnIndex) {
		Cell cell = row.getCell(columnIndex, MissingCellPolicy.RETURN_BLANK_AS_NULL);
		if (cell == null) {
			return "";
		}
		String value = PAST_DUE_DATA_FORMATTER.formatCellValue(cell);
		return value == null ? "" : value.trim();
	}

	private boolean isPastDueRowBlank(Row row) {
		for (int i = 0; i < PAST_DUE_UPLOAD_HEADERS.length; i++) {
			if (hasText(readPastDueStringCell(row, i))) {
				return false;
			}
		}
		return true;
	}

	private List<String> buildPastDueFailedRow(PastDueUploadRow row, String reason) {
		List<String> values = new ArrayList<>();
		values.add(safePastDueValue(row.flatId));
		values.add(safePastDueValue(row.dueFromText));
		values.add(safePastDueValue(row.dueTillText));
		values.add(safePastDueValue(row.dueName));
		values.add(safePastDueValue(row.dueAmount));
		values.add(safePastDueValue(row.penaltyAmount));
		values.add(safePastDueValue(row.fineEligible));
		values.add(safePastDueValue(row.finePercent));
		values.add(safePastDueValue(row.fineType));
		values.add(safePastDueValue(reason));
		return values;
	}

	private List<String> buildPastDueFailedRow(Row row, String reason) {
		List<String> values = new ArrayList<>();
		for (int i = 0; i < PAST_DUE_UPLOAD_HEADERS.length; i++) {
			values.add(readPastDueStringCell(row, i));
		}
		values.add(safePastDueValue(reason));
		return values;
	}

	private String generatePastDueFailedRowsWorkbook(List<List<String>> failedRows) throws Exception {
		try (Workbook failedWorkbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Sheet failedSheet = failedWorkbook.createSheet("failed_rows");
			CellStyle headerStyle = createPastDueHeaderStyle(failedWorkbook);
			CellStyle reasonStyle = createPastDueReasonStyle(failedWorkbook);
			Row headerRow = failedSheet.createRow(0);
			for (int i = 0; i < PAST_DUE_UPLOAD_HEADERS.length; i++) {
				createPastDueStyledCell(headerRow, i, PAST_DUE_UPLOAD_HEADERS[i], headerStyle);
			}
			createPastDueStyledCell(headerRow, PAST_DUE_UPLOAD_HEADERS.length, "Reason", headerStyle);

			for (int rowIndex = 0; rowIndex < failedRows.size(); rowIndex++) {
				Row sheetRow = failedSheet.createRow(rowIndex + 1);
				List<String> rowValues = failedRows.get(rowIndex);
				for (int col = 0; col < rowValues.size(); col++) {
					Cell cell = sheetRow.createCell(col);
					cell.setCellValue(rowValues.get(col));
					if (col == PAST_DUE_UPLOAD_HEADERS.length) {
						cell.setCellStyle(reasonStyle);
					}
				}
			}
			setPastDueColumnWidths(failedSheet, PAST_DUE_UPLOAD_HEADERS.length + 1);
			failedWorkbook.write(outputStream);
			return Base64.getEncoder().encodeToString(outputStream.toByteArray());
		}
	}

	private CellStyle createPastDueHeaderStyle(Workbook workbook) {
		CellStyle headerStyle = workbook.createCellStyle();
		headerStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		Font headerFont = workbook.createFont();
		headerFont.setColor(IndexedColors.WHITE.getIndex());
		headerFont.setBold(true);
		headerStyle.setFont(headerFont);
		return headerStyle;
	}

	private CellStyle createPastDueReasonStyle(Workbook workbook) {
		CellStyle reasonStyle = workbook.createCellStyle();
		reasonStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
		reasonStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		Font reasonFont = workbook.createFont();
		reasonFont.setColor(IndexedColors.BLACK.getIndex());
		reasonFont.setBold(true);
		reasonStyle.setFont(reasonFont);
		return reasonStyle;
	}

	private void createPastDueStyledCell(Row row, int columnIndex, String value, CellStyle style) {
		Cell cell = row.createCell(columnIndex);
		cell.setCellValue(value);
		cell.setCellStyle(style);
	}

	private void setPastDueColumnWidths(Sheet sheet, int columnCount) {
		for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
			int maxTextLength = 1;
			for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
				Row row = sheet.getRow(rowIndex);
				if (row == null) {
					continue;
				}
				Cell cell = row.getCell(columnIndex, MissingCellPolicy.RETURN_BLANK_AS_NULL);
				if (cell == null) {
					continue;
				}
				maxTextLength = Math.max(maxTextLength, PAST_DUE_DATA_FORMATTER.formatCellValue(cell).length());
			}
			sheet.setColumnWidth(columnIndex, Math.min((maxTextLength + 2) * 256, 10000));
		}
	}

	private boolean isNumeric(String value) {
		try {
			new BigDecimal(value.trim());
			return true;
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	private String normalizeNumeric(String value) {
		return new BigDecimal(value.trim()).stripTrailingZeros().toPlainString();
	}

	private String stripDataUrlPrefix(String base64Value) {
		if (base64Value == null) {
			return "";
		}
		int commaIndex = base64Value.indexOf(',');
		if (commaIndex >= 0) {
			return base64Value.substring(commaIndex + 1);
		}
		return base64Value;
	}

	private String safePastDueValue(String value) {
		return value == null ? "" : value;
	}

	private String trimValue(String value) {
		return value == null ? null : value.trim();
	}

	private boolean isPerHeadCapita(String paymentCapita) {
		if (!hasText(paymentCapita)) {
			return false;
		}
		String normalized = paymentCapita.toUpperCase(Locale.ROOT).replaceAll("[\\s_-]", "");
		return "PERHEAD".equals(normalized);
	}

	private boolean isPerSqftCapita(String paymentCapita) {
		if (!hasText(paymentCapita)) {
			return false;
		}
		String normalized = paymentCapita.toUpperCase(Locale.ROOT).replaceAll("[\\s_-]", "");
		return "PERSQFT".equals(normalized);
	}

	private static class PastDueUploadRow {
		private String flatId;
		private String dueFromText;
		private String dueTillText;
		private String dueName;
		private String dueAmount;
		private String penaltyAmount;
		private String fineEligible;
		private String finePercent;
		private String fineType;
		private LocalDate dueFrom;
		private LocalDate dueTill;
	}

}
