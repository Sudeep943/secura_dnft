package com.secura.dnft.request.response;

public class ReportPaymentData {

	private String paymentId;
	private String paymentName;
	private String paymentAmount;
	private String totalAddedCharges;
	private String totalAmountExcludingTax;
	private String totalAmountIncludingTax;
	private String taxCollected;

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

	public String getPaymentAmount() {
		return paymentAmount;
	}

	public void setPaymentAmount(String paymentAmount) {
		this.paymentAmount = paymentAmount;
	}

	public String getTotalAddedCharges() {
		return totalAddedCharges;
	}

	public void setTotalAddedCharges(String totalAddedCharges) {
		this.totalAddedCharges = totalAddedCharges;
	}

	public String getTotalAmountExcludingTax() {
		return totalAmountExcludingTax;
	}

	public void setTotalAmountExcludingTax(String totalAmountExcludingTax) {
		this.totalAmountExcludingTax = totalAmountExcludingTax;
	}

	public String getTotalAmountIncludingTax() {
		return totalAmountIncludingTax;
	}

	public void setTotalAmountIncludingTax(String totalAmountIncludingTax) {
		this.totalAmountIncludingTax = totalAmountIncludingTax;
	}

	public String getTaxCollected() {
		return taxCollected;
	}

	public void setTaxCollected(String taxCollected) {
		this.taxCollected = taxCollected;
	}
}
