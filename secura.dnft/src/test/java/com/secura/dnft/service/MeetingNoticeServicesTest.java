package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.secura.dnft.dao.ApartmentRepository;
import com.secura.dnft.dao.DocumentRepository;
import com.secura.dnft.dao.NoticeRepository;
import com.secura.dnft.entity.ApartmentMaster;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.GetLetterHeadRequest;
import com.secura.dnft.request.response.GetLetterHeadResponse;

@ExtendWith(MockitoExtension.class)
class MeetingNoticeServicesTest {

	@Mock
	private GenericService genericService;

	@Mock
	private NoticeRepository noticeRepository;

	@Mock
	private DocumentRepository documentRepository;

	@Mock
	private ApartmentRepository apartmentRepository;

	@InjectMocks
	private MeetingNoticeServices meetingNoticeServices;

	@Test
	void getLetterHead_shouldReturnApartmentLetterHead() throws Exception {
		GetLetterHeadRequest request = createRequest();
		ApartmentMaster apartment = new ApartmentMaster();
		apartment.setAprmntId("APR-1");
		apartment.setAprmntLetterHead("LETTER_HEAD_DATA");
		when(apartmentRepository.findById("APR-1")).thenReturn(Optional.of(apartment));

		GetLetterHeadResponse response = meetingNoticeServices.getLetterHead(request);

		assertEquals(request.getGenericHeader(), response.getGenericHeader());
		assertEquals("LETTER_HEAD_DATA", response.getLetterHead());
		assertEquals(SuccessMessage.SUCC_MESSAGE_36, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_36, response.getMessageCode());
	}

	@Test
	void getLetterHead_shouldHandleMissingApartment() throws Exception {
		GetLetterHeadRequest request = createRequest();
		when(apartmentRepository.findById("APR-1")).thenReturn(Optional.empty());

		GetLetterHeadResponse response = meetingNoticeServices.getLetterHead(request);

		assertNull(response.getLetterHead());
		assertEquals(SuccessMessage.SUCC_MESSAGE_36, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_36, response.getMessageCode());
	}

	private GetLetterHeadRequest createRequest() {
		GetLetterHeadRequest request = new GetLetterHeadRequest();
		GenericHeader genericHeader = new GenericHeader();
		genericHeader.setApartmentId("APR-1");
		request.setGenericHeader(genericHeader);
		return request;
	}
}
