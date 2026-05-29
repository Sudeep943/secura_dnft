package com.secura.dnft.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Map;

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
import com.secura.dnft.request.response.GetPaymentUtilDetailsRequest;
import com.secura.dnft.request.response.GetPaymentUtilDetailsResponse;
import com.secura.dnft.request.response.GenericResponse;
import com.secura.dnft.request.response.LedgerEntryRequest;
import com.secura.dnft.request.response.LedgerEntryResponse;
import com.secura.dnft.request.response.PaymentGayewayOrderRequest;
import com.secura.dnft.request.response.PaymentGayewayOrderResponse;
import com.secura.dnft.request.response.PaymentGayewayOrderVerificationRequest;
import com.secura.dnft.request.response.PaymentGayewayOrderVerificationResponse;
import com.secura.dnft.request.response.PaymentGayewayPayOrderRequest;
import com.secura.dnft.request.response.PaymentGayewayPayOrderResponse;
import com.secura.dnft.request.response.PaymentGayewayPaymentDetailRequest;
import com.secura.dnft.request.response.PaymentGayewayPaymentDetailResponse;
import com.secura.dnft.request.response.PaymentGayewayProcessRefundRequest;
import com.secura.dnft.request.response.PaymentGayewayProcessRefundResponse;
import com.secura.dnft.request.response.UploadPastDueRequest;
import com.secura.dnft.request.response.UploadPastDueResponse;
import com.secura.dnft.request.response.ValidatePriorDuePaymnentRequest;
import com.secura.dnft.service.AtomsPaymentServices;
import com.secura.dnft.service.PaymentServices;
import com.secura.dnft.service.PaymentUtilService;
import com.secura.dnft.service.RazorPayPaymentServices;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

	@Mock
	private RazorPayPaymentServices razorPayPaymentServices;
	
	@Mock
	private AtomsPaymentServices atomsPaymentServices;

	@Mock
	private PaymentServices paymentServices;

	@Mock
	private PaymentUtilService paymentUtilService;

	@InjectMocks
	private PaymentController paymentController;
	
	@Test
	void createOrder_shouldRouteToAtomsWhenGatewayIsAtoms() {
		PaymentGayewayOrderRequest request = new PaymentGayewayOrderRequest();
		request.setPaymentGateway("ATOMS");
		PaymentGayewayOrderResponse expected = new PaymentGayewayOrderResponse();
		expected.setMessage("ok");
		when(atomsPaymentServices.createOrder(request)).thenReturn(expected);

		PaymentGayewayOrderResponse actual = paymentController.createOrder(request);

		assertEquals(expected, actual);
	}
	
	@Test
	void verifyPayment_shouldRouteToRazorpayWhenGatewayIsRazorpay() {
		PaymentGayewayOrderVerificationRequest request = new PaymentGayewayOrderVerificationRequest();
		request.setPaymentGateway("RAZORPAY");
		request.setData(Map.of(
				"razorpay_order_id", "order_123",
				"razorpay_payment_id", "pay_123",
				"razorpay_signature", "sig_123"));
		PaymentGayewayOrderVerificationResponse expected = new PaymentGayewayOrderVerificationResponse();
		expected.setMessage("ok");
		when(razorPayPaymentServices.verifypayment(request)).thenReturn(expected);

		PaymentGayewayOrderVerificationResponse actual = paymentController.verifyPayment(request);

		assertEquals(expected, actual);
	}
	
	@Test
	void payOrder_shouldRouteToAtomsWhenGatewayIsAtoms() {
		PaymentGayewayPayOrderRequest request = new PaymentGayewayPayOrderRequest();
		request.setPaymentGateway("ATOMS");
		PaymentGayewayPayOrderResponse expected = new PaymentGayewayPayOrderResponse();
		expected.setMessage("ok");
		when(atomsPaymentServices.payorder(request)).thenReturn(expected);

		PaymentGayewayPayOrderResponse actual = paymentController.payOrder(request);

		assertEquals(expected, actual);
	}
	
	@Test
	void processRefund_shouldRouteToRazorpayWhenGatewayIsRazorpay() {
		PaymentGayewayProcessRefundRequest request = new PaymentGayewayProcessRefundRequest();
		request.setPaymentGateway("RAZORPAY");
		PaymentGayewayProcessRefundResponse expected = new PaymentGayewayProcessRefundResponse();
		expected.setMessage("ok");
		when(razorPayPaymentServices.processRefund(request)).thenReturn(expected);

		PaymentGayewayProcessRefundResponse actual = paymentController.processRefund(request);

		assertEquals(expected, actual);
	}
	
	@Test
	void getPaymentDetails_shouldRouteToRazorpayWhenGatewayIsRazorpay() {
		PaymentGayewayPaymentDetailRequest request = new PaymentGayewayPaymentDetailRequest();
		request.setPaymentGateway("RAZORPAY");
		PaymentGayewayPaymentDetailResponse expected = new PaymentGayewayPaymentDetailResponse();
		expected.setId("pay_123");
		when(razorPayPaymentServices.getPaymentDetails(request)).thenReturn(expected);

		PaymentGayewayPaymentDetailResponse actual = paymentController.getPaymentDetails(request);

		assertEquals(expected, actual);
	}

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
	void getPaymentUtilDetails_shouldReturnServiceResponse() {
		GetPaymentUtilDetailsRequest request = new GetPaymentUtilDetailsRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		request.setGenericHeader(header);
		request.setPaymentId("PAY-1");

		GetPaymentUtilDetailsResponse expected = new GetPaymentUtilDetailsResponse();
		expected.setGenericHeader(header);
		expected.setMessage("ok");
		when(paymentUtilService.getPaymentDetails(request)).thenReturn(expected);

		GetPaymentUtilDetailsResponse actual = paymentController.getPaymentUtilDetails(request);

		assertEquals(expected, actual);
	}

	@Test
	void getPaymentUtilDetails_shouldReturnGenericErrorWhenServiceThrows() {
		GetPaymentUtilDetailsRequest request = new GetPaymentUtilDetailsRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		request.setGenericHeader(header);
		request.setPaymentId("PAY-1");
		when(paymentUtilService.getPaymentDetails(request)).thenThrow(new RuntimeException("boom"));

		GetPaymentUtilDetailsResponse actual = paymentController.getPaymentUtilDetails(request);

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
	void uploadPastDue_shouldReturnServiceResponse() throws Exception {
		UploadPastDueRequest request = new UploadPastDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		request.setGenericHeader(header);
		request.setFile("base64");

		UploadPastDueResponse expected = new UploadPastDueResponse();
		expected.setGenericHeader(header);
		expected.setMessage("ok");
		expected.setMessageCode("CODE");
		when(paymentServices.uploadPastDue(request)).thenReturn(expected);

		UploadPastDueResponse actual = paymentController.uploadPastDue(request);

		assertEquals(expected, actual);
	}

	@Test
	void uploadPastDue_shouldReturnGenericErrorWhenServiceThrows() throws Exception {
		UploadPastDueRequest request = new UploadPastDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		request.setGenericHeader(header);
		request.setFile("base64");
		when(paymentServices.uploadPastDue(request)).thenThrow(new RuntimeException("boom"));

		UploadPastDueResponse actual = paymentController.uploadPastDue(request);

		assertEquals(header, actual.getGenericHeader());
		assertEquals(ErrorMessage.ERR_MESSAGE_33, actual.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_33, actual.getMessageCode());
	}

	@Test
	void validatePriorDuePaymnent_shouldReturnServiceResponse() throws Exception {
		ValidatePriorDuePaymnentRequest request = new ValidatePriorDuePaymnentRequest();
		GenericResponse expected = new GenericResponse();
		expected.setMessage("ok");
		expected.setMessageCode("CODE");
		when(paymentServices.validatePriorDuePaymnent(request)).thenReturn(expected);

		GenericResponse actual = paymentController.validatePriorDuePaymnent(request);

		assertEquals(expected, actual);
	}

	@Test
	void validatePriorDuePaymnent_shouldReturnGenericErrorWhenServiceThrows() throws Exception {
		ValidatePriorDuePaymnentRequest request = new ValidatePriorDuePaymnentRequest();
		when(paymentServices.validatePriorDuePaymnent(request)).thenThrow(new RuntimeException("boom"));

		GenericResponse actual = paymentController.validatePriorDuePaymnent(request);

		assertEquals(ErrorMessage.ERR_MESSAGE_33, actual.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_33, actual.getMessageCode());
	}
}
