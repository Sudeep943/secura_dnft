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
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.GetDefaulterRequest;
import com.secura.dnft.request.response.GetDefaulterResponse;
import com.secura.dnft.service.TransactionAndReportsService;

@ExtendWith(MockitoExtension.class)
class TransactionAndReportsControllerTest {

	@Mock
	private TransactionAndReportsService transactionAndReportsService;

	@InjectMocks
	private TransactionAndReportsController transactionAndReportsController;

	@Test
	void getDeaulterList_shouldReturnServiceResponse() {
		GetDefaulterRequest request = new GetDefaulterRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		request.setGenericHeader(header);

		GetDefaulterResponse expected = new GetDefaulterResponse();
		expected.setGenericHeader(header);
		expected.setMessage("ok");
		expected.setMessageCode("CODE");
		when(transactionAndReportsService.getDeaulterList(request)).thenReturn(expected);

		GetDefaulterResponse actual = transactionAndReportsController.getDeaulterList(request);

		assertEquals(expected, actual);
	}

	@Test
	void getDeaulterList_shouldReturnGenericErrorWhenServiceThrows() {
		GetDefaulterRequest request = new GetDefaulterRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		request.setGenericHeader(header);
		when(transactionAndReportsService.getDeaulterList(request)).thenThrow(new RuntimeException("boom"));

		GetDefaulterResponse actual = transactionAndReportsController.getDeaulterList(request);

		assertEquals(header, actual.getGenericHeader());
		assertEquals(ErrorMessage.ERR_MESSAGE_33, actual.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_33, actual.getMessageCode());
	}
}
