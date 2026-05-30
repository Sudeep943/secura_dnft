package com.secura.dnft.request.response;

import java.util.List;

import com.secura.dnft.entity.Transaction;

public class ActionQRPaymentResponse {

	private GenericHeader genericHeader;
	private String message;
	private String messageCode;
	private List<Transaction> notCompltedTransactionList;
	private String filedWorklistActionFileBase64Encoded;

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

	public List<Transaction> getNotCompltedTransactionList() {
		return notCompltedTransactionList;
	}

	public void setNotCompltedTransactionList(List<Transaction> notCompltedTransactionList) {
		this.notCompltedTransactionList = notCompltedTransactionList;
	}

	public String getFiledWorklistActionFileBase64Encoded() {
		return filedWorklistActionFileBase64Encoded;
	}

	public void setFiledWorklistActionFileBase64Encoded(String filedWorklistActionFileBase64Encoded) {
		this.filedWorklistActionFileBase64Encoded = filedWorklistActionFileBase64Encoded;
	}
}
