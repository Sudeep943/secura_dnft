package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.PaymentGayewayOrderRequest;
import com.secura.dnft.request.response.PaymentGayewayOrderResponse;

class DeepLinkServicesTest {

	private final DeepLinkServices deepLinkServices = new DeepLinkServices();

	@Test
	void createOrder_shouldCopyRequestDataToResponse() {
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APT-1");
		Map<String, Object> requestData = new HashMap<>();
		requestData.put("deeplinkUrl", "https://example.test/pay");
		requestData.put("amount", "1000");
		PaymentGayewayOrderRequest request = new PaymentGayewayOrderRequest();
		request.setGenericHeader(header);
		request.setData(requestData);

		PaymentGayewayOrderResponse response = deepLinkServices.createOrder(request);

		assertEquals(header, response.getGenericHeader());
		assertEquals(requestData, response.getData());
		assertNotSame(requestData, response.getData());
	}

	@Test
	void createOrder_shouldReturnNullDataWhenRequestDataIsNull() {
		PaymentGayewayOrderRequest request = new PaymentGayewayOrderRequest();

		PaymentGayewayOrderResponse response = deepLinkServices.createOrder(request);

		assertNull(response.getData());
	}

	@Test
	void createOrder_shouldReturnEmptyDataWhenRequestDataIsEmpty() {
		PaymentGayewayOrderRequest request = new PaymentGayewayOrderRequest();
		request.setData(Map.of());

		PaymentGayewayOrderResponse response = deepLinkServices.createOrder(request);

		assertTrue(response.getData().isEmpty());
	}
}
