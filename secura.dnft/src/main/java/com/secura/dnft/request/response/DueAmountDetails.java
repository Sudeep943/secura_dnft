package com.secura.dnft.request.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

public class DueAmountDetails {

	@JsonFormat(pattern = "d-MMM-yyyy")
	private LocalDate dueDate;
	private String paymentId;
	private String status;
	private String amount;

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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}
}
