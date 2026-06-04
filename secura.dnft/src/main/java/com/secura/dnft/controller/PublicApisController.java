package com.secura.dnft.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.request.response.GenericResponse;
import com.secura.dnft.request.response.GetAllFlatsRequest;
import com.secura.dnft.request.response.GetAllFlatsResponse;
import com.secura.dnft.request.response.GetDueAmountForFlatRequest;
import com.secura.dnft.request.response.GetDueAmountForFlatResponse;
import com.secura.dnft.request.response.GetOwnerRequest;
import com.secura.dnft.request.response.GetOwnerResponse;
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
import com.secura.dnft.request.response.ValidatePriorDuePaymnentRequest;
import com.secura.dnft.interfaceservice.ThirdPartyPaymentGayeway;
import com.secura.dnft.service.AtomsPaymentServices;
import com.secura.dnft.service.DeepLinkServices;
import com.secura.dnft.service.FlatServices;
import com.secura.dnft.service.PaymentServices;
import com.secura.dnft.service.ProfileServices;
import com.secura.dnft.service.RazorPayPaymentServices;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/publicapis")
public class PublicApisController {

	@Autowired
	private FlatServices flatServices;

	@Autowired
	private PaymentServices paymentServices;

	@Autowired
	private RazorPayPaymentServices razorPayPaymentServices;
	
	@Autowired
	private AtomsPaymentServices atomsPaymentServices;

	@Autowired
	private DeepLinkServices deepLinkServices;

	@Autowired
	private ProfileServices profileServices;

	@PostMapping("/getFlatsPublic")
	@CrossOrigin(origins = "*")
	public GetAllFlatsResponse getFlatsPublic(@RequestBody GetAllFlatsRequest request) {
		return flatServices.getAllFlats(request);
	}

	@PostMapping("/detDueDetailsForFlatPublic")
	@CrossOrigin(origins = "*")
	public GetDueAmountForFlatResponse getDueDetailsForFlatPublic(@RequestBody GetDueAmountForFlatRequest request) {
		try {
			return flatServices.getDueAmountForFlat(request);
		} catch (Exception e) {
			GetDueAmountForFlatResponse response = new GetDueAmountForFlatResponse();
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
			return response;
		}
	}

	@PostMapping("/payduesPublic")
	@CrossOrigin(origins = "*")
	public PayDueResponse payDuesPublic(@RequestBody PayDueRequest request) {
		PayDueResponse response = new PayDueResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		try {
			return paymentServices.payDues(request);
		} catch (Exception e) {
			e.printStackTrace();
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}

	@PostMapping("/getOwnerPublic")
	@CrossOrigin(origins = "*")
	public GetOwnerResponse getOwnerPublic(@RequestBody GetOwnerRequest request) {
		GetOwnerResponse response = new GetOwnerResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		try {
			return profileServices.getOwner(request);
		} catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
			return response;
		}
	}

	@PostMapping("/razorPayCreateOrderPublic")
	@CrossOrigin(origins = "*")
	public PaymentGayewayOrderResponse createOrderPublic(@RequestBody PaymentGayewayOrderRequest request) {
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

	@PostMapping("/verifyPaymentPublic")
	@CrossOrigin(origins = "*")
	public PaymentGayewayOrderVerificationResponse verifyPaymentPublic(@RequestBody PaymentGayewayOrderVerificationRequest request) {
		try {
			return resolvePaymentGatewayService(request != null ? request.getPaymentGateway() : null).verifypayment(request);
		} catch (Exception e) {
			PaymentGayewayOrderVerificationResponse response = new PaymentGayewayOrderVerificationResponse();
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
			return response;
		}
	}
	
	@PostMapping("/getPaymentDetailsPublic")
	@CrossOrigin(origins = "*")
	public PaymentGayewayPaymentDetailResponse getPaymentDetailsPublic(@RequestBody PaymentGayewayPaymentDetailRequest request) {
		try {
			return resolvePaymentGatewayService(request != null ? request.getPaymentGateway() : null).getPaymentDetails(request);
		} catch (Exception e) {
			return new PaymentGayewayPaymentDetailResponse();
		}
	}
	
	@PostMapping("/payOrderPublic")
	@CrossOrigin(origins = "*")
	public PaymentGayewayPayOrderResponse payOrderPublic(@RequestBody PaymentGayewayPayOrderRequest request) {
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
	
	@PostMapping("/processRefundPublic")
	@CrossOrigin(origins = "*")
	public PaymentGayewayProcessRefundResponse processRefundPublic(@RequestBody PaymentGayewayProcessRefundRequest request) {
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

	@PostMapping("/payment/validatePriorDuePaymnent")
	@CrossOrigin(origins = "*")
	public GenericResponse validatePriorDuePaymnentPublic(@RequestBody ValidatePriorDuePaymnentRequest request) {
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
