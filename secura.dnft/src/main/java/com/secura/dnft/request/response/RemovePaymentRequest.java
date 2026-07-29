package com.secura.dnft.request.response;

public class RemovePaymentRequest {

	private GenericHeader genericHeader;
	private String paymentId;
	private String dueId;
	private String flatId;
	private boolean confirmDeleteTransactionFlag;

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

	public String getDueId() {
		return dueId;
	}

	public void setDueId(String dueId) {
		this.dueId = dueId;
	}

	public String getFlatId() {
		return flatId;
	}

	public void setFlatId(String flatId) {
		this.flatId = flatId;
	}

	public boolean isConfirmDeleteTransactionFlag() {
		return confirmDeleteTransactionFlag;
	}

	public void setConfirmDeleteTransactionFlag(boolean confirmDeleteTransactionFlag) {
		this.confirmDeleteTransactionFlag = confirmDeleteTransactionFlag;
	}
}
