package com.secura.dnft.interfaceservice;

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

public interface ThirdPartyPaymentGayeway {

	PaymentGayewayOrderResponse createOrder(PaymentGayewayOrderRequest request);

	PaymentGayewayOrderVerificationResponse verifypayment(PaymentGayewayOrderVerificationRequest request);

	PaymentGayewayPayOrderResponse payorder(PaymentGayewayPayOrderRequest request);

	PaymentGayewayProcessRefundResponse processRefund(PaymentGayewayProcessRefundRequest request);

	PaymentGayewayPaymentDetailResponse getPaymentDetails(PaymentGayewayPaymentDetailRequest request);
}
