package com.secura.dnft.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.interfaceservice.ThirdPartyPaymentGayeway;
import com.secura.dnft.request.response.CreatePaymentRequest;
import com.secura.dnft.request.response.CreatePaymentResponse;
import com.secura.dnft.request.response.GenericResponse;
import com.secura.dnft.request.response.GetDuePaymentAmountDetailsResponse;
import com.secura.dnft.request.response.GetPaymentRequest;
import com.secura.dnft.request.response.GetPaymentResponse;
import com.secura.dnft.request.response.GetPaymentUtilDetailsRequest;
import com.secura.dnft.request.response.GetPaymentUtilDetailsResponse;
import com.secura.dnft.request.response.LedgerEntryRequest;
import com.secura.dnft.request.response.LedgerEntryResponse;
import com.secura.dnft.request.response.PayDueRequest;
import com.secura.dnft.request.response.PayDueResponse;
import com.secura.dnft.request.response.PaymentGayewayOrderRequest;
import com.secura.dnft.request.response.PaymentGayewayOrderResponse;
import com.secura.dnft.request.response.PaymentGayewayOrderVerificationRequest;
import com.secura.dnft.request.response.PaymentGayewayOrderVerificationResponse;
import com.secura.dnft.request.response.PaymentGayewayPayOrderRequest;
import com.secura.dnft.request.response.PaymentGayewayPayOrderResponse;
import com.secura.dnft.request.response.PaymentGayewayPaymentDetailRequest;
import com.secura.dnft.request.response.PaymentGayewayPaymentDetailResponse;
import com.secura.dnft.request.response.PaymentGayewayProcessRefundRequest;
import com.secura.dnft.request.response.PaymentGayewayProcessRefundResponse;
import com.secura.dnft.request.response.ReconcileQRPaymentRequest;
import com.secura.dnft.request.response.ReconcileQRPaymentResponse;
import com.secura.dnft.request.response.UploadPastDueRequest;
import com.secura.dnft.request.response.UploadPastDueResponse;
import com.secura.dnft.request.response.ValidatePriorDuePaymnentRequest;
import com.secura.dnft.service.AtomsPaymentServices;
import com.secura.dnft.service.DeepLinkServices;
import com.secura.dnft.service.PaymentServices;
import com.secura.dnft.service.PaymentUtilService;
import com.secura.dnft.service.RazorPayPaymentServices;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/payment")
public class PaymentController {


	@Autowired
	RazorPayPaymentServices razorPayPaymentServices;
	
	@Autowired
	AtomsPaymentServices atomsPaymentServices;

	@Autowired
	DeepLinkServices deepLinkServices;

	@Autowired
	PaymentServices paymentServices;

	@Autowired
	PaymentUtilService paymentUtilService;
	
	 @PostMapping("/payGatewayCreateOrder")
	    @CrossOrigin(origins = "*")
	    public PaymentGayewayOrderResponse createOrder(@RequestBody PaymentGayewayOrderRequest request) {
		 try {
			 return resolvePaymentGatewayService(request != null ? request.getPaymentGateway() : null).createOrder(request);
		 } catch (Exception e) {
			 PaymentGayewayOrderResponse response = new PaymentGayewayOrderResponse();
			 response.setGenericHeader(request != null ? request.getGenericHeader() : null);
			 response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			 response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
			 return response;
		 }
	            }
	 

	 @PostMapping("/verifyPayment")
	    @CrossOrigin(origins = "*")
	    public PaymentGayewayOrderVerificationResponse verifyPayment(@RequestBody PaymentGayewayOrderVerificationRequest request) {
		 try {
			 return resolvePaymentGatewayService(request != null ? request.getPaymentGateway() : null).verifypayment(request);
		 } catch (Exception e) {
			 PaymentGayewayOrderVerificationResponse response = new PaymentGayewayOrderVerificationResponse();
			 response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			 response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
			 return response;
		 }
	            }
	 
	 @PostMapping("/getPaymentDetails")
	    @CrossOrigin(origins = "*")
	    public PaymentGayewayPaymentDetailResponse getPaymentDetails(@RequestBody PaymentGayewayPaymentDetailRequest request) {
		 try {
			 return resolvePaymentGatewayService(request != null ? request.getPaymentGateway() : null).getPaymentDetails(request);
		 } catch (Exception e) {
			 return new PaymentGayewayPaymentDetailResponse();
		 }
	            }
	 
	 @PostMapping("/payOrder")
	 @CrossOrigin(origins = "*")
	 public PaymentGayewayPayOrderResponse payOrder(@RequestBody PaymentGayewayPayOrderRequest request) {
		 try {
			 return resolvePaymentGatewayService(request != null ? request.getPaymentGateway() : null).payorder(request);
		 } catch (Exception e) {
			 PaymentGayewayPayOrderResponse response = new PaymentGayewayPayOrderResponse();
			 response.setGenericHeader(request != null ? request.getGenericHeader() : null);
			 response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			 response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
			 return response;
		 }
	 }
	 
	 @PostMapping("/processRefund")
	 @CrossOrigin(origins = "*")
	 public PaymentGayewayProcessRefundResponse processRefund(@RequestBody PaymentGayewayProcessRefundRequest request) {
		 try {
			 return resolvePaymentGatewayService(request != null ? request.getPaymentGateway() : null).processRefund(request);
		 } catch (Exception e) {
			 PaymentGayewayProcessRefundResponse response = new PaymentGayewayProcessRefundResponse();
			 response.setGenericHeader(request != null ? request.getGenericHeader() : null);
			 response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			 response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
			 return response;
		 }
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

	 @PostMapping("/getPaymentUtilDetails")
	 @CrossOrigin(origins = "*")
	 public GetPaymentUtilDetailsResponse getPaymentUtilDetails(@RequestBody GetPaymentUtilDetailsRequest request) {
		 GetPaymentUtilDetailsResponse response = new GetPaymentUtilDetailsResponse();
		 response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		 try {
			 return paymentUtilService.getPaymentDetails(request);
		 } catch (Exception e) {
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

	@PostMapping("/validatePriorDuePaymnent")
	@CrossOrigin(origins = "*")
	public GenericResponse validatePriorDuePaymnent(@RequestBody ValidatePriorDuePaymnentRequest request) {
		GenericResponse response = new GenericResponse();
		try {
			return paymentServices.validatePriorDuePaymnent(request);
		}
		catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}

	@PostMapping("/reconcileQRPayment")
	@CrossOrigin(origins = "*")
	public ReconcileQRPaymentResponse reconcileQRPayment(@RequestBody ReconcileQRPaymentRequest request) {
		ReconcileQRPaymentResponse response = new ReconcileQRPaymentResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		try {
			return paymentServices.reconcileQRPayment(request);
		} catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}
	
	private ThirdPartyPaymentGayeway resolvePaymentGatewayService(String paymentGateway) {
		if ("RAZORPAY".equalsIgnoreCase(paymentGateway)) {
			return razorPayPaymentServices;
		}
		if ("ATOMS".equalsIgnoreCase(paymentGateway)) {
			return atomsPaymentServices;
		}
		if ("DEEPLINK".equalsIgnoreCase(paymentGateway)) {
			return deepLinkServices;
		}
		throw new IllegalArgumentException("Unsupported payment gateway");
	}
}
