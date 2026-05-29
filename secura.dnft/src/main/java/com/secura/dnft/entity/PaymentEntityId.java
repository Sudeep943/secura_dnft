package com.secura.dnft.entity;

import java.io.Serializable;
import java.util.Objects;

public class PaymentEntityId implements Serializable {

	private static final long serialVersionUID = 1L;

	private String paymentId;
	private String aprmtId;

	public PaymentEntityId() {
	}

	public PaymentEntityId(String paymentId, String aprmtId) {
		this.paymentId = paymentId;
		this.aprmtId = aprmtId;
	}

	public String getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}

	public String getAprmtId() {
		return aprmtId;
	}

	public void setAprmtId(String aprmtId) {
		this.aprmtId = aprmtId;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof PaymentEntityId other)) {
			return false;
		}
		return Objects.equals(paymentId, other.paymentId)
				&& Objects.equals(aprmtId, other.aprmtId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(paymentId, aprmtId);
	}
}
