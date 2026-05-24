package com.secura.dnft.service;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.secura.dnft.dao.BankEntityRepository;
import com.secura.dnft.entity.BankEntity;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.interfaceservice.ThirdPartyPaymentGayeway;
import com.secura.dnft.request.response.PaymentGayewayOrderVerificationRequest;
import com.secura.dnft.request.response.RazorPayOrderResponse;
import com.secura.dnft.request.response.PaymentGayewayOrderRequest;
import com.secura.dnft.request.response.PaymentGayewayOrderResponse;
import com.secura.dnft.request.response.PaymentGayewayOrderVerificationResponse;
import com.secura.dnft.request.response.PaymentGayewayPayOrderRequest;
import com.secura.dnft.request.response.PaymentGayewayPayOrderResponse;
import com.secura.dnft.request.response.PaymentGayewayPaymentDetailRequest;
import com.secura.dnft.request.response.PaymentGayewayPaymentDetailResponse;
import com.secura.dnft.request.response.PaymentGayewayPaymentDetailsResponse;
import com.secura.dnft.request.response.PaymentGayewayProcessRefundRequest;
import com.secura.dnft.request.response.PaymentGayewayProcessRefundResponse;

@Service
public class RazorPayPaymentServices implements ThirdPartyPaymentGayeway {
	
	@Autowired
	private BankEntityRepository bankRepository;

	@Autowired
	private GenericService genericService;
	
	
	//@Value("${razorPay.key}")
	//String key;
	
	//@Value("${razorPay.secret}")
	//String secret;
	private final String key="rzp_test_SRxceBfBqGmeGy";
	private final String secret="RO2SpqK1H96ShjlmkttpA2cX";
	
	
	
