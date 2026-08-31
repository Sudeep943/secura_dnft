package com.secura.dnft.request.response;

public class UpdateTransactionRefRequest {
	private GenericHeader genericHeader;
	private String transactionId;
    private String thirdPartyTrnsRef;

    // Getters and Setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
	public GenericHeader getGenericHeader() {
		return genericHeader;
	}
	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}
	public String getThirdPartyTrnsRef() {
		return thirdPartyTrnsRef;
	}
	public void setThirdPartyTrnsRef(String thirdPartyTrnsRef) {
		this.thirdPartyTrnsRef = thirdPartyTrnsRef;
	}

}
