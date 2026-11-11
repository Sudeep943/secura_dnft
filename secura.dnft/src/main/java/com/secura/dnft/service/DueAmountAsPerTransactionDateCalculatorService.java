package com.secura.dnft.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
			List<String> pendingDueKeys = parseStringList(flat.get().getFlatPndngPaymntLst());
			String flatArea=flat.get().getFlatArea();
			if (!flatAllDues.isEmpty()) {
				List<DueAmountDetailsEntity> alldueEntities = dueAmountDetailsRepository.findByDueIdsAndApartmentId(flatAllDues,apartmentId);
				List<DueAmountDetailsEntity> dueEntities=alldueEntities.stream().filter(due->due.getFlatArea().equals(flatArea)).collect(Collectors.toList());
				dueEntities.addAll(alldueEntities.stream().filter(due->!due.getPaymentCapita().equals(SecuraConstants.PAYMENT_CAPITA_PER_SQFT)).collect(Collectors.toList()));
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
					else {
						finalDueEntity = finalDueEntity.stream()
							    .filter(due -> pendingDueKeys.contains(
							        due.getDueId() + "_" +
							        due.getCollectionCycle() + "_" +
							        due.getFlatArea() + "_" +
							        due.getDueDate()
							    ))
							    .toList();					}
				 finalDueEntity=calculateDueTotalsAndRoundup(finalDueEntity);
				 Map<PaymentDetail, List<DueAmountDetailsEntity>> paymentDueDetailMap=createPaymentDetailsMap(finalDueEntity,allPaymentsEntities);
				 paymentDueDetailMap=updatePaymentDueTotalDetails(paymentDueDetailMap);
				 response.setDueDetails(paymentDueDetailMap);
				 response=updateTotals(paymentDueDetailMap,response);
				 return response;

			}
		} catch (Exception exp) {
			LOGGER.error("Due Couldn't get Fecthed . Error : "+exp.getMessage(),exp);
		}
		return null;

	}

    public GetDueAmountForFlatResponse updateTotals(
            Map<PaymentDetail, List<DueAmountDetailsEntity>> paymentDueDetailMap, GetDueAmountForFlatResponse response) {

        BigDecimal totalMandatoryPayment = BigDecimal.ZERO;
        BigDecimal totalOptionalPayment = BigDecimal.ZERO;

        if (paymentDueDetailMap != null) {
            for (PaymentDetail paymentDetail : paymentDueDetailMap.keySet()) {
                if (paymentDetail == null) {
                    continue;
                }
                Map<String, Set<DueAmountDetailsEntity>> causeWiseCollectionAllPaybleDues=response.getCauseWiseCollectionAllPaybleDues();
                if(null==causeWiseCollectionAllPaybleDues) {
                	causeWiseCollectionAllPaybleDues= new HashMap<>();
                }
                if(causeWiseCollectionAllPaybleDues.get(paymentDetail.getPaymentCause().toUpperCase())==null || causeWiseCollectionAllPaybleDues.get(paymentDetail.getPaymentCause().toUpperCase()).isEmpty()) {
              	  causeWiseCollectionAllPaybleDues.put(paymentDetail.getPaymentCause().toUpperCase(), paymentDueDetailMap.get(paymentDetail).stream().filter(due->due.isPayAllDue()).collect(Collectors.toSet())); 
                } 
                else {
                	causeWiseCollectionAllPaybleDues.get(paymentDetail.getPaymentCause().toUpperCase()).addAll(paymentDueDetailMap.get(paymentDetail).stream().filter(due->due.isPayAllDue()).collect(Collectors.toSet()));
                }
                BigDecimal remainingDue = parseAmount(
                        paymentDetail.getRemainingDue());

                String paymentType = paymentDetail.getPaymentType();

                if ("MANDATORY".equalsIgnoreCase(paymentType)) {
                    totalMandatoryPayment =
                            totalMandatoryPayment.add(remainingDue);

                } else if ("OPTIONAL".equalsIgnoreCase(paymentType)) {
                    totalOptionalPayment =
                            totalOptionalPayment.add(remainingDue);
                }
                response.setCauseWiseCollectionAllPaybleDues(causeWiseCollectionAllPaybleDues);
            }
        }

        BigDecimal totalDue =
                totalMandatoryPayment.add(totalOptionalPayment);

        response.setDueDetails(paymentDueDetailMap);
        response.setTotalMandatoryPayment(
                totalMandatoryPayment.toPlainString());
        response.setTotalOptionalPayment(
                totalOptionalPayment.toPlainString());
        response.setTotalDue(totalDue.toPlainString());

        return response;
    }

    private BigDecimal parseAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            return BigDecimal.ZERO;
        }

        return new BigDecimal(amount.trim());
    }
	
	
	 public Map<PaymentDetail, List<DueAmountDetailsEntity>>
     updatePaymentDueTotalDetails(
             Map<PaymentDetail, List<DueAmountDetailsEntity>>
                     paymentDueDetailMap) {
		 
		final  List<String> COLLECTION_CYCLE_PRIORITY =
		            List.of( SecuraConstants.DISC_FN_CYCLE_YEARLY,SecuraConstants.DISC_FN_CYCLE_HALF_YEARLY, SecuraConstants.DISC_FN_CYCLE_QUARTERLY_MISSPELLED,SecuraConstants.DISC_FN_CYCLE_QUARTERLY,SecuraConstants.DISC_FN_CYCLE_MONTHLY);

 if (paymentDueDetailMap == null || paymentDueDetailMap.isEmpty()) {
     return paymentDueDetailMap;
 }

 for (Map.Entry<PaymentDetail, List<DueAmountDetailsEntity>> entry
         : paymentDueDetailMap.entrySet()) {

     PaymentDetail paymentDetail = entry.getKey();
     List<DueAmountDetailsEntity> dueDetails = entry.getValue();
     updatePaybleNowFlag(dueDetails);
     paymentDetail.setPaymentType(dueDetails.get(0).getPaymentType());
     if (paymentDetail == null || dueDetails == null
             || dueDetails.isEmpty()) {
         continue;
     }

     // Priority 1: Once
     List<DueAmountDetailsEntity> onceDetails = dueDetails.stream()
             .filter(Objects::nonNull)
             .filter(due -> SecuraConstants.PAYMENT_CYCLE_ONCE.equalsIgnoreCase(
                     due.getCollectionCycle()))
             .toList();

     if (!onceDetails.isEmpty()) {

         // Set totalAmount of every Once entry to remainingDue.
         onceDetails.forEach(due ->
         paymentDetail.setRemainingDue(due.getTotalAmount())
         );
         onceDetails.forEach(due ->
         due.setPayAllDue(true)
         );

         continue;
     }

     
     // Priority 2 onwards: Monthly, Quarterly, Half-Yearly, Yearly
     for (String cycle : COLLECTION_CYCLE_PRIORITY) {

         List<DueAmountDetailsEntity> highestDues =
                 dueDetails.stream()
                         .filter(Objects::nonNull)
                         .filter(due -> cycle.equalsIgnoreCase(
                                 due.getCollectionCycle()))
                         .toList();
          if(highestDues.isEmpty()) {
	 continue;
            }
         List<DueAmountDetailsEntity> childDuesOfhighestDues=  highestDues.stream()
                 .flatMap(due -> getChildDuesOfRequiredDue(due, dueDetails).stream())
                 .toList();
         List<DueAmountDetailsEntity> matchingDetails= new ArrayList<>(highestDues);;
         matchingDetails.addAll(dueDetails.stream().filter(due->!childDuesOfhighestDues.contains(due) && !highestDues.contains(due)).collect(Collectors.toList()));
         dueDetails.forEach(due ->
         due.setPayAllDue(matchingDetails.contains(due))
     );
         if (!matchingDetails.isEmpty()) {

             BigDecimal total = matchingDetails.stream()
                     .map(DueAmountDetailsEntity::getTotalAmount)
                     .filter(Objects::nonNull)
                     .map(String::trim)
                     .filter(amount -> !amount.isEmpty())
                     .map(BigDecimal::new)
                     .reduce(BigDecimal.ZERO, BigDecimal::add);

             paymentDetail.setRemainingDue(total.toPlainString());

             // Stop after the first available cycle.
             break;
         }
     }
 }

 return paymentDueDetailMap;
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
		if(paymentEntity.get().getPaymentCollectionCycle().contains(SecuraConstants.PAYMENT_CYCLE_ONCE)) {
			return due;
		}
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
		if(null==payment || null==payment.getDiscFin()) {
			return due;
		}
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
		if(null==payment || null==payment.getDiscFin()) {
			return due;
		}
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
		 		if (due.getDueDate() != null
		 		        && due.getDueDate().isBefore(LocalDate.now())) {
		 			due.setStatus(SecuraConstants.DUE_STATUS_OVERDUE);
		 		}
		 		else{
		 			due.setStatus(SecuraConstants.DUE_STATUS_ACTIVE);
		 		}
			return due;}).collect(Collectors.toList());
		
	}
	
	

