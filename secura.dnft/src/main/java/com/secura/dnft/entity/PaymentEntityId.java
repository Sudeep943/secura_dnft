package com.secura.dnft.entity;

import java.io.Serializable;
import java.util.Objects;

public class PaymentEntityId implements Serializable {

	private static final long serialVersionUID = 1L;

	private String paymentId;
	private String paymentCollectionCycle;

	public PaymentEntityId() {
	}

	public PaymentEntityId(String paymentId, String paymentCollectionCycle) {
		this.paymentId = paymentId;
		this.paymentCollectionCycle = paymentCollectionCycle;
	}

	public String getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}

	public String getPaymentCollectionCycle() {
		return paymentCollectionCycle;
	}

	public void setPaymentCollectionCycle(String paymentCollectionCycle) {
		this.paymentCollectionCycle = paymentCollectionCycle;
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
				&& Objects.equals(paymentCollectionCycle, other.paymentCollectionCycle);
	}

	@Override
	public int hashCode() {
		return Objects.hash(paymentId, paymentCollectionCycle);
	}
}
