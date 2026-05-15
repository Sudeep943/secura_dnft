package com.secura.dnft.request.response;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

public class PaymentDetailKeySerializer extends JsonSerializer<PaymentDetail> {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Override
	public void serialize(PaymentDetail value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		gen.writeFieldName(OBJECT_MAPPER.writeValueAsString(value));
	}
}
