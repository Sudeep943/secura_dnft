package com.secura.dnft.request.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

public class DefaultPayment {

	private String paymentId;
	private String paymentName;
	private String paymentCapita;
	private String totalDue;
	private String amountPaid;
	private String amountTobePaid;
	private String penalty;
	@JsonFormat(pattern = "d-MMM-yyyy")
	private LocalDate lastDueDate;

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

	public String getPaymentCapita() {
		return paymentCapita;
	}

	public void setPaymentCapita(String paymentCapita) {
		this.paymentCapita = paymentCapita;
	}

	public String getTotalDue() {
		return totalDue;
	}

	public void setTotalDue(String totalDue) {
		this.totalDue = totalDue;
	}

	public String getAmountPaid() {
		return amountPaid;
	}

	public void setAmountPaid(String amountPaid) {
		this.amountPaid = amountPaid;
	}

	public String getAmountTobePaid() {
		return amountTobePaid;
	}

	public void setAmountTobePaid(String amountTobePaid) {
		this.amountTobePaid = amountTobePaid;
	}

	public String getPenalty() {
		return penalty;
	}

	public void setPenalty(String penalty) {
		this.penalty = penalty;
	}

	public LocalDate getLastDueDate() {
		return lastDueDate;
	}

	public void setLastDueDate(LocalDate lastDueDate) {
		this.lastDueDate = lastDueDate;
	}
}
