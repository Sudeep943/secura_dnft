package com.secura.dnft.request.response;

import java.util.List;

import com.secura.dnft.entity.Transaction;

public class ExternalTransactionNoDetails {
	private String externalTransactionReferenceNumber;
	private boolean usedInMultipleTransction;
	private List<Transaction> trnsactions;
	
	public String getExternalTransactionReferenceNumber() {
		return externalTransactionReferenceNumber;
	}
	public void setExternalTransactionReferenceNumber(String externalTransactionReferenceNumber) {
		this.externalTransactionReferenceNumber = externalTransactionReferenceNumber;
	}
	public boolean isUsedInMultipleTransction() {
		return usedInMultipleTransction;
	}
	public void setUsedInMultipleTransction(boolean usedInMultipleTransction) {
		this.usedInMultipleTransction = usedInMultipleTransction;
	}
	public List<Transaction> getTrnsactions() {
		return trnsactions;
	}
	public void setTrnsactions(List<Transaction> trnsactions) {
		this.trnsactions = trnsactions;
	}
	
	
}
