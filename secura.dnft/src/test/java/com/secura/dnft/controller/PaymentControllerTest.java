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
import com.secura.dnft.request.response.GetPaymentRequest;
import com.secura.dnft.request.response.GetPaymentResponse;
import com.secura.dnft.request.response.LedgerEntryRequest;
import com.secura.dnft.request.response.LedgerEntryResponse;
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
}
