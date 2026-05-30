package com.secura.dnft.request.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.secura.dnft.entity.Transaction;

public class ActionQRPaymentRequest {

	private GenericHeader genericHeader;
	@JsonAlias({ "foundTransactionsList" })
	private List<Transaction> foundTransactionsfoundTransactionsList;
	private String action;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public List<Transaction> getFoundTransactionsfoundTransactionsList() {
		return foundTransactionsfoundTransactionsList;
	}

	public void setFoundTransactionsfoundTransactionsList(List<Transaction> foundTransactionsfoundTransactionsList) {
		this.foundTransactionsfoundTransactionsList = foundTransactionsfoundTransactionsList;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}
}
