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
import com.secura.dnft.request.response.UpdateDiscfinRequest;
import com.secura.dnft.request.response.UpdateDiscfinResponse;
import com.secura.dnft.service.DiscFinServices;

@ExtendWith(MockitoExtension.class)
class DiscFinControllerTest {

	@Mock
	private DiscFinServices discFinServices;

	@InjectMocks
	private DiscFinController discFinController;

	@Test
	void updateDiscfin_shouldReturnServiceResponse() throws Exception {
		UpdateDiscfinRequest request = new UpdateDiscfinRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		request.setGenericHeader(header);

		UpdateDiscfinResponse expected = new UpdateDiscfinResponse();
		expected.setGenericHeader(header);
		expected.setMessage("ok");
		expected.setMessageCode("CODE");
		when(discFinServices.updateDiscfin(request)).thenReturn(expected);

		UpdateDiscfinResponse actual = discFinController.updateDiscfin(request);

		assertEquals(expected, actual);
	}

	@Test
	void updateDiscfin_shouldReturnGenericErrorWhenServiceThrows() throws Exception {
		UpdateDiscfinRequest request = new UpdateDiscfinRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		request.setGenericHeader(header);
		when(discFinServices.updateDiscfin(request)).thenThrow(new RuntimeException("boom"));

		UpdateDiscfinResponse actual = discFinController.updateDiscfin(request);

		assertEquals(header, actual.getGenericHeader());
		assertEquals(ErrorMessage.ERR_MESSAGE_33, actual.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_33, actual.getMessageCode());
	}
}
