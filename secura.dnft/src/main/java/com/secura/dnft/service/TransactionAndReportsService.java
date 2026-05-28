package com.secura.dnft.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.dao.DueAmountDetailsRepository;
import com.secura.dnft.dao.FlatRepository;
import com.secura.dnft.dao.OwnerRepository;
import com.secura.dnft.dao.PaymentRepository;
import com.secura.dnft.dao.ProfileRepository;
import com.secura.dnft.dao.TransactionRepository;
import com.secura.dnft.entity.DueAmountDetailsEntity;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.Owner;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.entity.Profile;
import com.secura.dnft.entity.Transaction;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.Name;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.BankInstrumentTenderDetails;
import com.secura.dnft.request.response.DefaultPayment;
import com.secura.dnft.request.response.Defaulter;
import com.secura.dnft.request.response.GetBalanceSheetRequest;
import com.secura.dnft.request.response.GetBalanceSheetResponse;
import com.secura.dnft.request.response.GetDefaulterRequest;
import com.secura.dnft.request.response.GetDefaulterResponse;
import com.secura.dnft.request.response.GetTransactionRequest;
import com.secura.dnft.request.response.GetTransactionResponse;
import com.secura.dnft.request.response.PaymentTenderData;
import com.secura.dnft.request.response.ReportPaymentData;
import com.secura.dnft.request.response.TransactionResponseItem;

@Service
public class TransactionAndReportsService {

	private static final String TRNS_TYPE_DEBIT = "DEBIT";
	private static final String TRNS_TYPE_CREDIT = "CREDIT";
	private static final String TRNS_STATUS_SUCCESS = "SUCCESS";
	private static final String OTHERS_KEY = "Others";
	private static final String PAYMENT_TYPE_MANDATORY = "MANDATORY";

	@Autowired
	TransactionRepository transactionRepository;

	@Autowired
	PaymentRepository paymentRepository;

	@Autowired
	GenericService genericService;

	@Autowired
	DueAmountDetailsRepository dueAmountDetailsRepository;

	@Autowired
	FlatRepository flatRepository;

	@Autowired
	OwnerRepository ownerRepository;

	@Autowired
	ProfileRepository profileRepository;

	public GetTransactionResponse getTransaction(GetTransactionRequest request) {
		GetTransactionResponse response = new GetTransactionResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);

		List<Transaction> transactions = new ArrayList<>();
		String transactionId = request != null ? request.getTransactionId() : null;
		String aprmntId = request != null && request.getGenericHeader() != null
				? request.getGenericHeader().getApartmentId()
				: null;

		if (transactionId != null && !transactionId.isBlank()) {
			Optional<Transaction> transaction = transactionRepository.findById(transactionId);
			transaction.ifPresent(transactions::add);
		} else if (aprmntId != null && !aprmntId.isBlank()) {
			transactions = transactionRepository.findByAprmntId(aprmntId);
		}

		//List<TransactionResponseItem> transactionList = new ArrayList<>();
		List<TransactionResponseItem> transactionList = transactions.stream().map(trns->toResponseItem(trns)).collect(Collectors.toList());

//		for (Transaction transaction : transactions) {
//			transactionList.add(toResponseItem(transaction));
//		}

