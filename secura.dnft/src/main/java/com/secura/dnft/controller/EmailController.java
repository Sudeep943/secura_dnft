package com.secura.dnft.controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.dao.PaymentRepository;
import com.secura.dnft.dao.TransactionRepository;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.entity.Transaction;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.service.EmailService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/email")
public class EmailController {
	
	@Autowired
	EmailService emailService;
	
	@Autowired
    private PaymentRepository paymentRepository;
	
	@Autowired
    private TransactionRepository transactionRepository;
	
	private static final Logger logger = LoggerFactory.getLogger(EmailController.class);

	@GetMapping("/sendPaymentEmail")
	@CrossOrigin(origins = "*")
	public String sendPaymentEmailPost(@RequestParam String paymentId) {
		logger.info("Email Controller sendPaymentEmailPost Received Request For PaymentID: {}",paymentId);
		try {

			if (null != paymentId) {
				CompletableFuture.runAsync(() -> {
	                try {
	                    sendPaymentEmail("APRT001", paymentId);
	                } catch (Exception e) {
	                    logger.error("Error sending payment email for paymentId: {}", paymentId, e);
	                }
	            });
			}
			else {
				 CompletableFuture.runAsync(() -> {
		                try {
		                    emailService.sendPaymentEmail();
		                } catch (Exception e) {
		                    logger.error("Error sending payment emails", e);
		                }
		            });
			}
			return "Process Started";

		} catch (Exception e) {

			return e.getMessage();
		}
	}
	
	@GetMapping("/sendTransactionEmail")
	@CrossOrigin(origins = "*")
	public String sendTransactionEmailPost(@RequestParam  String transctionId) {
		logger.info("Email Controller sendTransactionEmailPost Received Request For transctionId: {}",transctionId);

		try {

			if (null != transctionId) {
				sendTransactionEmail("APRT001",transctionId);
			}
			else {
				emailService.sendTransactionEmail();
			}
			return "Mail Sent for transId"+ transctionId;

		} catch (Exception e) {

			return e.getMessage();
		}
	}

	public void sendPaymentEmail(String apartmentId,String paymentId) {
		logger.info("Payment EmailService.sendEmail() started");
		try {
			List<PaymentEntity> filteredPayments = paymentRepository.findByPaymentIdAndAprmtId(paymentId,apartmentId);
			filteredPayments=emailService.filterPaymentList(filteredPayments);
			if (null != filteredPayments && !filteredPayments.isEmpty()) {
				emailService.sendPaymentMails(filteredPayments);
			}
		} catch (Exception e) {
			logger.error("Payment EmailService.sendEmail() encountered an error", e);
		}
		logger.info("Payment EmailService.sendEmail() completed");
	}
	
	   public void sendTransactionEmail(String apartmentId,String transctionId) {
	        logger.info("Transaction EmailService.sendEmail() started");
	        try {
	            List<Transaction>   allPendingTransactions = transactionRepository.findByAprmntIdAndTrnscId(apartmentId,transctionId);
	            //allPendingTransactions=allPendingTransactions.stream().filter(trn->trn.getAprmntId().equals(apartmentId) && trn.getTrnscId().equals(transctionId)).collect(Collectors.toList());
	            List<Transaction>   filteredTransactions = emailService.filterTransactionList(allPendingTransactions);
	            filteredTransactions=filteredTransactions.stream().filter(trn->trn.getTrnsStatus().equals(SecuraConstants.TRANSACTION_STATUS_SUCCESS)).collect(Collectors.toList());
	            if(null!=filteredTransactions && !filteredTransactions.isEmpty()) {
	            	emailService.sendTransactionMails(filteredTransactions);
	}
	        } catch (Exception e) {
	            logger.error("Transaction EmailService.sendEmail() encountered an error", e);
	        }
	        logger.info("Transaction EmailService.sendEmail() completed");
	    }
}