public void updatePaybleNowFlag(List<DueAmountDetailsEntity> dues) {

    if (dues == null || dues.isEmpty()) {
        return;
    }

    // Default all dues to not payable now.
    dues.stream()
            .filter(Objects::nonNull)
            .forEach(due -> due.setPaybleNow(false));

    // Get overdue dues.
    List<DueAmountDetailsEntity> overdueDues = dues.stream()
            .filter(Objects::nonNull)
            .filter(due -> SecuraConstants.DUE_STATUS_OVERDUE.equalsIgnoreCase(due.getStatus()))
            .toList();

    // Get active dues.
    List<DueAmountDetailsEntity> activeDues = dues.stream()
            .filter(Objects::nonNull)
            .filter(due ->  SecuraConstants.DUE_STATUS_ACTIVE.equalsIgnoreCase(due.getStatus()))
            .toList();

    // Prefer overdue dues; otherwise process active dues.
    List<DueAmountDetailsEntity> eligibleDues =
            !overdueDues.isEmpty() ? overdueDues : activeDues;

    if (eligibleDues.isEmpty()) {
        return;
    }

    // If Once exists, make the Once dues payable now.
    List<DueAmountDetailsEntity> onceDues = eligibleDues.stream()
            .filter(due -> SecuraConstants.PAYMENT_CYCLE_ONCE.equalsIgnoreCase(
                    due.getCollectionCycle()))
            .toList();

    if (!onceDues.isEmpty()) {
        onceDues.forEach(due -> due.setPaybleNow(true));
        return;
    }

    // Find the oldest due by due date.
    List<DueAmountDetailsEntity> oldestDues = eligibleDues.stream()
            .filter(due -> due.getDueDate() != null)
            .collect(Collectors.groupingBy(DueAmountDetailsEntity::getDueDate))
            .entrySet()
            .stream()
            .min(Map.Entry.comparingByKey())
            .map(Map.Entry::getValue)
            .orElse(Collections.emptyList());

    List<String> cyclePriority =
            List.of(SecuraConstants.DISC_FN_CYCLE_YEARLY, SecuraConstants.DISC_FN_CYCLE_HALF_YEARLY, SecuraConstants.PAYMENT_CYCLE_QUATERLY, SecuraConstants.PAYMENT_CYCLE_MONTHLY);

    String highestCycle = cyclePriority.stream()
            .filter(cycle -> oldestDues.stream()
                    .anyMatch(due -> cycle.equalsIgnoreCase(
                            due.getCollectionCycle())))
            .findFirst()
            .orElse(null);

    Comparator<DueAmountDetailsEntity> dueDateComparator =
            Comparator.comparing(DueAmountDetailsEntity::getDueDate);
    
    DueAmountDetailsEntity highestCycleOldestDues = oldestDues.stream()
            .filter(due -> highestCycle != null
                    && highestCycle.equalsIgnoreCase(due.getCollectionCycle())).sorted(dueDateComparator)
            .findFirst()
            .orElse(null);

    if (highestCycleOldestDues == null) {
        return;
    }

    // Determine the lowest available collection cycle.
    String lowestCycle = getLowestCollectionCycle(eligibleDues);

    if (lowestCycle == null) {
        return;
    }

    // Mark the oldest due as payable.
    highestCycleOldestDues.setPaybleNow(true);

    // Get child dues and mark them payable.
    List<DueAmountDetailsEntity> childDues =
    		getChildDuesOfRequiredDue(highestCycleOldestDues, eligibleDues);

    if (childDues != null) {
//        childDues.stream()
//                .filter(Objects::nonNull)
//                .forEach(due -> due.setPaybleNow(true));
        childDues.stream()
        .filter(childDue -> highestCycleOldestDues.getDueDate().equals(childDue.getDueDate()))
        .forEach(childDue -> childDue.setPaybleNow(true));
    }
}

