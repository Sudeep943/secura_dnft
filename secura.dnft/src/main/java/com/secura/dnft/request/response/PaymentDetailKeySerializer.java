package com.secura.dnft.request.response;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class PaymentDetailKeySerializer extends JsonSerializer<PaymentDetail> {

	@Override
	public void serialize(PaymentDetail value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		gen.writeFieldName("{\"paymentId\":" + quote(value != null ? value.getPaymentId() : null) + ",\"paymentName\":"
				+ quote(value != null ? value.getPaymentName() : null) + "}");
	}

	private String quote(String value) {
		if (value == null) {
			return "null";
		}
		return "\"" + new String(JsonStringEncoder.getInstance().quoteAsString(value)) + "\"";
	}
}
