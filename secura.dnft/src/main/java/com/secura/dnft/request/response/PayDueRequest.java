package com.secura.dnft.request.response;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.secura.dnft.entity.DueAmountDetailsEntity;

public class PayDueRequest {

	private GenericHeader genericHeader;
	private String paymentId;
	@JsonAlias({ "DueId" })
	private String dueId;
	private String amount;
	private List<BankInstrumentTenderDetails> bankInstrumentTenderDetails;
	private List<PaymentTenderData> paymentTenderDataList;
	private String paymentCycle;
	private String paymentName;
	@JsonFormat(pattern = "d-MMM-yyyy")
	private LocalDate dueDate;
	@JsonFormat(pattern = "d-MMM-yyyy")
	private LocalDate dueStartDate;
	@JsonFormat(pattern = "d-MMM-yyyy")
	private LocalDate dueEndDate;
	@JsonAlias({ "thirdPatyTransactioId", "thirdPartyTransactionId" })
	private String thirdPartyTransactionId;
	@JsonAlias({ "tarnsactionStatus", "trnsactionStatus", "transactionStatus" })
	private String transactionStatus;
	private String thirdPartyName;
	@JsonAlias({ "noOfPersons" })
	private String noOfPersons;
	@JsonAlias({ "listOfFiles" })
	private List<String> files;
	private DueAmountDetailsEntity paidDueDetails;

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

	public List<BankInstrumentTenderDetails> getBankInstrumentTenderDetails() {
		return bankInstrumentTenderDetails;
	}

	public void setBankInstrumentTenderDetails(List<BankInstrumentTenderDetails> bankInstrumentTenderDetails) {
		this.bankInstrumentTenderDetails = bankInstrumentTenderDetails;
	}

	public List<PaymentTenderData> getPaymentTenderDataList() {
		return paymentTenderDataList;
	}

	public void setPaymentTenderDataList(List<PaymentTenderData> paymentTenderDataList) {
		this.paymentTenderDataList = paymentTenderDataList;
	}

	public String getPaymentCycle() {
		return paymentCycle;
	}

	public void setPaymentCycle(String paymentCycle) {
		this.paymentCycle = paymentCycle;
	}

	public String getPaymentName() {
		return paymentName;
	}

	public void setPaymentName(String paymentName) {
		this.paymentName = paymentName;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public LocalDate getDueStartDate() {
		return dueStartDate;
	}

	public void setDueStartDate(LocalDate dueStartDate) {
		this.dueStartDate = dueStartDate;
	}

	public LocalDate getDueEndDate() {
		return dueEndDate;
	}

	public void setDueEndDate(LocalDate dueEndDate) {
		this.dueEndDate = dueEndDate;
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

	public DueAmountDetailsEntity getPaidDueDetails() {
		return paidDueDetails;
	}

	public void setPaidDueDetails(DueAmountDetailsEntity paidDueDetails) {
		this.paidDueDetails = paidDueDetails;
	}
}
