package com.secura.dnft.request.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.secura.dnft.entity.Transaction;

public class ActionQRPaymentRequest {

	private GenericHeader genericHeader;
	@JsonAlias({ "foundTransactionsfoundTransactionsList" })
	private List<Transaction> foundTransactionsList;
	private String action;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public List<Transaction> getFoundTransactionsList() {
		return foundTransactionsList;
	}

	public void setFoundTransactionsList(List<Transaction> foundTransactionsList) {
		this.foundTransactionsList = foundTransactionsList;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}
}
