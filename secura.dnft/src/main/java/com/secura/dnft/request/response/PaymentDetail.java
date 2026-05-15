package com.secura.dnft.request.response;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.io.JsonStringEncoder;

@JsonPropertyOrder({ "paymentId", "paymentName" })
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

	@Override
	public String toString() {
		return "{\"paymentId\":\"" + escapeJson(paymentId) + "\",\"paymentName\":\"" + escapeJson(paymentName) + "\"}";
	}

	private String escapeJson(String value) {
		if (value == null) {
			return "";
		}
		return new String(JsonStringEncoder.getInstance().quoteAsString(value));
	}
}
