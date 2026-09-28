package com.secura.dnft.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
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
import com.secura.dnft.entity.DiscFin;
import com.secura.dnft.entity.DueAmountDetailsEntity;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.request.response.GetDueAmountForFlatRequest;
import com.secura.dnft.request.response.GetDueAmountForFlatResponse;
import com.secura.dnft.request.response.PaymentDetail;

@Service
public class DueAmountAsPerTransactionDateCalculatorService extends DueAmountAsPerTransactionDateCalculatorUtilService {
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

	public GetDueAmountForFlatResponse getDuesForFlatAsPerTransactionDate(GetDueAmountForFlatRequest request) {
		GetDueAmountForFlatResponse response = new GetDueAmountForFlatResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		initializeDefaultDueResponse(response);
		List<DueAmountDetailsEntity> finalDueEntity= new ArrayList<>(); 
		try {
			String flatId = request != null ? request.getFlatId() : null;
			String apartmentId = request != null && request.getGenericHeader() != null
					? request.getGenericHeader().getApartmentId()
					: null;
			Optional<Flat> flat = flatRepository.findByAprmntIdAndFlatNo(apartmentId, flatId);
			if(flat.isEmpty()) {
				return null;
			}
			List<String> flatAllDues = getAllDuesForFlat(flat.get(), apartmentId);
			String flatArea=flat.get().getFlatArea();
			if (!flatAllDues.isEmpty()) {
				List<DueAmountDetailsEntity> dueEntities = dueAmountDetailsRepository.findByDueIdsAndApartmentId(flatAllDues,apartmentId);
				dueEntities=dueEntities.stream().filter(due->due.getFlatArea().equals(flatArea)).collect(Collectors.toList());
				 List<String>paymentIds=dueEntities.stream().map(due->due.getPaymentId()).distinct().collect(Collectors.toList());
				 List<PaymentEntity> allPaymentsEntities= paymentRepository.findByPaymentIdsAndAprmntId(paymentIds,apartmentId);
				 finalDueEntity=dueEntities.stream().map(due->setDirectDueData(due,findDuePaymentDetails(due,allPaymentsEntities))).collect(Collectors.toList());
				 //setDiscountData
				 finalDueEntity=finalDueEntity.stream().map(due->calculateAndSetDiscountAndFineData(due,findDuePaymentDetails(due,allPaymentsEntities),request.getTransactionDate())).collect(Collectors.toList());
				 
				 //setPenaltyData
				 finalDueEntity=finalDueEntity.stream().map(due->calculateAndSetPenalty(due,findDuePaymentDetails(due,allPaymentsEntities),request.getTransactionDate())).collect(Collectors.toList());
				 finalDueEntity=setPenlatyAsPerChildDue(finalDueEntity,allPaymentsEntities);
					if(null!=request.getSpecificDue()) {
						DueAmountDetailsEntity specificDue=request.getSpecificDue();
						finalDueEntity=finalDueEntity.stream().filter(due->due.getDueId().equals(specificDue.getDueId()) && due.getPaymentId().equals(specificDue.getPaymentId()) && due.getAprmntId().equals(specificDue.getAprmntId()) && due.getDueStartDate().equals(specificDue.getDueStartDate()) && due.getDueEndDate().equals(specificDue.getDueEndDate()))
								.collect(Collectors.toList());
					}
				 finalDueEntity=calculateDueTotalsAndRoundup(finalDueEntity);
				 response.setDueDetails(createPaymentDetailsMap(finalDueEntity,allPaymentsEntities));
				 return response;

			}
		} catch (Exception exp) {

		}
		return null;

	}
	

