package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.PaymentGayewayOrderRequest;
import com.secura.dnft.request.response.PaymentGayewayOrderResponse;

class DeepLinkServicesTest {

	private final DeepLinkServices deepLinkServices = new DeepLinkServices();

	@Test
	void createOrder_shouldPopulateResponseDataFromRequestMap() {
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		Map<String, Object> requestMap = new HashMap<>();
		requestMap.put("deepLinkUrl", "https://example.app/payment");
		requestMap.put("txnRef", "TXN-101");
		PaymentGayewayOrderRequest request = new PaymentGayewayOrderRequest();
		request.setGenericHeader(header);
		request.setData(requestMap);

		PaymentGayewayOrderResponse response = deepLinkServices.createOrder(request);

		assertEquals(header, response.getGenericHeader());
		assertEquals("https://example.app/payment", response.getData().get("deepLinkUrl"));
		assertEquals("TXN-101", response.getData().get("txnRef"));
		assertNotSame(requestMap, response.getData());
	}

	@Test
	void createOrder_shouldKeepDataNullWhenRequestMapMissing() {
		PaymentGayewayOrderRequest request = new PaymentGayewayOrderRequest();

		PaymentGayewayOrderResponse response = deepLinkServices.createOrder(request);

		assertNull(response.getData());
	}
}
