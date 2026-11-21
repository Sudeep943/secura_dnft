package com.secura.dnft.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secura.dnft.generic.bean.AtomAuthRequest;
import com.secura.dnft.interfaceservice.ThirdPartyPaymentGayeway;
import com.secura.dnft.request.response.GenericHeader;
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

@Service
public class AtomsPaymentServices implements ThirdPartyPaymentGayeway {

    private static final ObjectMapper objectMapper = new ObjectMapper();

	//private ObjectMapper objectMapper= new ObjectMapper();


    
	@Autowired
	private RestClient.Builder restClientBuilder;

	    @Value("${atom.auth.url}")
	    private String authUrl;

	    @Value("${atom.merchant.id}")
	    private String merchantId;

	    @Value("${atom.user.id}")
	    private String userId;

	    @Value("${atom.password}")
	    private String password;

	    @Value("${atom.product.id}")
	    private String productId;

	    @Value("${atom.currency}")
	    private String currency;

	    @Value("${atom.aes.request.key}")
	    private String requestKey;

	    @Value("${atom.aes.response.key}")
	    private String responseKey;
	    
	@Override
	public PaymentGayewayOrderResponse createOrder(PaymentGayewayOrderRequest request) {
		PaymentGayewayOrderResponse response = new PaymentGayewayOrderResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		
		Map<String, Object> requestMap=request.getData();
		String amountinPaisa=(String)requestMap.get("amount");
        String customerEmail=(String)requestMap.get("customerEmail");
        String customerMobile=(String)requestMap.get("customerMobile");
        String apartmentId=(String)requestMap.get("apartmentId");
         String internalTransactionId =(String)requestMap.get("internalTransactionId");	 
         String securaTransactionId =(String)requestMap.get("securaTransactionId");	 
         String paymentId =(String)requestMap.get("paymentId");	 
         
         String amountInRupee=new BigDecimal(amountinPaisa).movePointLeft(2).toPlainString();
         
        try {
			Map<String, Object> responseMap= generatePaymentToken(amountInRupee, customerEmail, customerMobile, apartmentId, internalTransactionId,request,securaTransactionId,paymentId);
			response.setData(responseMap);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		return response;
	}

	@Override
	public PaymentGayewayOrderVerificationResponse verifypayment(PaymentGayewayOrderVerificationRequest request) {
		return new PaymentGayewayOrderVerificationResponse();
	}

	@Override
	public PaymentGayewayPayOrderResponse payorder(PaymentGayewayPayOrderRequest request) {
		PaymentGayewayPayOrderResponse response = new PaymentGayewayPayOrderResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		return response;
	}

	@Override
	public PaymentGayewayProcessRefundResponse processRefund(PaymentGayewayProcessRefundRequest request) {
		PaymentGayewayProcessRefundResponse response = new PaymentGayewayProcessRefundResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		return response;
	}

	@Override
	public PaymentGayewayPaymentDetailResponse getPaymentDetails(PaymentGayewayPaymentDetailRequest request) {
		return new PaymentGayewayPaymentDetailResponse();
	}
	
	
	 public Map<String, Object> generatePaymentToken(
	            String amount,
	            String customerEmail,
	            String customerMobile,
	            String apartmentId,
	            String internalTransactionId,
	            PaymentGayewayOrderRequest paymentGayewayOrderRequest,String securaTransactionId,String paymentId
	    ) throws Exception {

	        // 1. Generate unique merchant transaction reference
	        String merchantTxnId =createAtomsMerchantTransactionID(paymentGayewayOrderRequest,securaTransactionId,paymentId) ;
//	        String merchantTxnId="SECURA-" +
//	                UUID.randomUUID().toString().replace("-", "")
//	                        .substring(0, 20);

	        String txnDate = LocalDateTime.now().format(
	                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
	        );

	        // 2. Build AUTH request
	        AtomAuthRequest request = new AtomAuthRequest();

	        AtomAuthRequest.PayInstrument instrument =
	                new AtomAuthRequest.PayInstrument();

	        AtomAuthRequest.HeadDetails head =
	                new AtomAuthRequest.HeadDetails();

	        head.setVersion("OTSv1.1");
	        head.setApi("AUTH");
	        head.setPlatform("FLASH");

	        AtomAuthRequest.MerchDetails merch =
	                new AtomAuthRequest.MerchDetails();

	        merch.setMerchId(merchantId);
	        merch.setUserId(userId);
	        merch.setPassword(password);
	        merch.setMerchTxnId(merchantTxnId);
	        merch.setMerchTxnDate(txnDate);

	        AtomAuthRequest.PayDetails pay =
	                new AtomAuthRequest.PayDetails();

	        pay.setAmount(amount);
	        pay.setProduct(productId);
	        pay.setTxnCurrency(currency);

	        AtomAuthRequest.CustDetails customer =
	                new AtomAuthRequest.CustDetails();

	        customer.setCustEmail(customerEmail);
	        customer.setCustMobile(customerMobile);

	        AtomAuthRequest.Extras extras =
	                new AtomAuthRequest.Extras();

	        extras.setUdf1(apartmentId);
	        extras.setUdf2(internalTransactionId);
	        AtomAuthRequest.PayModeSpecificData payModeSpecificData= new AtomAuthRequest.PayModeSpecificData();
	       // payModeSpecificData.setSubChannel(Arrays.asList("UP", "CC"));
	        
	        instrument.setHeadDetails(head);
	        instrument.setMerchDetails(merch);
	        instrument.setPayDetails(pay);
	        instrument.setCustDetails(customer);
	        instrument.setExtras(extras);
	        instrument.setPayModeSpecificData(payModeSpecificData);
	        request.setPayInstrument(instrument);

	        // 3. Convert request to JSON
	        String requestJson = objectMapper.writeValueAsString(request);

	        // 4. Encrypt JSON
	        String encryptedData = AtomEncryption.encrypt(
	                requestJson,
	                requestKey
	        );

	        // 5. Prepare HTTP form parameters
	        MultiValueMap<String, String> formData =
	                new LinkedMultiValueMap<>();

	        formData.add("merchId", merchantId);
	        formData.add("encData", encryptedData);

	        // 6. Call ATOM AUTH API
	        String response = restClientBuilder.build()
	                .post()
	                .uri(authUrl)
	                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
	                .body(formData)
	                .retrieve()
	                .body(String.class);

	        // 7. Decrypt response
	        // Response may contain merchId=...&encData=...
  String encryptedResponse = extractEncData(response);

	        String decryptedResponse = AtomEncryption.decrypt(
	                encryptedResponse,
	                responseKey
	        );
	        
//	        String decryptedResponse = AtomEncryption.decrypt(
//	                "5500FEA2F09DA7EF128CFE7D2D01F253A3FDF09BF6C5CCDAE9A9DBFB1D25E2DF037A5A0FB3C847EF4DD342B45BEA1FCEB0F90094C62C18C8921204C9CE9F7787F41C9C33FE66F83352E953609589352500D286AE912B361B7DC2FF7036B04CF1FBA234DC868BA40954931FA5B662CB68680CE039CD728E086296516C2CF41E7E4E334EF5CE2FFE9602A73EEFA98B8BBC2F0BF26E302088092C998C5B679D5586467E73CB2D2F03D04D733A0E77AB546B",
//	                responseKey
//	        );

	        // 8. Parse decrypted response
	        Map<String, Object> result =
	                objectMapper.readValue(decryptedResponse, Map.class);

	        Map<String, Object> responseMap= new HashMap<>();
	        responseMap.put("merchantTxnId", merchantTxnId);
	        responseMap.put("atomResponse", result);
	        responseMap.put("merchantId", merchantId);
	        
	        return responseMap;
	    }

	    private String extractEncData(String response) {

	        if (response == null || response.isBlank()) {
	            throw new IllegalStateException("Empty response received from ATOM");
	        }

	        for (String parameter : response.split("&")) {

	            String[] parts = parameter.split("=", 2);

	            if (parts.length == 2 && "encData".equals(parts[0])) {
	                return java.net.URLDecoder.decode(
	                        parts[1],
	                        java.nio.charset.StandardCharsets.UTF_8
	                );
	            }
	        }

	        throw new IllegalStateException(
	                "encData not found in ATOM response: " + response
	        );
	    }
	    
	    public String extractReturnURLlData(String encryptedData, String merChantId) {
	    	 String response = null;
			try {
				response = AtomEncryption.decrypt(encryptedData, responseKey);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	            return response;
	    }
	    
	    
	    public String createAtomsMerchantTransactionID(PaymentGayewayOrderRequest paymentGayewayOrderRequest,String securaTransactionId,String paymentId) {
	    	StringBuilder merchantTransactionId= new StringBuilder();
	    	GenericHeader genericHeader= paymentGayewayOrderRequest.getGenericHeader();
	    	merchantTransactionId.append("SECURA-");
	    	merchantTransactionId.append(genericHeader.getApartmentId());
	    	merchantTransactionId.append("-");
	    	merchantTransactionId.append(genericHeader.getFlatNo());
	    	merchantTransactionId.append("-");
	    	merchantTransactionId.append(genericHeader.getUserId());
	    	merchantTransactionId.append("-");
	    	merchantTransactionId.append(securaTransactionId);
	    	merchantTransactionId.append("-");
	    	merchantTransactionId.append(paymentId);
	    	
	    	return merchantTransactionId.toString();
	    }
}