	public Map<PaymentDetail, List<DueAmountDetailsEntity>> createPaymentDetailsMap(
	        List<DueAmountDetailsEntity> finalDueEntity,
	        List<PaymentEntity> allPaymentsEntities) {

	    // Create lookup map: paymentId -> PaymentEntity
	    Map<String, PaymentEntity> paymentEntityMap = allPaymentsEntities.stream()
	            .collect(Collectors.toMap(
	                    PaymentEntity::getPaymentId,
	                    Function.identity(),
	                    (existing, replacement) -> existing
	            ));

	    return finalDueEntity.stream()
	            .collect(Collectors.groupingBy(
	                    DueAmountDetailsEntity::getPaymentId
	            ))
	            .entrySet()
	            .stream()
	            .map(entry -> {

	                String paymentId = entry.getKey();

	                List<DueAmountDetailsEntity> dues = entry.getValue();

	                PaymentEntity paymentEntity = paymentEntityMap.get(paymentId);

	                if (paymentEntity == null) {
	                    return null;
	                }

	                PaymentDetail paymentDetail =
	                        createPaymentDetail(paymentEntity);

	                return new AbstractMap.SimpleEntry<>(
	                        paymentDetail,
	                        dues
	                );
	            })
	            .filter(Objects::nonNull)
	            .collect(Collectors.toMap(
	                    Map.Entry::getKey,
	                    Map.Entry::getValue
	            ));
	}
	
	private List<DueAmountDetailsEntity>  setPenlatyAsPerChildDue(List<DueAmountDetailsEntity> finalDueEntity, List<PaymentEntity> allPaymentsEntities) {
		finalDueEntity.stream().map(due->{return getCalculatedPenaltyFromChildDue(finalDueEntity,allPaymentsEntities,due);}).collect(Collectors.toList());
		return finalDueEntity;
		
	}
	
	private DueAmountDetailsEntity getCalculatedPenaltyFromChildDue(List<DueAmountDetailsEntity> finalDueEntity, List<PaymentEntity> allPaymentsEntities,DueAmountDetailsEntity due) {
		Optional<PaymentEntity> paymentEntity= allPaymentsEntities.stream().filter(pay->pay.getPaymentId().equals(due.getPaymentId())).findFirst();
		String lowestCycle=getTheLowestCollectionCycle(paymentEntity.get());
		if(lowestCycle.equalsIgnoreCase(due.getCollectionCycle())) {
			return due;
		}
		 List<DueAmountDetailsEntity> childDues= getChildDues(due,finalDueEntity,lowestCycle);
		 BigDecimal totalPenaltyAmountInChildDues=childDues.stream()
            .map(DueAmountDetailsEntity::getFineAmount)
            .filter(Objects::nonNull)
            .filter(amount -> !amount.isBlank())
            .map(BigDecimal::new)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
		
		 BigDecimal totalAmount = parseBigDecimal(due.getTotalAmount());
		 BigDecimal alreadyCalculatedFineAmount = parseBigDecimal(due.getFineAmount());
		 totalAmount=totalAmount.subtract(alreadyCalculatedFineAmount).add(totalPenaltyAmountInChildDues);
		 due.setTotalAmount(stringValue(totalAmount));
		 due.setFineAmount(stringValue(totalPenaltyAmountInChildDues));
		return due;
	}
	
 private PaymentDetail createPaymentDetail(PaymentEntity paymentEntity) {
	   PaymentDetail paymentDetail = new PaymentDetail();
	    paymentDetail.setPaymentId(paymentEntity.getPaymentId());
	    paymentDetail.setPaymentName(paymentEntity.getPaymentName());
	    paymentDetail.setBankId(paymentEntity.getBankAccountId());
	    paymentDetail.setPaymentGateway(paymentEntity.getPaymentCollectionMode());
	    paymentDetail.setPaymentCause(paymentEntity.getCauseId());

	 return paymentDetail;
 }
 
 

