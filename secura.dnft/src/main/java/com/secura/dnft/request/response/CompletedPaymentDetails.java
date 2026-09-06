package com.secura.dnft.request.response;

import java.time.LocalDateTime;
import java.util.List;

public class CompletedPaymentDetails {

	private String paymentId;
	private String paymentName;
	private String transactionId;
	private String flatId;
	private List<String> tenderList;
	private LocalDateTime transactionDate;
	private String cycleOfPayment;
	private String dueAmount;
	private String discount;
	private String penalty;
	private String roundUpAmount;
	private String transactionAmount;
	private String thirdPartyTransactionNumber;
	private String noOfHead;

	public String getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}

	public String getPaymentName() {
		return paymentName;
	}

	public void setPaymentName(String paymentName) {
		this.paymentName = paymentName;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public String getFlatId() {
		return flatId;
	}

	public void setFlatId(String flatId) {
		this.flatId = flatId;
	}

	public List<String> getTenderList() {
		return tenderList;
	}

	public void setTenderList(List<String> tenderList) {
		this.tenderList = tenderList;
	}

	public LocalDateTime getTransactionDate() {
		return transactionDate;
	}

	public void setTransactionDate(LocalDateTime transactionDate) {
		this.transactionDate = transactionDate;
	}

	public String getCycleOfPayment() {
		return cycleOfPayment;
	}

	public void setCycleOfPayment(String cycleOfPayment) {
		this.cycleOfPayment = cycleOfPayment;
	}

	public String getDueAmount() {
		return dueAmount;
	}

	public void setDueAmount(String dueAmount) {
		this.dueAmount = dueAmount;
	}

	public String getDiscount() {
		return discount;
	}

	public void setDiscount(String discount) {
		this.discount = discount;
	}

	public String getPenalty() {
		return penalty;
	}

	public void setPenalty(String penalty) {
		this.penalty = penalty;
	}

	public String getRoundUpAmount() {
		return roundUpAmount;
	}

	public void setRoundUpAmount(String roundUpAmount) {
		this.roundUpAmount = roundUpAmount;
	}

	public String getTransactionAmount() {
		return transactionAmount;
	}

	public void setTransactionAmount(String transactionAmount) {
		this.transactionAmount = transactionAmount;
	}

	public String getThirdPartyTransactionNumber() {
		return thirdPartyTransactionNumber;
	}

	public void setThirdPartyTransactionNumber(String thirdPartyTransactionNumber) {
		this.thirdPartyTransactionNumber = thirdPartyTransactionNumber;
	}

	public String getNoOfHead() {
		return noOfHead;
	}

	public void setNoOfHead(String noOfHead) {
		this.noOfHead = noOfHead;
	}
	
	
}
