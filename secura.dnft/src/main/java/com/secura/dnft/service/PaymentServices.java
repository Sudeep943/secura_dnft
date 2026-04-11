package com.secura.dnft.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.dao.FlatRepository;
import com.secura.dnft.dao.PaymentRepository;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.interfaceservice.PaymentInterface;
import com.secura.dnft.request.response.AddedCharges;
import com.secura.dnft.request.response.CreatePaymentRequest;
import com.secura.dnft.request.response.CreatePaymentResponse;
import com.secura.dnft.request.response.DueAmountDetails;
import com.secura.dnft.request.response.DuePaymentAmountDetailsRequest;
import com.secura.dnft.request.response.DuePaymentAmountDetailsResponse;
import com.secura.dnft.request.response.GetDuePaymentAmountDetailsResponse;
import com.secura.dnft.request.response.GetPaymentRequest;
import com.secura.dnft.request.response.GetPaymentResponse;
import com.secura.dnft.request.response.UpdatePaymentRequest;
import com.secura.dnft.request.response.UpdatePaymentResponse;

@Service
public class PaymentServices implements PaymentInterface {
	private static final Set<String> PERCENTAGE_CHARGE_TYPES = Set.of("percentage", "percent", "%");

	@Autowired
	GenericService genericService;

	@Autowired
	PaymentRepository paymentRepository;
	
