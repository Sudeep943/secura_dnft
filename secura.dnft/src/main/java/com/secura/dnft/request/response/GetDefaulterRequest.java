package com.secura.dnft.request.response;

import java.util.List;

public class GetDefaulterRequest {

	private GenericHeader genericHeader;
	private List<String> paymentId;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public List<String> getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(List<String> paymentId) {
		this.paymentId = paymentId;
	}
}