	public DueAmountDetailsEntity setDirectDueData(
	        DueAmountDetailsEntity dueDetails,
	        PaymentEntity payment) {

	    if (dueDetails == null || payment == null) {
	        return null;
	    }
	    if (isBlank(dueDetails.getPaymentId())) {
	        dueDetails.setPaymentId(payment.getPaymentId());
	    }
	    if (isBlank(dueDetails.getPaymentName())) {
	        dueDetails.setPaymentName(payment.getPaymentName());
	    }
	    if (isBlank(dueDetails.getPaymentCapita())) {
	        dueDetails.setPaymentCapita(payment.getPaymentCapita());
	    }
	    if (isBlank(dueDetails.getPaymentType())) {
	        dueDetails.setPaymentType(payment.getPaymentType());
	    }
	    if (isBlank(dueDetails.getPaymentStatus())) {
	        dueDetails.setPaymentStatus(payment.getStatus());
	    }
	    if (isBlank(dueDetails.getAprmntId())) {
	        dueDetails.setAprmntId(payment.getAprmtId());
	    }
	    if (isBlank(dueDetails.getCause())) {
	        dueDetails.setCause(payment.getCauseId());
	    }
	    if (isBlank(dueDetails.getCreatUsrId())) {
	        dueDetails.setCreatUsrId(payment.getCreatUsrId());
	    }
	    if (dueDetails.getCreatTs() == null) {
	        dueDetails.setCreatTs(payment.getCreatTs());
	    }
	    if (dueDetails.getLstUpdtUsrId() == null) {
	        dueDetails.setLstUpdtUsrId(payment.getLstUpdtUsrId());
	    }
	    if (dueDetails.getLstUpdtTs() == null) {
	        dueDetails.setLstUpdtTs(payment.getLstUpdtTs());
	    }
	    if (isBlank(dueDetails.getAmount())) {
	        dueDetails.setAmount(payment.getPaymentAmount());
	    }
	    if (isBlank(dueDetails.getAmountPerMonth())) {
	        dueDetails.setAmountPerMonth(payment.getPaymentAmount());
	    }
	    if (isBlank(dueDetails.getAlreadyPaidAmount())) {
	        dueDetails.setAlreadyPaidAmount("0");
	    }
	    if (isBlank(dueDetails.getEstimatedCollectionAmount())) {
	        dueDetails.setEstimatedCollectionAmount(payment.getPaymentAmount());
	    }
	    if (dueDetails.getDueDate() == null) {
	        dueDetails.setDueDate(payment.getDueDate());
	    }
	    if (dueDetails.getDueStartDate() == null) {
	        dueDetails.setDueStartDate(payment.getCollectionStartDate());
	    }

	    if (dueDetails.getDueEndDate() == null) {
	        dueDetails.setDueEndDate(payment.getCollectionEndDate());
	    }
	    if (isBlank(dueDetails.getCollectionCycle())) {
	        dueDetails.setCollectionCycle(payment.getPaymentCollectionCycle());
	    }
	    if (isBlank(dueDetails.getPaymentType())) {
	        dueDetails.setPaymentType(payment.getPaymentType());
	    }
	    return dueDetails;
	}

	public DueAmountDetailsEntity calculateAndSetDiscountAndFineData(
	        DueAmountDetailsEntity due,PaymentEntity payment,
	        LocalDate transactionDate) {

		DiscFin discFinData=null;
		List<DiscFinTagEntry> discFinEntry = genericService.fromJson(payment.getDiscFin(),
				new TypeReference<List<DiscFinTagEntry>>() {
				});
		
		String discountCode = null;
		for (DiscFinTagEntry entry : discFinEntry) {
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
			}
		}
		if(null!=discountCode)
		{
			discFinData=resolveDiscFin(discountCode, due.getCollectionCycle(), due.getAprmntId());
		
		
	    // Basic null validation
	    if (due == null || discFinData == null || transactionDate == null) {
	        return due;
	    }

	    // 1. Check transaction date
	    if (discFinData.getDiscFnStrtDt() != null
	            && transactionDate.isBefore(discFinData.getDiscFnStrtDt())) {
	        return due;
	    }

	    if (discFinData.getDiscFnEndDt() != null
	            && transactionDate.isAfter(discFinData.getDiscFnEndDt())) {
	        return due;
	    }

	    // 2. Check cycle if discount cycle is NOT FIXED
	    String discountCycleType = discFinData.getDiscFnCycleType();

	    if (discountCycleType != null
	            && !SecuraConstants.DISC_FN_CYCLE_FIXED.equalsIgnoreCase(discountCycleType)) {

	        if (!discountCycleType.equalsIgnoreCase(due.getCollectionCycle())) {
	            return due;
	        }
	    }

	    // 3. Calculate current total
	    BigDecimal amount = parseBigDecimal(due.getAmount());
	    BigDecimal alreadyPaidAmount = parseBigDecimal(due.getAlreadyPaidAmount());

	    BigDecimal totalAmount = amount.subtract(alreadyPaidAmount);

	    if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
	        totalAmount = BigDecimal.ZERO;
	    }

