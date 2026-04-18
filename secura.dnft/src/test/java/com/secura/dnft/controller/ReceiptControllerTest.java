package com.secura.dnft.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

	@Test
	void createReceipt_shouldDelegateToService() throws Exception {
		ReceiptController controller = controllerWithMockService();
		CreateReceiptRequest request = new CreateReceiptRequest();
		CreateReceiptResponse expected = new CreateReceiptResponse();
		when(receiptServices.createReceipt(request)).thenReturn(expected);

		CreateReceiptResponse response = controller.createReceipt(request);

		assertSame(expected, response);
	}

	@Test
	void createReceipt_shouldReturnServiceExceptionMessageOnFailure() throws Exception {
		ReceiptController controller = controllerWithMockService();
		CreateReceiptRequest request = new CreateReceiptRequest();
		GenericHeader header = new GenericHeader();
		request.setGenericHeader(header);
		when(receiptServices.createReceipt(request)).thenThrow(new RuntimeException("boom"));

		CreateReceiptResponse response = controller.createReceipt(request);

		assertSame(header, response.getGenericHeader());
		assertEquals(ErrorMessage.ERR_MESSAGE_33, response.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_33, response.getMessageCode());
	}

	private ReceiptController controllerWithMockService() throws Exception {
		ReceiptController controller = new ReceiptController();
		Field field = ReceiptController.class.getDeclaredField("receiptServices");
		field.setAccessible(true);
		field.set(controller, receiptServices);
		return controller;
	}
}
