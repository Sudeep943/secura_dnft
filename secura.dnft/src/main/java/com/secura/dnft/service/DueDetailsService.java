package com.secura.dnft.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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

	public Map<String, Map<String, DueAmountDetails>> calculateDuesForPayment(String paymentId) {
		return calculateDuesForPayment(paymentId, null);
	}

	public Map<String, Map<String, DueAmountDetails>> calculateDuesForPayment(String paymentId, GenericHeader genericHeader) {
		List<PaymentEntity> paymentEntityList = paymentRepository.findByPaymentId(paymentId);
		Map<String, Map<String, DueAmountDetails>> dueByCycle = new LinkedHashMap<>();
		if (paymentEntityList == null || paymentEntityList.isEmpty()) {
			return dueByCycle;
		}

		String dueId = createDueId();
		PaymentEntity baseEntity = paymentEntityList.get(0);
		List<Flat> apartmentFlats = flatRepository.findByAprmntId(baseEntity.getAprmtId());
		Map<String, Long> flatTypeCounts = buildFlatTypeCounts(apartmentFlats);

		Set<String> visitedCycles = new LinkedHashSet<>();
		List<DueRow> generatedRows = new ArrayList<>();
		for (PaymentEntity paymentEntity : paymentEntityList) {
			if (!visitedCycles.add(normalizeCycle(paymentEntity.getPaymentCollectionCycle()))) {
				continue;
			}
			Map<String, DueAmountDetails> duesByFlatType = createDuesForCycle(paymentEntity, dueId, flatTypeCounts);
			dueByCycle.put(paymentEntity.getPaymentCollectionCycle(), duesByFlatType);
			duesByFlatType.forEach((flatType, details) -> generatedRows
					.add(new DueRow(paymentEntity.getPaymentCollectionCycle(), flatType, details,
							paymentEntity.getPaymentAmount())));
		}

		String estimatedCollectionAmount = calculateEstimatedCollectionAmount(dueByCycle, flatTypeCounts, apartmentFlats.size(),
				baseEntity.getPaymentCapita());
		if (estimatedCollectionAmount != null) {
			for (DueRow row : generatedRows) {
				row.dueAmountDetails().setEstimatedCollectionAmount(estimatedCollectionAmount);
			}
		}

		List<DueAmountDetailsEntity> entityList = generatedRows.stream()
				.map(row -> toEntity(row, genericHeader != null ? genericHeader.getUserId() : null)).toList();
		dueAmountDetailsRepository.saveAll(entityList);
		appendDueToFlatPendingPayments(apartmentFlats, dueId);

		return dueByCycle;
	}

	private Map<String, DueAmountDetails> createDuesForCycle(PaymentEntity paymentEntity, String dueId,
			Map<String, Long> flatTypeCounts) {
		Map<String, DueAmountDetails> duesByFlatType = new LinkedHashMap<>();
		boolean perSqft = isPerSqft(paymentEntity.getPaymentCapita());
		if (perSqft) {
			for (String flatType : flatTypeCounts.keySet()) {
				duesByFlatType.put(flatType, buildDueDetails(paymentEntity, dueId, flatType, parseNumeric(flatType)));
			}
			return duesByFlatType;
		}
		duesByFlatType.put("ALL", buildDueDetails(paymentEntity, dueId, "ALL", BigDecimal.ONE));
		return duesByFlatType;
	}

	private DueAmountDetails buildDueDetails(PaymentEntity paymentEntity, String dueId, String flatTypeKey,
			BigDecimal areaMultiplier) {
		DueAmountDetails due = new DueAmountDetails();
		due.setPaymentId(paymentEntity.getPaymentId());
		due.setDueId(dueId);
		due.setCollectionCycle(paymentEntity.getPaymentCollectionCycle());
		LocalDate collectionStartDate = toLocalDate(paymentEntity.getCollectionStartDate());
		LocalDate collectionEndDate = toLocalDate(paymentEntity.getCollectionEndDate());
		due.setDueDate(calculateDueDate(collectionStartDate, collectionEndDate, paymentEntity.getPaymentCollectionCycle(),
				paymentEntity.getPaymentCollectionMode()));

		BigDecimal amount = calculateAmount(paymentEntity, collectionStartDate, collectionEndDate, areaMultiplier);
		DiscFinReference discFinReference = extractDiscFinReference(paymentEntity.getDiscFin());
		DiscFin discountDiscFin = resolveDiscFin(discFinReference.discountCode(), paymentEntity.getPaymentCollectionCycle());
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

		BigDecimal fineAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
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
		due.setFineCode(discFinReference.fineCode());
		due.setDiscFnValue(discountValue);
		due.setDiscountedAmount(format(discountedAmount));
		due.setFineAmount(format(fineAmount));
		due.setFineType("");
		due.setRoundUpAmount(format(roundUpAmount));
		due.setAlreadyPaidAmount(format(BigDecimal.ZERO));
		due.setAdminDiscount(format(BigDecimal.ZERO));
		due.setCause(paymentEntity.getCauseId());
		return due;
	}

	private DueAmountDetailsEntity toEntity(DueRow dueRow, String userId) {
		DueAmountDetails due = dueRow.dueAmountDetails();
		DueAmountDetailsEntity entity = new DueAmountDetailsEntity();
		entity.setDueId(due.getDueId());
		entity.setCollectionCycle(due.getCollectionCycle());
		entity.setFlatArea(dueRow.flatType());
		entity.setDueDate(due.getDueDate());
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
		entity.setFineCode(due.getFineCode());
		entity.setDiscFnValue(due.getDiscFnValue());
		entity.setDiscountedAmount(due.getDiscountedAmount());
		entity.setFineAmount(defaultZeroValue(due.getFineAmount()));
		entity.setFineType(due.getFineType());
		entity.setRoundUpAmount(due.getRoundUpAmount());
		entity.setAlreadyPaidAmount(defaultZeroValue(due.getAlreadyPaidAmount()));
		entity.setAdminDiscount(defaultZeroValue(due.getAdminDiscount()));
		entity.setPaymentStatus(due.getPaymentStatus());
		entity.setPaymentDate(due.getPaymentDate());
		entity.setCreatUsrId(userId);
		entity.setLstUpdtUsrId(null);
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

	private void appendDueToFlatPendingPayments(List<Flat> apartmentFlats, String dueId) {
		if (apartmentFlats == null || apartmentFlats.isEmpty()) {
			return;
		}
		List<Flat> updatedFlats = new ArrayList<>();
		for (Flat flat : apartmentFlats) {
			List<String> dueIds = parseStringList(flat.getFlatPndngPaymntLst());
			if (!dueIds.contains(dueId)) {
				dueIds.add(dueId);
				flat.setFlatPndngPaymntLst(genericService.toJson(dueIds));
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

	private String calculateEstimatedCollectionAmount(Map<String, Map<String, DueAmountDetails>> dueByCycle,
			Map<String, Long> flatTypeCounts, int flatCount, String paymentCapita) {
		if (dueByCycle.isEmpty()) {
			return null;
		}
		Map<String, DueAmountDetails> anyCycleDues = dueByCycle.values().stream().findFirst().orElse(Map.of());
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
			return null;
		}
		BigDecimal total = BigDecimal.ZERO;
		for (DueAmountDetails dueAmountDetails : anyCycleDues.values()) {
			total = total.add(parseNumeric(dueAmountDetails.getTotalAmount()));
		}
		return format(total.multiply(BigDecimal.valueOf(Math.max(flatCount, 0))));
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
		LocalDate discountStart = toLocalDate(discountDiscFin.getDiscFnStrtDt());
		LocalDate discountEnd = toLocalDate(discountDiscFin.getDiscFnEndDt());
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

		long totalCycleDays = ChronoUnit.DAYS.between(startDate, naturalCycleEnd.plusDays(1));
		long activeDays = ChronoUnit.DAYS.between(startDate, endDate.plusDays(1));
		if (totalCycleDays <= 0 || activeDays <= 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		return fullCycleAmount.multiply(BigDecimal.valueOf(activeDays)).divide(BigDecimal.valueOf(totalCycleDays), 2,
				RoundingMode.HALF_UP);
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
		if (SecuraConstants.PAYMENT_CYCLE_QUATERLY.equals(normalizedCycle) || "QUARTERLY".equals(normalizedCycle)) {
			return 3;
		}
		if (SecuraConstants.PAYMENT_CYCLE_HALF_YEARLY.equals(normalizedCycle) || "HALFYEARLY".equals(normalizedCycle)) {
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
		return cycle.toUpperCase(Locale.ROOT).replaceAll("[\\s_-]", "");
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

	private LocalDate toLocalDate(LocalDateTime localDateTime) {
		return localDateTime == null ? null : localDateTime.toLocalDate();
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

	private record DueRow(String cycle, String flatType, DueAmountDetails dueAmountDetails, String amountPerMonth) {
	}
}
