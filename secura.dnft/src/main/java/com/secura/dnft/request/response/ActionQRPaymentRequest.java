package com.secura.dnft.request.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.secura.dnft.entity.Transaction;

public class ActionQRPaymentRequest {

	private GenericHeader genericHeader;
	@JsonAlias({ "transactionsList" })
	private List<Transaction> transactionsList;
	private String action;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public List<Transaction> getTransactionsList() {
		return transactionsList;
	}

	public void setFTransactionsList(List<Transaction> transactionsList) {
		this.transactionsList = transactionsList;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}
}
