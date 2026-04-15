package com.secura.dnft.request.response;

import java.util.List;

public class GetDueAmountForFlatResponse {

	private GenericHeader genericHeader;
	private List<DueAmountDetails> duePaymentList;
	private String totalDueAmount;
	private String message;
	private String messageCode;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public List<DueAmountDetails> getDuePaymentList() {
		return duePaymentList;
	}

	public void setDuePaymentList(List<DueAmountDetails> duePaymentList) {
		this.duePaymentList = duePaymentList;
	}

	public String getTotalDueAmount() {
		return totalDueAmount;
	}

	public void setTotalDueAmount(String totalDueAmount) {
		this.totalDueAmount = totalDueAmount;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessageCode() {
		return messageCode;
	}

	public void setMessageCode(String messageCode) {
		this.messageCode = messageCode;
	}
}