	@Override
	public PaymentGayewayOrderResponse createOrder(PaymentGayewayOrderRequest paymentRequest) {
		PaymentGayewayOrderResponse paymentResponse = new PaymentGayewayOrderResponse();
		paymentResponse.setGenericHeader(paymentRequest.getGenericHeader());
		try {
		String apartmentId = paymentRequest != null && paymentRequest.getGenericHeader() != null
				? paymentRequest.getGenericHeader().getApartmentId()
				: null;
		Object bankIdObj = paymentRequest != null && paymentRequest.getData() != null
				? paymentRequest.getData().get("bankId")
				: null;
		RazorpayCredentials razorpayCredentials = resolveRazorpayCredentials(apartmentId,
				bankIdObj != null ? bankIdObj.toString() : null);
		RazorpayClient client = new RazorpayClient(razorpayCredentials.key(), razorpayCredentials.secret());

		String receipt = createPaymentReceiptIdentifier(paymentRequest.getTransactionType(),paymentRequest.getEventDate(),paymentRequest.getGenericHeader().getFlatNo());
		JSONObject options = new JSONObject();
		options.put("amount", paymentRequest.getAmountInPaisa()); 
		options.put("currency", paymentRequest.getCurrency());
		options.put("receipt", receipt);

		Order order;
		
			order = client.orders.create(options);
			
			String orderStatus=order.get("status");
			
			if(orderStatus.equalsIgnoreCase("created")) {
				paymentResponse.setMessage(SuccessMessage.SUCC_MESSAGE_06);
				paymentResponse.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_06);
				paymentResponse.setOrder(convertJsonToObject(order.toString()));
			}
		   
		} catch (RazorpayException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			paymentResponse.setMessage(ErrorMessage.ERR_MESSAGE_22);
			paymentResponse.setMessageCode(ErrorMessageCode.ERR_MESSAGE_22);
		}
		catch (Exception e) {
			e.printStackTrace();
			paymentResponse.setMessage(ErrorMessage.ERR_MESSAGE_22);
			paymentResponse.setMessageCode(ErrorMessageCode.ERR_MESSAGE_22);
		}

		
		return paymentResponse;
	}
	
	@Override
	public PaymentGayewayOrderVerificationResponse verifypayment(PaymentGayewayOrderVerificationRequest request) {
		PaymentGayewayOrderVerificationResponse response= new PaymentGayewayOrderVerificationResponse();   
		Map<String, String> data = request != null ? request.getData() : null;
		if (data == null || data.isEmpty()) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_23);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_23);
			return response;
		}
		String orderId = data.get("razorpay_order_id");
		    String paymentId = data.get("razorpay_payment_id");
		    String signature = data.get("razorpay_signature");
		    String bankId = data.get("bankId");
		    String apartmentId = request != null && request.getGenericHeader() != null
		    		? request.getGenericHeader().getApartmentId()
		    		: null;
		    RazorpayCredentials razorpayCredentials = resolveRazorpayCredentials(apartmentId, bankId);

			try {
				boolean isValid = Utils.verifySignature(
				    orderId + "|" + paymentId,
				    signature,
				    razorpayCredentials.secret()   
				);
				  if (isValid) {
				    	response.setMessage(SuccessMessage.SUCC_MESSAGE_07);
				    	response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_07);
				    } else {
				    	response.setMessage(ErrorMessage.ERR_MESSAGE_23);
				    	response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_23);
				    }
			} catch (RazorpayException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				response.setMessage(ErrorMessage.ERR_MESSAGE_23);
		    	response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_23);
			}

		  
         return response;
	}

	RazorpayCredentials resolveRazorpayCredentials(String apartmentId, String bankId) {
		if (apartmentId == null || apartmentId.isBlank() || bankId == null || bankId.isBlank()) {
			return new RazorpayCredentials(key, secret);
		}
		Optional<BankEntity> bankEntityOptional = bankRepository.findByAprmntIdAndBankDetailsID(apartmentId, bankId);
		if (bankEntityOptional.isEmpty()) {
			return new RazorpayCredentials(key, secret);
		}
		BankEntity bankEntity = bankEntityOptional.get();
		String paymentGatewayName = decryptNullable(bankEntity.getPgName());
		if (!"RAZORPAY".equalsIgnoreCase(paymentGatewayName)) {
			return new RazorpayCredentials(key, secret);
		}
		String dynamicKey = decryptNullable(bankEntity.getPgKey());
		String dynamicSecret = decryptNullable(bankEntity.getPgSecret());
		if (dynamicKey == null || dynamicKey.isBlank() || dynamicSecret == null || dynamicSecret.isBlank()) {
			return new RazorpayCredentials(key, secret);
		}
		return new RazorpayCredentials(dynamicKey, dynamicSecret);
	}

	private String decryptNullable(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return genericService.decrypt(value);
	}

	static final class RazorpayCredentials {
		private final String key;
		private final String secret;

		private RazorpayCredentials(String key, String secret) {
			this.key = key;
			this.secret = secret;
		}

		String key() {
			return key;
		}

		String secret() {
			return secret;
		}
	}

	@Override
	public PaymentGayewayPayOrderResponse payorder(PaymentGayewayPayOrderRequest request) {
		PaymentGayewayPayOrderResponse response = new PaymentGayewayPayOrderResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		return response;
	}
	
	public String  createPaymentReceiptIdentifier(String receiptType, Date eventDate, String flatNo) {
		StringBuffer receipt= new StringBuffer();
		
		if(receiptType.equals(SecuraConstants.RECIPT_TYPE_BOOKING)) {
			receipt.append("BKNG_");
			receipt.append(flatNo+"_");
			SimpleDateFormat sdf =new SimpleDateFormat("ddMMMyyyy");
			String evntDate=sdf.format(eventDate);
			receipt.append(evntDate+"_");
			Random random = new Random();
			receipt.append( 1000 + random.nextInt(9000));
		}
		return receipt.toString();
	}
	
	@Override
	public PaymentGayewayPaymentDetailResponse getPaymentDetails(PaymentGayewayPaymentDetailRequest request) {
		PaymentGayewayPaymentDetailResponse razorPayPaymentDetailsResponse = new PaymentGayewayPaymentDetailResponse(); 
		try {
	            RazorpayClient razorpay = new RazorpayClient(key, secret);

	            Payment payment = razorpay.payments.fetch(request != null ? request.getPaymentId() : null);
	            razorPayPaymentDetailsResponse=convertPaymentDetailsJsonToObject(payment.toString());
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
		
		return razorPayPaymentDetailsResponse;
	    
	}

	@Override
	public PaymentGayewayProcessRefundResponse processRefund(PaymentGayewayProcessRefundRequest request) {
		PaymentGayewayProcessRefundResponse response = new PaymentGayewayProcessRefundResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		return response;
	}
	
	   public PaymentGayewayPaymentDetailResponse convertPaymentDetailsJsonToObject(String json) {
	    	ObjectMapper objectMapper = new ObjectMapper();
	        try {
	            return objectMapper.readValue(json, PaymentGayewayPaymentDetailResponse.class);
	        } catch (Exception e) {
	            throw new RuntimeException("Failed to parse JSON", e);
	        }
	    }

		 
   public RazorPayOrderResponse convertJsonToObject(String json) {
		    	ObjectMapper objectMapper = new ObjectMapper();
		        try {
		            return objectMapper.readValue(json, RazorPayOrderResponse.class);
		        } catch (Exception e) {
		            throw new RuntimeException("Failed to parse JSON", e);
		        }
		    }

}
