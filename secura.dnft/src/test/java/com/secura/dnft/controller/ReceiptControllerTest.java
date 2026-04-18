package com.secura.dnft.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.request.response.CreateReceiptRequest;
import com.secura.dnft.request.response.CreateReceiptResponse;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.service.ReceiptServices;

@ExtendWith(MockitoExtension.class)
class ReceiptControllerTest {

	@Mock
	private ReceiptServices receiptServices;

	@InjectMocks
	private ReceiptController receiptController;

	@Test
	void createReceipt_shouldReturnServiceResponse() throws Exception {
		CreateReceiptRequest request = new CreateReceiptRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		request.setGenericHeader(header);

		CreateReceiptResponse expected = new CreateReceiptResponse();
		expected.setGenericHeader(header);
		expected.setMessage("ok");
		expected.setMessageCode("CODE");
		when(receiptServices.createReceipt(request)).thenReturn(expected);

		CreateReceiptResponse actual = receiptController.createReceipt(request);

		assertEquals(expected, actual);
	}

	@Test
	void createReceipt_shouldReturnGenericErrorWhenServiceThrows() throws Exception {
		CreateReceiptRequest request = new CreateReceiptRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		request.setGenericHeader(header);
		when(receiptServices.createReceipt(request)).thenThrow(new RuntimeException("boom"));

		CreateReceiptResponse actual = receiptController.createReceipt(request);

		assertEquals(header, actual.getGenericHeader());
		assertEquals(ErrorMessage.ERR_MESSAGE_33, actual.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_33, actual.getMessageCode());
	}
}
