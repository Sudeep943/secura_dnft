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
import com.secura.dnft.request.response.GetApartmentDetailsRequest;
import com.secura.dnft.request.response.GetApartmentDetailsResponse;
import com.secura.dnft.request.response.UpdateApartmentDetailsRequest;
import com.secura.dnft.request.response.UpdateApartmentDetailsResponse;
import com.secura.dnft.service.ApartmentService;

@ExtendWith(MockitoExtension.class)
class ApartmentControllerTest {

	@Mock
	private ApartmentService apartmentService;

	@InjectMocks
	private ApartmentController apartmentController;

	@Test
	void updateApatrmentdetails_shouldReturnServiceResponse() {
		UpdateApartmentDetailsRequest request = new UpdateApartmentDetailsRequest();
		GenericHeader header = buildHeader();
		request.setGenericHeader(header);
		UpdateApartmentDetailsResponse expected = new UpdateApartmentDetailsResponse();
		expected.setGenericHeader(header);
		expected.setMessage("ok");
		expected.setMessageCode("CODE");
		when(apartmentService.updateApatrmentdetails(request)).thenReturn(expected);

		UpdateApartmentDetailsResponse actual = apartmentController.updateApatrmentdetails(request);

		assertEquals(expected, actual);
	}

	@Test
	void getApatrmentdetails_shouldReturnGenericErrorWhenServiceThrows() {
		GetApartmentDetailsRequest request = new GetApartmentDetailsRequest();
		GenericHeader header = buildHeader();
		request.setGenericHeader(header);
		when(apartmentService.getApatrmentdetails(request)).thenThrow(new RuntimeException("boom"));

		GetApartmentDetailsResponse actual = apartmentController.getApatrmentdetails(request);

		assertEquals(header, actual.getGenericHeader());
		assertEquals(ErrorMessage.ERR_MESSAGE_33, actual.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_33, actual.getMessageCode());
	}

	private GenericHeader buildHeader() {
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		header.setUserId("ADMIN-1");
		header.setRole("ADMIN");
		return header;
	}
}
