package com.secura.dnft.request.response;

import java.util.List;

import com.secura.dnft.entity.Transaction;

public class ReconcileQRPaymentResponse {

	private GenericHeader genericHeader;
	private String highlithedBase64EncodedFile;
	private String message;
	private String messageCode;
	private Integer foundCount;
	private Integer notFoundCount;
	private List<Transaction> notFoundTransactionsList;
	private List<Transaction> foundTransactionsList;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public String getHighlithedBase64EncodedFile() {
		return highlithedBase64EncodedFile;
	}

	public void setHighlithedBase64EncodedFile(String highlithedBase64EncodedFile) {
		this.highlithedBase64EncodedFile = highlithedBase64EncodedFile;
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

	public Integer getFoundCount() {
		return foundCount;
	}

	public void setFoundCount(Integer foundCount) {
		this.foundCount = foundCount;
	}

	public Integer getNotFoundCount() {
		return notFoundCount;
	}

	public void setNotFoundCount(Integer notFoundCount) {
		this.notFoundCount = notFoundCount;
	}

	public List<Transaction> getNotFoundTransactionsList() {
		return notFoundTransactionsList;
	}

	public void setNotFoundTransactionsList(List<Transaction> notFoundTransactionsList) {
		this.notFoundTransactionsList = notFoundTransactionsList;
	}

	public List<Transaction> getFoundTransactionsList() {
		return foundTransactionsList;
	}

	public void setFoundTransactionsList(List<Transaction> foundTransactionsList) {
		this.foundTransactionsList = foundTransactionsList;
	}
}
