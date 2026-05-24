package com.secura.dnft.request.response;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;

@JsonPropertyOrder({ "paymentId", "paymentName", "bankId", "paymentGateway" })
@JsonInclude(Include.NON_NULL)
public class PaymentDetail {

	private String paymentId;
	private String paymentName;
	private String bankId;
	private String paymentGateway;

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

	public String getBankId() {
		return bankId;
	}

	public void setBankId(String bankId) {
		this.bankId = bankId;
	}

	public String getPaymentGateway() {
		return paymentGateway;
	}

	public void setPaymentGateway(String paymentGateway) {
		this.paymentGateway = paymentGateway;
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
		return Objects.equals(paymentId, other.paymentId) && Objects.equals(paymentName, other.paymentName)
				&& Objects.equals(bankId, other.bankId) && Objects.equals(paymentGateway, other.paymentGateway);
	}

	@Override
	public int hashCode() {
		return Objects.hash(paymentId, paymentName, bankId, paymentGateway);
	}

	@Override
	public String toString() {
		try {
			return PaymentDetailWriterHolder.PAYMENT_DETAIL_WRITER.writeValueAsString(this);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException(
					"Unable to serialize payment detail for paymentId=" + paymentId + ", paymentName=" + paymentName
							+ ", bankId=" + bankId + ", paymentGateway=" + paymentGateway,
					exception);
		}
	}

	private static final class PaymentDetailWriterHolder {
		private static final ObjectWriter PAYMENT_DETAIL_WRITER = JsonMapper.builder().findAndAddModules().build()
				.writerFor(PaymentDetail.class);

		private PaymentDetailWriterHolder() {
		}
	}
}
