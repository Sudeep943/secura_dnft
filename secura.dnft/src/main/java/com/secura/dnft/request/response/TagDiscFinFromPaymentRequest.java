package com.secura.dnft.request.response;

public class TagDiscFinFromPaymentRequest {

	private GenericHeader genericHeader;
	private String paymentId;
	private DiscfinRequestData discfinRequestData;

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

	public DiscfinRequestData getDiscfinRequestData() {
		return discfinRequestData;
	}

	public void setDiscfinRequestData(DiscfinRequestData discfinRequestData) {
		this.discfinRequestData = discfinRequestData;
	}
}
