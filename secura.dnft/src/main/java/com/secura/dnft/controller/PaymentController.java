package com.secura.dnft.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.request.response.PaymentGayewayPaymentDetailsResponse;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.request.response.CreatePaymentRequest;
import com.secura.dnft.request.response.CreatePaymentResponse;
import com.secura.dnft.request.response.GetDuePaymentAmountDetailsResponse;
import com.secura.dnft.request.response.GetPaymentRequest;
import com.secura.dnft.request.response.GetPaymentResponse;
import com.secura.dnft.request.response.LedgerEntryRequest;
import com.secura.dnft.request.response.LedgerEntryResponse;
import com.secura.dnft.request.response.PayDueRequest;
import com.secura.dnft.request.response.PayDueResponse;
import com.secura.dnft.request.response.PaymentGayewayOrderRequest;
import com.secura.dnft.request.response.PaymentGayewayOrderResponse;
import com.secura.dnft.request.response.PaymentGayewayOrderVerificationRequest;
import com.secura.dnft.request.response.PaymentGayewayOrderVerificationResponse;
import com.secura.dnft.request.response.PaymentGayewayPaymentDetailRequest;
import com.secura.dnft.request.response.PaymentGayewayPaymentDetailResponse;
import com.secura.dnft.request.response.UploadPastDueRequest;
import com.secura.dnft.request.response.UploadPastDueResponse;
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
	    public PaymentGayewayOrderResponse createOrder(@RequestBody PaymentGayewayOrderRequest request) {
		 PaymentGayewayOrderResponse reazorPayOrder = new PaymentGayewayOrderResponse();
		 reazorPayOrder=razorPayPaymentServices.createOrder(request);
	    	return reazorPayOrder;
	            }
	 

	 @PostMapping("/verifyPayment")
	    @CrossOrigin(origins = "*")
	    public PaymentGayewayOrderVerificationResponse verifyPayment(@RequestBody PaymentGayewayOrderVerificationRequest request) {
		 PaymentGayewayOrderVerificationResponse reazorPayOrder = new PaymentGayewayOrderVerificationResponse();
		 reazorPayOrder=razorPayPaymentServices.verifypayment(request);
	    	return reazorPayOrder;
	            }
	 
	 @PostMapping("/getPaymentDetails")
	    @CrossOrigin(origins = "*")
	    public PaymentGayewayPaymentDetailResponse getPaymentDetails(@RequestBody PaymentGayewayPaymentDetailRequest request) {
		 PaymentGayewayPaymentDetailResponse reazorPaymentDetails = new PaymentGayewayPaymentDetailResponse();
		 reazorPaymentDetails=razorPayPaymentServices.getPaymentDetails(request);
	    	return reazorPaymentDetails;
	            }

	 @PostMapping("/getDuePaymentAmountDetails")
	    @CrossOrigin(origins = "*")
	    public GetDuePaymentAmountDetailsResponse getDuePaymentAmountDetails(@RequestBody CreatePaymentRequest request) {
		 GetDuePaymentAmountDetailsResponse response = new GetDuePaymentAmountDetailsResponse();
		 try {
			 return paymentServices.getDuePaymentAmountDetails(request);
			}
			catch (Exception e) {
				response.setMessage(ErrorMessage.ERR_MESSAGE_33);
				response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
			}
			return response;
			
		
 	            }
 	 
 	 @PostMapping("/createPayment")
	    @CrossOrigin(origins = "*")
	    public CreatePaymentResponse createPayment(@RequestBody CreatePaymentRequest request) {
		 CreatePaymentResponse response = new CreatePaymentResponse();
		 try {
			 return paymentServices.createPayment(request);
			}
			catch (Exception e) {
				e.printStackTrace();
				response.setMessage(ErrorMessage.ERR_MESSAGE_33);
				response.setMessage_code(ErrorMessageCode.ERR_MESSAGE_33);
			}
			return response;
			
		
  	            }

	 @PostMapping("/getPayment")
	    @CrossOrigin(origins = "*")
	    public GetPaymentResponse getPayment(@RequestBody GetPaymentRequest request) {
		 GetPaymentResponse response = new GetPaymentResponse();
		 response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		 try {
			 return paymentServices.getPayments(request);
			}
			catch (Exception e) {
				e.printStackTrace();
				response.setMessage(ErrorMessage.ERR_MESSAGE_33);
				response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
			}
		 return response;
	            }

	 @PostMapping("/payDues")
	    @CrossOrigin(origins = "*")
	    public PayDueResponse payDues(@RequestBody PayDueRequest request) {
		 PayDueResponse response = new PayDueResponse();
		 response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		 try {
			 return paymentServices.payDues(request);
			}
			catch (Exception e) {
				e.printStackTrace();
				response.setMessage(ErrorMessage.ERR_MESSAGE_33);
				response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
			}
 			return response;
 	            }
 	 
 	 @PostMapping("/ledgerEntry")
	    @CrossOrigin(origins = "*")
	    public LedgerEntryResponse ledgerEntry(@RequestBody LedgerEntryRequest request) {
 		 LedgerEntryResponse response = new LedgerEntryResponse();
 		 response.setGenericHeader(request != null ? request.getGenericHeader() : null);
 		 try {
 			 return paymentServices.ledgerEntry(request);
 			}
 			catch (Exception e) {
 				e.printStackTrace();
 				response.setMessage(ErrorMessage.ERR_MESSAGE_33);
 				response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
 			}
 			return response;
	            }
 	 
	@PostMapping("/uploadPastDue")
	@CrossOrigin(origins = "*")
	public UploadPastDueResponse uploadPastDue(@RequestBody UploadPastDueRequest request) {
		 UploadPastDueResponse response = new UploadPastDueResponse();
		 response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		 try {
			 return paymentServices.uploadPastDue(request);
		 }
		 catch (Exception e) {
			 response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			 response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		 }
		 return response;
	}
}
