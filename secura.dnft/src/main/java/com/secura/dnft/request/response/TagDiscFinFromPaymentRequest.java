package com.secura.dnft.request.response;

public class TagDiscFinFromPaymentRequest {

	private GenericHeader genericHeader;
	private String paymentId;
	private AddDiscfinRequest discfinRequestData;

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

	public AddDiscfinRequest getDiscfinRequestData() {
		return discfinRequestData;
	}

	public void setDiscfinRequestData(AddDiscfinRequest discfinRequestData) {
		this.discfinRequestData = discfinRequestData;
	}
}
