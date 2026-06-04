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
import com.secura.dnft.request.response.GetAllFlatsRequest;
import com.secura.dnft.request.response.GetAllFlatsResponse;
import com.secura.dnft.request.response.GetDueAmountForFlatRequest;
import com.secura.dnft.request.response.GetDueAmountForFlatResponse;
import com.secura.dnft.request.response.GetOwnerRequest;
import com.secura.dnft.request.response.GetOwnerResponse;
import com.secura.dnft.request.response.PayDueRequest;
import com.secura.dnft.request.response.PayDueResponse;
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
import com.secura.dnft.service.AtomsPaymentServices;
import com.secura.dnft.service.FlatServices;
import com.secura.dnft.service.PaymentServices;
import com.secura.dnft.service.ProfileServices;
import com.secura.dnft.service.RazorPayPaymentServices;

@ExtendWith(MockitoExtension.class)
class PublicApisControllerTest {

	@Mock
	private FlatServices flatServices;

	@Mock
	private PaymentServices paymentServices;

	@Mock
	private RazorPayPaymentServices razorPayPaymentServices;
	
	@Mock
	private AtomsPaymentServices atomsPaymentServices;

	@Mock
	private ProfileServices profileServices;

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
	void getDueDetailsForFlatPublic_shouldReturnServiceResponse() throws Exception {
		GetDueAmountForFlatRequest request = new GetDueAmountForFlatRequest();
		GetDueAmountForFlatResponse expected = new GetDueAmountForFlatResponse();
		expected.setMessage("ok");
		expected.setMessageCode("CODE");
		when(flatServices.getDueAmountForFlat(request)).thenReturn(expected);

		GetDueAmountForFlatResponse actual = publicApisController.getDueDetailsForFlatPublic(request);

		assertEquals(expected, actual);
	}

	@Test
	void getDueDetailsForFlatPublic_shouldReturnGenericErrorWhenServiceThrows() throws Exception {
		GetDueAmountForFlatRequest request = new GetDueAmountForFlatRequest();
		when(flatServices.getDueAmountForFlat(request)).thenThrow(new RuntimeException("boom"));

		GetDueAmountForFlatResponse actual = publicApisController.getDueDetailsForFlatPublic(request);

		assertEquals(ErrorMessage.ERR_MESSAGE_33, actual.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_33, actual.getMessageCode());
	}

	@Test
	void payDuesPublic_shouldReturnServiceResponse() throws Exception {
		PayDueRequest request = new PayDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		request.setGenericHeader(header);

		PayDueResponse expected = new PayDueResponse();
		expected.setGenericHeader(header);
		expected.setMessage("ok");
		expected.setMessageCode("CODE");
		when(paymentServices.payDues(request)).thenReturn(expected);

		PayDueResponse actual = publicApisController.payDuesPublic(request);

		assertEquals(expected, actual);
	}

	@Test
	void payDuesPublic_shouldReturnGenericErrorWhenServiceThrows() throws Exception {
		PayDueRequest request = new PayDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		request.setGenericHeader(header);
		when(paymentServices.payDues(request)).thenThrow(new RuntimeException("boom"));

		PayDueResponse actual = publicApisController.payDuesPublic(request);

		assertEquals(header, actual.getGenericHeader());
		assertEquals(ErrorMessage.ERR_MESSAGE_33, actual.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_33, actual.getMessageCode());
	}

	@Test
	void getOwnerPublic_shouldReturnServiceResponse() {
		GetOwnerRequest request = new GetOwnerRequest();
		GetOwnerResponse expected = new GetOwnerResponse();
		expected.setMessage("ok");
		expected.setMessageCode("CODE");
		when(profileServices.getOwner(request)).thenReturn(expected);

		GetOwnerResponse actual = publicApisController.getOwnerPublic(request);

		assertEquals(expected, actual);
	}

	@Test
	void createOrderPublic_shouldReturnServiceResponse() {
		PaymentGayewayOrderRequest request = new PaymentGayewayOrderRequest();
		request.setPaymentGateway("RAZORPAY");
		PaymentGayewayOrderResponse expected = new PaymentGayewayOrderResponse();
		expected.setMessage("ok");
		expected.setMessageCode("CODE");
		when(razorPayPaymentServices.createOrder(request)).thenReturn(expected);

		PaymentGayewayOrderResponse actual = publicApisController.createOrderPublic(request);

		assertEquals(expected, actual);
	}

	@Test
	void verifyPaymentPublic_shouldReturnServiceResponse() {
		PaymentGayewayOrderVerificationRequest request = new PaymentGayewayOrderVerificationRequest();
		request.setPaymentGateway("RAZORPAY");
		request.setData(Map.of(
				"razorpay_order_id", "order_123",
				"razorpay_payment_id", "pay_123",
				"razorpay_signature", "sig_123"));

		PaymentGayewayOrderVerificationResponse expected = new PaymentGayewayOrderVerificationResponse();
		expected.setMessage("ok");
		expected.setMessageCode("CODE");
		when(razorPayPaymentServices.verifypayment(request)).thenReturn(expected);

		PaymentGayewayOrderVerificationResponse actual = publicApisController.verifyPaymentPublic(request);

		assertEquals(expected, actual);
	}
	
	@Test
	void createOrderPublic_shouldRouteToAtomsWhenGatewayIsAtoms() {
		PaymentGayewayOrderRequest request = new PaymentGayewayOrderRequest();
		request.setPaymentGateway("ATOMS");
		PaymentGayewayOrderResponse expected = new PaymentGayewayOrderResponse();
		expected.setMessage("ok");
		when(atomsPaymentServices.createOrder(request)).thenReturn(expected);

		PaymentGayewayOrderResponse actual = publicApisController.createOrderPublic(request);

		assertEquals(expected, actual);
	}
	
	@Test
	void payOrderPublic_shouldRouteToAtomsWhenGatewayIsAtoms() {
		PaymentGayewayPayOrderRequest request = new PaymentGayewayPayOrderRequest();
		request.setPaymentGateway("ATOMS");
		PaymentGayewayPayOrderResponse expected = new PaymentGayewayPayOrderResponse();
		expected.setMessage("ok");
		when(atomsPaymentServices.payorder(request)).thenReturn(expected);

		PaymentGayewayPayOrderResponse actual = publicApisController.payOrderPublic(request);

		assertEquals(expected, actual);
	}
	
	@Test
	void processRefundPublic_shouldRouteToRazorpayWhenGatewayIsRazorpay() {
		PaymentGayewayProcessRefundRequest request = new PaymentGayewayProcessRefundRequest();
		request.setPaymentGateway("RAZORPAY");
		PaymentGayewayProcessRefundResponse expected = new PaymentGayewayProcessRefundResponse();
		expected.setMessage("ok");
		when(razorPayPaymentServices.processRefund(request)).thenReturn(expected);

		PaymentGayewayProcessRefundResponse actual = publicApisController.processRefundPublic(request);

		assertEquals(expected, actual);
	}
	
	@Test
	void getPaymentDetailsPublic_shouldRouteToRazorpayWhenGatewayIsRazorpay() {
		PaymentGayewayPaymentDetailRequest request = new PaymentGayewayPaymentDetailRequest();
		request.setPaymentGateway("RAZORPAY");
		PaymentGayewayPaymentDetailResponse expected = new PaymentGayewayPaymentDetailResponse();
		expected.setId("pay_123");
		when(razorPayPaymentServices.getPaymentDetails(request)).thenReturn(expected);

		PaymentGayewayPaymentDetailResponse actual = publicApisController.getPaymentDetailsPublic(request);

		assertEquals(expected, actual);
	}
}
