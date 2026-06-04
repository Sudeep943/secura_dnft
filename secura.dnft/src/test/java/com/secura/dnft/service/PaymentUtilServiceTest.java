package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.dao.DueAmountDetailsRepository;
import com.secura.dnft.dao.FlatRepository;
import com.secura.dnft.dao.PaymentRepository;
import com.secura.dnft.dao.TransactionRepository;
import com.secura.dnft.entity.DueAmountDetailsEntity;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.entity.Transaction;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.GetPaymentUtilDetailsRequest;
import com.secura.dnft.request.response.GetPaymentUtilDetailsResponse;

@ExtendWith(MockitoExtension.class)
class PaymentUtilServiceTest {

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private TransactionRepository transactionRepository;

	@Mock
	private DueAmountDetailsRepository dueAmountDetailsRepository;

	@Mock
	private FlatRepository flatRepository;

	@Mock
	private GenericService genericService;

	@InjectMocks
	private PaymentUtilService paymentUtilService;

	@Test
	void getPaymentDetails_shouldAggregateFlatDetailsUsingHighestCycle() {
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		GetPaymentUtilDetailsRequest request = new GetPaymentUtilDetailsRequest();
		request.setGenericHeader(header);
		request.setPaymentId("PAY-1");
		request.setFlatId("FLAT-1");

		PaymentEntity monthlyPayment = new PaymentEntity();
		monthlyPayment.setPaymentId("PAY-1");
		monthlyPayment.setAprmtId("APR-1");
		monthlyPayment.setPaymentCollectionCycle("MONTHLY");

		PaymentEntity yearlyPayment = new PaymentEntity();
		yearlyPayment.setPaymentId("PAY-1");
		yearlyPayment.setAprmtId("APR-1");
		yearlyPayment.setPaymentCollectionCycle("YEARLY");

		when(paymentRepository.findByPaymentIdAndAprmtId("PAY-1", "APR-1"))
				.thenReturn(List.of(monthlyPayment, yearlyPayment));

		Transaction successOne = new Transaction();
		successOne.setTrnsAmt("100");
		Transaction successTwo = new Transaction();
		successTwo.setTrnsAmt("40");
		Transaction pending = new Transaction();
		pending.setTrnsAmt("20");

		when(transactionRepository.findByAprmntIdAndPymntIdAndFlatIdAndTrnsStatus("APR-1", "PAY-1", "FLAT-1", "SUCCESS"))
				.thenReturn(List.of(successOne, successTwo));
		when(transactionRepository.findByAprmntIdAndPymntIdAndFlatIdAndTrnsStatus("APR-1", "PAY-1", "FLAT-1", "PENDING"))
				.thenReturn(List.of(pending));

		DueAmountDetailsEntity yearlyDueOne = new DueAmountDetailsEntity();
		yearlyDueOne.setCollectionCycle("YEARLY");
		yearlyDueOne.setTotalAmount("500");
		yearlyDueOne.setApplicableFlats("[\"FLAT-1\"]");

		DueAmountDetailsEntity yearlyDueTwo = new DueAmountDetailsEntity();
		yearlyDueTwo.setCollectionCycle("YEARLY");
		yearlyDueTwo.setTotalAmount("300");
		yearlyDueTwo.setApplicableFlats("[\"FLAT-1\"]");

		DueAmountDetailsEntity monthlyDue = new DueAmountDetailsEntity();
		monthlyDue.setCollectionCycle("MONTHLY");
		monthlyDue.setTotalAmount("50");
		monthlyDue.setApplicableFlats("[\"FLAT-1\"]");

		when(dueAmountDetailsRepository.findByPaymentId("PAY-1"))
				.thenReturn(List.of(yearlyDueOne, yearlyDueTwo, monthlyDue));
		when(genericService.fromJson(eq("[\"FLAT-1\"]"), any(TypeReference.class))).thenReturn(List.of("FLAT-1"));

		GetPaymentUtilDetailsResponse response = paymentUtilService.getPaymentDetails(request);

		assertSame(yearlyPayment, response.getPaymentEntity());
		assertEquals("800", response.getExpectedCollection());
		assertEquals("140", response.getTotalCollection());
		assertEquals("20", response.getTotalPendingTransactionAmount());
		assertEquals("17.5", response.getCollectionPercentage());
		assertEquals(SuccessMessage.SUCC_MESSAGE_37, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_37, response.getMessageCode());
	}

