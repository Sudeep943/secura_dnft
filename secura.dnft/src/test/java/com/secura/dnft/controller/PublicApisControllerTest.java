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
import com.secura.dnft.request.response.GetAllFlatsRequest;
import com.secura.dnft.request.response.GetAllFlatsResponse;
import com.secura.dnft.request.response.GetDueAmountForFlatRequest;
import com.secura.dnft.request.response.GetDueAmountForFlatResponse;
import com.secura.dnft.request.response.PayDueRequest;
import com.secura.dnft.request.response.PayDueResponse;
import com.secura.dnft.service.FlatServices;
import com.secura.dnft.service.PaymentServices;

@ExtendWith(MockitoExtension.class)
class PublicApisControllerTest {

	@Mock
	private FlatServices flatServices;

	@Mock
	private PaymentServices paymentServices;

	@InjectMocks
	private PublicApisController publicApisController;

	@Test
	void getFlatsPublic_shouldReturnServiceResponse() {
		GetAllFlatsRequest request = new GetAllFlatsRequest();
		GetAllFlatsResponse expected = new GetAllFlatsResponse();
		expected.setMessage("ok");
		expected.setMessageCode("CODE");
		when(flatServices.getAllFlats(request)).thenReturn(expected);

		GetAllFlatsResponse actual = publicApisController.getFlatsPublic(request);

		assertEquals(expected, actual);
	}

	@Test
	void detDueDetailsForFlatPublic_shouldReturnServiceResponse() throws Exception {
		GetDueAmountForFlatRequest request = new GetDueAmountForFlatRequest();
		GetDueAmountForFlatResponse expected = new GetDueAmountForFlatResponse();
		expected.setMessage("ok");
		expected.setMessageCode("CODE");
		when(flatServices.getDueAmountForFlat(request)).thenReturn(expected);

		GetDueAmountForFlatResponse actual = publicApisController.detDueDetailsForFlatPublic(request);

		assertEquals(expected, actual);
	}

	@Test
	void detDueDetailsForFlatPublic_shouldReturnGenericErrorWhenServiceThrows() throws Exception {
		GetDueAmountForFlatRequest request = new GetDueAmountForFlatRequest();
		when(flatServices.getDueAmountForFlat(request)).thenThrow(new RuntimeException("boom"));

		GetDueAmountForFlatResponse actual = publicApisController.detDueDetailsForFlatPublic(request);

		assertEquals(ErrorMessage.ERR_MESSAGE_33, actual.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_33, actual.getMessageCode());
	}

	@Test
	void payduesPublic_shouldReturnServiceResponse() throws Exception {
		PayDueRequest request = new PayDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		request.setGenericHeader(header);

		PayDueResponse expected = new PayDueResponse();
		expected.setGenericHeader(header);
		expected.setMessage("ok");
		expected.setMessageCode("CODE");
		when(paymentServices.payDues(request)).thenReturn(expected);

		PayDueResponse actual = publicApisController.payduesPublic(request);

		assertEquals(expected, actual);
	}

	@Test
	void payduesPublic_shouldReturnGenericErrorWhenServiceThrows() throws Exception {
		PayDueRequest request = new PayDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		request.setGenericHeader(header);
		when(paymentServices.payDues(request)).thenThrow(new RuntimeException("boom"));

		PayDueResponse actual = publicApisController.payduesPublic(request);

		assertEquals(header, actual.getGenericHeader());
		assertEquals(ErrorMessage.ERR_MESSAGE_33, actual.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_33, actual.getMessageCode());
	}
}
