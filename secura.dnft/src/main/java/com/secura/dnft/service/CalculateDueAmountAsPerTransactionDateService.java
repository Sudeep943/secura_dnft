package com.secura.dnft.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.dao.BankEntityRepository;
import com.secura.dnft.dao.DiscFinRepository;
import com.secura.dnft.dao.DueAmountDetailsRepository;
import com.secura.dnft.dao.FlatRepository;
import com.secura.dnft.dao.PaymentRepository;
import com.secura.dnft.dao.TransactionRepository;
import com.secura.dnft.entity.BankEntity;
import com.secura.dnft.entity.DiscFin;
import com.secura.dnft.entity.DueAmountDetailsEntity;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.entity.Transaction;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.AddedCharges;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.GetDueAmountForFlatRequest;
import com.secura.dnft.request.response.GetDueAmountForFlatResponse;
import com.secura.dnft.request.response.PaymentDetail;

@Service
public class CalculateDueAmountAsPerTransactionDateService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CalculateDueAmountAsPerTransactionDateService.class);
	private static final String SUCCESS_TRANSACTION_STATUS = "SUCCESS";
	private static final long DEFAULT_CYCLE_DAYS = 30L;

	@Autowired
	private FlatRepository flatRepository;

	@Autowired
	private DueAmountDetailsRepository dueAmountDetailsRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private BankEntityRepository bankEntityRepository;

	@Autowired
	private DiscFinRepository discFinRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private GenericService genericService;

	public GetDueAmountForFlatResponse getDueDetailsAsTransactionDate(GetDueAmountForFlatRequest request) {
		GetDueAmountForFlatResponse response = new GetDueAmountForFlatResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		initializeDefaultDueResponse(response);
		LocalDate transactionDate = request != null ? request.getTransactionDate() : null;
		LOGGER.info("getDueDetailsAsTransactionDate started: apartmentId={}, flatId={}, transactionDate={}",
				resolveApartmentId(request), request != null ? request.getFlatId() : null, transactionDate);
		try {
			if (request == null || transactionDate == null) {
				LOGGER.warn("getDueDetailsAsTransactionDate validation failed: transactionDate is required");
				response.setMessage(ErrorMessage.ERR_MESSAGE_43);
				response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_43);
				return response;
			}
			String apartmentId = resolveApartmentId(request);
			String flatId = request.getFlatId();
			Optional<Flat> optionalFlat = flatRepository.findByAprmntIdAndFlatNo(apartmentId, flatId);
			if (optionalFlat.isPresent()) {
				Flat flat = optionalFlat.get();
				List<String> pendingDueKeys = parseStringList(flat.getFlatPndngPaymntLst());
				List<String> filteredPendingDueKeys = filterPendingDueKeys(pendingDueKeys, transactionDate);
				List<String> dueIds = extractDueIdsFromFlatPendingList(filteredPendingDueKeys);
				if (!dueIds.isEmpty()) {
					List<DueAmountDetailsEntity> dueEntities = dueAmountDetailsRepository.findByDueIdIn(dueIds);
					List<DueAmountDetailsEntity> filteredDues = filterDueEntitiesByPendingKeys(dueEntities, filteredPendingDueKeys);
					filteredDues = filterOptionalClosedPaymentDues(filteredDues, apartmentId, transactionDate);
					Map<String, List<String>> paymentIdToDueIdsMap = groupDueIdsByPayment(filteredDues);
					Map<String, List<DueAmountDetailsEntity>> finalPaymentMap = buildFinalPaymentMap(paymentIdToDueIdsMap,
							filteredDues, transactionDate);
					Map<PaymentDetail, List<DueAmountDetailsEntity>> dueDetails = buildDueDetails(finalPaymentMap, apartmentId,
							flatId, flat.getFlatArea(), transactionDate);
					response.setDueDetails(dueDetails);
					DueTotals dueTotals = calculateDueTotalsFromDueDetails(dueDetails, flatId, flat.getFlatArea(),
							transactionDate);
					response.setTotalDue(formatAmount(dueTotals.totalDue()));
					response.setTotalMandatoryPayment(formatAmount(dueTotals.totalMandatoryPayment()));
					response.setTotalOptionalPayment(formatAmount(dueTotals.totalOptionalPayment()));
					response.setPenaltyAdded(hasPenalty(dueDetails));
				}
			}
			response.setMessage(SuccessMessage.SUCC_MESSAGE_28);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_28);
			LOGGER.info("getDueDetailsAsTransactionDate completed: apartmentId={}, flatId={}, transactionDate={}, dueCount={}",
					resolveApartmentId(request), request != null ? request.getFlatId() : null, transactionDate,
					countDues(response.getDueDetails()));
			return response;
		} catch (Exception exception) {
			LOGGER.error("getDueDetailsAsTransactionDate failed: apartmentId={}, flatId={}, transactionDate={}",
					resolveApartmentId(request), request != null ? request.getFlatId() : null, transactionDate, exception);
			initializeDefaultDueResponse(response);
			response.setMessage(ErrorMessage.ERR_MESSAGE_43);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_43);
			return response;
		}
	}

	private String resolveApartmentId(GetDueAmountForFlatRequest request) {
		return request != null && request.getGenericHeader() != null ? request.getGenericHeader().getApartmentId() : null;
	}

	private Map<PaymentDetail, List<DueAmountDetailsEntity>> buildDueDetails(
			Map<String, List<DueAmountDetailsEntity>> finalPaymentMap, String apartmentId, String flatId, String flatArea,
			LocalDate transactionDate) {
		Map<PaymentDetail, List<DueAmountDetailsEntity>> dueDetails = new LinkedHashMap<>();
		if (finalPaymentMap == null || finalPaymentMap.isEmpty()) {
			return dueDetails;
		}
		for (Map.Entry<String, List<DueAmountDetailsEntity>> entry : finalPaymentMap.entrySet()) {
			String paymentId = entry.getKey();
			List<DueAmountDetailsEntity> selectedDues = entry.getValue();
			if (!hasText(paymentId) || selectedDues == null || selectedDues.isEmpty()) {
				continue;
			}
			PaymentEntity paymentEntity = paymentRepository.findFirstByPaymentIdAndAprmtId(paymentId, apartmentId)
					.or(() -> paymentRepository.findFirstByPaymentId(paymentId)).orElse(null);
			Map<String, DueAmountDetailsEntity> recalculatedByKey = recalculatePaymentDues(paymentEntity, apartmentId,
					transactionDate);
			List<DueAmountDetailsEntity> finalDueAmount = selectedDues.stream().map(this::copyDueEntity)
					.map(dueEntity -> getRecalculatedOrOriginalDue(dueEntity, recalculatedByKey))
					.sorted(Comparator.comparing(DueAmountDetailsEntity::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
					.collect(Collectors.toCollection(ArrayList::new));
			if (paymentEntity != null
					&& SecuraConstants.PAYMENT_COLLECTION_MODE_PRE.equalsIgnoreCase(paymentEntity.getPaymentCollectionMode())) {
				applyPreCollectionPenaltyAmounts(finalDueAmount, recalculatedByKey.values(), paymentEntity);
			}
			for (DueAmountDetailsEntity dueEntity : finalDueAmount) {
				LOGGER.info(
						"Processing due as transaction date: dueId={}, paymentId={}, flatId={}, dueDate={}, dueStartDate={}, dueEndDate={}, collectionCycle={}, transactionDate={}",
						dueEntity.getDueId(), dueEntity.getPaymentId(), flatId, dueEntity.getDueDate(),
						dueEntity.getDueStartDate(), dueEntity.getDueEndDate(), dueEntity.getCollectionCycle(), transactionDate);
			}
			PaymentDetail paymentDetail = buildPaymentDetail(paymentId, paymentEntity, finalDueAmount, apartmentId);
			dueDetails.put(paymentDetail, finalDueAmount);
		}
		return dueDetails;
	}

	private PaymentDetail buildPaymentDetail(String paymentId, PaymentEntity paymentEntity,
			List<DueAmountDetailsEntity> dueEntities, String apartmentId) {
		PaymentDetail paymentDetail = new PaymentDetail();
		paymentDetail.setPaymentId(paymentId);
		paymentDetail.setPaymentName(resolvePaymentName(paymentEntity, dueEntities));
		String bankId = paymentEntity != null ? paymentEntity.getBankAccountId() : null;
		paymentDetail.setBankId(bankId);
		paymentDetail.setPaymentGateway(resolvePaymentGateway(apartmentId, bankId));
		paymentDetail.setPaymentCause(paymentEntity != null ? paymentEntity.getCauseId() : null);
		return paymentDetail;
	}

	private Map<String, DueAmountDetailsEntity> recalculatePaymentDues(PaymentEntity paymentEntity, String apartmentId,
			LocalDate transactionDate) {
		Map<String, DueAmountDetailsEntity> recalculatedByKey = new LinkedHashMap<>();
		if (paymentEntity == null || !hasText(paymentEntity.getPaymentId())) {
			return recalculatedByKey;
		}
		List<DueAmountDetailsEntity> allPaymentDues = dueAmountDetailsRepository.findByPaymentId(paymentEntity.getPaymentId());
		if (allPaymentDues == null || allPaymentDues.isEmpty()) {
			return recalculatedByKey;
		}
		List<Flat> apartmentFlats = flatRepository.findByAprmntId(paymentEntity.getAprmtId());
		List<Flat> applicableApartmentFlats = filterApplicableFlats(apartmentFlats, paymentEntity.getApplicableFor());
		Map<String, Long> flatTypeCounts = buildFlatTypeCounts(applicableApartmentFlats);
		List<DueAmountDetailsEntity> recalculatedDues = new ArrayList<>();
		for (DueAmountDetailsEntity sourceDue : allPaymentDues) {
			DueAmountDetailsEntity recalculatedDue = buildDueDetailsAsTransactionDate(sourceDue, paymentEntity,
					transactionDate);
			recalculatedDues.add(recalculatedDue);
			recalculatedByKey.put(buildDueIdentityKey(sourceDue), recalculatedDue);
		}
		applyEstimatedCollectionAmount(recalculatedDues, paymentEntity, flatTypeCounts, applicableApartmentFlats.size());
		for (DueAmountDetailsEntity recalculatedDue : recalculatedDues) {
			recalculatedByKey.put(buildDueIdentityKey(recalculatedDue), recalculatedDue);
		}
		return recalculatedByKey;
	}

	private DueAmountDetailsEntity getRecalculatedOrOriginalDue(DueAmountDetailsEntity dueEntity,
			Map<String, DueAmountDetailsEntity> recalculatedByKey) {
		DueAmountDetailsEntity recalculatedDue = recalculatedByKey.get(buildDueIdentityKey(dueEntity));
		return recalculatedDue != null ? recalculatedDue : dueEntity;
	}

	private DueAmountDetailsEntity buildDueDetailsAsTransactionDate(DueAmountDetailsEntity sourceDue,
			PaymentEntity paymentEntity, LocalDate transactionDate) {
		DueAmountDetailsEntity targetDue = new DueAmountDetailsEntity();
		copySourceMetadata(targetDue, sourceDue);
		setDueId(targetDue, sourceDue);
		setDueCollectionCycle(targetDue, sourceDue);
		setFlatArea(targetDue, sourceDue);
		setDueStartDate(targetDue, sourceDue);
		setDueEndDate(targetDue, sourceDue);
		setDueDate(targetDue, sourceDue, paymentEntity);
		setPaymentId(targetDue, sourceDue, paymentEntity);
		setPaymentName(targetDue, sourceDue, paymentEntity);
		setPaymentType(targetDue, sourceDue, paymentEntity);
		setPaymentStatus(targetDue, sourceDue);
		setPaymentDate(targetDue, sourceDue);
		setCause(targetDue, sourceDue, paymentEntity);
		setPaymentCapita(targetDue, sourceDue, paymentEntity);
		setApplicableFlats(targetDue, sourceDue);
		setAllowedTenders(targetDue, paymentEntity, sourceDue);
		setAmountPerMonth(targetDue, sourceDue, paymentEntity);
		BigDecimal amount = calculateAmount(sourceDue, paymentEntity);
		setAmount(targetDue, amount);
		BigDecimal discountedAmount = calculateDiscountedAmount(targetDue, sourceDue, paymentEntity, amount, transactionDate);
		BigDecimal baseAmount = amount.subtract(discountedAmount);
		if (baseAmount.compareTo(BigDecimal.ZERO) < 0) {
			baseAmount = BigDecimal.ZERO;
		}
		BigDecimal gstAmount = setGstAmount(targetDue, paymentEntity, baseAmount);
		List<AddedCharges> addedCharges = setAddedCharges(targetDue, paymentEntity, baseAmount);
		BigDecimal totalAddedCharges = setTotalAddedCharges(targetDue, addedCharges);
		BigDecimal penaltyAmount = calculateFnValue(targetDue, sourceDue, paymentEntity, baseAmount, transactionDate);
		setAlreadyPaidAmount(targetDue, sourceDue);
		setAdminDiscount(targetDue, sourceDue);
		setRoundUpAndTotalAmount(targetDue, baseAmount, gstAmount, totalAddedCharges, penaltyAmount);
		LOGGER.debug(
				"Final due amount as transaction date: dueId={}, baseAmount={}, discountAmount={}, penaltyAmount={}, gstAmount={}, addedCharges={}, totalAmount={}",
				targetDue.getDueId(), amount, discountedAmount, penaltyAmount, gstAmount, totalAddedCharges,
				targetDue.getTotalAmount());
		return targetDue;
	}

	private void copySourceMetadata(DueAmountDetailsEntity targetDue, DueAmountDetailsEntity sourceDue) {
		if (sourceDue == null) {
			return;
		}
		targetDue.setAprmntId(sourceDue.getAprmntId());
		targetDue.setPaidFlats(sourceDue.getPaidFlats());
		targetDue.setCreatTs(sourceDue.getCreatTs());
		targetDue.setCreatUsrId(sourceDue.getCreatUsrId());
		targetDue.setLstUpdtTs(sourceDue.getLstUpdtTs());
		targetDue.setLstUpdtUsrId(sourceDue.getLstUpdtUsrId());
	}

	private void setDueCollectionCycle(DueAmountDetailsEntity targetDue, DueAmountDetailsEntity sourceDue) {
		targetDue.setCollectionCycle(sourceDue != null ? sourceDue.getCollectionCycle() : null);
	}

	private void setFlatArea(DueAmountDetailsEntity targetDue, DueAmountDetailsEntity sourceDue) {
		targetDue.setFlatArea(sourceDue != null ? sourceDue.getFlatArea() : null);
	}

	private void setPaymentId(DueAmountDetailsEntity targetDue, DueAmountDetailsEntity sourceDue, PaymentEntity paymentEntity) {
		targetDue.setPaymentId(paymentEntity != null && hasText(paymentEntity.getPaymentId()) ? paymentEntity.getPaymentId()
				: sourceDue != null ? sourceDue.getPaymentId() : null);
	}

	private void setPaymentName(DueAmountDetailsEntity targetDue, DueAmountDetailsEntity sourceDue,
			PaymentEntity paymentEntity) {
		targetDue.setPaymentName(paymentEntity != null && hasText(paymentEntity.getPaymentName()) ? paymentEntity.getPaymentName()
				: sourceDue != null ? sourceDue.getPaymentName() : null);
	}

	private void setPaymentType(DueAmountDetailsEntity targetDue, DueAmountDetailsEntity sourceDue,
			PaymentEntity paymentEntity) {
		targetDue.setPaymentType(paymentEntity != null && hasText(paymentEntity.getPaymentType()) ? paymentEntity.getPaymentType()
				: sourceDue != null ? sourceDue.getPaymentType() : null);
	}

	private void setPaymentStatus(DueAmountDetailsEntity targetDue, DueAmountDetailsEntity sourceDue) {
		targetDue.setPaymentStatus(sourceDue != null ? sourceDue.getPaymentStatus() : null);
	}

	private void setDueId(DueAmountDetailsEntity targetDue, DueAmountDetailsEntity sourceDue) {
		targetDue.setDueId(sourceDue != null ? sourceDue.getDueId() : null);
	}

	private void setDueStartDate(DueAmountDetailsEntity targetDue, DueAmountDetailsEntity sourceDue) {
		targetDue.setDueStartDate(sourceDue != null ? sourceDue.getDueStartDate() : null);
	}

	private void setDueEndDate(DueAmountDetailsEntity targetDue, DueAmountDetailsEntity sourceDue) {
		targetDue.setDueEndDate(sourceDue != null ? sourceDue.getDueEndDate() : null);
	}

	private void setDueDate(DueAmountDetailsEntity targetDue, DueAmountDetailsEntity sourceDue, PaymentEntity paymentEntity) {
		LocalDate dueStartDate = sourceDue != null ? sourceDue.getDueStartDate() : null;
		LocalDate dueEndDate = sourceDue != null ? sourceDue.getDueEndDate() : null;
		String collectionCycle = sourceDue != null ? sourceDue.getCollectionCycle() : null;
		String collectionMode = paymentEntity != null ? paymentEntity.getPaymentCollectionMode() : null;
		targetDue.setDueDate(calculateDueDate(dueStartDate, dueEndDate, collectionCycle, collectionMode));
	}

	private void setAmount(DueAmountDetailsEntity targetDue, BigDecimal amount) {
		targetDue.setAmount(format(amount));
	}

	private void setAmountPerMonth(DueAmountDetailsEntity targetDue, DueAmountDetailsEntity sourceDue,
			PaymentEntity paymentEntity) {
		if (paymentEntity != null && hasText(paymentEntity.getPaymentAmount())) {
			targetDue.setAmountPerMonth(format(paymentEntity.getPaymentAmount()));
			return;
		}
		targetDue.setAmountPerMonth(sourceDue != null ? sourceDue.getAmountPerMonth() : null);
	}

	private void setAlreadyPaidAmount(DueAmountDetailsEntity targetDue, DueAmountDetailsEntity sourceDue) {
		targetDue.setAlreadyPaidAmount(defaultZeroValue(sourceDue != null ? sourceDue.getAlreadyPaidAmount() : null));
	}

	private List<AddedCharges> setAddedCharges(DueAmountDetailsEntity targetDue, PaymentEntity paymentEntity,
			BigDecimal baseAmount) {
		List<AddedCharges> addedCharges = parseAddedCharges(paymentEntity != null ? paymentEntity.getAddedCharges() : null);
		BigDecimal totalAddedCharges = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		for (AddedCharges addedCharge : addedCharges) {
			if (addedCharge == null) {
				continue;
			}
			BigDecimal value = parseNumeric(addedCharge.getValue());
			BigDecimal finalValue = "percentage".equalsIgnoreCase(addedCharge.getChargeType())
					? calculatePercentageAmount(baseAmount, value)
					: value.setScale(2, RoundingMode.HALF_UP);
			addedCharge.setFinalChargeValue(format(finalValue));
			totalAddedCharges = totalAddedCharges.add(finalValue);
		}
		targetDue.setAddedCharges(genericService.toJson(addedCharges));
		targetDue.setTotalAddedCharges(format(totalAddedCharges));
		return addedCharges;
	}

	private BigDecimal setTotalAddedCharges(DueAmountDetailsEntity targetDue, List<AddedCharges> addedCharges) {
		BigDecimal totalAddedCharges = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		if (addedCharges != null) {
			for (AddedCharges addedCharge : addedCharges) {
				totalAddedCharges = totalAddedCharges.add(parseNumeric(addedCharge != null ? addedCharge.getFinalChargeValue() : null));
			}
		}
		targetDue.setTotalAddedCharges(format(totalAddedCharges));
		return totalAddedCharges;
	}

	private BigDecimal setGstAmount(DueAmountDetailsEntity targetDue, PaymentEntity paymentEntity, BigDecimal baseAmount) {
		BigDecimal gstPercentage = parseNumeric(paymentEntity != null ? paymentEntity.getGst() : null);
		targetDue.setGstPercentage(format(gstPercentage));
		BigDecimal gstAmount = calculatePercentageAmount(baseAmount, gstPercentage);
		targetDue.setGstAmount(format(gstAmount));
		return gstAmount;
	}

	private void setAdminDiscount(DueAmountDetailsEntity targetDue, DueAmountDetailsEntity sourceDue) {
		targetDue.setAdminDiscount(defaultZeroValue(sourceDue != null ? sourceDue.getAdminDiscount() : null));
	}

	private void setEstimatedCollectionAmount(DueAmountDetailsEntity targetDue, BigDecimal estimatedCollectionAmount) {
		targetDue.setEstimatedCollectionAmount(format(estimatedCollectionAmount));
	}

	private BigDecimal calculateDiscountedAmount(DueAmountDetailsEntity targetDue, DueAmountDetailsEntity sourceDue,
			PaymentEntity paymentEntity, BigDecimal amount, LocalDate transactionDate) {
		DiscFinReference discFinReference = extractDiscFinReference(paymentEntity != null ? paymentEntity.getDiscFin() : null);
		DiscFin discountDiscFin = resolveDiscFin(discFinReference.discountCode(), targetDue.getCollectionCycle());
		targetDue.setDiscountCode(discountDiscFin != null ? discFinReference.discountCode() : null);
		targetDue.setDiscountMode(discountDiscFin != null ? discountDiscFin.getDiscFnMode() : null);
		targetDue.setDiscValue(discountDiscFin != null ? format(discountDiscFin.getDiscFinValue())
				: sourceDue != null ? sourceDue.getDiscValue() : null);
		BigDecimal discountedAmount = calculateDiscountAmount(sourceDue, discountDiscFin, amount, transactionDate);
		targetDue.setDiscountedAmount(format(discountedAmount));
		return discountedAmount;
	}

	private BigDecimal calculateFnValue(DueAmountDetailsEntity targetDue, DueAmountDetailsEntity sourceDue,
			PaymentEntity paymentEntity, BigDecimal baseAmount, LocalDate transactionDate) {
		DiscFinReference discFinReference = extractDiscFinReference(paymentEntity != null ? paymentEntity.getDiscFin() : null);
		PenaltyCalculationResult penaltyResult = calculatePenaltyAmount(sourceDue, paymentEntity, baseAmount, transactionDate);
		DiscFin fineDiscFin = penaltyResult.discFin();
		targetDue.setFineCode(fineDiscFin != null ? fineDiscFin.getDiscFnId() : discFinReference.fineCode());
		targetDue.setFnValue(fineDiscFin != null ? format(fineDiscFin.getDiscFinValue())
				: sourceDue != null ? sourceDue.getFnValue() : null);
		targetDue.setFineMode(fineDiscFin != null ? fineDiscFin.getDiscFnMode() : null);
		targetDue.setFineType(fineDiscFin != null ? fineDiscFin.getFnCalculationType() : "");
		targetDue.setCummilationCycle(fineDiscFin != null ? fineDiscFin.getDiscFnCumlatonCycle() : null);
		targetDue.setFineAmount(format(penaltyResult.amount()));
		return penaltyResult.amount();
	}

	private void setRoundUpAndTotalAmount(DueAmountDetailsEntity targetDue, BigDecimal baseAmount, BigDecimal gstAmount,
			BigDecimal totalAddedCharges, BigDecimal penaltyAmount) {
		BigDecimal computedTotal = baseAmount.add(gstAmount).add(totalAddedCharges).add(penaltyAmount);
		BigDecimal roundedTotal = computedTotal.setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
		BigDecimal roundUpAmount = roundedTotal.subtract(computedTotal).setScale(2, RoundingMode.HALF_UP);
		targetDue.setRoundUpAmount(format(roundUpAmount));
		targetDue.setTotalAmount(format(roundedTotal));
	}

	private void setCause(DueAmountDetailsEntity targetDue, DueAmountDetailsEntity sourceDue, PaymentEntity paymentEntity) {
		targetDue.setCause(paymentEntity != null && hasText(paymentEntity.getCauseId()) ? paymentEntity.getCauseId()
				: sourceDue != null ? sourceDue.getCause() : null);
	}

	private void setPaymentCapita(DueAmountDetailsEntity targetDue, DueAmountDetailsEntity sourceDue,
			PaymentEntity paymentEntity) {
		targetDue.setPaymentCapita(paymentEntity != null && hasText(paymentEntity.getPaymentCapita())
				? paymentEntity.getPaymentCapita()
				: sourceDue != null ? sourceDue.getPaymentCapita() : null);
	}

	private void setPaymentDate(DueAmountDetailsEntity targetDue, DueAmountDetailsEntity sourceDue) {
		targetDue.setPaymentDate(sourceDue != null ? sourceDue.getPaymentDate() : null);
	}

	private void setApplicableFlats(DueAmountDetailsEntity targetDue, DueAmountDetailsEntity sourceDue) {
		targetDue.setApplicableFlats(sourceDue != null ? sourceDue.getApplicableFlats() : null);
	}

	private void setAllowedTenders(DueAmountDetailsEntity targetDue, PaymentEntity paymentEntity,
			DueAmountDetailsEntity sourceDue) {
		if (paymentEntity != null && hasText(paymentEntity.getAllowedPaymentModes())) {
			targetDue.setAllowedTenders(genericService.toJson(parseAllowedPaymentModes(paymentEntity.getAllowedPaymentModes())));
			return;
		}
		targetDue.setAllowedTenders(sourceDue != null ? sourceDue.getAllowedTenders() : null);
	}

	private BigDecimal calculateAmount(DueAmountDetailsEntity sourceDue, PaymentEntity paymentEntity) {
		if (sourceDue == null || paymentEntity == null) {
			return parseNumeric(sourceDue != null ? sourceDue.getAmount() : null).setScale(2, RoundingMode.HALF_UP);
		}
		if (validatePerHeadOnceCollection(paymentEntity)) {
			return parseNumeric(paymentEntity.getPaymentAmount()).setScale(2, RoundingMode.HALF_UP);
		}
		BigDecimal areaMultiplier = isPerSqft(paymentEntity.getPaymentCapita()) ? parseNumeric(sourceDue.getFlatArea())
				: BigDecimal.ONE;
		return calculateAmount(paymentEntity, sourceDue.getDueStartDate(), sourceDue.getDueEndDate(), areaMultiplier,
				sourceDue.getCollectionCycle());
	}

	private BigDecimal calculateDiscountAmount(DueAmountDetailsEntity dueDetails, DiscFin discountDiscFin,
			BigDecimal amount, LocalDate transactionDate) {
		if (discountDiscFin == null) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		LocalDate discountStart = discountDiscFin.getDiscFnStrtDt();
		LocalDate discountEnd = discountDiscFin.getDiscFnEndDt();
		if (discountStart != null && transactionDate.isBefore(discountStart)) {
			logDiscountCalculation(dueDetails, transactionDate, discountDiscFin, BigDecimal.ZERO);
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		if (discountEnd != null && transactionDate.isAfter(discountEnd)) {
			logDiscountCalculation(dueDetails, transactionDate, discountDiscFin, BigDecimal.ZERO);
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		BigDecimal minimumPaymentAmount = parseNumeric(discountDiscFin.getMinimumPaymentAmount());
		if (minimumPaymentAmount.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(minimumPaymentAmount) < 0) {
			logDiscountCalculation(dueDetails, transactionDate, discountDiscFin, BigDecimal.ZERO);
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		BigDecimal discountValue = parseNumeric(discountDiscFin.getDiscFinValue());
		BigDecimal discountAmount = SecuraConstants.DISC_FN_MODE_PERCENTAGE.equalsIgnoreCase(discountDiscFin.getDiscFnMode())
				? calculatePercentageAmount(amount, discountValue)
				: discountValue.setScale(2, RoundingMode.HALF_UP);
		logDiscountCalculation(dueDetails, transactionDate, discountDiscFin, discountAmount);
		return discountAmount;
	}

	private void logDiscountCalculation(DueAmountDetailsEntity dueDetails, LocalDate transactionDate, DiscFin discountDiscFin,
			BigDecimal calculatedDiscountAmount) {
		LOGGER.debug(
				"Discount calculated: dueId={}, transactionDate={}, discountMode={}, discountCode={}, discountValue={}, calculatedDiscountAmount={}",
				dueDetails != null ? dueDetails.getDueId() : null, transactionDate,
				discountDiscFin != null ? discountDiscFin.getDiscFnMode() : null,
				discountDiscFin != null ? discountDiscFin.getDiscFnId() : null,
				discountDiscFin != null ? discountDiscFin.getDiscFinValue() : null, calculatedDiscountAmount);
	}

	private PenaltyCalculationResult calculatePenaltyAmount(DueAmountDetailsEntity dueDetails, PaymentEntity paymentEntity,
			BigDecimal baseAmount, LocalDate transactionDate) {
		if (paymentEntity == null || dueDetails == null || baseAmount == null || baseAmount.compareTo(BigDecimal.ZERO) <= 0) {
			return new PenaltyCalculationResult(null, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
		}
		String fineCode = resolveActiveFineCode(paymentEntity.getDiscFin());
		if (!hasText(fineCode)) {
			return new PenaltyCalculationResult(null, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
		}
		List<DiscFin> discFins = discFinRepository.findByDiscFnId(fineCode);
		if (discFins == null || discFins.isEmpty()) {
			return new PenaltyCalculationResult(null, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
		}
		boolean isFixedFine = discFins.size() == 1 && normalizeCycle(discFins.get(0).getDiscFnCycleType())
				.equals(normalizeCycle(SecuraConstants.DISC_FN_CYCLE_FIXED));
		DiscFin applicableDiscFin;
		if (isFixedFine) {
			applicableDiscFin = discFins.get(0);
		} else {
			String dueCycle = normalizeCycle(dueDetails.getCollectionCycle());
			applicableDiscFin = discFins.stream().filter(Objects::nonNull)
					.filter(discFin -> dueCycle.equals(normalizeCycle(discFin.getDiscFnCycleType()))).findFirst().orElse(null);
			if (applicableDiscFin == null || !isBufferTimeElapsed(applicableDiscFin, dueDetails.getDueDate(), transactionDate)) {
				return new PenaltyCalculationResult(null, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
			}
		}
		BigDecimal penaltyAmount = calculatePenalty(dueDetails, paymentEntity, applicableDiscFin, baseAmount, transactionDate);
		LOGGER.debug(
				"Penalty calculated: dueId={}, transactionDate={}, fineType={}, fineMode={}, fineCode={}, fineValue={}, calculatedPenaltyAmount={}",
				dueDetails.getDueId(), transactionDate, applicableDiscFin != null ? applicableDiscFin.getFnCalculationType() : null,
				applicableDiscFin != null ? applicableDiscFin.getDiscFnMode() : null,
				applicableDiscFin != null ? applicableDiscFin.getDiscFnId() : null,
				applicableDiscFin != null ? applicableDiscFin.getDiscFinValue() : null, penaltyAmount);
		return new PenaltyCalculationResult(applicableDiscFin, penaltyAmount);
	}

	private BigDecimal calculatePenalty(DueAmountDetailsEntity dueDetails, PaymentEntity paymentEntity, DiscFin fineDiscFin,
			BigDecimal baseAmount, LocalDate transactionDate) {
		if (fineDiscFin == null || dueDetails == null || baseAmount == null || baseAmount.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		LocalDate dueDate = dueDetails.getDueDate();
		if (paymentEntity != null
				&& SecuraConstants.PAYMENT_COLLECTION_MODE_POST.equalsIgnoreCase(paymentEntity.getPaymentCollectionMode())) {
			DueAmountDetailsEntity highestDue = getHighestDueForCurrentDue(paymentEntity, dueDetails);
			dueDate = highestDue != null ? highestDue.getDueDate() : dueDate;
		}
		LocalDate fineStart = Boolean.TRUE.equals(fineDiscFin.getDueDateAsStartDateFlag()) && dueDate != null ? dueDate
				: fineDiscFin.getDiscFnStrtDt();
		LocalDate fineEnd = fineDiscFin.getDiscFnEndDt();
		if (fineStart != null && transactionDate.isBefore(fineStart)) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		if (fineEnd != null && transactionDate.isAfter(fineEnd)) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		LocalDate bufferTimeDate = getBufferTimeDate(fineDiscFin, dueDate);
		if (bufferTimeDate != null && transactionDate.isBefore(bufferTimeDate)) {
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
		if (penaltyStart == null || !penaltyStart.isBefore(transactionDate)) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		List<Transaction> transactions = getSuccessfulTransactionsAfterDueDate(dueDetails, transactionDate);
		Map<LocalDate, BigDecimal> paymentByDate = aggregatePaymentsByDate(transactions);
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
			LocalDate segmentEnd = paymentDate.isAfter(transactionDate) ? transactionDate : paymentDate;
			if (segmentEnd.isAfter(cursor)) {
				BigDecimal segmentPenalty = calculateSegmentPenalty(outstanding, rate, cursor, segmentEnd, cycleMonths,
						partCycleAsFull, isCumulativeFine(fineDiscFin.getFnCalculationType()), fineDiscFin.getSimpleFineCycle());
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
			if (!cursor.isBefore(transactionDate)) {
				return totalPenalty.setScale(2, RoundingMode.HALF_UP);
			}
		}
		if (cursor.isBefore(transactionDate) && outstanding.compareTo(BigDecimal.ZERO) > 0) {
			BigDecimal segmentPenalty = calculateSegmentPenalty(outstanding, rate, dueDate, transactionDate, cycleMonths,
					partCycleAsFull, isCumulativeFine(fineDiscFin.getFnCalculationType()), fineDiscFin.getSimpleFineCycle());
			totalPenalty = totalPenalty.add(segmentPenalty);
		}
		return totalPenalty.setScale(2, RoundingMode.HALF_UP);
	}

	private DueAmountDetailsEntity getHighestDueForCurrentDue(PaymentEntity paymentEntity, DueAmountDetailsEntity currentDue) {
		List<String> paymentCycles = parseStringList(paymentEntity != null ? paymentEntity.getPaymentCollectionCycle() : null);
		String finalParentCycle = getFinalParentCycle(paymentCycles, currentDue != null ? currentDue.getCollectionCycle() : null);
		List<DueAmountDetailsEntity> dues = dueAmountDetailsRepository.findByPaymentId(paymentEntity.getPaymentId()).stream()
				.filter(due -> due != null && finalParentCycle.equalsIgnoreCase(due.getCollectionCycle())).toList();
		if (dues.isEmpty()) {
			return null;
		}
		if (dues.size() == 1) {
			return dues.get(0);
		}
		return dues.stream().filter(due -> !currentDue.getDueDate().isBefore(due.getDueStartDate()))
				.filter(due -> !currentDue.getDueDate().isAfter(due.getDueEndDate())).findFirst().orElse(null);
	}

	private void applyEstimatedCollectionAmount(List<DueAmountDetailsEntity> recalculatedDues, PaymentEntity paymentEntity,
			Map<String, Long> flatTypeCounts, int flatCount) {
		if (recalculatedDues == null || recalculatedDues.isEmpty() || paymentEntity == null) {
			return;
		}
		Map<String, List<DueAmountDetailsEntity>> duesByCycle = recalculatedDues.stream().filter(Objects::nonNull)
				.collect(Collectors.groupingBy(DueAmountDetailsEntity::getCollectionCycle, LinkedHashMap::new, Collectors.toList()));
		for (Map.Entry<String, List<DueAmountDetailsEntity>> entry : duesByCycle.entrySet()) {
			List<DueAmountDetailsEntity> cycleDues = entry.getValue();
			List<LocalDate[]> intervals = cycleDues.stream().map(due -> new LocalDate[] { due.getDueStartDate(), due.getDueEndDate() })
					.collect(Collectors.toList());
			BigDecimal cycleMultiplier = calculateCycleMultiplier(intervals, getCycleMonths(entry.getKey()), entry.getKey());
			Map<String, List<DueAmountDetailsEntity>> intervalGroups = cycleDues.stream()
					.collect(Collectors.groupingBy(this::buildIntervalGroupKey, LinkedHashMap::new, Collectors.toList()));
			List<DueAmountDetailsEntity> firstIntervalDues = intervalGroups.values().stream().findFirst()
					.orElse(Collections.emptyList());
			BigDecimal estimatedCollectionAmount = calculateEstimatedCollectionAmount(firstIntervalDues, flatTypeCounts,
					flatCount, paymentEntity.getPaymentCapita(), cycleMultiplier);
			for (DueAmountDetailsEntity dueEntity : cycleDues) {
				setEstimatedCollectionAmount(dueEntity, estimatedCollectionAmount);
			}
		}
	}

	private String buildIntervalGroupKey(DueAmountDetailsEntity dueEntity) {
		return normalizeHierarchyKey(dueEntity.getCollectionCycle()).toUpperCase(Locale.ENGLISH) + "|"
				+ Objects.toString(dueEntity.getDueStartDate(), "") + "|" + Objects.toString(dueEntity.getDueEndDate(), "");
	}

	private BigDecimal calculateEstimatedCollectionAmount(List<DueAmountDetailsEntity> duesForInterval,
			Map<String, Long> flatTypeCounts, int flatCount, String paymentCapita, BigDecimal cycleMultiplier) {
		if (duesForInterval == null || duesForInterval.isEmpty()) {
			return BigDecimal.ZERO;
		}
		BigDecimal safeCycleMultiplier = cycleMultiplier == null || cycleMultiplier.compareTo(BigDecimal.ZERO) <= 0
				? BigDecimal.ONE
				: cycleMultiplier;
		if (isPerSqft(paymentCapita)) {
			BigDecimal total = BigDecimal.ZERO;
			for (DueAmountDetailsEntity dueEntity : duesForInterval) {
				long count = flatTypeCounts.getOrDefault(normalizeFlatArea(dueEntity.getFlatArea()), 0L);
				total = total.add(parseNumeric(dueEntity.getTotalAmount()).multiply(BigDecimal.valueOf(count)));
			}
			return total.multiply(safeCycleMultiplier);
		}
		if (isPerHead(paymentCapita)) {
			return BigDecimal.ZERO;
		}
		BigDecimal amountPerUnit = duesForInterval.stream().findFirst().map(DueAmountDetailsEntity::getTotalAmount)
				.map(this::parseNumeric).orElse(BigDecimal.ZERO);
		return amountPerUnit.multiply(BigDecimal.valueOf(Math.max(flatCount, 0))).multiply(safeCycleMultiplier);
	}

	private void applyPreCollectionPenaltyAmounts(List<DueAmountDetailsEntity> finalDueAmount,
			Collection<DueAmountDetailsEntity> recalculatedDues, PaymentEntity paymentEntity) {
		if (finalDueAmount == null || finalDueAmount.isEmpty() || recalculatedDues == null || paymentEntity == null) {
			return;
		}
		for (DueAmountDetailsEntity due : finalDueAmount) {
			List<DueAmountDetailsEntity> childDues = getChildDuesOfCurrentDue(paymentEntity, due, recalculatedDues);
			if (childDues == null || childDues.isEmpty()) {
				continue;
			}
			BigDecimal totalFineAmount = childDues.stream().filter(Objects::nonNull).map(DueAmountDetailsEntity::getFineAmount)
					.map(this::parseNumeric).reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal totalAmount = parseNumeric(due.getTotalAmount());
			BigDecimal fineAmount = parseNumeric(due.getFineAmount());
			totalAmount = totalAmount.subtract(fineAmount).add(totalFineAmount);
			BigDecimal roundedTotal = totalAmount.setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
			BigDecimal roundUpAmount = roundedTotal.subtract(totalAmount).setScale(2, RoundingMode.HALF_UP);
			due.setFineAmount(totalFineAmount.toString());
			due.setTotalAmount(format(roundedTotal));
			due.setRoundUpAmount(format(roundUpAmount));
		}
	}

	private List<DueAmountDetailsEntity> getChildDuesOfCurrentDue(PaymentEntity paymentEntity, DueAmountDetailsEntity currentDue,
			Collection<DueAmountDetailsEntity> recalculatedDues) {
		List<String> paymentCycles = parseStringList(paymentEntity != null ? paymentEntity.getPaymentCollectionCycle() : null);
		String childCycle = getChildCycle(paymentCycles, currentDue != null ? currentDue.getCollectionCycle() : null);
		if (!hasText(childCycle)) {
			return null;
		}
		List<DueAmountDetailsEntity> dues = recalculatedDues.stream().filter(Objects::nonNull)
				.filter(due -> childCycle.equalsIgnoreCase(due.getCollectionCycle())).collect(Collectors.toList());
		if (dues.size() <= 1) {
			return dues.isEmpty() ? null : new ArrayList<>(dues);
		}
		return dues.stream().filter(due -> !due.getDueDate().isBefore(currentDue.getDueStartDate()))
				.filter(due -> !due.getDueDate().isAfter(currentDue.getDueEndDate())).collect(Collectors.toList());
	}

	private String getChildCycle(List<String> paymentCycles, String cycle) {
		if ("YEARLY".equals(cycle) && paymentCycles.contains("HALF YEARLY")) {
			return "HALF YEARLY";
		}
		if ("HALF YEARLY".equals(cycle) && paymentCycles.contains("QUATERLY")) {
			return "QUATERLY";
		}
		if ("QUATERLY".equals(cycle) && paymentCycles.contains("MONTHLY")) {
			return "MONTHLY";
		}
		if ("MONTHLY".equals(cycle) && paymentCycles.contains("MONTHLY")) {
			return "MONTHLY";
		}
		return "";
	}

	private String getFinalParentCycle(List<String> paymentCycles, String cycle) {
		String requiredCycle;
		do {
			requiredCycle = getParentCycle(paymentCycles, cycle);
			if (!hasText(requiredCycle)) {
				requiredCycle = cycle;
				break;
			}
			cycle = requiredCycle;
		} while (true);
		return requiredCycle;
	}

	private String getParentCycle(List<String> paymentCycles, String cycle) {
		if ("HALF YEARLY".equals(cycle) && paymentCycles.contains("YEARLY")) {
			return "YEARLY";
		}
		if ("QUATERLY".equals(cycle) && paymentCycles.contains("HALF YEARLY")) {
			return "HALF YEARLY";
		}
		if ("MONTHLY".equals(cycle) && paymentCycles.contains("QUATERLY")) {
			return "QUATERLY";
		}
		return "";
	}

	private boolean validatePerHeadOnceCollection(PaymentEntity paymentEntity) {
		if (paymentEntity != null && isPerHead(paymentEntity.getPaymentCapita())) {
			List<String> collectionCycles = parseStringList(paymentEntity.getPaymentCollectionCycle());
			return collectionCycles.size() == 1 && SecuraConstants.PAYMENT_CYCLE_ONCE.equals(collectionCycles.get(0));
		}
		return false;
	}

	private BigDecimal calculateSegmentPenalty(BigDecimal outstanding, BigDecimal rate, LocalDate start, LocalDate end,
			int cycleMonths, boolean partCycleAsFull, boolean cumulative, String simpleFineCalculationCycle) {
		BigDecimal cycleUnits = calculateCycleUnits(start, end, cycleMonths, partCycleAsFull);
		if (cycleUnits.compareTo(BigDecimal.ZERO) <= 0 || outstanding.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		if (cumulative) {
			double exponent = cycleUnits.doubleValue();
			double factor = Math.pow(BigDecimal.ONE.add(rate).doubleValue(), exponent) - 1.0d;
			return outstanding.multiply(BigDecimal.valueOf(factor), MathContext.DECIMAL64).setScale(2, RoundingMode.HALF_UP);
		}
		if (simpleFineCalculationCycle != null) {
			cycleUnits = calculateFineCycleUnits(cycleUnits, simpleFineCalculationCycle, partCycleAsFull);
		}
		return outstanding.multiply(rate, MathContext.DECIMAL64).multiply(cycleUnits, MathContext.DECIMAL64).setScale(2,
				RoundingMode.HALF_UP);
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

	private BigDecimal calculateFineCycleUnits(BigDecimal cycleUnits, String fineCycle, boolean partCycleAsFull) {
		if (cycleUnits == null) {
			return BigDecimal.ZERO;
		}
		BigDecimal divisor;
		switch (fineCycle.trim().toUpperCase(Locale.ROOT)) {
		case "MONTHLY":
			divisor = BigDecimal.ONE;
			break;
		case "QUATERLY":
		case "QUARTERLY":
			divisor = new BigDecimal("3");
			break;
		case "HALF YEARLY":
		case "HALFYEARLY":
			divisor = new BigDecimal("6");
			break;
		case "YEARLY":
			divisor = BigDecimal.ONE;
			break;
		default:
			throw new IllegalArgumentException("Unsupported fine cycle: " + fineCycle);
		}
		BigDecimal result = cycleUnits.divide(divisor, 2, RoundingMode.HALF_UP);
		if (partCycleAsFull && result.stripTrailingZeros().scale() > 0) {
			result = result.setScale(0, RoundingMode.CEILING);
		}
		return result;
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

	private LocalDate getBufferTimeDate(DiscFin fineDiscFin, LocalDate dueDate) {
		if (fineDiscFin == null || dueDate == null) {
			return null;
		}
		int bufferTime = parseNumeric(fineDiscFin.getBufferTime()).intValue();
		String bufferTimeUnit = fineDiscFin.getBufferTimeUnit();
		if ("DAYS".equalsIgnoreCase(bufferTimeUnit)) {
			return dueDate.plusDays(bufferTime);
		}
		if ("MONTH".equalsIgnoreCase(bufferTimeUnit)) {
			return dueDate.plusMonths(bufferTime);
		}
		if ("QUATER".equalsIgnoreCase(bufferTimeUnit) || "QUARTER".equalsIgnoreCase(bufferTimeUnit)) {
			return dueDate.plusMonths(bufferTime * 3L);
		}
		return dueDate.plusDays(bufferTime);
	}

	private boolean isBufferTimeElapsed(DiscFin discFin, LocalDate dueDate, LocalDate transactionDate) {
		if (discFin == null || dueDate == null || transactionDate == null) {
			return false;
		}
		BigDecimal bufferValue = parseNumeric(discFin.getBufferTime());
		if (bufferValue.compareTo(BigDecimal.ZERO) <= 0) {
			return transactionDate.isAfter(dueDate);
		}
		int bufferUnits = bufferValue.intValue();
		LocalDate thresholdDate = dueDate;
		String unit = stringValue(discFin.getBufferTimeUnit());
		if ("MONTH".equalsIgnoreCase(unit)) {
			thresholdDate = thresholdDate.plusMonths(bufferUnits);
		} else {
			thresholdDate = thresholdDate.plusDays(bufferUnits);
		}
		return transactionDate.isAfter(thresholdDate);
	}

	private List<Transaction> getSuccessfulTransactionsAfterDueDate(DueAmountDetailsEntity dueDetails, LocalDate transactionDate) {
		if (dueDetails == null || dueDetails.getDueDate() == null || !hasText(dueDetails.getPaymentId())) {
			return List.of();
		}
		List<Transaction> transactions = transactionRepository.findByPymntIdAndTrnsStatusOrderByTrnsDateAsc(
				dueDetails.getPaymentId(), SecuraConstants.TRANSACTION_STATUS_SUCCESS);
		if (transactions == null || transactions.isEmpty()) {
			return List.of();
		}
		LocalDate dueDate = dueDetails.getDueDate();
		return transactions.stream().filter(Objects::nonNull).filter(transaction -> transaction.getTrnsDate() != null)
				.filter(transaction -> !transaction.getTrnsDate().toLocalDate().isAfter(transactionDate))
				.filter(transaction -> transaction.getTrnsDate().toLocalDate().isAfter(dueDate))
				.filter(transaction -> isTransactionForDue(transaction, dueDetails)).sorted(Comparator.comparing(Transaction::getTrnsDate))
				.toList();
	}

	private Map<LocalDate, BigDecimal> aggregatePaymentsByDate(List<Transaction> transactions) {
		Map<LocalDate, BigDecimal> paymentsByDate = new LinkedHashMap<>();
		if (transactions == null || transactions.isEmpty()) {
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

	private boolean isTransactionForDue(Transaction transaction, DueAmountDetailsEntity dueDetails) {
		if (transaction == null || dueDetails == null || !hasText(transaction.getDueDetails()) || dueDetails.getDueDate() == null) {
			return false;
		}
		return matchesDueDetailsKey(transaction.getDueDetails().trim(), dueDetails.getDueId(), dueDetails.getCollectionCycle(),
				dueDetails.getDueDate());
	}

	private boolean matchesDueDetailsKey(String dueDetails, String dueId, String collectionCycle, LocalDate dueDate) {
		if (dueDetails == null || dueId == null || collectionCycle == null || dueDate == null) {
			return false;
		}
		String duePrefix = dueId + "_" + collectionCycle + "_";
		String dueDateToken = dueDate.toString();
		return dueDetails.startsWith(duePrefix) && dueDetails.endsWith("_" + dueDateToken);
	}

	private DueTotals calculateDueTotalsFromDueDetails(Map<PaymentDetail, List<DueAmountDetailsEntity>> dueDetails, String flatId,
			String flatArea, LocalDate transactionDate) {
		if (dueDetails == null || dueDetails.isEmpty()) {
			return new DueTotals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
		}
		BigDecimal totalDue = BigDecimal.ZERO;
		BigDecimal totalMandatoryPayment = BigDecimal.ZERO;
		BigDecimal totalOptionalPayment = BigDecimal.ZERO;
		for (Map.Entry<PaymentDetail, List<DueAmountDetailsEntity>> entry : dueDetails.entrySet()) {
			List<DueAmountDetailsEntity> selectedCycleDues = selectCycleDuesForTotals(entry.getValue());
			List<DueAmountDetailsEntity> filteredCycleDues = filterDuesByFlatArea(selectedCycleDues, flatArea);
			BigDecimal totalDuePerPaymentId = filteredCycleDues.stream().filter(Objects::nonNull)
					.map(dueEntity -> parseAmount(dueEntity.getTotalAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
			String paymentId = entry.getKey() != null ? entry.getKey().getPaymentId() : null;
			List<Transaction> paymentTransactions = hasText(paymentId)
					? transactionRepository.findByPymntIdAndFlatIdAndTrnsStatus(paymentId, flatId, SUCCESS_TRANSACTION_STATUS)
					: Collections.emptyList();
			List<Transaction> normalizedTransactions = paymentTransactions == null ? Collections.emptyList() : paymentTransactions;
			boolean hasPerHeadCapita = filteredCycleDues.stream().filter(Objects::nonNull)
					.anyMatch(dueEntity -> isPerHeadCapita(dueEntity.getPaymentCapita()));
			BigDecimal totalAmountPaidPerPaymentId = hasPerHeadCapita ? BigDecimal.ZERO
					: normalizedTransactions.stream().filter(transaction -> transaction.getTrnsDate() != null)
							.filter(transaction -> !transaction.getTrnsDate().toLocalDate().isAfter(transactionDate))
							.map(Transaction::getTrnsAmt).map(this::parseAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal netDueForPaymentId = totalDuePerPaymentId.subtract(totalAmountPaidPerPaymentId);
			if (netDueForPaymentId.compareTo(BigDecimal.ZERO) < 0) {
				netDueForPaymentId = BigDecimal.ZERO;
			}
			totalDue = totalDue.add(netDueForPaymentId);
			String paymentType = filteredCycleDues.stream().filter(Objects::nonNull).findFirst()
					.map(DueAmountDetailsEntity::getPaymentType).orElse(null);
			if ("OPTIONAL".equalsIgnoreCase(paymentType)) {
				totalOptionalPayment = totalOptionalPayment.add(netDueForPaymentId);
			} else if ("MANDATORY".equalsIgnoreCase(paymentType)) {
				totalMandatoryPayment = totalMandatoryPayment.add(netDueForPaymentId);
			}
		}
		return new DueTotals(totalDue, totalMandatoryPayment, totalOptionalPayment);
	}

	private List<DueAmountDetailsEntity> filterDuesByFlatArea(List<DueAmountDetailsEntity> selectedCycleDues, String flatArea) {
		if (selectedCycleDues == null || selectedCycleDues.isEmpty()) {
			return Collections.emptyList();
		}
		DueAmountDetailsEntity firstDue = selectedCycleDues.stream().filter(Objects::nonNull).findFirst().orElse(null);
		if (firstDue == null) {
			return Collections.emptyList();
		}
		if ("ALL".equalsIgnoreCase(firstDue.getFlatArea()) || !hasText(flatArea)) {
			return selectedCycleDues.stream().filter(Objects::nonNull).collect(Collectors.toList());
		}
		return selectedCycleDues.stream().filter(Objects::nonNull)
				.filter(dueEntity -> hasText(dueEntity.getFlatArea()) && dueEntity.getFlatArea().equalsIgnoreCase(flatArea))
				.collect(Collectors.toList());
	}

	private List<DueAmountDetailsEntity> selectCycleDuesForTotals(List<DueAmountDetailsEntity> paymentDues) {
		if (paymentDues == null || paymentDues.isEmpty()) {
			return Collections.emptyList();
		}
		List<DueAmountDetailsEntity> onceCycleDues = paymentDues.stream().filter(Objects::nonNull)
				.filter(dueEntity -> isOnceCycleForTotals(dueEntity.getCollectionCycle())).collect(Collectors.toList());
		if (!onceCycleDues.isEmpty()) {
			return onceCycleDues;
		}
		int highestCyclePriority = paymentDues.stream().filter(Objects::nonNull).map(DueAmountDetailsEntity::getCollectionCycle)
				.map(this::getCyclePriorityForTotals).max(Integer::compareTo).orElse(Integer.MIN_VALUE);
		if (highestCyclePriority <= 0) {
			return Collections.emptyList();
		}
		return paymentDues.stream().filter(Objects::nonNull)
				.filter(dueEntity -> getCyclePriorityForTotals(dueEntity.getCollectionCycle()) == highestCyclePriority)
				.collect(Collectors.toList());
	}

	private boolean isOnceCycleForTotals(String cycle) {
		return hasText(cycle) && SecuraConstants.PAYMENT_CYCLE_ONCE.equalsIgnoreCase(cycle.trim());
	}

	private boolean isPerHeadCapita(String paymentCapita) {
		if (!hasText(paymentCapita)) {
			return false;
		}
		String normalized = paymentCapita.toUpperCase(Locale.ROOT).replaceAll("[\\s_-]", "");
		return "PERHEAD".equals(normalized);
	}

	private int getCyclePriorityForTotals(String cycle) {
		if (!hasText(cycle)) {
			return 0;
		}
		String normalizedCycle = normalizeCycleForTotals(cycle);
		switch (normalizedCycle) {
		case SecuraConstants.PAYMENT_CYCLE_YEARLY:
			return 4;
		case SecuraConstants.PAYMENT_CYCLE_HALF_YEARLY:
			return 3;
		case "QUARTERLY":
		case SecuraConstants.PAYMENT_CYCLE_QUATERLY:
			return 2;
		case SecuraConstants.PAYMENT_CYCLE_MONTHLY:
			return 1;
		default:
			return 0;
		}
	}

	private String normalizeCycleForTotals(String cycle) {
		String normalized = cycle == null ? null : cycle.trim().toUpperCase(Locale.ENGLISH).replace("_", " ");
		if ("HALFYEARLY".equals(normalized)) {
			return SecuraConstants.PAYMENT_CYCLE_HALF_YEARLY;
		}
		if ("QUATERLY".equals(normalized)) {
			return SecuraConstants.PAYMENT_CYCLE_QUATERLY;
		}
		return normalized;
	}

	private boolean hasPenalty(Map<PaymentDetail, List<DueAmountDetailsEntity>> dueDetails) {
		if (dueDetails == null || dueDetails.isEmpty()) {
			return false;
		}
		return dueDetails.values().stream().flatMap(List::stream).filter(Objects::nonNull)
				.anyMatch(dueEntity -> parseAmount(dueEntity.getFineAmount()).compareTo(BigDecimal.ZERO) > 0);
	}

	private Map<String, List<String>> groupDueIdsByPayment(List<DueAmountDetailsEntity> dueEntities) {
		Map<String, List<String>> paymentIdToDueIdsMap = new LinkedHashMap<>();
		if (dueEntities == null || dueEntities.isEmpty()) {
			return paymentIdToDueIdsMap;
		}
		for (DueAmountDetailsEntity dueEntity : dueEntities) {
			String paymentId = dueEntity != null ? dueEntity.getPaymentId() : null;
			String dueId = dueEntity != null ? dueEntity.getDueId() : null;
			if (!hasText(paymentId) || !hasText(dueId)) {
				continue;
			}
			List<String> paymentDueIds = paymentIdToDueIdsMap.computeIfAbsent(paymentId, key -> new ArrayList<>());
			if (!paymentDueIds.contains(dueId)) {
				paymentDueIds.add(dueId);
			}
		}
		return paymentIdToDueIdsMap;
	}

	private Map<String, List<DueAmountDetailsEntity>> buildFinalPaymentMap(Map<String, List<String>> paymentIdToDueIdsMap,
			List<DueAmountDetailsEntity> dueEntities, LocalDate transactionDate) {
		Map<String, List<DueAmountDetailsEntity>> finalPaymentMap = new LinkedHashMap<>();
		if (paymentIdToDueIdsMap == null || paymentIdToDueIdsMap.isEmpty()) {
			return finalPaymentMap;
		}
		Map<String, List<DueAmountDetailsEntity>> dueEntitiesByPayment = dueEntities == null ? Collections.emptyMap()
				: dueEntities.stream().filter(Objects::nonNull).filter(dueEntity -> hasText(dueEntity.getPaymentId()))
						.collect(Collectors.groupingBy(DueAmountDetailsEntity::getPaymentId, LinkedHashMap::new, Collectors.toList()));
		for (Map.Entry<String, List<String>> entry : paymentIdToDueIdsMap.entrySet()) {
			List<DueAmountDetailsEntity> paymentDues = dueEntitiesByPayment.getOrDefault(entry.getKey(), Collections.emptyList())
					.stream().filter(dueEntity -> entry.getValue().contains(dueEntity.getDueId())).collect(Collectors.toList());
			List<DueAmountDetailsEntity> selectedDues = selectEligibleDues(paymentDues, transactionDate);
			if (!selectedDues.isEmpty()) {
				finalPaymentMap.put(entry.getKey(), selectedDues);
			}
		}
		return finalPaymentMap;
	}

	private List<DueAmountDetailsEntity> selectEligibleDues(List<DueAmountDetailsEntity> dueEntities,
			LocalDate transactionDate) {
		if (dueEntities == null || dueEntities.isEmpty()) {
			return Collections.emptyList();
		}
		Map<String, List<DueAmountDetailsEntity>> duesByGroup = dueEntities.stream().filter(Objects::nonNull)
				.collect(Collectors.groupingBy(this::buildDueSelectionGroupKey, LinkedHashMap::new, Collectors.toList()));
		List<DueAmountDetailsEntity> selectedDues = new ArrayList<>();
		for (List<DueAmountDetailsEntity> groupDues : duesByGroup.values()) {
			List<DueAmountDetailsEntity> sortedGroupDues = groupDues.stream().filter(Objects::nonNull)
					.sorted(Comparator.comparing(DueAmountDetailsEntity::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
					.collect(Collectors.toList());
			selectedDues.addAll(sortedGroupDues.stream()
					.filter(dueEntity -> dueEntity.getDueDate() == null || !dueEntity.getDueDate().isAfter(transactionDate))
					.collect(Collectors.toList()));
			sortedGroupDues.stream()
					.filter(dueEntity -> dueEntity.getDueDate() != null && dueEntity.getDueDate().isAfter(transactionDate))
					.findFirst().ifPresent(selectedDues::add);
		}
		selectedDues.sort(Comparator.comparingInt(
				(DueAmountDetailsEntity dueEntity) -> getCyclePriority(dueEntity != null ? dueEntity.getCollectionCycle() : null))
				.thenComparing(dueEntity -> dueEntity != null ? dueEntity.getDueDate() : null,
						Comparator.nullsLast(Comparator.naturalOrder())));
		return selectedDues;
	}

	private String buildDueSelectionGroupKey(DueAmountDetailsEntity dueEntity) {
		if (dueEntity == null) {
			return "";
		}
		return normalizeHierarchyKey(dueEntity.getDueId()).toUpperCase(Locale.ENGLISH) + "|"
				+ normalizeHierarchyKey(dueEntity.getCollectionCycle()).toUpperCase(Locale.ENGLISH) + "|"
				+ normalizeHierarchyKey(dueEntity.getFlatArea()).toUpperCase(Locale.ENGLISH);
	}

	private int getCyclePriority(String cycle) {
		if (cycle == null) {
			return Integer.MAX_VALUE;
		}
		switch (cycle.trim().toUpperCase(Locale.ENGLISH)) {
		case "MONTHLY":
			return 1;
		case "QUARTERLY":
			return 2;
		case "HALF_YEARLY":
		case "HALF YEARLY":
			return 3;
		case "YEARLY":
			return 4;
		default:
			return Integer.MAX_VALUE;
		}
	}

	private List<DueAmountDetailsEntity> filterOptionalClosedPaymentDues(List<DueAmountDetailsEntity> dues, String apartmentId,
			LocalDate transactionDate) {
		if (dues == null || dues.isEmpty()) {
			return Collections.emptyList();
		}
		List<DueAmountDetailsEntity> filteredDues = new ArrayList<>();
		for (DueAmountDetailsEntity due : dues) {
			if (due == null) {
				continue;
			}
			if (SecuraConstants.PAYMENT_TYPE_OPTIONAL.equalsIgnoreCase(due.getPaymentType())) {
				List<PaymentEntity> paymentEntities = paymentRepository.findByPaymentIdAndAprmtId(due.getPaymentId(), apartmentId);
				PaymentEntity paymentEntity = paymentEntities == null || paymentEntities.isEmpty() ? null : paymentEntities.get(0);
				if (paymentEntity != null && paymentEntity.getCollectionEndDate() != null
						&& paymentEntity.getCollectionEndDate().isBefore(transactionDate)) {
					LOGGER.info(
							"Skipping optional due for transaction date: apartmentId={}, paymentId={}, dueId={}, collectionEndDate={}, transactionDate={}",
							apartmentId, paymentEntity.getPaymentId(), due.getDueId(), paymentEntity.getCollectionEndDate(),
							transactionDate);
					continue;
				}
			}
			filteredDues.add(due);
		}
		return filteredDues;
	}

	private List<String> parseStringList(String json) {
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

	private List<String> filterPendingDueKeys(List<String> pendingDueKeys, LocalDate transactionDate) {
		if (pendingDueKeys == null || pendingDueKeys.isEmpty()) {
			return new ArrayList<>();
		}
		Map<String, List<PendingDueKey>> groupedKeys = pendingDueKeys.stream().map(this::parsePendingDueKey)
				.filter(Objects::nonNull).collect(Collectors.groupingBy(this::buildPendingDueGroupKey, LinkedHashMap::new,
						Collectors.toList()));
		LinkedHashSet<String> selectedKeys = new LinkedHashSet<>();
		for (List<PendingDueKey> groupedDueKeys : groupedKeys.values()) {
			boolean allFuture = groupedDueKeys.stream().allMatch(
					pendingDueKey -> pendingDueKey.dueDate() != null && pendingDueKey.dueDate().isAfter(transactionDate));
			if (allFuture) {
				groupedDueKeys.stream().sorted(Comparator.comparing(PendingDueKey::dueDate)).findFirst()
						.ifPresent(dueKey -> selectedKeys.add(dueKey.originalKey()));
				continue;
			}
			groupedDueKeys.stream().map(PendingDueKey::originalKey).forEach(selectedKeys::add);
		}
		return new ArrayList<>(selectedKeys);
	}

	private List<String> extractDueIdsFromFlatPendingList(List<String> pendingDueKeys) {
		if (pendingDueKeys == null || pendingDueKeys.isEmpty()) {
			return new ArrayList<>();
		}
		LinkedHashSet<String> dueIds = new LinkedHashSet<>();
		for (String pendingDueKey : pendingDueKeys) {
			if (!hasText(pendingDueKey)) {
				continue;
			}
			int separatorIndex = pendingDueKey.indexOf('_');
			String dueId = separatorIndex > 0 ? pendingDueKey.substring(0, separatorIndex) : pendingDueKey;
			if (hasText(dueId)) {
				dueIds.add(dueId);
			}
		}
		return new ArrayList<>(dueIds);
	}

	private List<DueAmountDetailsEntity> filterDueEntitiesByPendingKeys(List<DueAmountDetailsEntity> dueEntities,
			List<String> filteredPendingDueKeys) {
		if (dueEntities == null || dueEntities.isEmpty() || filteredPendingDueKeys == null || filteredPendingDueKeys.isEmpty()) {
			return Collections.emptyList();
		}
		LinkedHashSet<String> validDueEntityKeys = filteredPendingDueKeys.stream().map(this::parsePendingDueKey)
				.filter(Objects::nonNull).map(this::buildPendingDueEntityKey).collect(Collectors.toCollection(LinkedHashSet::new));
		return dueEntities.stream().filter(Objects::nonNull)
				.filter(dueEntity -> validDueEntityKeys.contains(buildPendingDueEntityKey(dueEntity))).collect(Collectors.toList());
	}

	private PendingDueKey parsePendingDueKey(String pendingDueKey) {
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
			return new PendingDueKey(normalizedPendingKey, dueId.trim(), collectionCycle.trim(), flatArea.trim(),
					LocalDate.parse(dueDateValue.trim()));
		} catch (DateTimeParseException ex) {
			return null;
		}
	}

	private String buildPendingDueGroupKey(PendingDueKey pendingDueKey) {
		if (pendingDueKey == null) {
			return "";
		}
		return normalizeHierarchyKey(pendingDueKey.dueId()).toUpperCase(Locale.ENGLISH) + "|"
				+ normalizeHierarchyKey(pendingDueKey.collectionCycle()).toUpperCase(Locale.ENGLISH) + "|"
				+ normalizeHierarchyKey(pendingDueKey.flatArea()).toUpperCase(Locale.ENGLISH);
	}

	private String buildPendingDueEntityKey(PendingDueKey pendingDueKey) {
		if (pendingDueKey == null || pendingDueKey.dueDate() == null) {
			return "";
		}
		return normalizeHierarchyKey(pendingDueKey.dueId()).toUpperCase(Locale.ENGLISH) + "|"
				+ normalizeHierarchyKey(pendingDueKey.collectionCycle()).toUpperCase(Locale.ENGLISH) + "|"
				+ normalizeHierarchyKey(pendingDueKey.flatArea()).toUpperCase(Locale.ENGLISH) + "|"
				+ pendingDueKey.dueDate();
	}

	private String buildPendingDueEntityKey(DueAmountDetailsEntity dueEntity) {
		if (dueEntity == null || dueEntity.getDueDate() == null) {
			return "";
		}
		return normalizeHierarchyKey(dueEntity.getDueId()).toUpperCase(Locale.ENGLISH) + "|"
				+ normalizeHierarchyKey(dueEntity.getCollectionCycle()).toUpperCase(Locale.ENGLISH) + "|"
				+ normalizeHierarchyKey(dueEntity.getFlatArea()).toUpperCase(Locale.ENGLISH) + "|" + dueEntity.getDueDate();
	}

	private String buildDueIdentityKey(DueAmountDetailsEntity dueEntity) {
		if (dueEntity == null) {
			return "";
		}
		return normalizeHierarchyKey(dueEntity.getDueId()).toUpperCase(Locale.ENGLISH) + "|"
				+ normalizeHierarchyKey(dueEntity.getCollectionCycle()).toUpperCase(Locale.ENGLISH) + "|"
				+ normalizeHierarchyKey(dueEntity.getFlatArea()).toUpperCase(Locale.ENGLISH) + "|"
				+ Objects.toString(dueEntity.getDueStartDate(), "") + "|" + Objects.toString(dueEntity.getDueEndDate(), "");
	}

	private DueAmountDetailsEntity copyDueEntity(DueAmountDetailsEntity dueEntity) {
		DueAmountDetailsEntity copy = new DueAmountDetailsEntity();
		copy.setAprmntId(dueEntity.getAprmntId());
		copy.setDueId(dueEntity.getDueId());
		copy.setCollectionCycle(dueEntity.getCollectionCycle());
		copy.setFlatArea(dueEntity.getFlatArea());
		copy.setDueDate(dueEntity.getDueDate());
		copy.setPaymentId(dueEntity.getPaymentId());
		copy.setAmount(dueEntity.getAmount());
		copy.setGstAmount(dueEntity.getGstAmount());
		copy.setTotalAmount(dueEntity.getTotalAmount());
		copy.setPaymentName(dueEntity.getPaymentName());
		copy.setPaymentType(dueEntity.getPaymentType());
		copy.setCause(dueEntity.getCause());
		copy.setPaymentCapita(dueEntity.getPaymentCapita());
		copy.setAddedCharges(dueEntity.getAddedCharges());
		copy.setAmountPerMonth(dueEntity.getAmountPerMonth());
		copy.setTotalAddedCharges(dueEntity.getTotalAddedCharges());
		copy.setEstimatedCollectionAmount(dueEntity.getEstimatedCollectionAmount());
		copy.setGstPercentage(dueEntity.getGstPercentage());
		copy.setDiscountCode(dueEntity.getDiscountCode());
		copy.setDiscountMode(dueEntity.getDiscountMode());
		copy.setCummilationCycle(dueEntity.getCummilationCycle());
		copy.setFineCode(dueEntity.getFineCode());
		copy.setDiscValue(dueEntity.getDiscValue());
		copy.setFnValue(dueEntity.getFnValue());
		copy.setDiscountedAmount(dueEntity.getDiscountedAmount());
		copy.setFineAmount(dueEntity.getFineAmount());
		copy.setFineMode(dueEntity.getFineMode());
		copy.setFineType(dueEntity.getFineType());
		copy.setRoundUpAmount(dueEntity.getRoundUpAmount());
		copy.setAlreadyPaidAmount(dueEntity.getAlreadyPaidAmount());
		copy.setAdminDiscount(dueEntity.getAdminDiscount());
		copy.setApplicableFlats(dueEntity.getApplicableFlats());
		copy.setPaidFlats(dueEntity.getPaidFlats());
		copy.setAllowedTenders(dueEntity.getAllowedTenders());
		copy.setPaymentStatus(dueEntity.getPaymentStatus());
		copy.setDueEndDate(dueEntity.getDueEndDate());
		copy.setDueStartDate(dueEntity.getDueStartDate());
		copy.setPaymentDate(dueEntity.getPaymentDate());
		copy.setCreatTs(dueEntity.getCreatTs());
		copy.setCreatUsrId(dueEntity.getCreatUsrId());
		copy.setLstUpdtTs(dueEntity.getLstUpdtTs());
		copy.setLstUpdtUsrId(dueEntity.getLstUpdtUsrId());
		return copy;
	}

	private DiscFinReference extractDiscFinReference(String discFinJson) {
		if (!hasText(discFinJson)) {
			return new DiscFinReference(null, null);
		}
		try {
			List<DiscFinTagEntry> entries = genericService.fromJson(discFinJson, new TypeReference<List<DiscFinTagEntry>>() {
			});
			String discountCode = null;
			String fineCode = null;
			for (DiscFinTagEntry entry : entries) {
				if (entry == null || !isStatusActive(stringValue(entry.getStatus()))) {
					continue;
				}
				String type = stringValue(entry.getDistfinType());
				String code = stringValue(entry.getCode());
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
		if (!hasText(code)) {
			return null;
		}
		List<DiscFin> discFins = discFinRepository.findByDiscFnId(code);
		if (discFins == null || discFins.isEmpty()) {
			return null;
		}
		String normalizedCycle = normalizeCycle(paymentCycle);
		DiscFin cycleSpecificDiscFin = discFins.stream().filter(Objects::nonNull)
				.filter(discFin -> normalizedCycle.equals(normalizeCycle(discFin.getDiscFnCycleType()))).findFirst().orElse(null);
		if (cycleSpecificDiscFin != null) {
			return cycleSpecificDiscFin;
		}
		String fixedCycle = normalizeCycle(SecuraConstants.DISC_FN_CYCLE_FIXED);
		return discFins.stream().filter(Objects::nonNull)
				.filter(discFin -> fixedCycle.equals(normalizeCycle(discFin.getDiscFnCycleType()))).findFirst().orElse(null);
	}

	private String resolveActiveFineCode(String discFinJson) {
		if (!hasText(discFinJson)) {
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
					.filter(entry -> isStatusActive(stringValue(entry.getStatus()))).map(DiscFinTagEntry::getCode)
					.filter(Objects::nonNull).findFirst().orElse(null);
		} catch (Exception ex) {
			return null;
		}
	}

	private boolean isStatusActive(String status) {
		return SecuraConstants.DISC_FIN_STATUS_ACTIVE.equalsIgnoreCase(stringValue(status));
	}

	private List<AddedCharges> parseAddedCharges(String addedChargesJson) {
		if (!hasText(addedChargesJson)) {
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
		if (!hasText(allowedModesJson)) {
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

	private Set<String> parseApplicableFlatNos(String applicableFor) {
		if (!hasText(applicableFor)) {
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
					.collect(Collectors.toCollection(LinkedHashSet::new));
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

	private BigDecimal parseAmount(String amount) {
		if (!hasText(amount)) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(amount.trim());
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

	private String formatAmount(BigDecimal amount) {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO.toPlainString();
		}
		return amount.stripTrailingZeros().toPlainString();
	}

	private String defaultZeroValue(String value) {
		return value == null || value.isBlank() ? format(BigDecimal.ZERO) : value;
	}

	private String stringValue(Object value) {
		return value == null ? null : value.toString();
	}

	private String normalizeHierarchyKey(String value) {
		return value == null ? "" : value.trim();
	}

	private String resolvePaymentName(PaymentEntity paymentEntity, List<DueAmountDetailsEntity> dueEntities) {
		if (paymentEntity != null && hasText(paymentEntity.getPaymentName())) {
			return paymentEntity.getPaymentName();
		}
		if (dueEntities == null || dueEntities.isEmpty()) {
			return null;
		}
		return dueEntities.stream().map(DueAmountDetailsEntity::getPaymentName).filter(this::hasText).findFirst().orElse(null);
	}

	private String resolvePaymentGateway(String apartmentId, String bankId) {
		if (!hasText(apartmentId) || !hasText(bankId)) {
			return null;
		}
		Optional<BankEntity> bankEntity = bankEntityRepository.findByAprmntIdAndBankDetailsID(apartmentId, bankId);
		if (bankEntity == null || bankEntity.isEmpty()) {
			return null;
		}
		return decryptNullable(bankEntity.get().getPgName());
	}

	private String decryptNullable(String value) {
		if (!hasText(value)) {
			return null;
		}
		return genericService.decrypt(value);
	}

	private int countDues(Map<PaymentDetail, List<DueAmountDetailsEntity>> dueDetails) {
		if (dueDetails == null || dueDetails.isEmpty()) {
			return 0;
		}
		return dueDetails.values().stream().filter(Objects::nonNull).mapToInt(List::size).sum();
	}

	private void initializeDefaultDueResponse(GetDueAmountForFlatResponse response) {
		response.setDueDetails(new LinkedHashMap<>());
		response.setTotalDue(BigDecimal.ZERO.toPlainString());
		response.setTotalMandatoryPayment(BigDecimal.ZERO.toPlainString());
		response.setTotalOptionalPayment(BigDecimal.ZERO.toPlainString());
		response.setPenaltyAdded(Boolean.FALSE);
	}

	private boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	private record DueTotals(BigDecimal totalDue, BigDecimal totalMandatoryPayment, BigDecimal totalOptionalPayment) {
	}

	private record PendingDueKey(String originalKey, String dueId, String collectionCycle, String flatArea,
			LocalDate dueDate) {
	}

	private record DiscFinReference(String discountCode, String fineCode) {
	}

	private record PenaltyCalculationResult(DiscFin discFin, BigDecimal amount) {
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
}
