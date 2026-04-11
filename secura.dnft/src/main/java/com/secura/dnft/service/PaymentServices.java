package com.secura.dnft.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
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

	@Autowired
	GenericService genericService;

	@Autowired
	PaymentRepository paymentRepository;
	
	@Autowired
	FlatRepository flatRepository;

	private static final String DUE_STATUS_NOT_ACTIVE = "NOT ACTIVE";

	@Override
	public DuePaymentAmountDetailsResponse getDuePaymentAmountDetails(DuePaymentAmountDetailsRequest request) {
		DuePaymentAmountDetailsResponse response = new DuePaymentAmountDetailsResponse();
		response.setGenericHeader(request.getGenericHeader());
		response.setPaymentCapita(request.getPaymentCapita());

		if (request.getCollectionStartDate() == null || request.getCollectionEndDate() == null
				|| request.getTodayDate() == null) {
			return response;
		}

		int cycleMonths = getCycleMonths(request.getPaymentCollectionCycle());
		if (cycleMonths <= 0) {
			return response;
		}

		DueWindow dueWindow = calculateDueWindow(request.getCollectionStartDate(), request.getCollectionEndDate(),
				request.getTodayDate(), request.getPaymentCollectionMode(), cycleMonths);
		LocalDate dueDate = dueWindow.getDueDate();
		response.setDueDate(dueDate);

		BigDecimal cycleAmount = parseNumeric(request.getPaymentAmount());
		BigDecimal gstPercent = parseNumeric(request.getGst());

		BigDecimal dueBaseAmount = calculateDueBaseAmount(dueWindow.getChargePeriodStart(), cycleMonths,
				request.getCollectionEndDate(), cycleAmount);
		BigDecimal gstAmount = dueBaseAmount.multiply(gstPercent).divide(BigDecimal.valueOf(100), 2,
				RoundingMode.HALF_UP);
		BigDecimal totalWithGst = dueBaseAmount.add(gstAmount);

		response.setAmountExcludingGst(formatNumber(dueBaseAmount));
		response.setGstPercent(formatNumber(gstPercent));
		response.setGstAmount(formatNumber(gstAmount));
		response.setAmountIncludingGst(formatNumber(totalWithGst));

		return response;
	}
	
	@Override
	public GetDuePaymentAmountDetailsResponse getDuePaymentAmountDetails(CreatePaymentRequest request) {
		GetDuePaymentAmountDetailsResponse response = new GetDuePaymentAmountDetailsResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		response.setListOfDueAmountDetails(buildDueAmountDetails(request, null, LocalDate.now()));
		return response;
	}

	private List<DueAmountDetails> buildDueAmountDetails(CreatePaymentRequest request, String paymentId, LocalDate today) {
		List<DueAmountDetails> dueAmountDetails = new ArrayList<>();
		if (request == null || request.getCollectionStartDate() == null || request.getCollectionEndDate() == null) {
			return dueAmountDetails;
		}

		int cycleMonths = getCycleMonths(request.getPaymentCollectionCycle());
		if (cycleMonths <= 0) {
			return dueAmountDetails;
		}

		LocalDate start = request.getCollectionStartDate().toLocalDate();
		LocalDate end = request.getCollectionEndDate().toLocalDate();
		if (start.isAfter(end)) {
			return dueAmountDetails;
		}

		BigDecimal cycleAmount = parseNumeric(request.getPaymentAmount());
		BigDecimal gstPercent = parseNumeric(request.getGst());

		LocalDate periodStart = start;
		while (!periodStart.isAfter(end)) {
			LocalDate periodEnd = periodStart.plusMonths(cycleMonths).minusDays(1);
			if (periodEnd.isAfter(end)) {
				periodEnd = end;
			}
			DueAmountDetails details = new DueAmountDetails();
			details.setDueDate(isPost(request.getPaymentCollectionMode()) ? periodEnd.plusDays(1) : periodStart);
			details.setPaymentId(paymentId);
			details.setStatus(DUE_STATUS_NOT_ACTIVE);

			BigDecimal dueBaseAmount = calculateDueBaseAmount(periodStart, cycleMonths, end, cycleAmount);
			BigDecimal gstAmount = dueBaseAmount.multiply(gstPercent).divide(BigDecimal.valueOf(100), 2,
					RoundingMode.HALF_UP);
			details.setAmount(formatNumber(dueBaseAmount.add(gstAmount)));
			dueAmountDetails.add(details);

			periodStart = periodStart.plusMonths(cycleMonths);
		}

		markUpcomingDueAsActive(dueAmountDetails, today);
		return dueAmountDetails;
	}

	private void markUpcomingDueAsActive(List<DueAmountDetails> dueAmountDetails, LocalDate today) {
		for (DueAmountDetails details : dueAmountDetails) {
			details.setStatus(DUE_STATUS_NOT_ACTIVE);
		}
		for (DueAmountDetails details : dueAmountDetails) {
			if (!details.getDueDate().isBefore(today)) {
				details.setStatus(SecuraConstants.PAYMENT_STATUS_ACTIVE);
				break;
			}
		}
	}

	private LocalDate resolveEntityDueDate(List<DueAmountDetails> dueAmountDetails) {
		for (DueAmountDetails details : dueAmountDetails) {
			if (SecuraConstants.PAYMENT_STATUS_ACTIVE.equals(details.getStatus())) {
				return details.getDueDate();
			}
		}
		return dueAmountDetails.isEmpty() ? null : dueAmountDetails.get(0).getDueDate();
	}

	private Set<String> parseApplicableFlatNos(String applicableFor) {
		Set<String> flatNos = new LinkedHashSet<>();
		if (applicableFor == null || applicableFor.isBlank()) {
			return flatNos;
		}
		if ("ALL".equalsIgnoreCase(applicableFor.trim())) {
			return flatNos;
		}
		try {
			List<String> parsed = genericService.fromJson(applicableFor, new TypeReference<List<String>>() {
			});
			if (parsed != null) {
				flatNos.addAll(parsed.stream().filter(flatNo -> flatNo != null && !flatNo.isBlank()).map(String::trim)
						.collect(Collectors.toList()));
			}
		} catch (RuntimeException e) {
			// ignore and fallback to CSV parsing
		}
		if (!flatNos.isEmpty()) {
			return flatNos;
		}
		String[] split = applicableFor.split(",");
		for (String value : split) {
			if (value != null && !value.trim().isBlank()) {
				flatNos.add(value.trim());
			}
		}
		return flatNos;
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

	private void updatePendingDueAmountDetailsForFlats(CreatePaymentRequest request, List<DueAmountDetails> dueAmountDetails) {
		String apartmentId = request != null && request.getGenericHeader() != null ? request.getGenericHeader().getApartmentId()
				: null;
		List<Flat> apartmentFlats = (apartmentId == null || apartmentId.isBlank()) ? flatRepository.findAll()
				: flatRepository.findByAprmntId(apartmentId);
		Set<String> applicableFlatNos = parseApplicableFlatNos(request != null ? request.getApplicableFor() : null);

		List<Flat> targetFlats = apartmentFlats;
		if (!applicableFlatNos.isEmpty()) {
			targetFlats = apartmentFlats.stream().filter(flat -> applicableFlatNos.contains(flat.getFlatNo()))
					.collect(Collectors.toList());
		}

		for (Flat flat : targetFlats) {
			List<DueAmountDetails> existingDueAmountDetails = parsePendingDueAmountDetails(flat.getFlatPndngPaymntLst());
			existingDueAmountDetails.addAll(dueAmountDetails);
			flat.setFlatPndngPaymntLst(genericService.toJson(existingDueAmountDetails));
			flatRepository.save(flat);
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
		entity.setApplicableFor(request.getApplicableFor());
		entity.setPaymentType(request.getPaymentType());
		entity.setBankAccountId(request.getBankAccountId());
		entity.setStatus(SecuraConstants.PAYMENT_STATUS_CREATED);
		List<DueAmountDetails> dueAmountDetails = buildDueAmountDetails(request, paymentId, LocalDate.now());
		LocalDate activeDueDate = resolveEntityDueDate(dueAmountDetails);
		if (activeDueDate != null) {
			entity.setDueDate(activeDueDate.atStartOfDay());
		}
		paymentRepository.save(entity);
		updatePendingDueAmountDetailsForFlats(request, dueAmountDetails);
		response.setMessage(SuccessMessage.SUCC_MESSAGE_23);
		response.setMessage_code(SuccessMessageCode.SUCC_MESSAGE_23);
		return response;
	}

	public String getPaymentId(String paymentType) {
		StringBuffer paymentId = new StringBuffer();
		paymentId.append(SecuraConstants.PAYMENT_ID_PREFIX);
		paymentId.append(paymentType);
		Random random= new Random();
		paymentId.append( 1000 + random.nextInt(9000));
		return paymentId.toString().toUpperCase();
	}

}
