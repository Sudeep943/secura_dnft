package com.secura.dnft.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

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

	@Override
	public PaymentGayewayOrderResponse createOrder(PaymentGayewayOrderRequest request) {
		PaymentGayewayOrderResponse response = new PaymentGayewayOrderResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		response.setData(createOrderMethod(request != null ? request.getData() : null));
		return response;
	}

	Map<String, Object> createOrderMethod(Map<String, Object> requestMap) {
		if (requestMap == null || requestMap.isEmpty()) {
			return null;
		}
		return new HashMap<>(requestMap);
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
}
