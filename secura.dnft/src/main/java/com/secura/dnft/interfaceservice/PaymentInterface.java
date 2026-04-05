package com.secura.dnft.interfaceservice;

import com.secura.dnft.request.response.CreatePaymentRequest;
import com.secura.dnft.request.response.CreatePaymentResponse;
import com.secura.dnft.request.response.DuePaymentAmountDetailsRequest;
import com.secura.dnft.request.response.DuePaymentAmountDetailsResponse;
import com.secura.dnft.request.response.GetPaymentRequest;
import com.secura.dnft.request.response.GetPaymentResponse;
import com.secura.dnft.request.response.UpdatePaymentRequest;
import com.secura.dnft.request.response.UpdatePaymentResponse;

public interface PaymentInterface {

	
	public CreatePaymentResponse createPayment(CreatePaymentRequest request) throws Exception;
	
	public UpdatePaymentResponse updatePayment(UpdatePaymentRequest request) throws Exception;
	
	public GetPaymentResponse getPayments(GetPaymentRequest request) throws Exception;
	
	
    public DuePaymentAmountDetailsResponse getDuePaymentAmountDetails(DuePaymentAmountDetailsRequest request) throws Exception;

}
