package com.secura.dnft.request.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;

public class PayDueRequest {

	private GenericHeader genericHeader;
	private String paymentId;
	@JsonAlias({ "DueId" })
	private String dueId;
	private String amount;
	private String tender;
	@JsonAlias({ "thirdPatyTransactioId", "thirdPartyTransactionId" })
	private String thirdPartyTransactionId;
	@JsonAlias({ "tarnsactionStatus", "trnsactionStatus", "transactionStatus" })
	private String transactionStatus;
	private String thirdPartyName;
	@JsonAlias({ "noOfPersons" })
	private String noOfPersons;
	@JsonAlias({ "listOfFiles" })
	private List<String> files;

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

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public String getTender() {
		return tender;
	}

	public void setTender(String tender) {
		this.tender = tender;
	}

	public String getThirdPartyTransactionId() {
		return thirdPartyTransactionId;
	}

	public void setThirdPartyTransactionId(String thirdPartyTransactionId) {
		this.thirdPartyTransactionId = thirdPartyTransactionId;
	}

	public String getTransactionStatus() {
		return transactionStatus;
	}

	public void setTransactionStatus(String transactionStatus) {
		this.transactionStatus = transactionStatus;
	}

	public String getThirdPartyName() {
		return thirdPartyName;
	}

	public void setThirdPartyName(String thirdPartyName) {
		this.thirdPartyName = thirdPartyName;
	}

	public String getNoOfPersons() {
		return noOfPersons;
	}

	public void setNoOfPersons(String noOfPersons) {
		this.noOfPersons = noOfPersons;
	}

	public List<String> getFiles() {
		return files;
	}

	public void setFiles(List<String> files) {
		this.files = files;
	}
}
