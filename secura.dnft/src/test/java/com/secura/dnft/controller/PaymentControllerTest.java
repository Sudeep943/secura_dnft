package com.secura.dnft.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.GetPaymentRequest;
import com.secura.dnft.request.response.GetPaymentResponse;
import com.secura.dnft.request.response.LedgerEntryRequest;
import com.secura.dnft.request.response.LedgerEntryResponse;
import com.secura.dnft.request.response.PastDueUploadResponse;
import com.secura.dnft.service.PaymentServices;
import com.secura.dnft.service.RazorPayPaymentServices;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

	@Mock
	private RazorPayPaymentServices razorPayPaymentServices;

	@Mock
	private PaymentServices paymentServices;

	@InjectMocks
	private PaymentController paymentController;

	@Test
	void getPayment_shouldReturnServiceResponse() throws Exception {
		GetPaymentRequest request = new GetPaymentRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		request.setGenericHeader(header);

		GetPaymentResponse expected = new GetPaymentResponse();
		expected.setGenericHeader(header);
		expected.setMessage("ok");
		expected.setMessageCode("CODE");
		when(paymentServices.getPayments(request)).thenReturn(expected);

		GetPaymentResponse actual = paymentController.getPayment(request);

		assertEquals(expected, actual);
	}

	@Test
	void getPayment_shouldReturnGenericErrorWhenServiceThrows() throws Exception {
		GetPaymentRequest request = new GetPaymentRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		request.setGenericHeader(header);
		when(paymentServices.getPayments(request)).thenThrow(new RuntimeException("boom"));

		GetPaymentResponse actual = paymentController.getPayment(request);

		assertEquals(header, actual.getGenericHeader());
		assertEquals(ErrorMessage.ERR_MESSAGE_33, actual.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_33, actual.getMessageCode());
	}

	@Test
	void ledgerEntry_shouldReturnServiceResponse() throws Exception {
		LedgerEntryRequest request = new LedgerEntryRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		request.setGenericHeader(header);

		LedgerEntryResponse expected = new LedgerEntryResponse();
		expected.setGenericHeader(header);
		expected.setMessage("ok");
		expected.setMessageCode("CODE");
		when(paymentServices.ledgerEntry(request)).thenReturn(expected);

		LedgerEntryResponse actual = paymentController.ledgerEntry(request);

		assertEquals(expected, actual);
	}

	@Test
	void ledgerEntry_shouldReturnGenericErrorWhenServiceThrows() throws Exception {
		LedgerEntryRequest request = new LedgerEntryRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		request.setGenericHeader(header);
		when(paymentServices.ledgerEntry(request)).thenThrow(new RuntimeException("boom"));

		LedgerEntryResponse actual = paymentController.ledgerEntry(request);

		assertEquals(header, actual.getGenericHeader());
		assertEquals(ErrorMessage.ERR_MESSAGE_33, actual.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_33, actual.getMessageCode());
	}

	@Test
	void uploadPastDue_shouldBuildGenericHeaderFromRequestHeaders() {
		MockMultipartFile file = new MockMultipartFile("file", "past-due.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] { 1, 2, 3 });
		PastDueUploadResponse expected = new PastDueUploadResponse();
		expected.setMessage("ok");
		expected.setMessageCode("CODE");

		when(paymentServices.uploadPastDue(any(), any())).thenAnswer(invocation -> {
			GenericHeader header = invocation.getArgument(1);
			assertEquals("USR-1", header.getUserId());
			assertEquals("APR-1", header.getApartmentId());
			assertEquals("A-101", header.getFlatNo());
			return expected;
		});

		PastDueUploadResponse actual = paymentController.uploadPastDue(file,
				Map.of("user-id", "USR-1", "apartment-id", "APR-1", "flat-no", "A-101"));

		assertEquals(expected, actual);
	}

	@Test
	void uploadPastDue_shouldReturnGenericErrorWhenServiceThrows() {
		MockMultipartFile file = new MockMultipartFile("file", "past-due.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] { 1, 2, 3 });
		when(paymentServices.uploadPastDue(any(), any())).thenThrow(new RuntimeException("boom"));

		PastDueUploadResponse actual = paymentController.uploadPastDue(file, Map.of());

		assertEquals(ErrorMessage.ERR_MESSAGE_33, actual.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_33, actual.getMessageCode());
	}
}
