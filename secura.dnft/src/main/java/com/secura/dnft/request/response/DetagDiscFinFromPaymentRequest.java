package com.secura.dnft.request.response;

public class DetagDiscFinFromPaymentRequest {

	private GenericHeader genericHeader;
	private String paymentId;
	private String discFinType;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public String getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}

	public String getDiscFinType() {
		return discFinType;
	}

	public void setDiscFinType(String discFinType) {
		this.discFinType = discFinType;
	}


}
