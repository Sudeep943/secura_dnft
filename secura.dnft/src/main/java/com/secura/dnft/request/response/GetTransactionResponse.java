package com.secura.dnft.request.response;

import java.util.List;

public class GetTransactionResponse {

	private GenericHeader genericHeader;
	private String message;
	private String messageCode;
	private List<TransactionData> transactionList;

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

	public List<TransactionData> getTransactionList() {
		return transactionList;
	}

	public void setTransactionList(List<TransactionData> transactionList) {
		this.transactionList = transactionList;
	}
}
