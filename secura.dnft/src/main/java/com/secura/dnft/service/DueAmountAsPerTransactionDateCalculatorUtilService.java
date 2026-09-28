package com.secura.dnft.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.dao.DiscFinRepository;
import com.secura.dnft.entity.DiscFin;
import com.secura.dnft.entity.DueAmountDetailsEntity;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.request.response.GetDueAmountForFlatResponse;

@Service
public class DueAmountAsPerTransactionDateCalculatorUtilService {

	@Autowired
	private GenericService genericService;

	@Autowired
	private DiscFinRepository discFinRepository;

	public void initializeDefaultDueResponse(GetDueAmountForFlatResponse response) {
		response.setDueDetails(new LinkedHashMap<>());
		response.setTotalDue(BigDecimal.ZERO.toPlainString());
		response.setTotalMandatoryPayment(BigDecimal.ZERO.toPlainString());
		response.setTotalOptionalPayment(BigDecimal.ZERO.toPlainString());
		response.setPenaltyAdded(Boolean.FALSE);
	}

	public List<String> parseStringList(String json) {
		if (!hasText(json)) {
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

	public boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	public boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	public PaymentEntity findDuePaymentDetails(DueAmountDetailsEntity due, List<PaymentEntity> allPaymentsEntities) {
		if (null != due) {
			Optional<PaymentEntity> payment = allPaymentsEntities.stream()
					.filter(pymt -> pymt.getPaymentId().equalsIgnoreCase(due.getPaymentId())).findFirst();
			if (payment.isPresent()) {
				return payment.get();
			}
		}
		return null;
	}

	public BigDecimal parseBigDecimal(String value) {

		if (value == null || value.trim().isEmpty()) {
			return BigDecimal.ZERO;
		}

		try {
			return new BigDecimal(value.trim());
		} catch (NumberFormatException e) {
			return BigDecimal.ZERO;
		}
	}

	public LocalDate calculateBufferEndDate(LocalDate dueDate, String bufferTime, String bufferTimeUnit) {

		if (dueDate == null || bufferTime == null || bufferTime.trim().isEmpty() || bufferTimeUnit == null
				|| bufferTimeUnit.trim().isEmpty()) {
			return dueDate;
		}

		try {

			long buffer = Long.parseLong(bufferTime.trim());

			String unit = bufferTimeUnit.trim().toLowerCase();

			if (unit.startsWith("day")) {

				return dueDate.plusDays(buffer);

			} else if (unit.startsWith("month")) {

				return dueDate.plusMonths(buffer);

			} else if (unit.startsWith("quarter") || unit.startsWith("quater")) {

				return dueDate.plusMonths(buffer * 3);

			} else {

				return dueDate;
			}

		} catch (NumberFormatException e) {
			return dueDate;
		}
	}

	public BigDecimal calculatePenaltyDuration(LocalDate dueDate, LocalDate transactionDate, String fineCycle,
			Boolean partOfCycleAsFull) {

		if (dueDate == null || transactionDate == null || fineCycle == null) {
			return BigDecimal.ZERO;
		}

		if (!transactionDate.isAfter(dueDate)) {
			return BigDecimal.ZERO;
		}

		/*
		 * Calculate number of complete months between dates.
		 *
		 * Example: 01-Sep -> 01-Oct = 1 month 01-Sep -> 01-Nov = 2 months
		 */
		long months = ChronoUnit.MONTHS.between(dueDate, transactionDate);

		/*
		 * If transaction date has remaining days after complete months, count that as a
		 * fractional month.
		 */
		LocalDate afterCompleteMonths = dueDate.plusMonths(months);

		long remainingDays = ChronoUnit.DAYS.between(afterCompleteMonths, transactionDate);

		BigDecimal totalMonths = BigDecimal.valueOf(months);

		if (remainingDays > 0) {

			long daysInMonth = afterCompleteMonths.lengthOfMonth();

			BigDecimal fraction = BigDecimal.valueOf(remainingDays).divide(BigDecimal.valueOf(daysInMonth), 10,
					RoundingMode.HALF_UP);

			totalMonths = totalMonths.add(fraction);
		}

		/*
		 * Determine cycle length in months.
		 */
		int cycleMonths;

		String cycle = fineCycle.trim().toLowerCase();

		if (cycle.startsWith("month")) {

			cycleMonths = 1;

		} else if (cycle.startsWith("quarter") || cycle.startsWith("qurter")) {

			cycleMonths = 3;

		} else if (cycle.startsWith("half")) {

			cycleMonths = 6;

		} else if (cycle.startsWith("year")) {

			cycleMonths = 12;

		} else {

			return BigDecimal.ZERO;
		}

		/*
		 * Convert elapsed months into cycle duration.
		 *
		 * Example:
		 *
		 * 1 month / 3 = 0.3333 quarter 4 months / 3 = 1.3333 quarter 6 months / 6 = 1
		 * half-year 12 months / 12 = 1 year
		 */
		BigDecimal duration = totalMonths.divide(BigDecimal.valueOf(cycleMonths), 10, RoundingMode.HALF_UP);

		/*
		 * If partOfCycleAsFull = true, round any fraction up to the next complete
		 * cycle.
		 *
		 * Example:
		 *
		 * 1 month -> 1 quarter 4 months -> 2 quarters 1 month -> 1 half-year 4 months
		 * -> 1 half-year 7 months -> 2 half-years
		 */
		if (Boolean.TRUE.equals(partOfCycleAsFull)) {

			duration = duration.setScale(0, RoundingMode.CEILING);
		}

		return duration;
	}

	public static class DiscFinTagEntry {
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

	public String stringValue(Object value) {
		return value == null ? null : value.toString();
	}

	public boolean isStatusActive(String status) {
		return SecuraConstants.DISC_FIN_STATUS_ACTIVE.equalsIgnoreCase(stringValue(status));
	}

	public DiscFin resolveDiscFin(String code, String paymentCycle, String apartmentId) {
		if (code == null || code.isBlank()) {
			return null;
		}
		List<DiscFin> discFins = discFinRepository.findByDiscFnId(code);
		if (discFins == null || discFins.isEmpty()) {
			return null;
		}
		discFins = discFins.stream().filter(discfin -> discfin.getAprmtId().equals(apartmentId))
				.collect(Collectors.toList());
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

	private String normalizeCycle(String cycle) {
		if (cycle == null) {
			return "";
		}
		String normalized = cycle.toUpperCase(Locale.ROOT).replaceAll("[\\s_-]", "");
		if ("QUATERLY".equals(normalized) || SecuraConstants.DISC_FN_CYCLE_QUARTERLY.equals(normalized)) {
			normalized = "QUARTERLY";
		}
		if (SecuraConstants.DISC_FN_CYCLE_MONTHLY.equals(normalized)) {
			normalized = SecuraConstants.DISC_FN_CYCLE_MONTHLY;
		}
		return normalized;
	}

	public List<String> getAllDuesForFlat(Flat flat, String apartmentId) {
		List<String> pendingDueKeys = parseStringList(flat.getFlatPndngPaymntLst());
		List<String> dueids = extractDueIdsOfDue(pendingDueKeys);
		return dueids;

	}

	public List<String> extractDueIdsOfDue(List<String> dueIds) {
		List<String> result = dueIds.stream().map(s -> s.split("_")[0]).toList();
		return result;

	}

	public String resolvePaymentName(Optional<PaymentEntity> paymentEntity, List<DueAmountDetailsEntity> dueEntities) {
		if (paymentEntity.isPresent() && hasText(paymentEntity.get().getPaymentName())) {
			return paymentEntity.get().getPaymentName();
		}
		if (dueEntities == null || dueEntities.isEmpty()) {
			return null;
		}
		return dueEntities.stream().map(DueAmountDetailsEntity::getPaymentName).filter(this::hasText).findFirst()
				.orElse(null);
	}

	public String getTheLowestCollectionCycle(PaymentEntity paymentEntity) {
		List<String> paymentCycles = genericService.fromJson(paymentEntity.getPaymentCollectionCycle(),
				new TypeReference<List<String>>() {
				});

		if (paymentCycles.contains(SecuraConstants.DISC_FN_CYCLE_MONTHLY)) {
			return SecuraConstants.DISC_FN_CYCLE_MONTHLY;
		}
		if (paymentCycles.contains(SecuraConstants.DISC_FN_CYCLE_QUARTERLY)) {
			return SecuraConstants.DISC_FN_CYCLE_QUARTERLY;
		}
		if (paymentCycles.contains(SecuraConstants.DISC_FN_CYCLE_QUARTERLY_MISSPELLED)) {
			return SecuraConstants.DISC_FN_CYCLE_QUARTERLY_MISSPELLED;
		}
		if (paymentCycles.contains(SecuraConstants.DISC_FN_CYCLE_HALF_YEARLY)) {
			return SecuraConstants.DISC_FN_CYCLE_HALF_YEARLY;
		}
		if (paymentCycles.contains(SecuraConstants.DISC_FN_CYCLE_YEARLY)) {
			return SecuraConstants.DISC_FN_CYCLE_YEARLY;
		}
		return null;
	}
	
 public List<DueAmountDetailsEntity> getChildDues(DueAmountDetailsEntity requiredDue,  List<DueAmountDetailsEntity> allDue, String lowestCycle){
	 List<DueAmountDetailsEntity> childDues= new ArrayList<>();
	 childDues=allDue.stream().filter(due->due.getCollectionCycle().equals(lowestCycle) &&
			 !due.getDueStartDate().isBefore(requiredDue.getDueStartDate()) &&! due.getDueEndDate().isAfter(requiredDue.getDueEndDate())).collect(Collectors.toList());
	 return childDues;
 }
}
