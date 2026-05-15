package com.secura.dnft.request.response;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

public class PaymentDetailKeySerializer extends JsonSerializer<PaymentDetail> {

	private static final ObjectWriter PAYMENT_DETAIL_WRITER = JsonMapper.builder().findAndAddModules().build()
			.writerFor(PaymentDetail.class);

	@Override
	public void serialize(PaymentDetail value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		gen.writeFieldName(PAYMENT_DETAIL_WRITER.writeValueAsString(value));
	}
}
