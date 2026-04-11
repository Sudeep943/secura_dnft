package com.secura.dnft.request.response;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

public class DueAmountDetails {

	@JsonFormat(pattern = "d-MMM-yyyy")
	private LocalDate dueDate;
	private String paymentId;
	private String dueId;
	private String amount;
	private String gstAmount;
	private String totalAmount;
	private List<AddedCharges> addedCharges;

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
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

	public String getGstAmount() {
		return gstAmount;
	}

	public void setGstAmount(String gstAmount) {
		this.gstAmount = gstAmount;
	}

	public String getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(String totalAmount) {
		this.totalAmount = totalAmount;
	}

	public List<AddedCharges> getAddedCharges() {
		return addedCharges;
	}

	public void setAddedCharges(List<AddedCharges> addedCharges) {
		this.addedCharges = addedCharges;
	}
}