	    // 4. Discount value
	    BigDecimal discountValue = parseBigDecimal(discFinData.getDiscFinValue());

	    BigDecimal discountedAmount;

	    // 5. Calculate discount
	    if (SecuraConstants.DISC_FN_MODE_PERCENTAGE.equalsIgnoreCase(discFinData.getDiscFnMode())) {
	        discountedAmount = totalAmount
	                .multiply(discountValue)
	                .divide(BigDecimal.valueOf(100));

	    } else if (SecuraConstants.DISC_FN_MODE_AMOUNT.equalsIgnoreCase(discFinData.getDiscFnMode())) {

	        discountedAmount = totalAmount.subtract(discountValue);

	    } else {
	        return due;
	    }
	    if (discountedAmount.compareTo(BigDecimal.ZERO) < 0) {
	        discountedAmount = BigDecimal.ZERO;
	    }
	    due.setDiscountCode(discFinData.getDiscFnId());
	    due.setDiscountMode(discFinData.getDiscFnMode());
	    due.setDiscValue(discFinData.getDiscFinValue());
	    due.setDiscountedAmount(discountedAmount.toPlainString());
	    due.setTotalAmount(String.valueOf(totalAmount.subtract(discountedAmount)));
		}
	    return due;
	}

	public DueAmountDetailsEntity calculateAndSetPenalty(
	        DueAmountDetailsEntity due,PaymentEntity payment,
	        LocalDate transactionDate) {

		DiscFin discFinData=null;
		List<DiscFinTagEntry> discFinEntry = genericService.fromJson(payment.getDiscFin(),
				new TypeReference<List<DiscFinTagEntry>>() {
				});
		
		String fineCode = null;
		for (DiscFinTagEntry entry : discFinEntry) {
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
			if (SecuraConstants.DISC_FN_TYPE_FINE.equalsIgnoreCase(type)) {
				fineCode = code;
			}
		}
		if(null!=fineCode)
		{
			discFinData=resolveDiscFin(fineCode, due.getCollectionCycle(), due.getAprmntId());
		
		}
	    if (due == null || discFinData == null || transactionDate == null) {
	        return due;
	    }

	    /*
	     * 1. Check penalty validity period
	     */
	    if (discFinData.getDiscFnStrtDt() != null
	            && transactionDate.isBefore(discFinData.getDiscFnStrtDt())) {
	        return due;
	    }

	    if (discFinData.getDiscFnEndDt() != null
	            && transactionDate.isAfter(discFinData.getDiscFnEndDt())) {
	        return due;
	    }

	    /*
	     * 2. Calculate buffer end date
	     *
	     * Example:
	     * Due date      = 01-Sep-2026
	     * Buffer        = 2 Months
	     * Buffer end    = 01-Nov-2026
	     *
	     * If transaction date is before 01-Nov-2026,
	     * no penalty will be applied.
	     */
	    LocalDate dueDate = due.getDueDate();

	    if (dueDate == null) {
	        return due;
	    }

	    LocalDate bufferEndDate = calculateBufferEndDate(
	            dueDate,
	            discFinData.getBufferTime(),
	            discFinData.getBufferTimeUnit()
	    );

	    if (bufferEndDate != null
	            && transactionDate.isBefore(bufferEndDate)) {
	        return due;
	    }

	    /*
	     * 3. Calculate duration based on:
	     *
	     * Monthly       = 1 month
	     * Quarterly     = 3 months
	     * Half Yearly   = 6 months
	     * Yearly        = 12 months
	     */
	    BigDecimal duration = calculatePenaltyDuration(
	            dueDate,
	            transactionDate,
	            discFinData.getSimpleFineCycle(),
	            discFinData.getPartOfCycleAsFull()
	    );

	    if (duration.compareTo(BigDecimal.ZERO) <= 0) {
	        return due;
	    }

	    /*
	     * 4. Principal = current due amount
	     *
	     * totalAmount = amount - alreadyPaidAmount
	     */
	    BigDecimal amount = parseBigDecimal(due.getAmount());
	    BigDecimal alreadyPaidAmount =
	            parseBigDecimal(due.getAlreadyPaidAmount());

	    BigDecimal principal = amount.subtract(alreadyPaidAmount);

	    if (principal.compareTo(BigDecimal.ZERO) < 0) {
	        principal = BigDecimal.ZERO;
	    }

	    /*
	     * 5. Penalty calculation
	     */
	    BigDecimal fineValue =
	            parseBigDecimal(discFinData.getDiscFinValue());

	    BigDecimal penaltyAmount;

	    if (SecuraConstants.DISC_FN_MODE_PERCENTAGE.equalsIgnoreCase(discFinData.getDiscFnMode())) {

	        BigDecimal rate = fineValue.divide(
	                BigDecimal.valueOf(100),
	                10,
	                RoundingMode.HALF_UP
	        );

	        if (SecuraConstants.DISC_FN_CYCLE_TYPE_SIMPLE.equalsIgnoreCase(
	                discFinData.getFnCalculationType())) {

	            /*
	             * Simple Interest
	             *
	             * Interest = P * R * T
	             */
	            penaltyAmount = principal
	                    .multiply(rate)
	                    .multiply(duration);

	        } else {

	            /*
	             * Compound Interest
	             *
	             * A = P(1 + R)^T
	             *
	             * Interest = A - P
	             *
	             * Math.pow is used because duration can be decimal.
	             */
	            double principalDouble = principal.doubleValue();
	            double rateDouble = rate.doubleValue();
	            double durationDouble = duration.doubleValue();

	            double compoundAmount =
	                    principalDouble
	                            * Math.pow(
	                                    1 + rateDouble,
	                                    durationDouble
	                            );

	            penaltyAmount = BigDecimal.valueOf(compoundAmount)
	                    .subtract(principal);
	        }

	    } else if (SecuraConstants.DISC_FN_MODE_AMOUNT.equalsIgnoreCase(
	            discFinData.getDiscFnMode())) {

	        /*
	         * Fixed amount penalty
	         *
	         * Penalty = Duration * Fine Value
	         */
	        penaltyAmount = duration.multiply(fineValue);

	    } else {
	        // Unknown penalty mode
	        return due;
	    }

	    /*
	     * 6. Prevent negative penalty
	     */
	    if (penaltyAmount.compareTo(BigDecimal.ZERO) < 0) {
	        penaltyAmount = BigDecimal.ZERO;
	    }

	    /*
	     * 7. Set penalty details
	     */
	    due.setFineAmount(
	            penaltyAmount.setScale(2, RoundingMode.HALF_UP)
	                    .toPlainString()
	    );

	    due.setFineCode(discFinData.getDiscFnId());
	    due.setFineMode(discFinData.getDiscFnMode());
	    due.setFineType(discFinData.getDiscFnCycleType());
	    due.setFnValue(discFinData.getDiscFinValue());
	    due.setTotalAmount(String.valueOf(principal.add(penaltyAmount)));

	    return due;
	}
	public void calculateAddedCharges() {

	}

	public List<DueAmountDetailsEntity> calculateDueTotalsAndRoundup(List<DueAmountDetailsEntity> finalDueEntity) {
		
		return finalDueEntity.stream().map(due->{
			BigDecimal totalAmount = new BigDecimal(due.getTotalAmount());
			 BigDecimal roundedTotal = totalAmount.setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
		 		BigDecimal roundUpAmount = roundedTotal.subtract(totalAmount).setScale(2, RoundingMode.HALF_UP);
		 		due.setTotalAmount(roundedTotal.toString());
		 		due.setRoundUpAmount(roundUpAmount.toString());
			return due;}).collect(Collectors.toList());
		
	}
	
	

}
