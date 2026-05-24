package com.secura.dnft.service;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
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
		RazorpayClient client = new RazorpayClient(key, secret);

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
				paymentResponse.setData(convertJsonToMap(order.toString()));
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
			response.setData(new HashMap<>());
			return response;
		}
		String orderId = data.get("razorpay_order_id");
		    String paymentId = data.get("razorpay_payment_id");
		    String signature = data.get("razorpay_signature");

			try {
				boolean isValid = Utils.verifySignature(
				    orderId + "|" + paymentId,
				    signature,
				    secret   
				);
				  if (isValid) {
				    	response.setMessage(SuccessMessage.SUCC_MESSAGE_07);
				    	response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_07);
				    } else {
				    	response.setMessage(ErrorMessage.ERR_MESSAGE_23);
				    	response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_23);
				    }
				Map<String, Object> responseData = new HashMap<>();
				responseData.put("razorpay_order_id", orderId);
				responseData.put("razorpay_payment_id", paymentId);
				responseData.put("razorpay_signature", signature);
				responseData.put("signature_valid", isValid);
				response.setData(responseData);
			} catch (RazorpayException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				response.setMessage(ErrorMessage.ERR_MESSAGE_23);
		    	response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_23);
			}

		  
         return response;
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

	public Map<String, Object> convertJsonToMap(String json) {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse JSON to map: " + json, e);
		}
	}

}
