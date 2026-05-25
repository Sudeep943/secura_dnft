package com.secura.dnft.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.secura.dnft.dao.BankEntityRepository;
import com.secura.dnft.entity.BankEntity;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.interfaceservice.ThirdPartyPaymentGayeway;
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
public class DeepLinkServices implements ThirdPartyPaymentGayeway {


	@Autowired
	private BankEntityRepository bankRepository;
	

	@Autowired
	private GenericService genericService;
	
	
	@Override
	public PaymentGayewayOrderResponse createOrder(PaymentGayewayOrderRequest request) {
		PaymentGayewayOrderResponse response = new PaymentGayewayOrderResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		response.setData(createOrderMethod( request.getData(),request.getGenericHeader().getApartmentId(),request.getAmountInPaisa()));
		response.setMessage(SuccessMessage.SUCC_MESSAGE_01);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_44);
		return response;
	}

	Map<String, Object> createOrderMethod(Map<String, Object> requestMap,String apartmentId,String amountInPaisa) {
		if (requestMap == null) {
			return null;
		}
		String tid=(String)requestMap.get("tid");
		String tn=(String)requestMap.get("tn");
		String bankId=(String)requestMap.get("bankId");
		Optional<BankEntity> bankEntityOptional = bankRepository.findByAprmntIdAndBankDetailsID(apartmentId, bankId);
		BankEntity bankEntity = bankEntityOptional.get();
		String upiId = decryptNullable(bankEntity.getUpiId());
		double amountInRupee=Double.valueOf(amountInPaisa)/100;
		StringBuilder upiUrl= new StringBuilder("upi://pay?pa=");
		upiUrl.append(upiId+"&");
		upiUrl.append("tid="+tid+"&");
		upiUrl.append("tn="+tn+"&");
		upiUrl.append("am="+amountInRupee+"&cu=INR");
		
		Map<String, Object> responseMap= new HashMap<>();
		responseMap.put("upiPaymentURL", upiUrl.toString());
		responseMap.put("amount", amountInRupee);
		responseMap.put("bankName", decryptNullable(bankEntity.getBankName()));
		responseMap.put("accountHolderName", decryptNullable(bankEntity.getAccountName()));
		return responseMap;
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
	
	private String decryptNullable(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return genericService.decrypt(value);
	}
}
