package com.secura.dnft.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.request.response.RazorPayPaymentDetailsResponse;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.request.response.DuePaymentAmountDetailsRequest;
import com.secura.dnft.request.response.DuePaymentAmountDetailsResponse;
import com.secura.dnft.request.response.RazorPayPaymentRequest;
import com.secura.dnft.request.response.RazorPayPaymentResponse;
import com.secura.dnft.request.response.RazorPayPaymentVerificationResponse;
import com.secura.dnft.service.PaymentServices;
import com.secura.dnft.service.RazorPayPaymentServices;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/payment")
public class PaymentController {


	@Autowired
	RazorPayPaymentServices razorPayPaymentServices;

	@Autowired
	PaymentServices paymentServices;
	
	 @PostMapping("/razorPayCreateOrder")
	    @CrossOrigin(origins = "*")
	    public RazorPayPaymentResponse createOrder(@RequestBody RazorPayPaymentRequest request) {
		 RazorPayPaymentResponse reazorPayOrder = new RazorPayPaymentResponse();
		 reazorPayOrder=razorPayPaymentServices.createOrder(request);
	    	return reazorPayOrder;
	            }
	 

	 @PostMapping("/verifyPayment")
	    @CrossOrigin(origins = "*")
	    public RazorPayPaymentVerificationResponse verifyPayment(@RequestBody Map<String, String> request) {
		 RazorPayPaymentVerificationResponse reazorPayOrder = new RazorPayPaymentVerificationResponse();
		 reazorPayOrder=razorPayPaymentServices.paymentVerification(request);
	    	return reazorPayOrder;
	            }
	 
	 @PostMapping("/getPaymentDetails")
	    @CrossOrigin(origins = "*")
	    public RazorPayPaymentDetailsResponse getPaymentDetails(@RequestBody String paymentId) {
		 RazorPayPaymentDetailsResponse reazorPaymentDetails = new RazorPayPaymentDetailsResponse();
		 reazorPaymentDetails=razorPayPaymentServices.getRazorPayPaymentDetails(paymentId);
	    	return reazorPaymentDetails;
	            }

	 @PostMapping("/getDuePaymentAmountDetails")
	    @CrossOrigin(origins = "*")
	    public DuePaymentAmountDetailsResponse getDuePaymentAmountDetails(@RequestBody DuePaymentAmountDetailsRequest request) {
		 DuePaymentAmountDetailsResponse response = new DuePaymentAmountDetailsResponse();
		 try {
			 return paymentServices.getDuePaymentAmountDetails(request);
			}
			catch (Exception e) {
				response.setMessage(ErrorMessage.ERR_MESSAGE_33);
				response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
			}
			return response;
			
		
	            }
}
