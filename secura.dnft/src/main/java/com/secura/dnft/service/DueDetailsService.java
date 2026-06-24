package com.secura.dnft.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.dao.DiscFinRepository;
import com.secura.dnft.dao.DueAmountDetailsRepository;
import com.secura.dnft.dao.FlatRepository;
import com.secura.dnft.dao.PaymentRepository;
import com.secura.dnft.dao.TransactionRepository;
import com.secura.dnft.entity.DiscFin;
import com.secura.dnft.entity.DueAmountDetailsEntity;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.entity.Transaction;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.request.response.AddedCharges;
import com.secura.dnft.request.response.DueAmountDetails;
import com.secura.dnft.request.response.GenericHeader;

@Service
public class DueDetailsService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DueDetailsService.class);
	// Fallback when cycle metadata is invalid; monthly-equivalent duration.
	private static final long DEFAULT_CYCLE_DAYS = 30L;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private FlatRepository flatRepository;

	@Autowired
	private DiscFinRepository discFinRepository;

	@Autowired
	private DueAmountDetailsRepository dueAmountDetailsRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private GenericService genericService;

	public Map<String, List<Map<String, DueAmountDetails>>> calculateDuesForPayment(String paymentId) {
		return calculateDuesForPayment(paymentId, null);
	}

	public Map<String, List<Map<String, DueAmountDetails>>> calculateDuesForPayment(String paymentId, GenericHeader genericHeader) {
		LOGGER.info("calculateDuesForPayment called with paymentId={}, userId={}", paymentId,
				genericHeader != null ? genericHeader.getUserId() : null);
		List<PaymentEntity> paymentEntityList = paymentRepository.findByPaymentId(paymentId);
		return calculateDuesForPayment(paymentEntityList, genericHeader, true, true);
	}

	public Map<String, List<Map<String, DueAmountDetails>>> calculateDuesForPaymentWithoutDiscFine(String paymentId,
			GenericHeader genericHeader) {
		LOGGER.info("calculateDuesForPaymentWithoutDiscFine called with paymentId={}, userId={}", paymentId,
				genericHeader != null ? genericHeader.getUserId() : null);
		List<PaymentEntity> paymentEntityList = paymentRepository.findByPaymentId(paymentId);
		return calculateDuesForPayment(paymentEntityList, genericHeader, true, false);
	}

	/**
	 * Calculates due details using the same logic as persisted dues, but does not
	 * write due rows or flat pending payment links to the database.
	 */
	public Map<String, List<Map<String, DueAmountDetails>>> previewDuesForPayment(
			List<PaymentEntity> paymentEntityList, GenericHeader genericHeader) {
		return calculateDuesForPayment(paymentEntityList, genericHeader, false, true);
	}

	private Map<String, List<Map<String, DueAmountDetails>>> calculateDuesForPayment(
			List<PaymentEntity> paymentEntityList, GenericHeader genericHeader, boolean persistResults,
			boolean applyDiscFin) {
		Map<String, List<Map<String, DueAmountDetails>>> dueByCycle = new LinkedHashMap<>();
		if (paymentEntityList == null || paymentEntityList.isEmpty()) {
			return dueByCycle;
		}

		PaymentEntity baseEntity = paymentEntityList.get(0);
		String apartmentId = genericHeader != null && genericHeader.getApartmentId() != null
				&& !genericHeader.getApartmentId().trim().isEmpty() ? genericHeader.getApartmentId() : baseEntity.getAprmtId();
		List<Flat> apartmentFlats = flatRepository.findByAprmntId(baseEntity.getAprmtId());

		Set<String> visitedCycles = new LinkedHashSet<>();
		List<DueRow> generatedRows = new ArrayList<>();

		for (PaymentEntity paymentEntity : paymentEntityList) {
			List<Flat> applicableApartmentFlats = filterApplicableFlats(apartmentFlats, paymentEntity.getApplicableFor());
			Map<String, Long> flatTypeCounts = buildFlatTypeCounts(applicableApartmentFlats);
			for (String paymentCycle : resolvePaymentCycles(paymentEntity)) {
				if (!visitedCycles.add(normalizeCycle(paymentCycle))) {
					continue;
				}

				LocalDate startDate = paymentEntity.getCollectionStartDate();
				LocalDate endDate = paymentEntity.getCollectionEndDate();
				int cycleMonths = getCycleMonths(paymentCycle);
				List<LocalDate[]> intervals = generateCycleIntervals(startDate, endDate, cycleMonths, paymentCycle);

				List<Map<String, DueAmountDetails>> duesForCycle = new ArrayList<>();
				String dueId = createDueId();
				for (LocalDate[] interval : intervals) {
					Map<String, DueAmountDetails> duesByFlatType = createDuesForInterval(
							paymentEntity, dueId, flatTypeCounts, applicableApartmentFlats, interval[0], interval[1],
							paymentCycle, applyDiscFin);
					duesForCycle.add(duesByFlatType);
					duesByFlatType.forEach((flatType, details) -> generatedRows
							.add(new DueRow(paymentCycle, flatType, details, paymentEntity.getPaymentAmount(), interval[0])));
				}
				String estimatedCollectionAmount = calculateEstimatedCollectionAmount(duesForCycle, flatTypeCounts,
						applicableApartmentFlats.size(), paymentEntity.getPaymentCapita(),
						calculateCycleMultiplier(intervals, cycleMonths, paymentCycle));
				if (estimatedCollectionAmount != null) {
					for (Map<String, DueAmountDetails> duesByFlatType : duesForCycle) {
						for (DueAmountDetails details : duesByFlatType.values()) {
							details.setEstimatedCollectionAmount(estimatedCollectionAmount);
						}
					}
				}
				dueByCycle.put(paymentCycle, duesForCycle);
			}
		}

		if (persistResults) {
			List<DueAmountDetailsEntity> entityList = generatedRows.stream()
					.map(row -> toEntity(row, genericHeader != null ? genericHeader.getUserId() : null, apartmentId)).toList();
			dueAmountDetailsRepository.saveAll(entityList);
			appendDuesToFlatPendingPayments(apartmentFlats, entityList);
		}

		return dueByCycle;
	}

	private List<LocalDate[]> generateCycleIntervals(LocalDate startDate, LocalDate endDate, int cycleMonths,
			String paymentCycle) {
		List<LocalDate[]> intervals = new ArrayList<>();
		if (SecuraConstants.PAYMENT_CYCLE_ONCE.equals(normalizeCycle(paymentCycle))) {
			intervals.add(new LocalDate[]{startDate, endDate});
			return intervals;
		}
		if (startDate == null || cycleMonths <= 0) {
			intervals.add(new LocalDate[]{startDate, endDate});
			return intervals;
		}
		if (endDate == null) {
			intervals.add(new LocalDate[]{startDate, startDate.plusMonths(cycleMonths).minusDays(1)});
			return intervals;
		}
		LocalDate intervalStart = startDate;
		while (!intervalStart.isAfter(endDate)) {
			LocalDate naturalIntervalEnd = intervalStart.plusMonths(cycleMonths).minusDays(1);
			LocalDate intervalEnd = naturalIntervalEnd.isAfter(endDate) ? endDate : naturalIntervalEnd;
			intervals.add(new LocalDate[]{intervalStart, intervalEnd});
			intervalStart = intervalEnd.plusDays(1);
		}
		return intervals;
	}

	private Map<String, DueAmountDetails> createDuesForInterval(PaymentEntity paymentEntity, String dueId,
			Map<String, Long> flatTypeCounts, List<Flat> apartmentFlats, LocalDate intervalStart, LocalDate intervalEnd,
			String paymentCycle, boolean applyDiscFin) {
		Map<String, DueAmountDetails> duesByFlatType = new LinkedHashMap<>();
		boolean perSqft = isPerSqft(paymentEntity.getPaymentCapita());
		if (perSqft) {
			for (String flatType : flatTypeCounts.keySet()) {
				DueAmountDetails due = buildDueDetails(paymentEntity, dueId, flatType, parseNumeric(flatType),
						intervalStart, intervalEnd, paymentCycle, applyDiscFin);
				due.setApplicableFlats(getApplicableFlatNos(apartmentFlats, flatType));
				duesByFlatType.put(flatType, due);
			}
			return duesByFlatType;
		}
		DueAmountDetails due = buildDueDetails(paymentEntity, dueId, "ALL", BigDecimal.ONE, intervalStart, intervalEnd,
				paymentCycle, applyDiscFin);
		due.setApplicableFlats(getApplicableFlatNos(apartmentFlats, null));
		duesByFlatType.put("ALL", due);
		return duesByFlatType;
	}

	private List<String> getApplicableFlatNos(List<Flat> apartmentFlats, String flatArea) {
		if (apartmentFlats == null || apartmentFlats.isEmpty()) {
			return new ArrayList<>();
		}
		List<String> flatNos = new ArrayList<>();
		for (Flat flat : apartmentFlats) {
			if (flat == null || flat.getFlatNo() == null) {
				continue;
			}
			if (flatArea == null || flatArea.equalsIgnoreCase("ALL")
					|| flatArea.equals(normalizeFlatArea(flat.getFlatArea()))) {
				flatNos.add(flat.getFlatNo());
			}
		}
		return flatNos;
	}

	private DueAmountDetails buildDueDetails(PaymentEntity paymentEntity, String dueId, String flatTypeKey,
			BigDecimal areaMultiplier, LocalDate intervalStart, LocalDate intervalEnd, String paymentCycle,
			boolean applyDiscFin) {
		LOGGER.debug("buildDueDetails: paymentId={}, dueId={}, flatTypeKey={}, areaMultiplier={}, intervalStart={}, intervalEnd={}",
				paymentEntity.getPaymentId(), dueId, flatTypeKey, areaMultiplier, intervalStart, intervalEnd);
		DueAmountDetails due = new DueAmountDetails();
		due.setPaymentId(paymentEntity.getPaymentId());
		due.setDueId(dueId);
		due.setCollectionCycle(paymentCycle);
		due.setDueDate(calculateDueDate(intervalStart, intervalEnd, paymentCycle,
				paymentEntity.getPaymentCollectionMode()));
		due.setDueEndDate(intervalEnd);

		BigDecimal amount = calculateAmount(paymentEntity, intervalStart, intervalEnd, areaMultiplier, paymentCycle);
		DiscFinReference discFinReference = applyDiscFin ? extractDiscFinReference(paymentEntity.getDiscFin())
				: new DiscFinReference(null, null);
		DiscFin discountDiscFin = applyDiscFin ? resolveDiscFin(discFinReference.discountCode(), paymentCycle) : null;
		BigDecimal discountedAmount = applyDiscFin ? calculateDiscountAmount(discountDiscFin, amount) : BigDecimal.ZERO;
		BigDecimal baseAmount = amount.subtract(discountedAmount);
		if (baseAmount.compareTo(BigDecimal.ZERO) < 0) {
			baseAmount = BigDecimal.ZERO;
		}
		BigDecimal gstPercentage = parseNumeric(paymentEntity.getGst());
		BigDecimal gstAmount = calculatePercentageAmount(baseAmount, gstPercentage);

		List<AddedCharges> addedCharges = parseAddedCharges(paymentEntity.getAddedCharges());
		BigDecimal totalAddedCharges = applyAddedChargesAndCalculateTotal(addedCharges, baseAmount);
		PenaltyCalculationResult penaltyResult = applyDiscFin
				? calculatePenaltyAmount(paymentEntity, baseAmount, due)
				: new PenaltyCalculationResult(null, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
		DiscFin fineDiscFin = penaltyResult.discFin();
		String discountValue = discountDiscFin != null ? format(discountDiscFin.getDiscFinValue()) : null;
		String fineValue = fineDiscFin != null ? format(fineDiscFin.getDiscFinValue()) : null;
		BigDecimal fineAmount = penaltyResult.amount();
		LOGGER.debug("buildDueDetails result: paymentId={}, dueId={}, amount={}, discountedAmount={}, baseAmount={}, gstAmount={}, fineAmount={}",
				paymentEntity.getPaymentId(), dueId, amount, discountedAmount, baseAmount, gstAmount, fineAmount);
		BigDecimal computedTotal = baseAmount.add(gstAmount).add(totalAddedCharges).add(fineAmount);
		BigDecimal roundedTotal = computedTotal.setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
		BigDecimal roundUpAmount = roundedTotal.subtract(computedTotal).setScale(2, RoundingMode.HALF_UP);

		due.setAmount(format(amount));
		due.setGstAmount(format(gstAmount));
		due.setTotalAmount(format(roundedTotal));
		due.setPaymentName(paymentEntity.getPaymentName());
		due.setPaymentType(paymentEntity.getPaymentType());
		due.setAllowedPaymentModes(parseAllowedPaymentModes(paymentEntity.getAllowedPaymentModes()));
		due.setPaymentCapita(paymentEntity.getPaymentCapita());
		due.setAddedCharges(addedCharges);
		due.setTotalAddedCharges(format(totalAddedCharges));
		due.setGstPercentage(format(gstPercentage));
		due.setDiscountCode(discountDiscFin != null ? discFinReference.discountCode() : null);
		due.setFineCode(fineDiscFin != null ? fineDiscFin.getDiscFnId() : discFinReference.fineCode());
		due.setDiscountMode(discountDiscFin != null ? discountDiscFin.getDiscFnMode() : null);
		due.setCummilationCycle(fineDiscFin != null ? fineDiscFin.getDiscFnCumlatonCycle() : null);
		due.setDiscValue(discountValue);
		due.setDiscountedAmount(format(discountedAmount));
		due.setFineAmount(format(fineAmount));
		due.setFineMode(fineDiscFin != null ? fineDiscFin.getDiscFnMode() : null);
		due.setFineType(fineDiscFin != null ? fineDiscFin.getFnCalculationType() : "");
		due.setRoundUpAmount(format(roundUpAmount));
		due.setAlreadyPaidAmount(format(BigDecimal.ZERO));
		due.setAdminDiscount(format(BigDecimal.ZERO));
		due.setCause(paymentEntity.getCauseId());
		due.setFnValue(fineValue);
		return due;
	}

	private DueAmountDetailsEntity toEntity(DueRow dueRow, String userId, String apartmentId) {
		DueAmountDetails due = dueRow.dueAmountDetails();
		DueAmountDetailsEntity entity = new DueAmountDetailsEntity();
		entity.setAprmntId(apartmentId);
		entity.setDueId(due.getDueId());
		entity.setCollectionCycle(due.getCollectionCycle());
		entity.setFlatArea(dueRow.flatType());
		entity.setDueDate(due.getDueDate());
		entity.setDueEndDate(due.getDueEndDate());
		entity.setDueStartDate(dueRow.dueStartDate());
		entity.setPaymentId(due.getPaymentId());
		entity.setAmount(due.getAmount());
		entity.setGstAmount(due.getGstAmount());
		entity.setTotalAmount(due.getTotalAmount());
		entity.setPaymentName(due.getPaymentName());
		entity.setPaymentType(due.getPaymentType());
		entity.setCause(due.getCause());
		entity.setPaymentCapita(due.getPaymentCapita());
		entity.setAmountPerMonth(dueRow.amountPerMonth());
		entity.setAddedCharges(genericService.toJson(due.getAddedCharges() != null ? due.getAddedCharges() : List.of()));
		entity.setTotalAddedCharges(due.getTotalAddedCharges());
		entity.setEstimatedCollectionAmount(due.getEstimatedCollectionAmount());
		entity.setGstPercentage(due.getGstPercentage());
		entity.setDiscountCode(due.getDiscountCode());
		entity.setDiscountMode(due.getDiscountMode());
		entity.setCummilationCycle(due.getCummilationCycle());
		entity.setFineCode(due.getFineCode());
		entity.setDiscValue(due.getDiscValue());
		entity.setDiscountedAmount(due.getDiscountedAmount());
		entity.setFineAmount(defaultZeroValue(due.getFineAmount()));
		entity.setFineMode(due.getFineMode());
		entity.setFineType(due.getFineType());
		entity.setRoundUpAmount(due.getRoundUpAmount());
		entity.setAlreadyPaidAmount(defaultZeroValue(due.getAlreadyPaidAmount()));
		entity.setAdminDiscount(defaultZeroValue(due.getAdminDiscount()));
		entity.setApplicableFlats(genericService.toJson(due.getApplicableFlats() != null ? due.getApplicableFlats() : List.of()));
		entity.setAllowedTenders(serializeAllowedTenders(due.getAllowedPaymentModes()));
		entity.setPaymentStatus(due.getPaymentStatus());
		entity.setPaymentDate(due.getPaymentDate());
		entity.setCreatUsrId(userId);
		entity.setLstUpdtUsrId(null);
		entity.setFnValue(due.getFnValue());
		return entity;
	}

	private Map<String, Long> buildFlatTypeCounts(List<Flat> apartmentFlats) {
		Map<String, Long> flatTypeCounts = new LinkedHashMap<>();
		if (apartmentFlats == null || apartmentFlats.isEmpty()) {
			return flatTypeCounts;
		}
		for (Flat flat : apartmentFlats) {
			String flatArea = normalizeFlatArea(flat != null ? flat.getFlatArea() : null);
			flatTypeCounts.put(flatArea, flatTypeCounts.getOrDefault(flatArea, 0L) + 1L);
		}
		return flatTypeCounts;
	}

	private List<Flat> filterApplicableFlats(List<Flat> apartmentFlats, String applicableFor) {
		if (apartmentFlats == null || apartmentFlats.isEmpty()) {
			return new ArrayList<>();
		}
		Set<String> applicableFlatNos = parseApplicableFlatNos(applicableFor);
		if (applicableFlatNos.isEmpty()) {
			return new ArrayList<>(apartmentFlats);
		}
		return apartmentFlats.stream().filter(Objects::nonNull)
				.filter(flat -> applicableFlatNos.contains(normalizeFlatNo(flat.getFlatNo()))).toList();
	}

	private void appendDuesToFlatPendingPayments(List<Flat> apartmentFlats, Collection<DueAmountDetailsEntity> dueEntities) {
		if (apartmentFlats == null || apartmentFlats.isEmpty() || dueEntities == null || dueEntities.isEmpty()) {
			return;
		}
		List<Flat> updatedFlats = new ArrayList<>();
		for (Flat flat : apartmentFlats) {
			List<String> existingDueIds = parseStringList(flat.getFlatPndngPaymntLst());
			String normalizedFlatArea = normalizeFlatArea(flat.getFlatArea());
			String normalizedFlatNo = normalizeFlatNo(flat.getFlatNo());
			boolean modified = false;
			for (DueAmountDetailsEntity dueEntity : dueEntities) {
				if (!isDueApplicableToFlat(dueEntity, normalizedFlatArea, normalizedFlatNo)) {
					continue;
				}
				String dueEntityKey = buildFlatPendingDueKey(dueEntity);
				if (dueEntityKey == null) {
					continue;
				}
				if (!existingDueIds.contains(dueEntityKey)) {
					existingDueIds.add(dueEntityKey);
					modified = true;
				}
			}
			if (modified) {
				flat.setFlatPndngPaymntLst(genericService.toJson(existingDueIds));
				updatedFlats.add(flat);
			}
		}
		if (!updatedFlats.isEmpty()) {
			flatRepository.saveAll(updatedFlats);
		}
	}

	private boolean isDueApplicableToFlat(DueAmountDetailsEntity dueEntity, String normalizedFlatArea, String normalizedFlatNo) {
		if (dueEntity == null || normalizedFlatNo == null) {
			return false;
		}
		String dueFlatArea = dueEntity.getFlatArea();
		if (dueFlatArea == null || dueFlatArea.isBlank()) {
			return false;
		}
		String normalizedDueFlatArea = dueFlatArea.trim();
		boolean flatAreaMatches = "ALL".equalsIgnoreCase(normalizedDueFlatArea) || normalizedDueFlatArea.equals(normalizedFlatArea);
		if (!flatAreaMatches) {
			return false;
		}
		List<String> applicableFlats = parseStringList(dueEntity.getApplicableFlats());
		if (applicableFlats.isEmpty()) {
			return true;
		}
		return applicableFlats.stream().map(this::normalizeFlatNo).filter(Objects::nonNull)
				.anyMatch(normalizedFlatNo::equals);
	}

	private String buildFlatPendingDueKey(DueAmountDetailsEntity dueEntity) {
		if (dueEntity == null || dueEntity.getDueDate() == null) {
			return null;
		}
		return dueEntity.getDueId() + "_"
				+ stringValue(dueEntity.getCollectionCycle()) + "_"
				+ stringValue(dueEntity.getFlatArea()) + "_"
				+ dueEntity.getDueDate();
	}

	private List<String> parseStringList(String json) {
		if (json == null || json.isBlank()) {
			return new ArrayList<>();
		}
		try {
			List<String> values = genericService.fromJson(json, new TypeReference<List<String>>() {
			});
			return values == null ? new ArrayList<>() : new ArrayList<>(values);
		} catch (Exception ex) {
			return new ArrayList<>();
		}
	}

	private Set<String> parseApplicableFlatNos(String applicableFor) {
		if (applicableFor == null || applicableFor.isBlank()) {
			return new LinkedHashSet<>();
		}
		String trimmedApplicableFor = applicableFor.trim();
		if ("ALL".equalsIgnoreCase(trimmedApplicableFor)) {
			return new LinkedHashSet<>();
		}
		try {
			List<String> values = genericService.fromJson(trimmedApplicableFor, new TypeReference<List<String>>() {
			});
			if (values == null || values.isEmpty()) {
				return new LinkedHashSet<>();
			}
			return values.stream().map(this::normalizeFlatNo).filter(Objects::nonNull)
					.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
		} catch (Exception exception) {
			Set<String> flatNos = new LinkedHashSet<>();
			for (String value : trimmedApplicableFor.split(",")) {
				String normalizedFlatNo = normalizeFlatNo(value);
				if (normalizedFlatNo != null) {
					flatNos.add(normalizedFlatNo);
				}
			}
			return flatNos;
		}
	}

	private String calculateEstimatedCollectionAmount(List<Map<String, DueAmountDetails>> duesForCycle,
			Map<String, Long> flatTypeCounts, int flatCount, String paymentCapita, BigDecimal cycleMultiplier) {
		if (duesForCycle == null || duesForCycle.isEmpty()) {
			return null;
		}
		Map<String, DueAmountDetails> anyCycleDues = duesForCycle.get(0);
		BigDecimal safeCycleMultiplier = cycleMultiplier;
		if (safeCycleMultiplier == null || safeCycleMultiplier.compareTo(BigDecimal.ZERO) <= 0) {
			LOGGER.warn("Invalid cycle multiplier {}. Falling back to 1 for estimated collection calculation.",
					safeCycleMultiplier);
			safeCycleMultiplier = BigDecimal.ONE;
		}
		if (isPerSqft(paymentCapita)) {
			BigDecimal total = BigDecimal.ZERO;
			for (Map.Entry<String, DueAmountDetails> entry : anyCycleDues.entrySet()) {
				BigDecimal typeAmount = parseNumeric(entry.getValue().getTotalAmount());
				long count = flatTypeCounts.getOrDefault(entry.getKey(), 0L);
				total = total.add(typeAmount.multiply(BigDecimal.valueOf(count)));
			}
			return format(total.multiply(safeCycleMultiplier));
		}
		if (isPerHead(paymentCapita)) {
			return format(BigDecimal.ZERO);
		}
		BigDecimal amountPerUnit = anyCycleDues.values().stream()
				.findFirst()
				.map(DueAmountDetails::getTotalAmount)
				.map(this::parseNumeric)
				.orElse(BigDecimal.ZERO);
		return format(amountPerUnit.multiply(BigDecimal.valueOf(Math.max(flatCount, 0))).multiply(safeCycleMultiplier));
	}

	private BigDecimal calculateCycleMultiplier(List<LocalDate[]> intervals, int cycleMonths, String paymentCycle) {
		if (intervals == null || intervals.isEmpty() || SecuraConstants.PAYMENT_CYCLE_ONCE.equals(normalizeCycle(paymentCycle))) {
			return BigDecimal.ONE;
		}
		if (cycleMonths <= 0) {
			return BigDecimal.valueOf(intervals.size());
		}
		BigDecimal totalCycles = BigDecimal.ZERO;
		for (LocalDate[] interval : intervals) {
			if (interval == null || interval.length < 2) {
				continue;
			}
			LocalDate intervalStart = interval[0];
			LocalDate intervalEnd = interval[1];
			if (intervalStart == null || intervalEnd == null || intervalStart.isAfter(intervalEnd)) {
				continue;
			}
			LocalDate naturalIntervalEnd = intervalStart.plusMonths(cycleMonths).minusDays(1);
			long totalDays = ChronoUnit.DAYS.between(intervalStart, naturalIntervalEnd) + 1;
			long activeDays = ChronoUnit.DAYS.between(intervalStart, intervalEnd) + 1;
			if (totalDays <= 0 || activeDays <= 0) {
				continue;
			}
			BigDecimal intervalFraction = BigDecimal.valueOf(activeDays).divide(BigDecimal.valueOf(totalDays), 8,
					RoundingMode.HALF_UP);
			totalCycles = totalCycles.add(intervalFraction);
		}
		if (totalCycles.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ONE;
		}
		return totalCycles.setScale(2, RoundingMode.HALF_UP);
	}

	private List<AddedCharges> parseAddedCharges(String addedChargesJson) {
		if (addedChargesJson == null || addedChargesJson.isBlank()) {
			return new ArrayList<>();
		}
		try {
			List<AddedCharges> charges = genericService.fromJson(addedChargesJson, new TypeReference<List<AddedCharges>>() {
			});
			return charges == null ? new ArrayList<>() : charges;
		} catch (Exception ex) {
			return new ArrayList<>();
		}
	}

	private List<String> parseAllowedPaymentModes(String allowedModesJson) {
		if (allowedModesJson == null || allowedModesJson.isBlank()) {
			return new ArrayList<>();
		}
		try {
			List<String> modes = genericService.fromJson(allowedModesJson, new TypeReference<List<String>>() {
			});
			return modes == null ? new ArrayList<>() : modes;
		} catch (Exception ex) {
			return new ArrayList<>();
		}
	}

	private String serializeAllowedTenders(List<String> allowedTenders) {
		if (allowedTenders == null || allowedTenders.isEmpty()) {
			return null;
		}
		return genericService.toJson(allowedTenders);
	}

	private BigDecimal applyAddedChargesAndCalculateTotal(List<AddedCharges> addedCharges, BigDecimal baseAmount) {
		BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		for (AddedCharges addedCharge : addedCharges) {
			if (addedCharge == null) {
				continue;
			}
			BigDecimal value = parseNumeric(addedCharge.getValue());
			BigDecimal finalValue;
			if ("percentage".equalsIgnoreCase(addedCharge.getChargeType())) {
				finalValue = calculatePercentageAmount(baseAmount, value);
			} else {
				finalValue = value.setScale(2, RoundingMode.HALF_UP);
			}
			addedCharge.setFinalChargeValue(format(finalValue));
			total = total.add(finalValue);
		}
		return total.setScale(2, RoundingMode.HALF_UP);
	}

	private DiscFinReference extractDiscFinReference(String discFinJson) {
		if (discFinJson == null || discFinJson.isBlank()) {
			return new DiscFinReference(null, null);
		}
		try {
			List<DiscFinTagEntry> entries = genericService.fromJson(discFinJson,
					new TypeReference<List<DiscFinTagEntry>>() {
					});
			String discountCode = null;
			String fineCode = null;
			for (DiscFinTagEntry entry : entries) {
				if (entry == null) {
					continue;
				}
				String type = stringValue(entry.getDistfinType());
				String code = stringValue(entry.getCode());
				String status = stringValue(entry.getStatus());
				boolean isActive = isStatusActive(status);
				if (!isActive) {
					continue;
				}
				if (SecuraConstants.DISC_FN_TYPE_DISCOUNT.equalsIgnoreCase(type)) {
					discountCode = code;
				} else if (SecuraConstants.DISC_FN_TYPE_FINE.equalsIgnoreCase(type)) {
					fineCode = code;
				}
			}
			return new DiscFinReference(discountCode, fineCode);
		} catch (Exception ex) {
			return new DiscFinReference(null, null);
		}
	}

	private DiscFin resolveDiscFin(String code, String paymentCycle) {
		if (code == null || code.isBlank()) {
			return null;
		}
		List<DiscFin> discFins = discFinRepository.findByDiscFnId(code);
		if (discFins == null || discFins.isEmpty()) {
			return null;
		}
		String normalizedCycle = normalizeCycle(paymentCycle);
		DiscFin cycleSpecificDiscFin = discFins.stream().filter(Objects::nonNull)
				.filter(discFin -> normalizedCycle.equals(normalizeCycle(discFin.getDiscFnCycleType()))).findFirst()
				.orElse(null);
		if (cycleSpecificDiscFin != null) {
			return cycleSpecificDiscFin;
		}
		String fixedCycle = normalizeCycle(SecuraConstants.DISC_FN_CYCLE_FIXED);
		return discFins.stream().filter(Objects::nonNull)
				.filter(discFin -> fixedCycle.equals(normalizeCycle(discFin.getDiscFnCycleType()))).findFirst()
				.orElse(null);
	}

	private BigDecimal calculateDiscountAmount(DiscFin discountDiscFin, BigDecimal amount) {
		if (discountDiscFin == null) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		LocalDate today = LocalDate.now();
		LocalDate discountStart = discountDiscFin.getDiscFnStrtDt();
		LocalDate discountEnd = discountDiscFin.getDiscFnEndDt();
		if (discountStart != null && today.isBefore(discountStart)) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		if (discountEnd != null && today.isAfter(discountEnd)) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}

		BigDecimal minimumPaymentAmount = parseNumeric(discountDiscFin.getMinimumPaymentAmount());
		if (minimumPaymentAmount.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(minimumPaymentAmount) < 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}

		BigDecimal discountValue = parseNumeric(discountDiscFin.getDiscFinValue());
		if (SecuraConstants.DISC_FN_MODE_PERCENTAGE.equalsIgnoreCase(discountDiscFin.getDiscFnMode())) {
			return calculatePercentageAmount(amount, discountValue);
		}
		return discountValue.setScale(2, RoundingMode.HALF_UP);
	}

	private PenaltyCalculationResult calculatePenaltyAmount(PaymentEntity paymentEntity, BigDecimal baseAmount, DueAmountDetails due) {
		if (paymentEntity == null || due == null || baseAmount == null || baseAmount.compareTo(BigDecimal.ZERO) <= 0) {
			return new PenaltyCalculationResult(null, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
		}
		String fineCode = resolveActiveFineCode(paymentEntity.getDiscFin());
		if (fineCode == null || fineCode.isBlank()) {
			return new PenaltyCalculationResult(null, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
		}
		List<DiscFin> discFins = discFinRepository.findByDiscFnId(fineCode);
		if (discFins == null || discFins.isEmpty()) {
			return new PenaltyCalculationResult(null, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
		}
		boolean isFixedFine = discFins.size() == 1
				&& normalizeCycle(discFins.get(0).getDiscFnCycleType()).equals(normalizeCycle(SecuraConstants.DISC_FN_CYCLE_FIXED));
		DiscFin applicableDiscFin;
		if (isFixedFine) {
			applicableDiscFin = discFins.get(0);
		} else {
			String dueCycle = normalizeCycle(due.getCollectionCycle());
			applicableDiscFin = discFins.stream().filter(Objects::nonNull)
					.filter(discFin -> dueCycle.equals(normalizeCycle(discFin.getDiscFnCycleType()))).findFirst().orElse(null);
			if (applicableDiscFin == null || !isBufferTimeElapsed(applicableDiscFin, due.getDueDate())) {
				return new PenaltyCalculationResult(null, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
			}
		}
		return new PenaltyCalculationResult(applicableDiscFin, calculatePenalty(applicableDiscFin, baseAmount, due));
	}

	private BigDecimal calculatePenalty(DiscFin fineDiscFin, BigDecimal baseAmount, DueAmountDetails due) {
		LOGGER.debug("calculatePenalty: fineDiscFin={}, baseAmount={}, dueDate={}",
				fineDiscFin != null ? fineDiscFin.getDiscFnId() : null, baseAmount, due != null ? due.getDueDate() : null);
		if (fineDiscFin == null || due == null || baseAmount == null || baseAmount.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		LocalDate dueDate = due.getDueDate();
		LocalDate today = LocalDate.now();
		LocalDate fineStart = Boolean.TRUE.equals(fineDiscFin.getDueDateAsStartDateFlag()) && dueDate != null ? dueDate
				: fineDiscFin.getDiscFnStrtDt();
		LocalDate fineEnd = fineDiscFin.getDiscFnEndDt();
		if (fineStart != null && today.isBefore(fineStart)) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		if (fineEnd != null && today.isAfter(fineEnd)) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}

		BigDecimal outstanding = baseAmount.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
		if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}

		BigDecimal fineValue = parseNumeric(fineDiscFin.getDiscFinValue());
		if (SecuraConstants.DISC_FN_MODE_AMOUNT.equalsIgnoreCase(fineDiscFin.getDiscFnMode())) {
			return fineValue.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
		}
		if (!SecuraConstants.DISC_FN_MODE_PERCENTAGE.equalsIgnoreCase(fineDiscFin.getDiscFnMode())) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}

		LocalDate penaltyStart = fineStart != null ? fineStart : dueDate;
		if (penaltyStart == null || !penaltyStart.isBefore(today)) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}

		List<Transaction> transactions = getSuccessfulTransactionsAfterDueDate(due);
		Map<LocalDate, BigDecimal> paymentByDate = aggregatePaymentsByDate(transactions, due);
		for (Map.Entry<LocalDate, BigDecimal> entry : paymentByDate.entrySet()) {
			if (!entry.getKey().isAfter(penaltyStart)) {
				outstanding = outstanding.subtract(entry.getValue());
			}
		}
		if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}

		LocalDate cursor = penaltyStart;
		BigDecimal totalPenalty = BigDecimal.ZERO;
		int cycleMonths = getFineCycleMonths(fineDiscFin.getDiscFnCumlatonCycle());
		if (cycleMonths <= 0) {
			cycleMonths = 1;
		}
		boolean partCycleAsFull = Boolean.TRUE.equals(fineDiscFin.getPartOfCycleAsFull());
		BigDecimal rate = fineValue.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
		for (Map.Entry<LocalDate, BigDecimal> entry : paymentByDate.entrySet()) {
			LocalDate paymentDate = entry.getKey();
			if (!paymentDate.isAfter(cursor)) {
				continue;
			}
			LocalDate segmentEnd = paymentDate.isAfter(today) ? today : paymentDate;
			if (segmentEnd.isAfter(cursor)) {
				BigDecimal segmentPenalty = calculateSegmentPenalty(outstanding, rate, cursor, segmentEnd, cycleMonths,
						partCycleAsFull, isCumulativeFine(fineDiscFin.getFnCalculationType()));
				totalPenalty = totalPenalty.add(segmentPenalty);
				if (isCumulativeFine(fineDiscFin.getFnCalculationType())) {
					outstanding = outstanding.add(segmentPenalty);
				}
			}
			outstanding = outstanding.subtract(entry.getValue());
			if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
				return totalPenalty.setScale(2, RoundingMode.HALF_UP);
			}
			cursor = paymentDate;
			if (!cursor.isBefore(today)) {
				return totalPenalty.setScale(2, RoundingMode.HALF_UP);
			}
		}

		if (cursor.isBefore(today) && outstanding.compareTo(BigDecimal.ZERO) > 0) {
			BigDecimal segmentPenalty = calculateSegmentPenalty(outstanding, rate, cursor, today, cycleMonths,
					partCycleAsFull, isCumulativeFine(fineDiscFin.getFnCalculationType()));
			totalPenalty = totalPenalty.add(segmentPenalty);
		}
		return totalPenalty.setScale(2, RoundingMode.HALF_UP);
	}

	private boolean isCumulativeFine(String fnCalculationType) {
		if (fnCalculationType == null) {
			return false;
		}
		String normalized = fnCalculationType.toUpperCase(Locale.ROOT).replaceAll("[\\s_-]", "");
		return normalized.equals(SecuraConstants.DISC_FN_CYCLE_TYPE_CUMULATIVE)
				|| normalized.equals(SecuraConstants.DISC_FN_CYCLE_TYPE_CUMMULATIVE)
				|| normalized.equals(SecuraConstants.DISC_FN_CYCLE_TYPE_CUMMILATIVE)
				|| normalized.equals(SecuraConstants.DISC_FN_CYCLE_TYPE_CUMILATIVE);
	}

	private String resolveActiveFineCode(String discFinJson) {
		if (discFinJson == null || discFinJson.isBlank()) {
			return null;
		}
		try {
			List<DiscFinTagEntry> entries = genericService.fromJson(discFinJson, new TypeReference<List<DiscFinTagEntry>>() {
			});
			if (entries == null || entries.isEmpty()) {
				return null;
			}
			return entries.stream().filter(Objects::nonNull)
					.filter(entry -> SecuraConstants.DISC_FN_TYPE_FINE.equalsIgnoreCase(stringValue(entry.getDistfinType())))
					.filter(entry -> {
						String status = stringValue(entry.getStatus());
						return isStatusActive(status);
					})
					.map(DiscFinTagEntry::getCode).filter(Objects::nonNull).findFirst().orElse(null);
		} catch (Exception ex) {
			return null;
		}
	}

	private boolean isBufferTimeElapsed(DiscFin discFin, LocalDate dueDate) {
		if (discFin == null || dueDate == null) {
			return false;
		}
		BigDecimal bufferValue = parseNumeric(discFin.getBufferTime());
		if (bufferValue.compareTo(BigDecimal.ZERO) <= 0) {
			return LocalDate.now().isAfter(dueDate);
		}
		int bufferUnits = bufferValue.intValue();
		LocalDate thresholdDate = dueDate;
		String unit = stringValue(discFin.getBufferTimeUnit());
		if ("MONTH".equalsIgnoreCase(unit)) {
			thresholdDate = thresholdDate.plusMonths(bufferUnits);
		} else {
			thresholdDate = thresholdDate.plusDays(bufferUnits);
		}
		return LocalDate.now().isAfter(thresholdDate);
	}

	private List<Transaction> getSuccessfulTransactionsAfterDueDate(DueAmountDetails due) {
		if (due == null || due.getDueDate() == null || due.getPaymentId() == null || due.getPaymentId().isBlank()) {
			return List.of();
		}
		List<Transaction> transactions = transactionRepository.findByPymntIdAndTrnsStatusOrderByTrnsDateAsc(
				due.getPaymentId(), SecuraConstants.TRANSACTION_STATUS_SUCCESS);
		if (transactions == null || transactions.isEmpty()) {
			return List.of();
		}
		LocalDate dueDate = due.getDueDate();
		return transactions.stream().filter(Objects::nonNull).filter(transaction -> transaction.getTrnsDate() != null)
				.filter(transaction -> transaction.getTrnsDate().toLocalDate().isAfter(dueDate))
				.filter(transaction -> isTransactionForDue(transaction, due))
				.sorted(Comparator.comparing(Transaction::getTrnsDate)).toList();
	}

	private Map<LocalDate, BigDecimal> aggregatePaymentsByDate(List<Transaction> transactions, DueAmountDetails due) {
		Map<LocalDate, BigDecimal> paymentsByDate = new LinkedHashMap<>();
		if (transactions == null || transactions.isEmpty() || due == null) {
			return paymentsByDate;
		}
		for (Transaction transaction : transactions) {
			LocalDate transactionDate = transaction != null && transaction.getTrnsDate() != null
					? transaction.getTrnsDate().toLocalDate()
					: null;
			if (transactionDate == null) {
				continue;
			}
			BigDecimal amount = parseNumeric(transaction.getTrnsAmt());
			if (amount.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			paymentsByDate.merge(transactionDate, amount, BigDecimal::add);
		}
		return paymentsByDate;
	}

	private boolean isTransactionForDue(Transaction transaction, DueAmountDetails due) {
		if (transaction == null || due == null || transaction.getDueDetails() == null || transaction.getDueDetails().isBlank()) {
			return false;
		}
		if (due.getDueId() == null || due.getCollectionCycle() == null || due.getDueDate() == null) {
			return false;
		}
		return matchesDueDetailsKey(transaction.getDueDetails().trim(), due.getDueId(), due.getCollectionCycle(), due.getDueDate());
	}

	private boolean matchesDueDetailsKey(String dueDetails, String dueId, String collectionCycle, LocalDate dueDate) {
		if (dueDetails == null || dueId == null || collectionCycle == null || dueDate == null) {
			return false;
		}
		String duePrefix = dueId + "_" + collectionCycle + "_";
		String dueDateToken = dueDate.toString();
		return dueDetails.startsWith(duePrefix) && dueDetails.endsWith("_" + dueDateToken);
	}

	private boolean isStatusActive(String status) {
		return SecuraConstants.DISC_FIN_STATUS_ACTIVE.equalsIgnoreCase(stringValue(status));
	}

	private BigDecimal calculateSegmentPenalty(BigDecimal outstanding, BigDecimal rate, LocalDate start, LocalDate end,
			int cycleMonths, boolean partCycleAsFull, boolean cumulative) {
		BigDecimal cycleUnits = calculateCycleUnits(start, end, cycleMonths, partCycleAsFull);
		if (cycleUnits.compareTo(BigDecimal.ZERO) <= 0 || outstanding.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		if (cumulative) {
			double exponent = cycleUnits.doubleValue();
			double factor = Math.pow(BigDecimal.ONE.add(rate).doubleValue(), exponent) - 1.0d;
			return outstanding.multiply(BigDecimal.valueOf(factor), MathContext.DECIMAL64).setScale(2, RoundingMode.HALF_UP);
		}
		return outstanding.multiply(rate, MathContext.DECIMAL64).multiply(cycleUnits, MathContext.DECIMAL64)
				.setScale(2, RoundingMode.HALF_UP);
	}

	private BigDecimal calculateCycleUnits(LocalDate start, LocalDate end, int cycleMonths, boolean partCycleAsFull) {
		if (start == null || end == null || !end.isAfter(start)) {
			return BigDecimal.ZERO;
		}
		long days = ChronoUnit.DAYS.between(start, end);
		if (days <= 0) {
			return BigDecimal.ZERO;
		}
		long cycleDays = ChronoUnit.DAYS.between(start, start.plusMonths(Math.max(cycleMonths, 1)));
		if (cycleDays <= 0) {
			cycleDays = DEFAULT_CYCLE_DAYS;
		}
		BigDecimal cycleUnits = BigDecimal.valueOf(days).divide(BigDecimal.valueOf(cycleDays), 10, RoundingMode.HALF_UP);
		if (partCycleAsFull && cycleUnits.compareTo(BigDecimal.ZERO) > 0) {
			cycleUnits = cycleUnits.setScale(0, RoundingMode.CEILING);
		}
		return cycleUnits;
	}

	private int getFineCycleMonths(String cumulationCycle) {
		String normalized = normalizeCycle(cumulationCycle);
		if (SecuraConstants.DISC_FN_CYCLE_QUARTERLY.equals(normalized)) {
			return 3;
		}
		if (SecuraConstants.DISC_FN_CYCLE_HALFYEARLY.equals(normalized)
				|| normalizeCycle(SecuraConstants.DISC_FN_CYCLE_HALF_YEARLY).equals(normalized)
				|| normalizeCycle(SecuraConstants.DISC_FN_CYCLE_HALF_DASH_YEARLY).equals(normalized)) {
			return 6;
		}
		if (SecuraConstants.DISC_FN_CYCLE_YEARLY.equals(normalized)) {
			return 12;
		}
		return 1;
	}

	private BigDecimal calculateAmount(PaymentEntity paymentEntity, LocalDate startDate, LocalDate endDate,
			BigDecimal areaMultiplier, String paymentCycle) {
		BigDecimal paymentAmountPerMonth = parseNumeric(paymentEntity.getPaymentAmount());
		int cycleMonths = getCycleMonths(paymentCycle);
		if (cycleMonths <= 0) {
			cycleMonths = 1;
		}
		BigDecimal fullCycleAmount = paymentAmountPerMonth.multiply(BigDecimal.valueOf(cycleMonths)).multiply(areaMultiplier);
		if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
			return fullCycleAmount.setScale(2, RoundingMode.HALF_UP);
		}

		LocalDate naturalCycleEnd = startDate.plusMonths(cycleMonths).minusDays(1);
		if (!naturalCycleEnd.isAfter(endDate)) {
			return fullCycleAmount.setScale(2, RoundingMode.HALF_UP);
		}
		BigDecimal coveredMonths = BigDecimal.ZERO;
		LocalDate cursor = startDate;
		while (!cursor.isAfter(endDate)) {
			LocalDate monthEnd = cursor.withDayOfMonth(cursor.lengthOfMonth());
			LocalDate segmentEnd = monthEnd.isBefore(endDate) ? monthEnd : endDate;
			long coveredDays = ChronoUnit.DAYS.between(cursor, segmentEnd) + 1;
			int daysInMonth = cursor.lengthOfMonth();
			if (coveredDays > 0 && daysInMonth > 0) {
				coveredMonths = coveredMonths.add(BigDecimal.valueOf(coveredDays)
						.divide(BigDecimal.valueOf(daysInMonth), 8, RoundingMode.HALF_UP));
			}
			cursor = segmentEnd.plusDays(1);
		}
		if (coveredMonths.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		return paymentAmountPerMonth.multiply(coveredMonths).multiply(areaMultiplier).setScale(2, RoundingMode.HALF_UP);
	}

	private LocalDate calculateDueDate(LocalDate startDate, LocalDate endDate, String paymentCycle, String paymentCollectionMode) {
		if (startDate == null) {
			return null;
		}
		if (!"post".equalsIgnoreCase(paymentCollectionMode)) {
			return startDate;
		}
		int cycleMonths = getCycleMonths(paymentCycle);
		if (cycleMonths <= 0) {
			return endDate;
		}
		LocalDate firstCycleEnd = startDate.plusMonths(cycleMonths).minusDays(1);
		if (endDate == null) {
			return firstCycleEnd;
		}
		return firstCycleEnd.isAfter(endDate) ? endDate : firstCycleEnd;
	}

	private int getCycleMonths(String cycle) {
		String normalizedCycle = normalizeCycle(cycle);
		if (SecuraConstants.PAYMENT_CYCLE_MONTHLY.equals(normalizedCycle)) {
			return 1;
		}
		if ("QUARTERLY".equals(normalizedCycle)) {
			return 3;
		}
		if ("HALFYEARLY".equals(normalizedCycle)) {
			return 6;
		}
		if (SecuraConstants.PAYMENT_CYCLE_YEARLY.equals(normalizedCycle)) {
			return 12;
		}
		if (SecuraConstants.PAYMENT_CYCLE_ONCE.equals(normalizedCycle)) {
			return 1;
		}
		return 0;
	}

	private String normalizeCycle(String cycle) {
		if (cycle == null) {
			return "";
		}
		String normalized = cycle.toUpperCase(Locale.ROOT).replaceAll("[\\s_-]", "");
		if ("QUATERLY".equals(normalized)) {
			normalized = "QUARTERLY";
		}
		if ("MONTLY".equals(normalized)) {
			normalized = "MONTHLY";
		}
		return normalized;
	}

	private List<String> resolvePaymentCycles(PaymentEntity paymentEntity) {
		if (paymentEntity == null) {
			return List.of();
		}
		String paymentCollectionCycleValue = paymentEntity.getPaymentCollectionCycle();
		if (paymentCollectionCycleValue == null || paymentCollectionCycleValue.isBlank()) {
			return List.of();
		}
		String trimmedPaymentCollectionCycleValue = paymentCollectionCycleValue.trim();
		if (!trimmedPaymentCollectionCycleValue.startsWith("[") || !trimmedPaymentCollectionCycleValue.endsWith("]")) {
			return List.of(trimmedPaymentCollectionCycleValue);
		}
		try {
			List<String> paymentCollectionCycleList = genericService.fromJson(trimmedPaymentCollectionCycleValue,
					new TypeReference<List<String>>() {
					});
			if (paymentCollectionCycleList == null || paymentCollectionCycleList.isEmpty()) {
				return List.of(trimmedPaymentCollectionCycleValue);
			}
			return paymentCollectionCycleList.stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isBlank())
					.distinct().toList();
		} catch (Exception exception) {
			LOGGER.warn("Failed to parse payment collection cycles JSON '{}'; falling back to raw cycle",
					trimmedPaymentCollectionCycleValue, exception);
			return List.of(trimmedPaymentCollectionCycleValue);
		}
	}

	private boolean isPerSqft(String paymentCapita) {
		if (paymentCapita == null) {
			return false;
		}
		String normalized = paymentCapita.toUpperCase(Locale.ROOT).replaceAll("[\\s_-]", "");
		return normalized.contains("PERSQFT");
	}

	private boolean isPerHead(String paymentCapita) {
		if (paymentCapita == null) {
			return false;
		}
		String normalized = paymentCapita.toUpperCase(Locale.ROOT).replaceAll("[\\s_-]", "");
		return normalized.contains("PERHEAD");
	}

	private String normalizeFlatArea(String flatArea) {
		return flatArea == null || flatArea.isBlank() ? "UNKNOWN" : flatArea.trim();
	}

	private String normalizeFlatNo(String flatNo) {
		if (flatNo == null || flatNo.isBlank()) {
			return null;
		}
		return flatNo.trim().toUpperCase(Locale.ROOT);
	}

	private BigDecimal calculatePercentageAmount(BigDecimal base, BigDecimal percentage) {
		return base.multiply(percentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
	}

	private BigDecimal parseNumeric(String value) {
		if (value == null || value.isBlank()) {
			return BigDecimal.ZERO;
		}
		String normalized = value.replace(",", "").replace("%", "").replaceAll("[^\\d.\\-]", "").trim();
		if (normalized.isBlank() || ".".equals(normalized) || "-".equals(normalized)) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(normalized);
		} catch (NumberFormatException ex) {
			return BigDecimal.ZERO;
		}
	}

	private String format(String value) {
		return format(parseNumeric(value));
	}

	private String format(BigDecimal value) {
		if (value == null) {
			return "0";
		}
		BigDecimal normalized = value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
		return normalized.toPlainString();
	}

	private String defaultZeroValue(String value) {
		return value == null || value.isBlank() ? format(BigDecimal.ZERO) : value;
	}

	private String stringValue(Object value) {
		return value == null ? null : value.toString();
	}

	private String createDueId() {
		return "DUE" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
	}

	private static class DiscFinTagEntry {
		@com.fasterxml.jackson.annotation.JsonProperty("DISTFIN_TYPE")
		private String distfinType;
		private String code;
		@com.fasterxml.jackson.annotation.JsonProperty("Status")
		private String status;

		public String getDistfinType() {
			return distfinType;
		}

		public void setDistfinType(String distfinType) {
			this.distfinType = distfinType;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}
	}

	private record DiscFinReference(String discountCode, String fineCode) {
	}

	private record PenaltyCalculationResult(DiscFin discFin, BigDecimal amount) {
	}

	private record DueRow(String cycle, String flatType, DueAmountDetails dueAmountDetails, String amountPerMonth,
			LocalDate dueStartDate) {
	}
}
