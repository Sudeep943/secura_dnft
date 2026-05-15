package com.secura.dnft.request.response;

import java.util.Objects;

public class PaymentDetail {

	private String paymentId;
	private String paymentName;

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

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof PaymentDetail)) {
			return false;
		}
		PaymentDetail other = (PaymentDetail) obj;
		return Objects.equals(paymentId, other.paymentId) && Objects.equals(paymentName, other.paymentName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(paymentId, paymentName);
	}
}
