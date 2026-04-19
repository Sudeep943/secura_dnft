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
import com.secura.dnft.request.response.GetLetterHeadRequest;
import com.secura.dnft.request.response.GetLetterHeadResponse;
import com.secura.dnft.service.MeetingNoticeServices;

@ExtendWith(MockitoExtension.class)
class MeetingNoticeControllerTest {

	@Mock
	private MeetingNoticeServices meetingNoticeServices;

	@InjectMocks
	private MeetingNoticeController meetingNoticeController;

	@Test
	void getLetterHead_shouldReturnServiceResponse() throws Exception {
		GetLetterHeadRequest request = createRequest();
		GetLetterHeadResponse expected = new GetLetterHeadResponse();
		expected.setGenericHeader(request.getGenericHeader());
		expected.setLetterHead("LETTER_HEAD_DATA");
		expected.setMessage("ok");
		expected.setMessageCode("CODE");
		when(meetingNoticeServices.getLetterHead(request)).thenReturn(expected);

		GetLetterHeadResponse actual = meetingNoticeController.getLetterHead(request);

		assertEquals(expected, actual);
	}

	@Test
	void getLetterHead_shouldReturnGenericErrorWhenServiceThrows() throws Exception {
		GetLetterHeadRequest request = createRequest();
		when(meetingNoticeServices.getLetterHead(request)).thenThrow(new RuntimeException("boom"));

		GetLetterHeadResponse actual = meetingNoticeController.getLetterHead(request);

		assertEquals(request.getGenericHeader(), actual.getGenericHeader());
		assertEquals(ErrorMessage.ERR_MESSAGE_33, actual.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_33, actual.getMessageCode());
	}

	private GetLetterHeadRequest createRequest() {
		GetLetterHeadRequest request = new GetLetterHeadRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		request.setGenericHeader(header);
		return request;
	}
}
