package com.secura.dnft.request.response;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;

@JsonPropertyOrder({ "paymentId", "paymentName" })
public class PaymentDetail {

	private static final ObjectWriter PAYMENT_DETAIL_WRITER = JsonMapper.builder().findAndAddModules().build()
			.writerFor(PaymentDetail.class);

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
		try {
			return PAYMENT_DETAIL_WRITER.writeValueAsString(this);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Unable to serialize payment detail", exception);
		}
	}
}
