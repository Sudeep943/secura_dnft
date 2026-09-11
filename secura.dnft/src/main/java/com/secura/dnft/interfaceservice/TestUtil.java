package com.secura.dnft.interfaceservice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.secura.dnft.dao.DueAmountDetailsRepository;
import com.secura.dnft.dao.TransDueDetailsRepository;
import com.secura.dnft.dao.TransactionRepository;
import com.secura.dnft.entity.DueAmountDetailsEntity;
import com.secura.dnft.entity.DueAmountDetailsEntityId;
import com.secura.dnft.entity.TransDueDetailsEntity;
import com.secura.dnft.entity.Transaction;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.request.response.ActionTransactionReviewWorkListRequest;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.service.WorklistService;

import jakarta.transaction.Transactional;

@Service
public class TestUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestUtil.class);
	@Autowired
	TransactionRepository transactionRepository;
	
	@Autowired
	TransDueDetailsRepository transDueDetailsRepository;
	
	@Autowired
	WorklistService worklistService;
	
	@Autowired
	DueAmountDetailsRepository dueAmountDetailsRepository;


@Transactional
public void createMissingTransDueDetails(String transacId) {

    LocalDateTime startDate = LocalDateTime.of(2026, 9, 10, 0, 0, 0);
    LocalDateTime endDate = LocalDateTime.of(2026, 9, 11, 0, 0, 0);
    LocalDateTime discountedDate = LocalDateTime.of(2026, 8, 16, 0, 0, 0);

    
    List<Transaction> transactions =new ArrayList<>();
    if(null==transacId) {
    	transactions =
    	            transactionRepository.findByLstUpdtTsGreaterThanEqualAndLstUpdtTsLessThan(
    	                    startDate, endDate);
    	  LOGGER.info(
      			"Fetched All Transaction Id after {} and before :{}",
      			startDate,endDate);
    }
    else {
    	transactions=transactionRepository.findByAprmntIdAndTrnscId("APRT001",transacId);
    }
    
    LOGGER.info(
			"Total Transactions: {}",
			transactions.size());
    
    
    for (Transaction transaction : transactions) {

    	if(transaction.getCreatTs().isBefore(discountedDate)) {
    		  LOGGER.info(
    	    			"transaction created before Discounted Date transaction Id: {} , Flat:{} Created Date:{}",
    	    			transaction.getTrnscId(),transaction.getFlatId(),transaction.getCreatTs());
    		  
    	}
        String transactionId = transaction.getTrnscId();
        LOGGER.info(
    			"Executing for  Transaction Id: {} , Flat:{}",
    			transaction.getTrnscId(),transaction.getFlatId());
        // Already exists - skip
        if (transDueDetailsRepository.existsByTransactionId(transactionId)) {
        	  LOGGER.info(
          			"Trans Due Details Present for Transaction Id: {} , Flat:{}",
          			transaction.getTrnscId(),transaction.getFlatId());
            continue;
        }

        /*
         * Example:
         * DUE64B0D4A0_YEARLY_1315_2026-08-01
         */
        String dueDetails = transaction.getDueDetails();

        if (dueDetails == null || dueDetails.isBlank()) {
            continue;
        }

        String[] details = dueDetails.split("_");

        if (details.length != 4) {
            // Invalid dueDetails format
            continue;
        }

        String dueId = details[0];
        String collectionCycle = details[1];
        String flatArea = details[2];
        LocalDate dueDate;

        try {
            dueDate = LocalDate.parse(details[3]);
        } catch (DateTimeParseException e) {
            // Invalid date
            continue;
        }

        String aprmntId = transaction.getAprmntId();

        /*
         * Build DueAmountDetailsEntity PK
         */
        DueAmountDetailsEntityId dueAmountDetailsId =
                new DueAmountDetailsEntityId();

        dueAmountDetailsId.setAprmntId(aprmntId);
        dueAmountDetailsId.setDueId(dueId);
        dueAmountDetailsId.setCollectionCycle(collectionCycle);
        dueAmountDetailsId.setFlatArea(flatArea);
        dueAmountDetailsId.setDueDate(dueDate);

        /*
         * Fetch DueAmountDetailsEntity
         */
        Optional<DueAmountDetailsEntity> dueAmountDetailsOptional =
                dueAmountDetailsRepository.findById(dueAmountDetailsId);

        if (dueAmountDetailsOptional.isEmpty()) {
            // No matching due details found
            continue;
        }

        DueAmountDetailsEntity dueAmountDetails =
                dueAmountDetailsOptional.get();

        if (new BigDecimal(dueAmountDetails.getTotalAmount())
                .compareTo(new BigDecimal(transaction.getTrnsAmt())) != 0) {
        	 LOGGER.info(
         			"Transaction Amount Doesn't Match for  Transaction Id: {} , Flat:{} Status: {},Trans Amount: {}, Due Amount:{} ",
         			transaction.getTrnscId(),transaction.getFlatId(),transaction.getTrnsStatus(), transaction.getTrnsAmt(), dueAmountDetails.getTotalAmount());
        	 ActionTransactionReviewWorkListRequest actionTransactionReviewWorkListRequest= new ActionTransactionReviewWorkListRequest();
        	 actionTransactionReviewWorkListRequest.setAction(SecuraConstants.ACTION_REJECT);
        	 GenericHeader genericHeader = new GenericHeader();
        	 genericHeader.setUserId("dnadminusr");
        	 genericHeader.setApartmentId("APRT001");
        	 genericHeader.setRole("MEMBER");
        	 genericHeader.setFlatNo("2054");
        	 genericHeader.setPosition("MEMBER");
        	 genericHeader.setProfileName("DN Fairytale Admin User");
        	 genericHeader.setApartmentName("DN Fairytale");
        	 genericHeader.setProfilepic("");
        	 actionTransactionReviewWorkListRequest.setGenericHeader(genericHeader);
        	 actionTransactionReviewWorkListRequest.setWorklistId(transaction.getWorkListId());
        	 if(!transaction.getTrnsStatus().equalsIgnoreCase(SecuraConstants.TRANSACTION_STATUS_SUCCESS)) {
        	 worklistService.actionTransactionReviewWorkList(actionTransactionReviewWorkListRequest);
        	 LOGGER.info(
          			"Worklist rejected for Transaction Id: {} , Flat:{} Status: {},Trans Amount: {}, Due Amount:{} ",
          			transaction.getTrnscId(),transaction.getFlatId(),transaction.getTrnsStatus(), transaction.getTrnsAmt(), dueAmountDetails.getTotalAmount());
        	 }
        	 else {
        		 LOGGER.info(
               			"trasnsaction Is Success Transaction Id: {} , Flat:{} Status: {},Trans Amount: {}, Due Amount:{} ",
               			transaction.getTrnscId(),transaction.getFlatId(),transaction.getTrnsStatus(), transaction.getTrnsAmt(), dueAmountDetails.getTotalAmount());
        	 }
        	 continue;
        }
        /*
         * Create TransDueDetailsEntity
         */
        TransDueDetailsEntity transDueDetails =
                new TransDueDetailsEntity();

        /*
         * PK
         */
        transDueDetails.setTransactionId(transactionId);
        transDueDetails.setAprmntId(aprmntId);
        transDueDetails.setDueId(dueDetails);

        /*
         * Copy fields from DueAmountDetailsEntity
         */
        transDueDetails.setCollectionCycle(
                dueAmountDetails.getCollectionCycle());

        transDueDetails.setFlatArea(
                dueAmountDetails.getFlatArea());

        transDueDetails.setDueDate(
                dueAmountDetails.getDueDate());

        transDueDetails.setPaymentId(
                dueAmountDetails.getPaymentId());

        transDueDetails.setAmount(
                dueAmountDetails.getAmount());

        transDueDetails.setGstAmount(
                dueAmountDetails.getGstAmount());

        transDueDetails.setTotalAmount(
                dueAmountDetails.getTotalAmount());

        transDueDetails.setPaymentName(
                dueAmountDetails.getPaymentName());

        transDueDetails.setPaymentType(
                dueAmountDetails.getPaymentType());

        transDueDetails.setCause(
                dueAmountDetails.getCause());

        transDueDetails.setPaymentCapita(
                dueAmountDetails.getPaymentCapita());

        transDueDetails.setAddedCharges(
                dueAmountDetails.getAddedCharges());

        transDueDetails.setAmountPerMonth(
                dueAmountDetails.getAmountPerMonth());

        transDueDetails.setTotalAddedCharges(
                dueAmountDetails.getTotalAddedCharges());

        transDueDetails.setEstimatedCollectionAmount(
                dueAmountDetails.getEstimatedCollectionAmount());

        transDueDetails.setGstPercentage(
                dueAmountDetails.getGstPercentage());

        transDueDetails.setDiscountCode(
                dueAmountDetails.getDiscountCode());

        transDueDetails.setDiscountMode(
                dueAmountDetails.getDiscountMode());

        transDueDetails.setCummilationCycle(
                dueAmountDetails.getCummilationCycle());

        transDueDetails.setFineCode(
                dueAmountDetails.getFineCode());

        transDueDetails.setDiscValue(
                dueAmountDetails.getDiscValue());

        transDueDetails.setFnValue(
                dueAmountDetails.getFnValue());

        transDueDetails.setDiscountedAmount(
                dueAmountDetails.getDiscountedAmount());

        transDueDetails.setFineAmount(
                dueAmountDetails.getFineAmount());

        transDueDetails.setFineMode(
                dueAmountDetails.getFineMode());

        transDueDetails.setFineType(
                dueAmountDetails.getFineType());

        transDueDetails.setRoundUpAmount(
                dueAmountDetails.getRoundUpAmount());

        transDueDetails.setAlreadyPaidAmount(
                dueAmountDetails.getAlreadyPaidAmount());

        transDueDetails.setAdminDiscount(
                dueAmountDetails.getAdminDiscount());

        transDueDetails.setApplicableFlats(
                dueAmountDetails.getApplicableFlats());

        transDueDetails.setPaidFlats(
                dueAmountDetails.getPaidFlats());

        transDueDetails.setAllowedTenders(
                dueAmountDetails.getAllowedTenders());

        transDueDetails.setPaymentStatus(
                dueAmountDetails.getPaymentStatus());

        transDueDetails.setDueEndDate(
                dueAmountDetails.getDueEndDate());

        transDueDetails.setDueStartDate(
                dueAmountDetails.getDueStartDate());

        transDueDetails.setPaymentDate(
                dueAmountDetails.getPaymentDate());

        transDueDetails.setCreatTs(
                dueAmountDetails.getCreatTs());

        transDueDetails.setCreatUsrId(
               "ext");

        transDueDetails.setLstUpdtTs(
                dueAmountDetails.getLstUpdtTs());

        transDueDetails.setLstUpdtUsrId(
                dueAmountDetails.getLstUpdtUsrId());

        /*
         * Save
         */
        transDueDetailsRepository.save(transDueDetails);
        LOGGER.info(
    			"Completed for  Transaction Id: {} , Flat:{}",
    			transaction.getTrnscId(),transaction.getFlatId());
    }}
}