		response.setTransactionList(transactionList);
		if (transactionList.isEmpty()) {
			response.setMessage(SuccessMessage.SUCC_MESSAGE_42);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_42);
		} else {
			response.setMessage(SuccessMessage.SUCC_MESSAGE_41);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_41);
		}
		return response;
	}

	public GetBalanceSheetResponse getBalanceSheet(GetBalanceSheetRequest request) {
		GetBalanceSheetResponse response = new GetBalanceSheetResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);

		LocalDate toDate = (request != null && request.getToDate() != null) ? request.getToDate() : LocalDate.now();
		LocalDate fromDate = (request != null && request.getFromDate() != null) ? request.getFromDate() : toDate.minusYears(1);
		response.setFromDate(fromDate);
		response.setToDate(toDate);

		String aprmntId = request != null && request.getGenericHeader() != null
				? request.getGenericHeader().getApartmentId()
				: null;

		LocalDateTime fromDateTime = fromDate.atStartOfDay();
		LocalDateTime toDateTime = toDate.atTime(LocalTime.MAX);

		response.setDebitPaymentData(buildDebitPaymentData(aprmntId, fromDateTime, toDateTime));
		response.setCreditPaymentData(buildCreditPaymentData(aprmntId, fromDateTime, toDateTime));

		response.setMessage(SuccessMessage.SUCC_MESSAGE_41);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_41);
		return response;
	}

	public GetDefaulterResponse getDefaulterList(GetDefaulterRequest request) {
		GetDefaulterResponse response = initializeDefaulterResponse(request);
		String apartmentId = request != null && request.getGenericHeader() != null
				? request.getGenericHeader().getApartmentId()
				: null;
		if (!hasText(apartmentId)) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_05);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_05);
			return response;
		}

		List<String> requestedPaymentIds = normalizeRequestPaymentIds(request != null ? request.getPaymentId() : null);
		if (requestedPaymentIds.isEmpty()) {
			markDefaulterResponseSuccess(response);
			return response;
		}

		Map<String, DefaulterAccumulator> defaulterMap = new LinkedHashMap<>();
		Map<String, BigDecimal> amountPaidCache = new HashMap<>();
		Map<String, Optional<Flat>> flatCache = new HashMap<>();
		Map<String, String> flatAreaCache = new HashMap<>();
		Map<String, List<Owner>> ownerCache = new HashMap<>();
		Map<String, Optional<Profile>> profileCache = new HashMap<>();

		for (String paymentId : requestedPaymentIds) {
			List<PaymentEntity> paymentEntities = paymentRepository.findByPaymentIdAndAprmtId(paymentId, apartmentId);
			if (paymentEntities == null || paymentEntities.isEmpty() || !isActiveMandatoryPayment(paymentEntities)) {
				continue;
			}

			String paymentCapita = resolvePaymentCapita(paymentEntities);
			List<DueAmountDetailsEntity> overdueDues = dueAmountDetailsRepository.findByPaymentId(paymentId).stream()
					.filter(Objects::nonNull)
					.filter(this::isOverdueDue)
					.collect(Collectors.toList());
			if (isPerSqftCapita(paymentCapita)) {
				processPerSqftDues(paymentId, paymentEntities, paymentCapita, overdueDues, defaulterMap, flatCache,
						flatAreaCache);
			} else {
				processSelectedCycleDues(paymentId, paymentEntities, paymentCapita, selectHighestPriorityDues(overdueDues),
						defaulterMap);
			}
		}

		List<Defaulter> defaulterList = new ArrayList<>();
		for (DefaulterAccumulator accumulator : defaulterMap.values()) {
			List<DefaultPayment> defaultPayments = new ArrayList<>();
			for (DefaultPaymentAccumulator paymentAccumulator : accumulator.defaultPaymentMap().values()) {
				BigDecimal amountPaid = resolveAmountPaid(paymentAccumulator.paymentId(), accumulator.flatId(), amountPaidCache);
				BigDecimal amountToBePaid = paymentAccumulator.totalDue().subtract(amountPaid);
				if (amountToBePaid.compareTo(BigDecimal.ZERO) <= 0) {
					continue;
				}
				DefaultPayment defaultPayment = new DefaultPayment();
				defaultPayment.setPaymentId(paymentAccumulator.paymentId());
				defaultPayment.setPaymentName(paymentAccumulator.paymentName());
				defaultPayment.setPaymentCapita(formatDisplayValue(paymentAccumulator.paymentCapita()));
				defaultPayment.setTotalDue(formatAmount(paymentAccumulator.totalDue()));
				defaultPayment.setAmountPaid(formatAmount(amountPaid));
				defaultPayment.setAmountTobePaid(formatAmount(amountToBePaid));
				defaultPayment.setPenalty(formatAmount(paymentAccumulator.penalty()));
				defaultPayment.setLastDueDate(paymentAccumulator.lastDueDate());
				defaultPayments.add(defaultPayment);
			}
			if (defaultPayments.isEmpty()) {
				continue;
			}

			List<Profile> ownerProfiles = resolveOwnerProfiles(accumulator.flatId(), flatCache, ownerCache, profileCache);
			Defaulter defaulter = new Defaulter();
			defaulter.setFlatId(accumulator.flatId());
			defaulter.setBuiltUpArea(resolveFlatAreaTypeDisplay(accumulator.flatId(), flatCache));
			defaulter.setOwnerNames(resolveOwnerNames(ownerProfiles));
			defaulter.setPhoneNumber(resolvePhoneNumber(ownerProfiles));
			defaulter.setEmailId(resolveEmailId(ownerProfiles));
			defaulter.setDefaultPaymentList(defaultPayments);
			defaulterList.add(defaulter);
		}
		response.setDefaulterList(defaulterList);
		response.setTotalDefaulters(defaulterList.size());
		updateDefaulterTotals(response, apartmentId, requestedPaymentIds);
		markDefaulterResponseSuccess(response);
		return response;
	}

	private List<ReportPaymentData> buildDebitPaymentData(String aprmntId, LocalDateTime from, LocalDateTime to) {
		List<Transaction> debitTrns = aprmntId != null
				? transactionRepository.findByAprmntIdAndTrnsTypeAndTrnsDateBetween(aprmntId, TRNS_TYPE_DEBIT, from, to)
				: new ArrayList<>();

		Map<String, BigDecimal> causeAmountMap = new LinkedHashMap<>();
		for (Transaction trns : debitTrns) {
			String cause = trns.getCause() != null && !trns.getCause().isBlank() ? trns.getCause() : OTHERS_KEY;
			BigDecimal amt = parseBigDecimal(trns.getTrnsAmt());
			causeAmountMap.merge(cause, amt, BigDecimal::add);
		}

		List<ReportPaymentData> result = new ArrayList<>();
		for (Map.Entry<String, BigDecimal> entry : causeAmountMap.entrySet()) {
			ReportPaymentData data = new ReportPaymentData();
			data.setPaymentName(entry.getKey());
			data.setTotalAmountIncludingTax(entry.getValue().toPlainString());
			result.add(data);
		}
		return result;
	}

	private static final String KEY_PREFIX_PAYMENT = "P:";
	private static final String KEY_PREFIX_CAUSE = "C:";
	private static final String KEY_PREFIX_OTHERS = "O:";

	private List<ReportPaymentData> buildCreditPaymentData(String aprmntId, LocalDateTime from, LocalDateTime to) {
		List<Transaction> creditTrns = aprmntId != null
				? transactionRepository.findByAprmntIdAndTrnsTypeAndTrnsStatusAndTrnsDateBetween(
						aprmntId, TRNS_TYPE_CREDIT, TRNS_STATUS_SUCCESS, from, to)
				: new ArrayList<>();

		// Accumulate per-row sums. Keys are prefixed to distinguish grouping type:
		// "P:<paymentId>" for payment-based, "C:<cause>" for cause-based, "O:Others" for fallback.
		// index: 0=totalAddedCharges, 1=totalAmountExcludingTax, 2=totalAmountIncludingTax, 3=taxCollected
		Map<String, BigDecimal[]> accumulator = new LinkedHashMap<>();

		for (Transaction trns : creditTrns) {
			String effectiveKey;
			if (trns.getPymntId() != null && !trns.getPymntId().isBlank()) {
				effectiveKey = KEY_PREFIX_PAYMENT + trns.getPymntId();
			} else if (trns.getCause() != null && !trns.getCause().isBlank()) {
				effectiveKey = KEY_PREFIX_CAUSE + trns.getCause();
			} else {
				effectiveKey = KEY_PREFIX_OTHERS + OTHERS_KEY;
			}

			BigDecimal trnsAmt = parseBigDecimal(trns.getTrnsAmt());

			// TODO: restore totalAddedCharges and gstAmount from DueAmountDetails on reimplementation
			BigDecimal totalAddedCharges = BigDecimal.ZERO;
			BigDecimal gstAmount = BigDecimal.ZERO;

			BigDecimal totalAmountIncludingTax = trnsAmt;
			BigDecimal totalAmountExcludingTax = trnsAmt.subtract(totalAddedCharges).subtract(gstAmount);
			BigDecimal taxCollected = totalAddedCharges.add(gstAmount);

			BigDecimal[] sums = accumulator.computeIfAbsent(effectiveKey, k -> new BigDecimal[]{
					BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
			sums[0] = sums[0].add(totalAddedCharges);
			sums[1] = sums[1].add(totalAmountExcludingTax);
			sums[2] = sums[2].add(totalAmountIncludingTax);
			sums[3] = sums[3].add(taxCollected);
		}

		List<ReportPaymentData> result = new ArrayList<>();
		for (Map.Entry<String, BigDecimal[]> entry : accumulator.entrySet()) {
			String effectiveKey = entry.getKey();
			BigDecimal[] sums = entry.getValue();

			ReportPaymentData data = new ReportPaymentData();
			if (effectiveKey.startsWith(KEY_PREFIX_PAYMENT)) {
				String paymentId = effectiveKey.substring(KEY_PREFIX_PAYMENT.length());
				data.setPaymentId(paymentId);
				Optional<PaymentEntity> paymentOpt = paymentRepository.findFirstByPaymentId(paymentId);
				if (paymentOpt.isPresent()) {
					data.setPaymentName(paymentOpt.get().getPaymentName());
					data.setPaymentAmount(paymentOpt.get().getPaymentAmount());
				}
			} else if (effectiveKey.startsWith(KEY_PREFIX_CAUSE)) {
				String cause = effectiveKey.substring(KEY_PREFIX_CAUSE.length());
				data.setPaymentId(cause);
				data.setPaymentName(cause);
			} else {
				data.setPaymentId(OTHERS_KEY);
				data.setPaymentName(OTHERS_KEY);
			}
			data.setTotalAddedCharges(sums[0].toPlainString());
			data.setTotalAmountExcludingTax(roundUp(sums[1]).toPlainString());
			data.setTotalAmountIncludingTax(sums[2].toPlainString());
			data.setTaxCollected(sums[3].toPlainString());
			result.add(data);
		}
		return result;
	}

	private GetDefaulterResponse initializeDefaulterResponse(GetDefaulterRequest request) {
		GetDefaulterResponse response = new GetDefaulterResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		response.setDefaulterList(new ArrayList<>());
		response.setTotalDefaulters(0);
		response.setTotalMoneyCollected(BigDecimal.ZERO.toPlainString());
		response.setTotalExpectedToBeCollect(BigDecimal.ZERO.toPlainString());
		return response;
	}

	private void markDefaulterResponseSuccess(GetDefaulterResponse response) {
		response.setMessage(SuccessMessage.SUCC_MESSAGE_45);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_45);
	}

	private void updateDefaulterTotals(GetDefaulterResponse response, String apartmentId, List<String> requestedPaymentIds) {
		if (response == null) {
			return;
		}
		BigDecimal totalMoneyCollected = resolveTotalMoneyCollected(apartmentId, requestedPaymentIds);
		BigDecimal totalExpectedToCollect = BigDecimal.ZERO;
		if (response.getDefaulterList() != null) {
			for (Defaulter defaulter : response.getDefaulterList()) {
				if (defaulter == null || defaulter.getDefaultPaymentList() == null) {
					continue;
				}
				for (DefaultPayment defaultPayment : defaulter.getDefaultPaymentList()) {
					if (defaultPayment == null) {
						continue;
					}
					totalExpectedToCollect = totalExpectedToCollect
							.add(parseBigDecimal(defaultPayment.getAmountTobePaid()));
				}
			}
		}
		response.setTotalMoneyCollected(formatAmount(totalMoneyCollected));
		response.setTotalExpectedToBeCollect(formatAmount(totalExpectedToCollect));
	}

	private BigDecimal resolveTotalMoneyCollected(String apartmentId, List<String> requestedPaymentIds) {
		if (!hasText(apartmentId) || requestedPaymentIds == null || requestedPaymentIds.isEmpty()) {
			return BigDecimal.ZERO;
		}
		List<Transaction> transactions = transactionRepository.findByAprmntIdAndPymntIdInAndTrnsStatus(
				apartmentId, requestedPaymentIds, TRNS_STATUS_SUCCESS);
		if (transactions == null || transactions.isEmpty()) {
			return BigDecimal.ZERO;
		}
		return transactions.stream().filter(Objects::nonNull).map(Transaction::getTrnsAmt).map(this::parseBigDecimal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private List<String> normalizeRequestPaymentIds(List<String> paymentIds) {
		if (paymentIds == null || paymentIds.isEmpty()) {
			return Collections.emptyList();
		}
		LinkedHashSet<String> normalized = new LinkedHashSet<>();
		for (String paymentId : paymentIds) {
			if (hasText(paymentId)) {
				normalized.add(paymentId.trim());
			}
		}
		return new ArrayList<>(normalized);
	}

	private boolean isActiveMandatoryPayment(List<PaymentEntity> paymentEntities) {
		boolean active = paymentEntities.stream().filter(Objects::nonNull).map(PaymentEntity::getStatus).filter(this::hasText)
				.anyMatch(status -> SecuraConstants.PAYMENT_STATUS_ACTIVE.equalsIgnoreCase(status.trim()));
		if (!active) {
			return false;
		}
		return paymentEntities.stream().filter(Objects::nonNull).map(PaymentEntity::getPaymentType).filter(this::hasText)
				.anyMatch(type -> PAYMENT_TYPE_MANDATORY.equalsIgnoreCase(type.trim()));
	}

	private boolean isOverdueDue(DueAmountDetailsEntity due) {
		return due != null && due.getDueDate() != null && due.getDueDate().isBefore(LocalDate.now());
	}

	private List<String> findPendingFlatIds(DueAmountDetailsEntity due) {
		List<String> applicableFlats = parseStringList(due != null ? due.getApplicableFlats() : null);
		if (applicableFlats.isEmpty()) {
			return Collections.emptyList();
		}
		List<String> paidFlats = parseStringList(due != null ? due.getPaidFlats() : null);
		return applicableFlats.stream().filter(this::hasText).map(String::trim)
				.filter(flatId -> paidFlats.stream().filter(this::hasText).map(String::trim)
						.noneMatch(paidFlat -> paidFlat.equalsIgnoreCase(flatId)))
				.distinct().collect(Collectors.toList());
	}

	private String resolvePaymentName(List<PaymentEntity> paymentEntities, DueAmountDetailsEntity due) {
		String paymentName = paymentEntities == null ? null
				: paymentEntities.stream().filter(Objects::nonNull).map(PaymentEntity::getPaymentName).filter(this::hasText)
						.findFirst().orElse(null);
		if (hasText(paymentName)) {
			return paymentName;
		}
		return due != null ? due.getPaymentName() : null;
	}

	private String resolvePaymentCapita(List<PaymentEntity> paymentEntities) {
		return paymentEntities == null ? null
				: paymentEntities.stream().filter(Objects::nonNull).map(PaymentEntity::getPaymentCapita).filter(this::hasText)
						.findFirst().orElse(null);
	}

	private void processPerSqftDues(String paymentId, List<PaymentEntity> paymentEntities, String paymentCapita,
			List<DueAmountDetailsEntity> overdueDues, Map<String, DefaulterAccumulator> defaulterMap,
			Map<String, Optional<Flat>> flatCache, Map<String, String> flatAreaCache) {
		List<String> pendingFlatIds = overdueDues.stream().flatMap(due -> findPendingFlatIds(due).stream()).distinct()
				.collect(Collectors.toList());
		for (String flatId : pendingFlatIds) {
			String flatArea = resolveFlatArea(flatId, flatCache, flatAreaCache);
			List<DueAmountDetailsEntity> areaMatchedDues = overdueDues.stream()
					.filter(due -> matchesFlatArea(due, flatArea)).collect(Collectors.toList());
			List<DueAmountDetailsEntity> selectedDues = selectHighestPriorityDues(areaMatchedDues);
			List<DueAmountDetailsEntity> selectedDuesForFlat = selectedDues.stream()
					.filter(due -> findPendingFlatIds(due).stream().anyMatch(pendingFlat -> pendingFlat.equalsIgnoreCase(flatId)))
					.collect(Collectors.toList());
			addSelectedDues(paymentId, paymentEntities, paymentCapita, flatId, selectedDuesForFlat, defaulterMap);
		}
	}

	private void processSelectedCycleDues(String paymentId, List<PaymentEntity> paymentEntities, String paymentCapita,
			List<DueAmountDetailsEntity> selectedDues, Map<String, DefaulterAccumulator> defaulterMap) {
		for (DueAmountDetailsEntity due : selectedDues) {
			for (String flatId : findPendingFlatIds(due)) {
				addSelectedDues(paymentId, paymentEntities, paymentCapita, flatId, List.of(due), defaulterMap);
			}
		}
	}

	private void addSelectedDues(String paymentId, List<PaymentEntity> paymentEntities, String paymentCapita, String flatId,
			List<DueAmountDetailsEntity> dues, Map<String, DefaulterAccumulator> defaulterMap) {
		if (dues == null || dues.isEmpty()) {
			return;
		}
		DefaulterAccumulator defaulter = defaulterMap.computeIfAbsent(flatId, DefaulterAccumulator::new);
		DefaultPaymentAccumulator defaultPayment = defaulter.defaultPaymentMap()
				.computeIfAbsent(paymentId, ignored -> new DefaultPaymentAccumulator(paymentId,
						resolvePaymentName(paymentEntities, dues.get(0)), paymentCapita));
		dues.forEach(defaultPayment::addDue);
	}

	private List<DueAmountDetailsEntity> selectHighestPriorityDues(List<DueAmountDetailsEntity> dues) {
		if (dues == null || dues.isEmpty()) {
			return Collections.emptyList();
		}
		int highestPriority = dues.stream().filter(Objects::nonNull).map(DueAmountDetailsEntity::getCollectionCycle)
				.mapToInt(DefaultPaymentAccumulator::resolveCyclePriority).max().orElse(0);
		if (highestPriority <= 0) {
			return Collections.emptyList();
		}
		return dues.stream().filter(Objects::nonNull)
				.filter(due -> DefaultPaymentAccumulator.resolveCyclePriority(due.getCollectionCycle()) == highestPriority)
				.collect(Collectors.toList());
	}

	private String resolveFlatArea(String flatId, Map<String, Optional<Flat>> flatCache, Map<String, String> flatAreaCache) {
		if (!hasText(flatId)) {
			return null;
		}
		return flatAreaCache.computeIfAbsent(flatId, ignored -> flatCache.computeIfAbsent(flatId,
				key -> flatRepository.findById(key)).map(Flat::getFlatArea).orElse(null));
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
		String dueFlatArea = due.getFlatArea();
		if (!hasText(dueFlatArea) || "ALL".equalsIgnoreCase(dueFlatArea.trim())) {
			return true;
		}
		return hasText(flatArea) && dueFlatArea.trim().equalsIgnoreCase(flatArea.trim());
	}

	private BigDecimal resolveAmountPaid(String paymentId, String flatId, Map<String, BigDecimal> amountPaidCache) {
		String cacheKey = paymentId + "::" + flatId;
		return amountPaidCache.computeIfAbsent(cacheKey, ignored -> {
			List<Transaction> transactions = transactionRepository.findByPymntIdAndFlatIdAndTrnsStatus(paymentId, flatId,
					TRNS_STATUS_SUCCESS);
			if (transactions == null || transactions.isEmpty()) {
				return BigDecimal.ZERO;
			}
			return transactions.stream().filter(Objects::nonNull).map(Transaction::getTrnsAmt).map(this::parseBigDecimal)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
		});
	}

	private List<Profile> resolveOwnerProfiles(String flatId, Map<String, Optional<Flat>> flatCache,
			Map<String, List<Owner>> ownerCache, Map<String, Optional<Profile>> profileCache) {
		LinkedHashSet<String> profileIds = new LinkedHashSet<>();
		List<Owner> owners = ownerCache.computeIfAbsent(flatId, ignored -> {
			List<Owner> records = ownerRepository.findByFlatNo(flatId);
			return records == null ? Collections.emptyList() : records;
		});
		for (Owner owner : owners) {
			if (owner == null || !SecuraConstants.PROFILE_STATUS_ACTIVE.equalsIgnoreCase(owner.getStatus())) {
				continue;
			}
			profileIds.addAll(parseStringList(owner.getPrflId()));
		}
		if (profileIds.isEmpty()) {
			Optional<Flat> flat = flatCache.computeIfAbsent(flatId, ignored -> flatRepository.findById(flatId));
			flat.map(Flat::getFlatOwnerList).ifPresent(ownerList -> profileIds.addAll(parseStringList(ownerList)));
		}
		List<Profile> profiles = new ArrayList<>();
		for (String profileId : profileIds) {
			Optional<Profile> profile = profileCache.computeIfAbsent(profileId, profileRepository::findById);
			profile.ifPresent(profiles::add);
		}
		return profiles;
	}

	private String resolveFlatAreaTypeDisplay(String flatId, Map<String, Optional<Flat>> flatCache) {
		Optional<Flat> flat = flatCache.computeIfAbsent(flatId, ignored -> flatRepository.findById(flatId));
		return formatDisplayValue(flat.map(Flat::getFlatArea).orElse(null));
	}

	private List<String> resolveOwnerNames(List<Profile> profiles) {
		if (profiles == null || profiles.isEmpty()) {
			return new ArrayList<>();
		}
		return profiles.stream().map(this::resolveOwnerName).filter(this::hasText).distinct().collect(Collectors.toList());
	}

	private String resolveOwnerName(Profile profile) {
		if (profile == null) {
			return null;
		}
		if (hasText(profile.getPrflName())) {
			try {
				Name name = genericService.fromJson(profile.getPrflName(), Name.class);
				if (name != null) {
					String resolvedName = buildDisplayName(name);
					if (hasText(resolvedName)) {
						return resolvedName;
					}
				}
			} catch (RuntimeException exception) {
				// ignore and fallback to profile id
			}
		}
		return profile.getPrflId();
	}

	private String buildDisplayName(Name name) {
		List<String> parts = new ArrayList<>();
		if (name == null) {
			return null;
		}
		if (hasText(name.getFirstName())) {
			parts.add(name.getFirstName().trim());
		}
		if (hasText(name.getMiddleName())) {
			parts.add(name.getMiddleName().trim());
		}
		if (hasText(name.getLastName())) {
			parts.add(name.getLastName().trim());
		}
		return parts.isEmpty() ? null : String.join(" ", parts);
	}

	private String resolvePhoneNumber(List<Profile> profiles) {
		if (profiles == null || profiles.isEmpty()) {
			return null;
		}
		List<String> phoneNumbers = profiles.stream().filter(Objects::nonNull).map(Profile::getPrflPhoneNo).filter(this::hasText)
				.map(String::trim).distinct().collect(Collectors.toList());
		return phoneNumbers.isEmpty() ? null : String.join(", ", phoneNumbers);
	}

	private String resolveEmailId(List<Profile> profiles) {
		if (profiles == null || profiles.isEmpty()) {
			return null;
		}
		List<String> emailIds = profiles.stream().filter(Objects::nonNull).map(Profile::getPrflEmailAdrss)
				.filter(this::hasText).map(String::trim).distinct().collect(Collectors.toList());
		return emailIds.isEmpty() ? null : String.join(", ", emailIds);
	}

	private List<String> parseStringList(String json) {
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
		} catch (RuntimeException exception) {
			return new ArrayList<>();
		}
	}

	private String formatAmount(BigDecimal amount) {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO.toPlainString();
		}
		return amount.stripTrailingZeros().toPlainString();
	}

	private boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	private String formatDisplayValue(String value) {
		if (!hasText(value)) {
			return null;
		}
		return value.trim().replace('_', ' ');
	}

	private BigDecimal parseBigDecimal(String value) {
		if (value == null || value.isBlank()) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(value.trim());
		} catch (NumberFormatException e) {
			return BigDecimal.ZERO;
		}
	}

	private BigDecimal roundUp(BigDecimal value) {
		return value.setScale(0, RoundingMode.CEILING);
	}

	private TransactionResponseItem toResponseItem(Transaction transaction) {
		TransactionResponseItem item = new TransactionResponseItem();
		item.setAprmntId(transaction.getAprmntId());
		item.setTrnscId(transaction.getTrnscId());
		item.setTrnsDate(transaction.getTrnsDate());
		item.setTrnsBy(transaction.getTrnsBy());
		item.setTrnsTender(parseList(transaction.getTrnsTender(), new TypeReference<List<PaymentTenderData>>() {}));
		item.setTrnsType(transaction.getTrnsType());
		item.setTrnsShrtDesc(transaction.getTrnsShrtDesc());
		item.setTrnsFiles(parseList(transaction.getTrnsFiles(), new TypeReference<List<String>>() {}));
		item.setTrnsBnkAccnt(transaction.getTrnsBnkAccnt());
		item.setTrnsAmt(transaction.getTrnsAmt());
		item.setTrnsCurrency(transaction.getTrnsCurrency());
		item.setPymntId(transaction.getPymntId());
		item.setTrnsStatus(transaction.getTrnsStatus());
		item.setNoOfPerson(transaction.getNoOfPerson());
		item.setThirdPartyTrnsRef(transaction.getThirdPartyTrnsRef());
		item.setThirdPartyName(transaction.getThirdPartyName());
		item.setDueDetails(null); // TODO: restore from DueAmountDetails on reimplementation
		item.setCause(transaction.getCause());
		item.setBankInstrumentTenderDetails(
				parseList(transaction.getBankInstrumentTenderDetails(),
						new TypeReference<List<BankInstrumentTenderDetails>>() {}));
		item.setWorkListId(transaction.getWorkListId());
		item.setReceiptNumber(TRNS_STATUS_SUCCESS.equalsIgnoreCase(transaction.getTrnsStatus())
				? transaction.getReceiptNumber()
				: null);
		item.setCreatTs(transaction.getCreatTs());
		item.setCreatUsrId(transaction.getCreatUsrId());
		item.setLstUpdtTs(transaction.getLstUpdtTs());
		item.setLstUpdtUsrId(transaction.getLstUpdtUsrId());
		return item;
	}

	private <T> List<T> parseList(String json, TypeReference<List<T>> typeReference) {
		if (json == null || json.isBlank()) {
			return new ArrayList<>();
		}
		try {
			return genericService.fromJson(json, typeReference);
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	private record DefaulterAccumulator(String flatId, Map<String, DefaultPaymentAccumulator> defaultPaymentMap) {
		private DefaulterAccumulator(String flatId) {
			this(flatId, new LinkedHashMap<>());
		}
	}

	private static final class DefaultPaymentAccumulator {
		private final String paymentId;
		private final String paymentName;
		private final String paymentCapita;
		private BigDecimal totalDue = BigDecimal.ZERO;
		private BigDecimal penalty = BigDecimal.ZERO;
		private LocalDate lastDueDate;
		private int selectedCyclePriority;

		private DefaultPaymentAccumulator(String paymentId, String paymentName, String paymentCapita) {
			this.paymentId = paymentId;
			this.paymentName = paymentName;
			this.paymentCapita = paymentCapita;
		}

		private String paymentId() {
			return paymentId;
		}

		private String paymentName() {
			return paymentName;
		}

		private String paymentCapita() {
			return paymentCapita;
		}

		private BigDecimal totalDue() {
			return totalDue;
		}

		private BigDecimal penalty() {
			return penalty;
		}

		private LocalDate lastDueDate() {
			return lastDueDate;
		}

		private void addDue(DueAmountDetailsEntity due) {
			if (due == null) {
				return;
			}
			int cyclePriority = resolveCyclePriority(due.getCollectionCycle());
			if (cyclePriority < selectedCyclePriority) {
				return;
			}
			if (cyclePriority > selectedCyclePriority) {
				totalDue = BigDecimal.ZERO;
				penalty = BigDecimal.ZERO;
				lastDueDate = null;
				selectedCyclePriority = cyclePriority;
			}
			totalDue = totalDue.add(parseAmount(due.getTotalAmount()));
			penalty = penalty.add(parseAmount(due.getFineAmount()));
			trackLastDueDate(due.getDueDate());
		}

		private BigDecimal parseAmount(String value) {
			if (value == null || value.isBlank()) {
				return BigDecimal.ZERO;
			}
			try {
				return new BigDecimal(value.trim());
			} catch (NumberFormatException exception) {
				return BigDecimal.ZERO;
			}
		}

		private static int resolveCyclePriority(String cycle) {
			String normalizedCycle = normalizeCycle(cycle);
			switch (normalizedCycle) {
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

		private static String normalizeCycle(String cycle) {
			if (cycle == null) {
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

		private void trackLastDueDate(LocalDate dueDate) {
			if (dueDate == null) {
				return;
			}
			if (lastDueDate == null || dueDate.isAfter(lastDueDate)) {
				lastDueDate = dueDate;
			}
		}
	}
}
