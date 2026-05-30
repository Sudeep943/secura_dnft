package com.secura.dnft.request.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.secura.dnft.entity.Transaction;

public class ActionQRPaymentResponse {

	private GenericHeader genericHeader;
	private String message;
	private String messageCode;
	@JsonProperty("notCompltedTransactionList")
	private List<Transaction> notCompletedTransactionList;
	@JsonProperty("filedWorklistActionFileBase64Encoded")
	private String failedWorklistActionFileBase64Encoded;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
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

	public List<Transaction> getNotCompletedTransactionList() {
		return notCompletedTransactionList;
	}

	public void setNotCompletedTransactionList(List<Transaction> notCompletedTransactionList) {
		this.notCompletedTransactionList = notCompletedTransactionList;
	}

	public String getFailedWorklistActionFileBase64Encoded() {
		return failedWorklistActionFileBase64Encoded;
	}

	public void setFailedWorklistActionFileBase64Encoded(String failedWorklistActionFileBase64Encoded) {
		this.failedWorklistActionFileBase64Encoded = failedWorklistActionFileBase64Encoded;
	}
}
