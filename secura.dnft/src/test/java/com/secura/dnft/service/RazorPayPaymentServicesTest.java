package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.secura.dnft.dao.BankEntityRepository;
import com.secura.dnft.entity.BankEntity;

@ExtendWith(MockitoExtension.class)
class RazorPayPaymentServicesTest {

	@Mock
	private BankEntityRepository bankRepository;

	@Mock
	private GenericService genericService;

	@InjectMocks
	private RazorPayPaymentServices razorPayPaymentServices;

	@Test
	void resolveRazorpayCredentials_shouldUseDynamicCredentialsWhenGatewayIsRazorpay() {
		BankEntity bankEntity = new BankEntity();
		bankEntity.setPgName("encPgName");
		bankEntity.setPgKey("encPgKey");
		bankEntity.setPgSecret("encPgSecret");
		when(bankRepository.findByAprmntIdAndBankDetailsID("APT-1", "BANK-1")).thenReturn(Optional.of(bankEntity));
		when(genericService.decrypt("encPgName")).thenReturn("RAZORPAY");
		when(genericService.decrypt("encPgKey")).thenReturn("dynamicKey");
		when(genericService.decrypt("encPgSecret")).thenReturn("dynamicSecret");

		RazorPayPaymentServices.RazorpayCredentials credentials = razorPayPaymentServices.resolveRazorpayCredentials("APT-1",
				"BANK-1");

		assertEquals("dynamicKey", credentials.key());
		assertEquals("dynamicSecret", credentials.secret());
	}

	@Test
	void resolveRazorpayCredentials_shouldFallbackWhenGatewayIsNotRazorpay() {
		BankEntity bankEntity = new BankEntity();
		bankEntity.setPgName("encPgName");
		when(bankRepository.findByAprmntIdAndBankDetailsID("APT-1", "BANK-1")).thenReturn(Optional.of(bankEntity));
		when(genericService.decrypt("encPgName")).thenReturn("ATOMS");

		RazorPayPaymentServices.RazorpayCredentials credentials = razorPayPaymentServices.resolveRazorpayCredentials("APT-1",
				"BANK-1");

		assertEquals("rzp_test_SRxceBfBqGmeGy", credentials.key());
		assertEquals("RO2SpqK1H96ShjlmkttpA2cX", credentials.secret());
	}

	@Test
	void resolveRazorpayCredentials_shouldFallbackWhenBankIdMissing() {
		RazorPayPaymentServices.RazorpayCredentials credentials = razorPayPaymentServices.resolveRazorpayCredentials("APT-1",
				null);

		assertEquals("rzp_test_SRxceBfBqGmeGy", credentials.key());
		assertEquals("RO2SpqK1H96ShjlmkttpA2cX", credentials.secret());
	}
}
