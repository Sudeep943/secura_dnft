package com.secura.dnft.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
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
import com.secura.dnft.entity.DiscFin;
import com.secura.dnft.entity.DueAmountDetailsEntity;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.request.response.AddedCharges;
import com.secura.dnft.request.response.DueAmountDetails;
import com.secura.dnft.request.response.GenericHeader;

@Service
public class DueDetailsService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DueDetailsService.class);

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private FlatRepository flatRepository;

	@Autowired
	private DiscFinRepository discFinRepository;

	@Autowired
	private DueAmountDetailsRepository dueAmountDetailsRepository;

	@Autowired
	private GenericService genericService;

	public Map<String, List<Map<String, DueAmountDetails>>> calculateDuesForPayment(String paymentId) {
		return calculateDuesForPayment(paymentId, null);
	}

	public Map<String, List<Map<String, DueAmountDetails>>> calculateDuesForPayment(String paymentId, GenericHeader genericHeader) {
		LOGGER.info("calculateDuesForPayment called with paymentId={}, userId={}", paymentId,
				genericHeader != null ? genericHeader.getUserId() : null);
		List<PaymentEntity> paymentEntityList = paymentRepository.findByPaymentId(paymentId);
		return calculateDuesForPayment(paymentEntityList, genericHeader, true);
	}

	/**
	 * Calculates due details using the same logic as persisted dues, but does not
	 * write due rows or flat pending payment links to the database.
	 */
	public Map<String, List<Map<String, DueAmountDetails>>> previewDuesForPayment(
			List<PaymentEntity> paymentEntityList, GenericHeader genericHeader) {
		return calculateDuesForPayment(paymentEntityList, genericHeader, false);
	}

	private Map<String, List<Map<String, DueAmountDetails>>> calculateDuesForPayment(
			List<PaymentEntity> paymentEntityList, GenericHeader genericHeader, boolean persistResults) {
		Map<String, List<Map<String, DueAmountDetails>>> dueByCycle = new LinkedHashMap<>();
		if (paymentEntityList == null || paymentEntityList.isEmpty()) {
			return dueByCycle;
		}

		PaymentEntity baseEntity = paymentEntityList.get(0);
		List<Flat> apartmentFlats = flatRepository.findByAprmntId(baseEntity.getAprmtId());
		Map<String, Long> flatTypeCounts = buildFlatTypeCounts(apartmentFlats);

		Set<String> visitedCycles = new LinkedHashSet<>();
		List<DueRow> generatedRows = new ArrayList<>();
		Collection<String> allGeneratedDueIds = new ArrayList<>();

		for (PaymentEntity paymentEntity : paymentEntityList) {
			if (!visitedCycles.add(normalizeCycle(paymentEntity.getPaymentCollectionCycle()))) {
				continue;
			}

			LocalDate startDate = paymentEntity.getCollectionStartDate();
			LocalDate endDate = paymentEntity.getCollectionEndDate();
			int cycleMonths = getCycleMonths(paymentEntity.getPaymentCollectionCycle());
			List<LocalDate[]> intervals = generateCycleIntervals(startDate, endDate, cycleMonths,
					paymentEntity.getPaymentCollectionCycle());

			List<Map<String, DueAmountDetails>> duesForCycle = new ArrayList<>();
			String dueId = createDueId();
			allGeneratedDueIds.add(dueId);
			for (LocalDate[] interval : intervals) {
				Map<String, DueAmountDetails> duesByFlatType = createDuesForInterval(
						paymentEntity, dueId, flatTypeCounts, apartmentFlats, interval[0], interval[1]);
				duesForCycle.add(duesByFlatType);
				duesByFlatType.forEach((flatType, details) -> generatedRows.add(
						new DueRow(paymentEntity.getPaymentCollectionCycle(), flatType, details,
								paymentEntity.getPaymentAmount(), interval[0])));
			}
			dueByCycle.put(paymentEntity.getPaymentCollectionCycle(), duesForCycle);
		}

		String estimatedCollectionAmount = calculateEstimatedCollectionAmount(dueByCycle, flatTypeCounts, apartmentFlats.size(),
				baseEntity.getPaymentCapita());
		if (estimatedCollectionAmount != null) {
			for (DueRow row : generatedRows) {
				row.dueAmountDetails().setEstimatedCollectionAmount(estimatedCollectionAmount);
			}
		}

		if (persistResults) {
			List<DueAmountDetailsEntity> entityList = generatedRows.stream()
					.map(row -> toEntity(row, genericHeader != null ? genericHeader.getUserId() : null)).toList();
			dueAmountDetailsRepository.saveAll(entityList);
			appendDuesToFlatPendingPayments(apartmentFlats, allGeneratedDueIds);
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
			Map<String, Long> flatTypeCounts, List<Flat> apartmentFlats, LocalDate intervalStart, LocalDate intervalEnd) {
		Map<String, DueAmountDetails> duesByFlatType = new LinkedHashMap<>();
		boolean perSqft = isPerSqft(paymentEntity.getPaymentCapita());
		if (perSqft) {
			for (String flatType : flatTypeCounts.keySet()) {
				DueAmountDetails due = buildDueDetails(paymentEntity, dueId, flatType, parseNumeric(flatType),
						intervalStart, intervalEnd);
				due.setApplicableFlats(getApplicableFlatNos(apartmentFlats, flatType));
				duesByFlatType.put(flatType, due);
			}
			return duesByFlatType;
		}
		DueAmountDetails due = buildDueDetails(paymentEntity, dueId, "ALL", BigDecimal.ONE, intervalStart, intervalEnd);
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
			BigDecimal areaMultiplier, LocalDate intervalStart, LocalDate intervalEnd) {
		LOGGER.debug("buildDueDetails: paymentId={}, dueId={}, flatTypeKey={}, areaMultiplier={}, intervalStart={}, intervalEnd={}",
				paymentEntity.getPaymentId(), dueId, flatTypeKey, areaMultiplier, intervalStart, intervalEnd);
		DueAmountDetails due = new DueAmountDetails();
		due.setPaymentId(paymentEntity.getPaymentId());
		due.setDueId(dueId);
		due.setCollectionCycle(paymentEntity.getPaymentCollectionCycle());
		due.setDueDate(calculateDueDate(intervalStart, intervalEnd, paymentEntity.getPaymentCollectionCycle(),
				paymentEntity.getPaymentCollectionMode()));
		due.setDueEndDate(intervalEnd);

		BigDecimal amount = calculateAmount(paymentEntity, intervalStart, intervalEnd, areaMultiplier);
		DiscFinReference discFinReference = extractDiscFinReference(paymentEntity.getDiscFin());
		DiscFin discountDiscFin = resolveDiscFin(discFinReference.discountCode(), paymentEntity.getPaymentCollectionCycle());
		DiscFin fineDiscFin = resolveDiscFin(discFinReference.fineCode(), paymentEntity.getPaymentCollectionCycle());
		BigDecimal discountedAmount = calculateDiscountAmount(discountDiscFin, amount);
		BigDecimal baseAmount = amount.subtract(discountedAmount);
		if (baseAmount.compareTo(BigDecimal.ZERO) < 0) {
			baseAmount = BigDecimal.ZERO;
		}
		BigDecimal gstPercentage = parseNumeric(paymentEntity.getGst());
		BigDecimal gstAmount = calculatePercentageAmount(baseAmount, gstPercentage);

		List<AddedCharges> addedCharges = parseAddedCharges(paymentEntity.getAddedCharges());
		BigDecimal totalAddedCharges = applyAddedChargesAndCalculateTotal(addedCharges, baseAmount);
		String discountValue = discountDiscFin != null ? format(discountDiscFin.getDiscFinValue()) : null;
		String fineValue = fineDiscFin != null ? format(fineDiscFin.getDiscFinValue()) : null;

		BigDecimal fineAmount = calculateFineAmount(fineDiscFin, baseAmount, due.getDueDate());
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

	private DueAmountDetailsEntity toEntity(DueRow dueRow, String userId) {
		DueAmountDetails due = dueRow.dueAmountDetails();
		DueAmountDetailsEntity entity = new DueAmountDetailsEntity();
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

	private void appendDuesToFlatPendingPayments(List<Flat> apartmentFlats, Collection<String> dueIds) {
		if (apartmentFlats == null || apartmentFlats.isEmpty() || dueIds == null || dueIds.isEmpty()) {
			return;
		}
		List<Flat> updatedFlats = new ArrayList<>();
		for (Flat flat : apartmentFlats) {
			List<String> existingDueIds = parseStringList(flat.getFlatPndngPaymntLst());
			boolean modified = false;
			for (String dueId : dueIds) {
				if (!existingDueIds.contains(dueId)) {
					existingDueIds.add(dueId);
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

	private String calculateEstimatedCollectionAmount(Map<String, List<Map<String, DueAmountDetails>>> dueByCycle,
			Map<String, Long> flatTypeCounts, int flatCount, String paymentCapita) {
		if (dueByCycle.isEmpty()) {
			return null;
		}
		List<Map<String, DueAmountDetails>> firstCycleDues = dueByCycle.values().stream().findFirst().orElse(List.of());
		Map<String, DueAmountDetails> anyCycleDues = firstCycleDues.isEmpty() ? Map.of() : firstCycleDues.get(0);
		if (isPerSqft(paymentCapita)) {
			BigDecimal total = BigDecimal.ZERO;
			for (Map.Entry<String, DueAmountDetails> entry : anyCycleDues.entrySet()) {
				BigDecimal typeAmount = parseNumeric(entry.getValue().getTotalAmount());
				long count = flatTypeCounts.getOrDefault(entry.getKey(), 0L);
				total = total.add(typeAmount.multiply(BigDecimal.valueOf(count)));
			}
			return format(total);
		}
		if (isPerHead(paymentCapita)) {
			return format(BigDecimal.ZERO);
		}
		BigDecimal amountPerUnit = anyCycleDues.values().stream()
				.findFirst()
				.map(DueAmountDetails::getTotalAmount)
				.map(this::parseNumeric)
				.orElse(BigDecimal.ZERO);
		return format(amountPerUnit.multiply(BigDecimal.valueOf(Math.max(flatCount, 0))));
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
			List<Map<String, Object>> entries = genericService.fromJson(discFinJson,
					new TypeReference<List<Map<String, Object>>>() {
					});
			String discountCode = null;
			String fineCode = null;
			for (Map<String, Object> entry : entries) {
				if (entry == null) {
					continue;
				}
				String type = stringValue(entry.get("DISTFIN_TYPE"));
				String code = stringValue(entry.get("code"));
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

	private BigDecimal calculateFineAmount(DiscFin fineDiscFin, BigDecimal baseAmount, LocalDate dueDate) {
		LOGGER.debug("calculateFineAmount: fineDiscFin={}, baseAmount={}, dueDate={}",
				fineDiscFin != null ? fineDiscFin.getDiscFnId() : null, baseAmount, dueDate);
		if (fineDiscFin == null) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		LocalDate today = LocalDate.now();
		LocalDate fineStart = Boolean.TRUE.equals(fineDiscFin.getDueDateAsStartDateFlag()) ? dueDate
				: fineDiscFin.getDiscFnStrtDt();
		LocalDate fineEnd = fineDiscFin.getDiscFnEndDt();
		if (fineStart != null && today.isBefore(fineStart)) {
			LOGGER.debug("calculateFineAmount: today {} is before fineStart {}, returning zero", today, fineStart);
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		if (fineEnd != null && today.isAfter(fineEnd)) {
			LOGGER.debug("calculateFineAmount: today {} is after fineEnd {}, returning zero", today, fineEnd);
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}

		BigDecimal fineValue = parseNumeric(fineDiscFin.getDiscFinValue());
		if (SecuraConstants.DISC_FN_MODE_AMOUNT.equalsIgnoreCase(fineDiscFin.getDiscFnMode())) {
			LOGGER.debug("calculateFineAmount: fixed amount fine={}", fineValue);
			return fineValue.setScale(2, RoundingMode.HALF_UP);
		}
		if (!SecuraConstants.DISC_FN_MODE_PERCENTAGE.equalsIgnoreCase(fineDiscFin.getDiscFnMode())) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		if (isCumulativeFine(fineDiscFin.getFnCalculationType())) {
			LocalDate interestStartDate = fineStart != null ? fineStart : today;
			// Calculate elapsed time in months (complete months + fractional days, inclusive)
			long completeMonths = ChronoUnit.MONTHS.between(interestStartDate, today);
			if (completeMonths < 0) {
				completeMonths = 0;
			}
			LocalDate monthBoundary = interestStartDate.plusMonths(completeMonths);
			long daysInPartialMonth = ChronoUnit.DAYS.between(monthBoundary, today) + 1; // inclusive per-problem-spec
			int daysInCurrentMonth = today.lengthOfMonth();
			double totalMonths = completeMonths + (double) daysInPartialMonth / daysInCurrentMonth;
			if (totalMonths <= 0) {
				return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
			}
			// Convert months to periods based on the cumulation cycle
			double monthsPerPeriod = getMonthsPerPeriod(fineDiscFin.getDiscFnCumlatonCycle());
			double t = BigDecimal.valueOf(totalMonths / monthsPerPeriod).setScale(2, RoundingMode.DOWN).doubleValue();
			double r = fineValue.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP).doubleValue();
			double compoundedFactor = Math.pow(1 + r, t);
			LOGGER.debug("calculateFineAmount cumulative: fineCode={}, baseAmount={}, r={}, t={} periods (totalMonths={}, monthsPerPeriod={}), compoundedFactor={}",
					fineDiscFin.getDiscFnId(), baseAmount, r, t, totalMonths, monthsPerPeriod, compoundedFactor);
			BigDecimal compoundedAmount = baseAmount.multiply(BigDecimal.valueOf(compoundedFactor));
			return compoundedAmount.subtract(baseAmount).setScale(2, RoundingMode.HALF_UP);
		}
		LOGGER.debug("calculateFineAmount simple percentage: fineCode={}, baseAmount={}, fineValue={}",
				fineDiscFin.getDiscFnId(), baseAmount, fineValue);
		return calculatePercentageAmount(baseAmount, fineValue);
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

	private double getMonthsPerPeriod(String cumulationCycle) {
		if (cumulationCycle == null) {
			return 1.0;
		}
		String normalized = cumulationCycle.toUpperCase(Locale.ROOT).replaceAll("[\\s_-]", "");
		if (SecuraConstants.DISC_FN_CYCLE_MONTHLY.equals(normalized)
				|| SecuraConstants.DISC_FN_CYCLE_MONTHLY_MISSPELLED.equals(normalized)) {
			return 1.0;
		}
		if (SecuraConstants.DISC_FN_CYCLE_QUARTERLY.equals(normalized)
				|| SecuraConstants.DISC_FN_CYCLE_QUARTERLY_MISSPELLED.equals(normalized)) {
			return 3.0;
		}
		if (SecuraConstants.DISC_FN_CYCLE_HALFYEARLY.equals(normalized)
				|| SecuraConstants.DISC_FN_CYCLE_HALF_YEARLY.replaceAll("[\\s_-]", "").equals(normalized)
				|| SecuraConstants.DISC_FN_CYCLE_HALF_DASH_YEARLY.replaceAll("[\\s_-]", "").equals(normalized)) {
			return 6.0;
		}
		if (SecuraConstants.DISC_FN_CYCLE_YEARLY.equals(normalized)) {
			return 12.0;
		}
		if (SecuraConstants.DISC_FN_CYCLE_DAILY.equals(normalized)) {
			// Daily rate: return months per day (1 day ≈ 12/365.25 months)
			return 12.0 / 365.25;
		}
		return 1.0;
	}

	private BigDecimal calculateAmount(PaymentEntity paymentEntity, LocalDate startDate, LocalDate endDate,
			BigDecimal areaMultiplier) {
		BigDecimal paymentAmountPerMonth = parseNumeric(paymentEntity.getPaymentAmount());
		int cycleMonths = getCycleMonths(paymentEntity.getPaymentCollectionCycle());
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

		long coveredMonths = (long) YearMonth.from(endDate).compareTo(YearMonth.from(startDate)) + 1;
		if (coveredMonths <= 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		return paymentAmountPerMonth.multiply(BigDecimal.valueOf(coveredMonths)).multiply(areaMultiplier)
				.setScale(2, RoundingMode.HALF_UP);
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

	private record DiscFinReference(String discountCode, String fineCode) {
	}

	private record DueRow(String cycle, String flatType, DueAmountDetails dueAmountDetails, String amountPerMonth,
			LocalDate dueStartDate) {
	}
}
