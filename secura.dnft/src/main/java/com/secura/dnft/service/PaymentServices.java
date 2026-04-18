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
			BigDecimal dueBaseAmount = cycleAmount.setScale(2, RoundingMode.HALF_UP);
			AddedChargesCalculation addedChargesCalculation = calculateAddedCharges(request.getAddedCharges(), dueBaseAmount);
			BigDecimal dueAmountWithAddedCharges = dueBaseAmount.add(addedChargesCalculation.getTotalChargeAmount())
					.setScale(2, RoundingMode.HALF_UP);
			BigDecimal gstAmount = dueBaseAmount.multiply(gstPercent).divide(BigDecimal.valueOf(100), 2,
					RoundingMode.HALF_UP);
			details.setAmount(formatNumber(dueBaseAmount));
			details.setGstPercentage(formatNumber(gstPercent));
			details.setGstAmount(formatNumber(gstAmount));
			details.setTotalAmount(formatNumber(dueAmountWithAddedCharges.add(gstAmount)));
			details.setAddedCharges(addedChargesCalculation.getFinalAddedCharges());
			details.setTotalAddedCharges(formatNumber(addedChargesCalculation.getTotalChargeAmount()));
			populateDueRequestMetadata(details, request);
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

				BigDecimal dueBaseAmount = calculateDueBaseAmount(periodStart, cycleMonths, end, cycleAmount);
				AddedChargesCalculation addedChargesCalculation = calculateAddedCharges(request.getAddedCharges(), dueBaseAmount);
				BigDecimal dueAmountWithAddedCharges = dueBaseAmount.add(addedChargesCalculation.getTotalChargeAmount())
						.setScale(2, RoundingMode.HALF_UP);
				BigDecimal gstAmount = dueBaseAmount.multiply(gstPercent).divide(BigDecimal.valueOf(100), 2,
						RoundingMode.HALF_UP);
				details.setAmount(formatNumber(dueBaseAmount));
				details.setGstPercentage(formatNumber(gstPercent));
				details.setGstAmount(formatNumber(gstAmount));
				details.setTotalAmount(formatNumber(dueAmountWithAddedCharges.add(gstAmount)));
				details.setAddedCharges(addedChargesCalculation.getFinalAddedCharges());
				details.setTotalAddedCharges(formatNumber(addedChargesCalculation.getTotalChargeAmount()));
				populateDueRequestMetadata(details, request);
				dueAmountDetails.add(details);

				periodStart = periodStart.plusMonths(cycleMonths);
			}
		}

		applyPaymentTypeTotals(dueAmountDetails, request.getPaymentType());
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

	private List<DueAmountDetails> resolveDueAmountDetailsForEntity(
			GetDuePaymentAmountDetailsResponse duePaymentAmountDetailsResponse) {
		if (duePaymentAmountDetailsResponse == null) {
			return List.of();
		}
		List<DueAmountDetails> listOfDueAmountDetails = duePaymentAmountDetailsResponse.getListOfDueAmountDetails();
		if (listOfDueAmountDetails != null && !listOfDueAmountDetails.isEmpty()) {
			return listOfDueAmountDetails;
		}
		Map<String, List<DueAmountDetails>> dueAmountByFlatArea = duePaymentAmountDetailsResponse.getFlatTypeDueAmountDetails();
		if (dueAmountByFlatArea == null || dueAmountByFlatArea.isEmpty()) {
			return List.of();
		}
		return dueAmountByFlatArea.values().stream().filter(dueAmountDetails -> dueAmountDetails != null)
				.filter(dueAmountDetails -> !dueAmountDetails.isEmpty()).findFirst().orElse(List.of());
	}

	private List<DueAmountDetails> resolveDueAmountDetailsForFlat(Flat flat,
			GetDuePaymentAmountDetailsResponse duePaymentAmountDetailsResponse) {
		if (duePaymentAmountDetailsResponse == null) {
			return List.of();
		}
		List<DueAmountDetails> listOfDueAmountDetails = duePaymentAmountDetailsResponse.getListOfDueAmountDetails();
		if (listOfDueAmountDetails != null && !listOfDueAmountDetails.isEmpty()) {
			return listOfDueAmountDetails;
		}
		Map<String, List<DueAmountDetails>> dueAmountByFlatArea = duePaymentAmountDetailsResponse.getFlatTypeDueAmountDetails();
		if (dueAmountByFlatArea == null || dueAmountByFlatArea.isEmpty() || flat == null || flat.getFlatArea() == null
				|| flat.getFlatArea().isBlank()) {
			return List.of();
		}
		String exactFlatArea = flat.getFlatArea().trim();
		List<DueAmountDetails> exactMatchDueAmountDetails = dueAmountByFlatArea.get(exactFlatArea);
		if (exactMatchDueAmountDetails != null) {
			return exactMatchDueAmountDetails;
		}
		String normalizedFlatArea = normalizeAreaKey(exactFlatArea);
		if (normalizedFlatArea == null) {
			return List.of();
		}
		for (Map.Entry<String, List<DueAmountDetails>> entry : dueAmountByFlatArea.entrySet()) {
			if (normalizedFlatArea.equals(normalizeAreaKey(entry.getKey()))) {
				return entry.getValue();
			}
		}
		return List.of();
	}

	private String normalizeAreaKey(String flatArea) {
		if (flatArea == null || flatArea.isBlank()) {
			return null;
		}
		String normalized = flatArea.trim().replace(",", "");
		try {
			return new BigDecimal(normalized).stripTrailingZeros().toPlainString();
		} catch (NumberFormatException e) {
			return normalized;
		}
	}

	private void populateDueRequestMetadata(DueAmountDetails details, CreatePaymentRequest request) {
		if (details == null || request == null) {
			return;
		}
		details.setPaymentName(request.getPaymentName());
		details.setPaymentType(request.getPaymentType());
		details.setAllowedPaymentModes(normalizeAllowedPaymentModes(request.getAllowedPaymentModes()));
		details.setPaymentCapita(request.getPaymentCapita());
	}

	private List<String> normalizeAllowedPaymentModes(List<String> allowedPaymentModes) {
		if (allowedPaymentModes == null || allowedPaymentModes.isEmpty()) {
			return null;
		}
		List<String> normalizedModes = allowedPaymentModes.stream().filter(mode -> mode != null).map(String::trim)
				.filter(mode -> !mode.isEmpty()).collect(Collectors.toList());
		if (normalizedModes.isEmpty()) {
			return null;
		}
		return normalizedModes;
	}

	private void applyPaymentTypeTotals(List<DueAmountDetails> dueAmountDetails, String paymentType) {
		if (dueAmountDetails == null || dueAmountDetails.isEmpty()) {
			return;
		}
		BigDecimal totalAmount = dueAmountDetails.stream().map(DueAmountDetails::getTotalAmount).map(this::parseNumeric)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		boolean optionalPaymentType = paymentType != null && paymentType.trim().equalsIgnoreCase("optional");
		String totalMandatoryAmount = optionalPaymentType ? formatNumber(BigDecimal.ZERO) : formatNumber(totalAmount);
		String totalOptionalAmount = optionalPaymentType ? formatNumber(totalAmount) : formatNumber(BigDecimal.ZERO);
		for (DueAmountDetails details : dueAmountDetails) {
			details.setTotalMandatoryPaymentAmount(totalMandatoryAmount);
			details.setTotalOptionalPaymentAmount(totalOptionalAmount);
		}
	}

	private void updatePendingDueAmountDetailsForFlats(CreatePaymentRequest request,
			GetDuePaymentAmountDetailsResponse duePaymentAmountDetailsResponse, String paymentId) {
		String apartmentId = request != null && request.getGenericHeader() != null ? request.getGenericHeader().getApartmentId()
				: null;
		List<Flat> apartmentFlats = (apartmentId == null || apartmentId.isBlank()) ? flatRepository.findAll()
				: flatRepository.findByAprmntId(apartmentId);
		Set<String> applicableFlatNos = parseApplicableFlatNos(request != null ? request.getApplicableFor() : null);

		List<Flat> targetFlats = apartmentFlats.stream().filter(flat->applicableFlatNos.contains(flat.getFlatNo())).collect(Collectors.toList());
		if (!applicableFlatNos.isEmpty()) {
			Set<String> normalizedApplicableFlatNos = applicableFlatNos.stream().map(this::normalizeFlatNoForMatch)
					.filter(value -> value != null).collect(Collectors.toCollection(LinkedHashSet::new));
			targetFlats = apartmentFlats.stream().filter(flat -> flat != null && flat.getFlatNo() != null
					&& normalizedApplicableFlatNos.contains(normalizeFlatNoForMatch(flat.getFlatNo())))
					.collect(Collectors.toList());
		}

		LocalDate today = LocalDate.now();
		for (Flat flat : targetFlats) {
			List<DueAmountDetails> dueAmountDetailsForFlat = resolveDueAmountDetailsForFlat(flat, duePaymentAmountDetailsResponse);
			List<DueAmountDetails> existingDueAmountDetails = parsePendingDueAmountDetails(flat.getFlatPndngPaymntLst());
			existingDueAmountDetails.addAll(cloneDueAmountDetails(dueAmountDetailsForFlat));
			ensureDueIdsForFlatSave(existingDueAmountDetails, paymentId);
			applyRequestCodesToFutureDues(existingDueAmountDetails, request, today);
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
			copy.setGstPercentage(details.getGstPercentage());
			copy.setGstAmount(details.getGstAmount());
			copy.setTotalAmount(details.getTotalAmount());
			copy.setPaymentName(details.getPaymentName());
			copy.setPaymentType(details.getPaymentType());
			copy.setAllowedPaymentModes(details.getAllowedPaymentModes() == null ? null : new ArrayList<>(details.getAllowedPaymentModes()));
			copy.setPaymentCapita(details.getPaymentCapita());
			copy.setTotalMandatoryPaymentAmount(details.getTotalMandatoryPaymentAmount());
			copy.setTotalOptionalPaymentAmount(details.getTotalOptionalPaymentAmount());
			copy.setAddedCharges(cloneAddedCharges(details.getAddedCharges()));
			copy.setTotalAddedCharges(details.getTotalAddedCharges());
			copy.setDiscountCode(details.getDiscountCode());
			copy.setFineCode(details.getFineCode());
			cloned.add(copy);
		}
		return cloned;
	}

	private void applyRequestCodesToFutureDues(List<DueAmountDetails> dueAmountDetails, CreatePaymentRequest request,
			LocalDate today) {
		if (dueAmountDetails == null || dueAmountDetails.isEmpty() || request == null) {
			return;
		}
		boolean hasDiscountCode = hasText(request.getDiscountCode());
		boolean hasFineCode = hasText(request.getFineCode());
		if (!hasDiscountCode && !hasFineCode) {
			return;
		}

		for (DueAmountDetails details : dueAmountDetails) {
			if (details == null || details.getDueDate() == null || !details.getDueDate().isAfter(today)) {
				continue;
			}
			if (!hasText(details.getDueId())) {
				continue;
			}
			if (hasDiscountCode) {
				details.setDiscountCode(request.getDiscountCode());
			}
			if (hasFineCode) {
				details.setFineCode(request.getFineCode());
			}
		}
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
			String paymentId = details.getPaymentId();
			if ((paymentId == null || paymentId.isBlank()) && fallbackPaymentId != null && !fallbackPaymentId.isBlank()) {
				paymentId = fallbackPaymentId;
				details.setPaymentId(paymentId);
			}
			if (details.getDueId() != null && !details.getDueId().isBlank()) {
				continue;
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
		entity.setPaymentCollectionCycle(normalizePaymentCollectionCycle(request.getPaymentCollectionCycle()));
		entity.setPaymentCollectionMode(request.getPaymentCollectionMode());
		entity.setApplicableFor(serializeApplicableFor(request.getApplicableFor()));
		entity.setAllowedPaymentModes(serializeAllowedPaymentModes(request.getAllowedPaymentModes()));
		entity.setAddedCharges(serializeAddedCharges(request.getAddedCharges()));
		entity.setDiscFin(serializeDiscFin(request));
		entity.setPaymentType(request.getPaymentType());
		entity.setBankAccountId(request.getBankAccountId());
		entity.setAprmtId(request.getGenericHeader() != null ? request.getGenericHeader().getApartmentId() : null);
		entity.setStatus(SecuraConstants.PAYMENT_STATUS_ACTIVE);
		entity.setCreatUsrId(request.getGenericHeader() != null ? request.getGenericHeader().getUserId() : null);
		entity.setMaintainanceFee(request != null && request.isCamPayment());
		GetDuePaymentAmountDetailsResponse duePaymentAmountDetailsResponse = getDuePaymentAmountDetails(request);
		List<DueAmountDetails> dueAmountDetails = resolveDueAmountDetailsForEntity(duePaymentAmountDetailsResponse);
		if (dueAmountDetails.isEmpty()) {
			dueAmountDetails = buildDueAmountDetails(request, paymentId);
		}
		LocalDate activeDueDate = resolveEntityDueDate(dueAmountDetails, LocalDate.now());
		if (activeDueDate != null) {
			entity.setDueDate(activeDueDate.atStartOfDay());
		}
		paymentRepository.save(entity);
		updatePendingDueAmountDetailsForFlats(request, duePaymentAmountDetailsResponse, paymentId);
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
			discFin.add(discount);
		}
		if (hasText(request.getFineCode())) {
			Map<String, String> fine = new LinkedHashMap<>();
			fine.put("DISTFIN_TYPE", "FINE");
			fine.put("code", request.getFineCode());
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

}