	@Autowired
	FlatRepository flatRepository;

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
		if (isPerSqftPaymentCapita(request != null ? request.getPaymentCapita() : null)) {
			response.setFlatTypeDueAmountDetails(buildFlatTypeDueAmountDetails(request));
		} else {
			response.setListOfDueAmountDetails(buildDueAmountDetails(request, null));
		}
		response.setMessage(SuccessMessage.SUCC_MESSAGE_28);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_28);
		return response;
	}

	private List<DueAmountDetails> buildDueAmountDetails(CreatePaymentRequest request, String paymentId) {
		return buildDueAmountDetails(request, paymentId, null);
	}

	private List<DueAmountDetails> buildDueAmountDetails(CreatePaymentRequest request, String paymentId,
			BigDecimal cycleAmountOverride) {
		List<DueAmountDetails> dueAmountDetails = new ArrayList<>();
		if (request == null || request.getCollectionStartDate() == null || request.getCollectionEndDate() == null) {
			return dueAmountDetails;
		}

		LocalDate start = request.getCollectionStartDate().toLocalDate();
		LocalDate end = request.getCollectionEndDate().toLocalDate();
		if (start.isAfter(end)) {
			return dueAmountDetails;
		}

		BigDecimal cycleAmount = cycleAmountOverride != null ? cycleAmountOverride
				: resolveCycleAmount(request.getPaymentAmount(), request.getPaymentCapita());
		BigDecimal gstPercent = parseNumeric(request.getGst());
		Set<String> usedDueIds = new LinkedHashSet<>();

		if (isOnceCycle(request.getPaymentCollectionCycle())) {
			DueAmountDetails details = new DueAmountDetails();
			details.setDueDate(isPost(request.getPaymentCollectionMode()) ? end.plusDays(1) : start);
			details.setPaymentId(paymentId);
			details.setDueId(generateUniqueDueId(paymentId, usedDueIds));
			BigDecimal dueBaseAmount = roundAmountByThreshold(cycleAmount.setScale(2, RoundingMode.HALF_UP));
			AddedChargesCalculation addedChargesCalculation = calculateAddedCharges(request.getAddedCharges(), dueBaseAmount);
			BigDecimal dueAmountWithAddedCharges = dueBaseAmount.add(addedChargesCalculation.getTotalChargeAmount())
					.setScale(2, RoundingMode.HALF_UP);
			BigDecimal gstAmount = dueBaseAmount.multiply(gstPercent).divide(BigDecimal.valueOf(100), 2,
					RoundingMode.HALF_UP);
			details.setAmount(formatNumber(dueAmountWithAddedCharges));
			details.setGstAmount(formatNumber(gstAmount));
			details.setTotalAmount(formatNumber(roundAmountByThreshold(dueAmountWithAddedCharges.add(gstAmount))));
			details.setAddedCharges(addedChargesCalculation.getFinalAddedCharges());
			dueAmountDetails.add(details);
		} else {
			int cycleMonths = getCycleMonths(request.getPaymentCollectionCycle());
			if (cycleMonths <= 0) {
				return dueAmountDetails;
			}
			LocalDate periodStart = start;
			while (!periodStart.isAfter(end)) {
				LocalDate periodEnd = periodStart.plusMonths(cycleMonths).minusDays(1);
				if (periodEnd.isAfter(end)) {
					periodEnd = end;
				}
				DueAmountDetails details = new DueAmountDetails();
				details.setDueDate(isPost(request.getPaymentCollectionMode()) ? periodEnd.plusDays(1) : periodStart);
				details.setPaymentId(paymentId);
				details.setDueId(generateUniqueDueId(paymentId, usedDueIds));

				BigDecimal dueBaseAmount = roundAmountByThreshold(
						calculateDueBaseAmount(periodStart, cycleMonths, end, cycleAmount));
				AddedChargesCalculation addedChargesCalculation = calculateAddedCharges(request.getAddedCharges(), dueBaseAmount);
				BigDecimal dueAmountWithAddedCharges = dueBaseAmount.add(addedChargesCalculation.getTotalChargeAmount())
						.setScale(2, RoundingMode.HALF_UP);
				BigDecimal gstAmount = dueBaseAmount.multiply(gstPercent).divide(BigDecimal.valueOf(100), 2,
						RoundingMode.HALF_UP);
				details.setAmount(formatNumber(dueAmountWithAddedCharges));
				details.setGstAmount(formatNumber(gstAmount));
				details.setTotalAmount(formatNumber(roundAmountByThreshold(dueAmountWithAddedCharges.add(gstAmount))));
				details.setAddedCharges(addedChargesCalculation.getFinalAddedCharges());
				dueAmountDetails.add(details);

				periodStart = periodStart.plusMonths(cycleMonths);
			}
		}

		return dueAmountDetails;
	}

	private Map<String, List<DueAmountDetails>> buildFlatTypeDueAmountDetails(CreatePaymentRequest request) {
		Map<String, List<DueAmountDetails>> dueAmountByFlatArea = new LinkedHashMap<>();
		if (request == null) {
			return dueAmountByFlatArea;
		}

		String apartmentId = request.getGenericHeader() != null ? request.getGenericHeader().getApartmentId() : null;
		List<Flat> apartmentFlats = (apartmentId == null || apartmentId.isBlank()) ? flatRepository.findAll()
				: flatRepository.findByAprmntId(apartmentId);
		Set<String> flatAreas = apartmentFlats.stream().map(Flat::getFlatArea).filter(area -> area != null && !area.isBlank())
				.map(String::trim).collect(Collectors.toCollection(LinkedHashSet::new));

		BigDecimal ratePerSqft = parseNumeric(request.getPaymentAmount());
		for (String flatArea : flatAreas) {
			BigDecimal parsedFlatArea = parseNumeric(flatArea);
			if (parsedFlatArea.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			BigDecimal cycleAmount = parsedFlatArea.multiply(ratePerSqft);
			List<DueAmountDetails> dueAmountDetails = buildDueAmountDetails(request, null, cycleAmount);
			dueAmountByFlatArea.put(flatArea, dueAmountDetails);
		}
		return dueAmountByFlatArea;
	}

	private LocalDate resolveEntityDueDate(List<DueAmountDetails> dueAmountDetails, LocalDate today) {
		LocalDate upcomingDueDate = dueAmountDetails.stream().map(DueAmountDetails::getDueDate).filter(dueDate -> dueDate != null)
				.filter(dueDate -> !dueDate.isBefore(today)).min(LocalDate::compareTo).orElse(null);
		if (upcomingDueDate != null) {
			return upcomingDueDate;
		}
		return dueAmountDetails.stream().map(DueAmountDetails::getDueDate).filter(dueDate -> dueDate != null)
				.min(LocalDate::compareTo).orElse(null);
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

	private List<DueAmountDetails> parsePendingDueAmountDetails(String pendingDueJson) {
		if (pendingDueJson == null || pendingDueJson.isBlank()) {
			return new ArrayList<>();
		}
		try {
			List<DueAmountDetails> existing = genericService.fromJson(pendingDueJson,
					new TypeReference<List<DueAmountDetails>>() {
					});
			return existing != null ? new ArrayList<>(existing) : new ArrayList<>();
		} catch (RuntimeException e) {
			return new ArrayList<>();
		}
	}

	private void updatePendingDueAmountDetailsForFlats(CreatePaymentRequest request, List<DueAmountDetails> dueAmountDetails,
			String paymentId) {
		String apartmentId = request != null && request.getGenericHeader() != null ? request.getGenericHeader().getApartmentId()
				: null;
		List<Flat> apartmentFlats = (apartmentId == null || apartmentId.isBlank()) ? flatRepository.findAll()
				: flatRepository.findByAprmntId(apartmentId);
		Set<String> applicableFlatNos = parseApplicableFlatNos(request != null ? request.getApplicableFor() : null);

		List<Flat> targetFlats = apartmentFlats;
		if (!applicableFlatNos.isEmpty()) {
			Set<String> normalizedApplicableFlatNos = applicableFlatNos.stream().map(String::trim)
					.map(String::toUpperCase).collect(Collectors.toCollection(LinkedHashSet::new));
			targetFlats = apartmentFlats.stream().filter(flat -> flat != null && flat.getFlatNo() != null
					&& normalizedApplicableFlatNos.contains(flat.getFlatNo().trim().toUpperCase()))
					.collect(Collectors.toList());
		}

		LocalDate today = LocalDate.now();
		for (Flat flat : targetFlats) {
			List<DueAmountDetails> existingDueAmountDetails = parsePendingDueAmountDetails(flat.getFlatPndngPaymntLst());
			existingDueAmountDetails.addAll(cloneDueAmountDetails(dueAmountDetails));
			ensureDueIdsForFlatSave(existingDueAmountDetails, paymentId);
			if (request == null || !request.isAddLeftOverPayment()) {
				existingDueAmountDetails
						.removeIf(details -> details.getDueDate() != null && details.getDueDate().isBefore(today));
			}
			flat.setFlatPndngPaymntLst(genericService.toJson(existingDueAmountDetails));
		}
		flatRepository.saveAll(targetFlats);
	}

	private List<DueAmountDetails> cloneDueAmountDetails(List<DueAmountDetails> dueAmountDetails) {
		List<DueAmountDetails> cloned = new ArrayList<>();
		for (DueAmountDetails details : dueAmountDetails) {
			DueAmountDetails copy = new DueAmountDetails();
			copy.setDueDate(details.getDueDate());
			copy.setPaymentId(details.getPaymentId());
			copy.setDueId(details.getDueId());
			copy.setAmount(details.getAmount());
			copy.setGstAmount(details.getGstAmount());
			copy.setTotalAmount(details.getTotalAmount());
			copy.setAddedCharges(cloneAddedCharges(details.getAddedCharges()));
			cloned.add(copy);
		}
		return cloned;
	}

	private List<AddedCharges> cloneAddedCharges(List<AddedCharges> addedCharges) {
		List<AddedCharges> cloned = new ArrayList<>();
		if (addedCharges == null) {
			return cloned;
		}
		for (AddedCharges charge : addedCharges) {
			if (charge == null) {
				continue;
			}
			AddedCharges copy = new AddedCharges();
			copy.setChargeName(charge.getChargeName());
			copy.setChargeType(charge.getChargeType());
			copy.setValue(charge.getValue());
			copy.setFinalChargeValue(charge.getFinalChargeValue());
			cloned.add(copy);
		}
		return cloned;
	}

	private void ensureDueIdsForFlatSave(List<DueAmountDetails> dueAmountDetails, String fallbackPaymentId) {
		Set<String> usedDueIds = dueAmountDetails.stream().map(DueAmountDetails::getDueId)
				.filter(dueId -> dueId != null && !dueId.isBlank()).collect(Collectors.toCollection(LinkedHashSet::new));
		for (DueAmountDetails details : dueAmountDetails) {
			if (details.getDueId() != null && !details.getDueId().isBlank()) {
				continue;
			}
			String paymentId = details.getPaymentId();
			if ((paymentId == null || paymentId.isBlank()) && fallbackPaymentId != null && !fallbackPaymentId.isBlank()) {
				paymentId = fallbackPaymentId;
			}
			if (paymentId != null && !paymentId.isBlank()) {
				details.setDueId(generateUniqueDueId(paymentId, usedDueIds));
			}
		}
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

	private AddedChargesCalculation calculateAddedCharges(List<AddedCharges> addedCharges, BigDecimal baseAmount) {
		List<AddedCharges> finalAddedCharges = new ArrayList<>();
		if (addedCharges == null || addedCharges.isEmpty()) {
			return new AddedChargesCalculation(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), finalAddedCharges);
		}
		BigDecimal totalChargeAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		for (AddedCharges charge : addedCharges) {
			if (charge == null) {
				continue;
			}
			BigDecimal finalChargeAmount = resolveFinalChargeAmount(charge.getChargeType(), charge.getValue(), baseAmount);
			AddedCharges finalCharge = new AddedCharges();
			finalCharge.setChargeName(charge.getChargeName());
			finalCharge.setChargeType(charge.getChargeType());
			finalCharge.setValue(charge.getValue());
			finalCharge.setFinalChargeValue(formatNumber(finalChargeAmount));
			finalAddedCharges.add(finalCharge);
			totalChargeAmount = totalChargeAmount.add(finalChargeAmount);
		}
		return new AddedChargesCalculation(totalChargeAmount, finalAddedCharges);
	}

	private BigDecimal resolveFinalChargeAmount(String chargeType, String chargeValue, BigDecimal baseAmount) {
		BigDecimal numericValue = parseNumeric(chargeValue);
		if (isPercentageChargeType(chargeType)) {
			return baseAmount.multiply(numericValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		}
		return numericValue.setScale(2, RoundingMode.HALF_UP);
	}

	private boolean isPercentageChargeType(String chargeType) {
		if (chargeType == null) {
			return false;
		}
		return PERCENTAGE_CHARGE_TYPES.contains(chargeType.trim().toLowerCase());
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

	private boolean isPerSqftPaymentCapita(String paymentCapita) {
		if (paymentCapita == null || paymentCapita.isBlank()) {
			return false;
		}
		String normalized = paymentCapita.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
		return "persqft".equals(normalized);
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

	private static final class AddedChargesCalculation {
		private final BigDecimal totalChargeAmount;
		private final List<AddedCharges> finalAddedCharges;

		private AddedChargesCalculation(BigDecimal totalChargeAmount, List<AddedCharges> finalAddedCharges) {
			this.totalChargeAmount = totalChargeAmount;
			this.finalAddedCharges = finalAddedCharges;
		}

		private BigDecimal getTotalChargeAmount() {
			return totalChargeAmount;
		}

		private List<AddedCharges> getFinalAddedCharges() {
			return finalAddedCharges;
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
		response.setGenericHeader(request.getGenericHeader());
		return response;
	}

	@Override
	public CreatePaymentResponse createPayment(CreatePaymentRequest request) throws Exception {
		CreatePaymentResponse response = new CreatePaymentResponse();
		response.setGenericHeader(request.getGenericHeader());
		PaymentEntity entity = new PaymentEntity();
		String paymentId = getPaymentId(request.getPaymentType());
		entity.setPaymentId(paymentId);
		entity.setPaymentName(request.getPaymentName());
		entity.setShortDetails(request.getShortDetails());
		entity.setPaymentCapita(request.getPaymentCapita());
		entity.setPaymentAmount(request.getPaymentAmount());
		entity.setGst(request.getGst());
		entity.setCurrency(SecuraConstants.PAYMENT_CURRENCY);
		entity.setCollectionStartDate(genericService.getCorrectLocalDateForInputDate(request.getCollectionStartDate()));
		entity.setCollectionEndDate(genericService.getCorrectLocalDateForInputDate(request.getCollectionEndDate()));
		entity.setPaymentCollectionCycle(request.getPaymentCollectionCycle());
		entity.setPaymentCollectionMode(request.getPaymentCollectionMode());
		entity.setApplicableFor(serializeApplicableFor(request.getApplicableFor()));
		entity.setPaymentType(request.getPaymentType());
		entity.setBankAccountId(request.getBankAccountId());
		entity.setStatus(SecuraConstants.PAYMENT_STATUS_CREATED);
		entity.setMaintainanceFee(request != null && request.isCamPayment());
		List<DueAmountDetails> dueAmountDetails = buildDueAmountDetails(request, paymentId);
		LocalDate activeDueDate = resolveEntityDueDate(dueAmountDetails, LocalDate.now());
		if (activeDueDate != null) {
			entity.setDueDate(activeDueDate.atStartOfDay());
		}
		paymentRepository.save(entity);
		updatePendingDueAmountDetailsForFlats(request, dueAmountDetails, paymentId);
		response.setMessage(SuccessMessage.SUCC_MESSAGE_23);
		response.setMessage_code(SuccessMessageCode.SUCC_MESSAGE_23);
		return response;
	}

	public String getPaymentId(String paymentType) {
		StringBuffer paymentId = new StringBuffer();
		paymentId.append(SecuraConstants.PAYMENT_ID_PREFIX);
		paymentId.append(paymentType);
		paymentId.append(1000 + ThreadLocalRandom.current().nextInt(9000));
		return paymentId.toString().toUpperCase();
	}

	private String generateUniqueDueId(String paymentId, Set<String> usedDueIds) {
		if (paymentId == null || paymentId.isBlank()) {
			return null;
		}
		for (int attempts = 0; attempts < 1000; attempts++) {
			String dueId = ("DUE" + paymentId + String.format("%03d", ThreadLocalRandom.current().nextInt(1000)))
					.toUpperCase();
			if (usedDueIds.add(dueId)) {
				return dueId;
			}
		}
		throw new IllegalStateException("Unable to generate unique dueId for paymentId: " + paymentId);
	}

}