public List<DueAmountDetailsEntity> getChildDuesOfRequiredDue(DueAmountDetailsEntity requiredDue,  List<DueAmountDetailsEntity> allDue){
	 List<DueAmountDetailsEntity> childDues= new ArrayList<>();
	 
	 childDues=allDue.stream().filter(due->!due.getCollectionCycle().equals(requiredDue.getCollectionCycle()) &&
			 !due.getDueStartDate().isBefore(requiredDue.getDueStartDate()) &&! due.getDueEndDate().isAfter(requiredDue.getDueEndDate())).collect(Collectors.toList());
	 
	 return childDues;
}

private String getLowestCollectionCycle(
        List<DueAmountDetailsEntity> dues) {

    List<String> cyclePriority =
            List.of(SecuraConstants.PAYMENT_CYCLE_MONTHLY, SecuraConstants.PAYMENT_CYCLE_QUATERLY, SecuraConstants.DISC_FN_CYCLE_HALF_YEARLY, SecuraConstants.DISC_FN_CYCLE_YEARLY);

    for (String cycle : cyclePriority) {
        boolean exists = dues.stream()
                .filter(Objects::nonNull)
                .anyMatch(due -> cycle.equalsIgnoreCase(
                        due.getCollectionCycle()));

        if (exists) {
            return cycle;
        }
    }

    return null;
}
}