	@Test
	void getPaymentDetails_shouldUseApartmentTransactionsWhenFlatMissing() {
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		GetPaymentUtilDetailsRequest request = new GetPaymentUtilDetailsRequest();
		request.setGenericHeader(header);
		request.setPaymentId("PAY-1");

		PaymentEntity payment = new PaymentEntity();
		payment.setPaymentId("PAY-1");
		payment.setAprmtId("APR-1");
		payment.setPaymentCollectionCycle("MONTHLY");
		when(paymentRepository.findByPaymentIdAndAprmtId("PAY-1", "APR-1")).thenReturn(List.of(payment));

		Transaction success = new Transaction();
		success.setTrnsAmt("125");
		Transaction pending = new Transaction();
		pending.setTrnsAmt("35");
		when(transactionRepository.findByAprmntIdAndPymntIdAndTrnsStatus("APR-1", "PAY-1", "SUCCESS"))
				.thenReturn(List.of(success));
		when(transactionRepository.findByAprmntIdAndPymntIdAndTrnsStatus("APR-1", "PAY-1", "PENDING"))
				.thenReturn(List.of(pending));

		GetPaymentUtilDetailsResponse response = paymentUtilService.getPaymentDetails(request);

		assertSame(payment, response.getPaymentEntity());
		assertEquals("0", response.getExpectedCollection());
		assertEquals("125", response.getTotalCollection());
		assertEquals("35", response.getTotalPendingTransactionAmount());
		assertEquals("0", response.getCollectionPercentage());
		assertEquals(SuccessMessage.SUCC_MESSAGE_37, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_37, response.getMessageCode());
	}

	@Test
	void getPaymentDetails_shouldRequireApartmentId() {
		GetPaymentUtilDetailsRequest request = new GetPaymentUtilDetailsRequest();

		GetPaymentUtilDetailsResponse response = paymentUtilService.getPaymentDetails(request);

		assertNull(response.getPaymentEntity());
		assertEquals("0", response.getExpectedCollection());
		assertEquals("0", response.getTotalCollection());
		assertEquals("0", response.getTotalPendingTransactionAmount());
		assertEquals("0", response.getCollectionPercentage());
		assertEquals(ErrorMessage.ERR_MESSAGE_05, response.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_05, response.getMessageCode());
	}

	@Test
	void getPaymentDetails_shouldMatchFlatAreaBeforeSelectingHighestCycleForPerSqftPayments() {
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		GetPaymentUtilDetailsRequest request = new GetPaymentUtilDetailsRequest();
		request.setGenericHeader(header);
		request.setPaymentId("PAY-1");
		request.setFlatId("FLAT-1");

		PaymentEntity payment = new PaymentEntity();
		payment.setPaymentId("PAY-1");
		payment.setAprmtId("APR-1");
		payment.setPaymentCollectionCycle("YEARLY");
		payment.setPaymentCapita("PER_SQFT");
		when(paymentRepository.findByPaymentIdAndAprmtId("PAY-1", "APR-1")).thenReturn(List.of(payment));

		Flat flat = new Flat();
		flat.setFlatNo("FLAT-1");
		flat.setFlatArea("SMALL_2_BHK");
		when(flatRepository.findByAprmntIdAndFlatNo(any(), "FLAT-1")).thenReturn(java.util.Optional.of(flat));

		DueAmountDetailsEntity yearlyDueForOtherArea = new DueAmountDetailsEntity();
		yearlyDueForOtherArea.setCollectionCycle("YEARLY");
		yearlyDueForOtherArea.setFlatArea("LARGE_3_BHK");
		yearlyDueForOtherArea.setTotalAmount("500");
		yearlyDueForOtherArea.setApplicableFlats("[\"FLAT-1\"]");

		DueAmountDetailsEntity monthlyDueForFlatArea = new DueAmountDetailsEntity();
		monthlyDueForFlatArea.setCollectionCycle("MONTHLY");
		monthlyDueForFlatArea.setFlatArea("SMALL_2_BHK");
		monthlyDueForFlatArea.setTotalAmount("75");
		monthlyDueForFlatArea.setApplicableFlats("[\"FLAT-1\"]");

		when(dueAmountDetailsRepository.findByPaymentId("PAY-1"))
				.thenReturn(List.of(yearlyDueForOtherArea, monthlyDueForFlatArea));
		when(genericService.fromJson(eq("[\"FLAT-1\"]"), any(TypeReference.class))).thenReturn(List.of("FLAT-1"));
		when(transactionRepository.findByAprmntIdAndPymntIdAndFlatIdAndTrnsStatus("APR-1", "PAY-1", "FLAT-1", "SUCCESS"))
				.thenReturn(List.of());
		when(transactionRepository.findByAprmntIdAndPymntIdAndFlatIdAndTrnsStatus("APR-1", "PAY-1", "FLAT-1", "PENDING"))
				.thenReturn(List.of());

		GetPaymentUtilDetailsResponse response = paymentUtilService.getPaymentDetails(request);

		assertEquals("75", response.getExpectedCollection());
		assertEquals("0", response.getTotalCollection());
		assertEquals("0", response.getTotalPendingTransactionAmount());
		assertEquals("0", response.getCollectionPercentage());
	}
}
