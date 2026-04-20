package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.dao.FlatRepository;
import com.secura.dnft.dao.PaymentRepository;
import com.secura.dnft.dao.TransactionRepository;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.entity.Transaction;
import com.secura.dnft.entity.Worklist;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.interfaceservice.FlatInterface;
import com.secura.dnft.request.response.AddedCharges;
import com.secura.dnft.request.response.CreateReceiptRequest;
import com.secura.dnft.request.response.CreateReceiptResponse;
import com.secura.dnft.request.response.CreatePaymentRequest;
import com.secura.dnft.request.response.DiscFinReceipt;
import com.secura.dnft.request.response.DueAmountDetails;
import com.secura.dnft.request.response.DuePaymentAmountDetailsRequest;
import com.secura.dnft.request.response.DuePaymentAmountDetailsResponse;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.GetDueAmountForFlatResponse;
import com.secura.dnft.request.response.GetDueAmountForPerHeadCalculationResponse;
import com.secura.dnft.request.response.GetDuePaymentAmountDetailsResponse;
import com.secura.dnft.request.response.GetPaymentRequest;
import com.secura.dnft.request.response.GetPaymentResponse;
import com.secura.dnft.request.response.LedgerEntryRequest;
import com.secura.dnft.request.response.LedgerEntryResponse;
import com.secura.dnft.request.response.PayDueRequest;
import com.secura.dnft.request.response.PayDueResponse;
import com.secura.dnft.request.response.PaymentTenderData;

@ExtendWith(MockitoExtension.class)
class PaymentServicesTest {

	@Mock
	private GenericService genericService;

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private FlatRepository flatRepository;

	@Mock
	private FlatInterface flatInterface;

	@Mock
	private TransactionRepository transactionRepository;

	@Mock
	private ReceiptServices receiptServices;

	@InjectMocks
	private PaymentServices paymentServices;

	@Test
	void getPayments_shouldReturnApartmentPaymentsWhenPaymentIdMissing() throws Exception {
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		GetPaymentRequest request = new GetPaymentRequest();
		request.setGenericHeader(header);

		PaymentEntity payment = new PaymentEntity();
		payment.setPaymentId("PAY-1");
		payment.setAprmtId("APR-1");
		when(paymentRepository.findByAprmtId("APR-1")).thenReturn(List.of(payment));

		GetPaymentResponse response = paymentServices.getPayments(request);

		assertEquals(1, response.getPaymentList().size());
		assertEquals("PAY-1", response.getPaymentList().get(0).getPaymentId());
		assertEquals(SuccessMessage.SUCC_MESSAGE_37, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_37, response.getMessageCode());
	}

	@Test
	void getPayments_shouldReturnMatchingPaymentWhenPaymentIdPresent() throws Exception {
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		GetPaymentRequest request = new GetPaymentRequest();
		request.setGenericHeader(header);
		request.setPaymentId("PAY-1");

		PaymentEntity payment = new PaymentEntity();
		payment.setPaymentId("PAY-1");
		payment.setAprmtId("APR-1");
		when(paymentRepository.findById("PAY-1")).thenReturn(Optional.of(payment));

		GetPaymentResponse response = paymentServices.getPayments(request);

		assertEquals(1, response.getPaymentList().size());
		assertEquals("PAY-1", response.getPaymentList().get(0).getPaymentId());
		assertEquals(SuccessMessage.SUCC_MESSAGE_37, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_37, response.getMessageCode());
	}

	@Test
	void getPayments_shouldReturnNoDataMessageWhenPaymentDoesNotMatchApartment() throws Exception {
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		GetPaymentRequest request = new GetPaymentRequest();
		request.setGenericHeader(header);
		request.setPaymentId("PAY-1");

		PaymentEntity payment = new PaymentEntity();
		payment.setPaymentId("PAY-1");
		payment.setAprmtId("APR-2");
		when(paymentRepository.findById("PAY-1")).thenReturn(Optional.of(payment));

		GetPaymentResponse response = paymentServices.getPayments(request);

		assertTrue(response.getPaymentList().isEmpty());
		assertEquals(SuccessMessage.SUCC_MESSAGE_38, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_38, response.getMessageCode());
	}

	@Test
	void getPayments_shouldRequireApartmentId() throws Exception {
		GetPaymentRequest request = new GetPaymentRequest();

		GetPaymentResponse response = paymentServices.getPayments(request);

		assertTrue(response.getPaymentList().isEmpty());
		assertEquals(com.secura.dnft.generic.bean.ErrorMessage.ERR_MESSAGE_05, response.getMessage());
		assertEquals(com.secura.dnft.generic.bean.ErrorMessageCode.ERR_MESSAGE_05, response.getMessageCode());
	}

	@Test
	void getDuePaymentAmountDetails_shouldReturnPostYearlyDueDateAsEndPlusOneDayAndGstAmounts() {
		DuePaymentAmountDetailsRequest request = new DuePaymentAmountDetailsRequest();
		request.setPaymentAmount("15000");
		request.setGst("10");
		request.setCollectionStartDate(LocalDate.parse("2026-03-01"));
		request.setCollectionEndDate(LocalDate.parse("2027-02-28"));
		request.setPaymentCollectionCycle("yearly");
		request.setPaymentCollectionMode("POST");
		request.setPaymentCapita("PER_FLAT");
		request.setTodayDate(LocalDate.parse("2026-04-05"));

		DuePaymentAmountDetailsResponse response = paymentServices.getDuePaymentAmountDetails(request);

		assertEquals(LocalDate.parse("2027-03-01"), response.getDueDate());
		assertEquals("15000", response.getAmountExcludingGst());
		assertEquals("1500", response.getGstAmount());
		assertEquals("16500", response.getAmountIncludingGst());
	}

	@Test
	void getDuePaymentAmountDetails_shouldApplyGstOnMonthlyPostWhenWithinCollectionRange() {
		DuePaymentAmountDetailsRequest request = new DuePaymentAmountDetailsRequest();
		request.setPaymentAmount("1000");
		request.setGst("10");
		request.setCollectionStartDate(LocalDate.parse("2026-03-01"));
		request.setCollectionEndDate(LocalDate.parse("2026-12-31"));
		request.setPaymentCollectionCycle("monthly");
		request.setPaymentCollectionMode("POST");
		request.setPaymentCapita("PER_FLAT");
		request.setTodayDate(LocalDate.parse("2026-03-15"));

		DuePaymentAmountDetailsResponse response = paymentServices.getDuePaymentAmountDetails(request);

		assertEquals(LocalDate.parse("2026-04-01"), response.getDueDate());
		assertEquals("1000", response.getAmountExcludingGst());
		assertEquals("100", response.getGstAmount());
		assertEquals("1100", response.getAmountIncludingGst());
		assertEquals(SuccessMessage.SUCC_MESSAGE_28, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_28, response.getMessageCode());
	}

	@Test
	void getDuePaymentAmountDetails_shouldReturnAllDueAmountDetailsFromCreatePaymentRequest() {
		CreatePaymentRequest request = new CreatePaymentRequest();
		request.setPaymentName("Maintenance");
		request.setPaymentType("MANDATORY");
		request.setPaymentCapita("PER_FLAT");
		request.setAllowedPaymentModes(List.of("UPI", "CARD"));
		request.setEventPayment(true);
		request.setPaymentAmount("1000");
		request.setGst("10");
		request.setCollectionStartDate(Date.valueOf(LocalDate.now()));
		request.setCollectionEndDate(Date.valueOf(LocalDate.now().plusMonths(2)));
		request.setPaymentCollectionCycle("monthly");
		request.setPaymentCollectionMode("pre");

		GetDuePaymentAmountDetailsResponse response = paymentServices.getDuePaymentAmountDetails(request);
		assertNotNull(response.getListOfDueAmountDetails());
		assertEquals(3, response.getListOfDueAmountDetails().size());
		assertEquals("1000", response.getListOfDueAmountDetails().get(0).getAmount());
		assertEquals("10", response.getListOfDueAmountDetails().get(0).getGstPercentage());
		assertEquals("100", response.getListOfDueAmountDetails().get(0).getGstAmount());
		assertEquals("1100", response.getListOfDueAmountDetails().get(0).getTotalAmount());
		assertEquals("Maintenance", response.getListOfDueAmountDetails().get(0).getPaymentName());
		assertEquals("MANDATORY", response.getListOfDueAmountDetails().get(0).getPaymentType());
		assertTrue(response.getListOfDueAmountDetails().get(0).isEventPayment());
		assertEquals("PER_FLAT", response.getListOfDueAmountDetails().get(0).getPaymentCapita());
		assertEquals(List.of("UPI", "CARD"), response.getListOfDueAmountDetails().get(0).getAllowedPaymentModes());
		long dueIdCount = response.getListOfDueAmountDetails().stream().filter(d -> d.getDueId() != null)
				.count();
		assertEquals(0, dueIdCount);
		assertEquals(SuccessMessage.SUCC_MESSAGE_28, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_28, response.getMessageCode());
	}

	@Test
	void getDuePaymentAmountDetails_shouldRoundHalfYearlyLastCycleAmountDown() {
		DuePaymentAmountDetailsRequest request = new DuePaymentAmountDetailsRequest();
		request.setPaymentAmount("4355");
		request.setGst("10");
		request.setCollectionStartDate(LocalDate.parse("2026-04-01"));
		request.setCollectionEndDate(LocalDate.parse("2026-10-23"));
		request.setPaymentCollectionCycle("half yearly");
		request.setPaymentCollectionMode("pre");
		request.setTodayDate(LocalDate.parse("2026-10-11"));
		request.setPaymentCapita("PER_FLAT");

		DuePaymentAmountDetailsResponse response = paymentServices.getDuePaymentAmountDetails(request);

		assertEquals(LocalDate.parse("2026-10-01"), response.getDueDate());
		assertEquals("550", response.getAmountExcludingGst());
		assertEquals("55", response.getGstAmount());
		assertEquals("605", response.getAmountIncludingGst());
	}

	@Test
	void getDuePaymentAmountDetails_shouldSupportOnceCycleForPreAndPostMode() {
		DuePaymentAmountDetailsRequest preRequest = new DuePaymentAmountDetailsRequest();
		preRequest.setPaymentAmount("4355");
		preRequest.setGst("10");
		preRequest.setCollectionStartDate(LocalDate.parse("2026-04-01"));
		preRequest.setCollectionEndDate(LocalDate.parse("2027-05-23"));
		preRequest.setPaymentCollectionCycle("once");
		preRequest.setPaymentCollectionMode("pre");
		preRequest.setTodayDate(LocalDate.parse("2026-04-11"));

		DuePaymentAmountDetailsResponse preResponse = paymentServices.getDuePaymentAmountDetails(preRequest);
		assertEquals(LocalDate.parse("2026-04-01"), preResponse.getDueDate());
		assertEquals("4355", preResponse.getAmountExcludingGst());
		assertEquals("435.5", preResponse.getGstAmount());
		assertEquals("4790", preResponse.getAmountIncludingGst());

		DuePaymentAmountDetailsRequest postRequest = new DuePaymentAmountDetailsRequest();
		postRequest.setPaymentAmount("4355");
		postRequest.setGst("10");
		postRequest.setCollectionStartDate(LocalDate.parse("2026-04-01"));
		postRequest.setCollectionEndDate(LocalDate.parse("2027-05-23"));
		postRequest.setPaymentCollectionCycle("once");
		postRequest.setPaymentCollectionMode("post");
		postRequest.setTodayDate(LocalDate.parse("2026-04-11"));

		DuePaymentAmountDetailsResponse postResponse = paymentServices.getDuePaymentAmountDetails(postRequest);
		assertEquals(LocalDate.parse("2027-05-24"), postResponse.getDueDate());
		assertEquals("4355", postResponse.getAmountExcludingGst());
		assertEquals("435.5", postResponse.getGstAmount());
		assertEquals("4790", postResponse.getAmountIncludingGst());
	}

	@Test
	void getDuePaymentAmountDetails_shouldRoundGstAndTotalByThreshold_withDecimalBoundaryCases() {
		DuePaymentAmountDetailsRequest lowDecimalRequest = new DuePaymentAmountDetailsRequest();
		lowDecimalRequest.setPaymentAmount("47223");
		lowDecimalRequest.setGst("10");
		lowDecimalRequest.setCollectionStartDate(LocalDate.parse("2026-04-01"));
		lowDecimalRequest.setCollectionEndDate(LocalDate.parse("2026-12-31"));
		lowDecimalRequest.setPaymentCollectionCycle("once");
		lowDecimalRequest.setPaymentCollectionMode("pre");
		lowDecimalRequest.setTodayDate(LocalDate.parse("2026-04-11"));

		DuePaymentAmountDetailsResponse lowDecimalResponse = paymentServices.getDuePaymentAmountDetails(lowDecimalRequest);
		assertEquals("4722.3", lowDecimalResponse.getGstAmount());
		assertEquals("51945", lowDecimalResponse.getAmountIncludingGst());

		DuePaymentAmountDetailsRequest highDecimalRequest = new DuePaymentAmountDetailsRequest();
		highDecimalRequest.setPaymentAmount("47226");
		highDecimalRequest.setGst("10");
		highDecimalRequest.setCollectionStartDate(LocalDate.parse("2026-04-01"));
		highDecimalRequest.setCollectionEndDate(LocalDate.parse("2026-12-31"));
		highDecimalRequest.setPaymentCollectionCycle("once");
		highDecimalRequest.setPaymentCollectionMode("pre");
		highDecimalRequest.setTodayDate(LocalDate.parse("2026-04-11"));

		DuePaymentAmountDetailsResponse highDecimalResponse = paymentServices.getDuePaymentAmountDetails(highDecimalRequest);
		assertEquals("4722.6", highDecimalResponse.getGstAmount());
		assertEquals("51949", highDecimalResponse.getAmountIncludingGst());

		DuePaymentAmountDetailsRequest halfDecimalRequest = new DuePaymentAmountDetailsRequest();
		halfDecimalRequest.setPaymentAmount("47225");
		halfDecimalRequest.setGst("10");
		halfDecimalRequest.setCollectionStartDate(LocalDate.parse("2026-04-01"));
		halfDecimalRequest.setCollectionEndDate(LocalDate.parse("2026-12-31"));
		halfDecimalRequest.setPaymentCollectionCycle("once");
		halfDecimalRequest.setPaymentCollectionMode("pre");
		halfDecimalRequest.setTodayDate(LocalDate.parse("2026-04-11"));

		DuePaymentAmountDetailsResponse halfDecimalResponse = paymentServices.getDuePaymentAmountDetails(halfDecimalRequest);
		assertEquals("4722.5", halfDecimalResponse.getGstAmount());
		assertEquals("51947", halfDecimalResponse.getAmountIncludingGst());
	}

	@Test
	void getDuePaymentAmountDetails_shouldMarkPastDuesActiveWhenAddLeftOverPaymentTrue() {
		LocalDate today = LocalDate.now();
		CreatePaymentRequest request = new CreatePaymentRequest();
		request.setPaymentAmount("1000");
		request.setGst("10");
		request.setCollectionStartDate(Date.valueOf(today.minusMonths(2)));
		request.setCollectionEndDate(Date.valueOf(today.plusMonths(1)));
		request.setPaymentCollectionCycle("monthly");
		request.setPaymentCollectionMode("pre");
		request.setEventPayment(true);
		request.setAddLeftOverPayment(true);

		GetDuePaymentAmountDetailsResponse response = paymentServices.getDuePaymentAmountDetails(request);
		long oldDueCount = response.getListOfDueAmountDetails().stream().filter(d -> d.getDueDate().isBefore(today)).count();
		long oldDueWithoutDueIdCount = response.getListOfDueAmountDetails().stream()
				.filter(d -> d.getDueDate().isBefore(today) && d.getDueId() == null).count();

		assertTrue(oldDueCount > 0);
		assertEquals(oldDueCount, oldDueWithoutDueIdCount);
	}

	@Test
	void getDuePaymentAmountDetails_shouldApplyAddedChargesForAmountAndPercentageTypes() {
		LocalDate today = LocalDate.now();
		CreatePaymentRequest request = new CreatePaymentRequest();
		request.setPaymentAmount("1000");
		request.setGst("10");
		request.setCollectionStartDate(Date.valueOf(today));
		request.setCollectionEndDate(Date.valueOf(today.plusMonths(1)));
		request.setPaymentCollectionCycle("once");
		request.setPaymentCollectionMode("pre");

		AddedCharges amountCharge = new AddedCharges();
		amountCharge.setChargeName("Late Fee");
		amountCharge.setChargeType("amount");
		amountCharge.setValue("100");
		AddedCharges percentageCharge = new AddedCharges();
		percentageCharge.setChargeName("Convenience");
		percentageCharge.setChargeType("percentage");
		percentageCharge.setValue("10");
		request.setAddedCharges(List.of(amountCharge, percentageCharge));

		GetDuePaymentAmountDetailsResponse response = paymentServices.getDuePaymentAmountDetails(request);
		DueAmountDetails dueDetails = response.getListOfDueAmountDetails().get(0);

		assertEquals("100", dueDetails.getGstAmount());
		assertEquals("1300", dueDetails.getTotalAmount());
		assertNotNull(dueDetails.getAddedCharges());
		assertEquals(2, dueDetails.getAddedCharges().size());
		assertEquals("100", dueDetails.getAddedCharges().get(0).getFinalChargeValue());
		assertEquals("100", dueDetails.getAddedCharges().get(1).getFinalChargeValue());
		BigDecimal chargeTotal = dueDetails.getAddedCharges().stream().map(c -> new BigDecimal(c.getFinalChargeValue()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		assertEquals("1000", dueDetails.getAmount());
		assertEquals(chargeTotal.toPlainString(), dueDetails.getTotalAddedCharges());
	}

	@Test
	void getDuePaymentAmountDetails_shouldCalculateGstFromBaseAmountWhenPercentageAddedChargeExists() {
		LocalDate today = LocalDate.now();
		CreatePaymentRequest request = new CreatePaymentRequest();
		request.setPaymentAmount("1000");
		request.setGst("10");
		request.setCollectionStartDate(Date.valueOf(today));
		request.setCollectionEndDate(Date.valueOf(today.plusMonths(1)));
		request.setPaymentCollectionCycle("once");
		request.setPaymentCollectionMode("pre");

		AddedCharges percentageCharge = new AddedCharges();
		percentageCharge.setChargeName("Surcharge");
		percentageCharge.setChargeType("percentage");
		percentageCharge.setValue("10");
		request.setAddedCharges(List.of(percentageCharge));

		GetDuePaymentAmountDetailsResponse response = paymentServices.getDuePaymentAmountDetails(request);
		DueAmountDetails dueDetails = response.getListOfDueAmountDetails().get(0);

		BigDecimal chargeTotal = dueDetails.getAddedCharges().stream().map(c -> new BigDecimal(c.getFinalChargeValue()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		assertEquals("1000", dueDetails.getAmount());
		assertEquals(chargeTotal.toPlainString(), dueDetails.getTotalAddedCharges());
		assertEquals("100", dueDetails.getGstAmount());
		assertEquals("1200", dueDetails.getTotalAmount());
		assertEquals("100", dueDetails.getAddedCharges().get(0).getFinalChargeValue());
	}

	@Test
	void getDuePaymentAmountDetails_shouldNotThresholdRoundGstAndAddedChargesUntilTotal() {
		LocalDate today = LocalDate.now();
		CreatePaymentRequest createRequest = new CreatePaymentRequest();
		createRequest.setPaymentAmount("1000");
		createRequest.setGst("10");
		createRequest.setCollectionStartDate(Date.valueOf(today));
		createRequest.setCollectionEndDate(Date.valueOf(today.plusMonths(1)));
		createRequest.setPaymentCollectionCycle("once");
		createRequest.setPaymentCollectionMode("pre");

		AddedCharges amountCharge = new AddedCharges();
		amountCharge.setChargeName("Late Fee");
		amountCharge.setChargeType("amount");
		amountCharge.setValue("100.4");
		AddedCharges percentageCharge = new AddedCharges();
		percentageCharge.setChargeName("Convenience");
		percentageCharge.setChargeType("percentage");
		percentageCharge.setValue("10");
		createRequest.setAddedCharges(List.of(amountCharge, percentageCharge));

		GetDuePaymentAmountDetailsResponse response = paymentServices.getDuePaymentAmountDetails(createRequest);
		DueAmountDetails dueDetails = response.getListOfDueAmountDetails().get(0);

		assertEquals("100.4", dueDetails.getAddedCharges().get(0).getFinalChargeValue());
		assertEquals("100", dueDetails.getAddedCharges().get(1).getFinalChargeValue());
		assertEquals("1000", dueDetails.getAmount());
		assertEquals("200.4", dueDetails.getTotalAddedCharges());
		assertEquals("100", dueDetails.getGstAmount());
		BigDecimal unroundedTotal = new BigDecimal(dueDetails.getAmount()).add(new BigDecimal(dueDetails.getTotalAddedCharges()))
				.add(new BigDecimal(dueDetails.getGstAmount()));
		assertEquals("1300.4", unroundedTotal.toPlainString());
		assertEquals(unroundedTotal.stripTrailingZeros().toPlainString(), dueDetails.getTotalAmount());
	}

	@Test
	void getDuePaymentAmountDetails_shouldReturnFlatAreaWiseDueAmountsWhenPaymentCapitaIsPerSqft() {
		LocalDate today = LocalDate.now();
		CreatePaymentRequest request = new CreatePaymentRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		request.setGenericHeader(header);
		request.setPaymentName("CAM");
		request.setPaymentType("OPTIONAL");
		request.setPaymentCapita("Per Sqft");
		request.setAllowedPaymentModes(List.of("UPI", "NETBANKING"));
		request.setPaymentAmount("2");
		request.setGst("10");
		request.setCollectionStartDate(Date.valueOf(today));
		request.setCollectionEndDate(Date.valueOf(today.plusMonths(1)));
		request.setPaymentCollectionCycle("monthly");
		request.setPaymentCollectionMode("pre");

		Flat flatA = new Flat();
		flatA.setFlatNo("A-101");
		flatA.setFlatArea("1000");
		Flat flatB = new Flat();
		flatB.setFlatNo("A-102");
		flatB.setFlatArea("1200");
		Flat flatC = new Flat();
		flatC.setFlatNo("A-103");
		flatC.setFlatArea("1000");
		when(flatRepository.findByAprmntId("APR-001")).thenReturn(List.of(flatA, flatB, flatC));

		GetDuePaymentAmountDetailsResponse response = paymentServices.getDuePaymentAmountDetails(request);

		assertNotNull(response.getFlatTypeDueAmountDetails());
		assertNull(response.getListOfDueAmountDetails());
		assertEquals(2, response.getFlatTypeDueAmountDetails().size());
		assertEquals("2000", response.getFlatTypeDueAmountDetails().get("1000").get(0).getAmount());
		assertEquals("10", response.getFlatTypeDueAmountDetails().get("1000").get(0).getGstPercentage());
		assertEquals("200", response.getFlatTypeDueAmountDetails().get("1000").get(0).getGstAmount());
		assertEquals("2200", response.getFlatTypeDueAmountDetails().get("1000").get(0).getTotalAmount());
		assertEquals("2400", response.getFlatTypeDueAmountDetails().get("1200").get(0).getAmount());
		assertEquals("10", response.getFlatTypeDueAmountDetails().get("1200").get(0).getGstPercentage());
		assertEquals("240", response.getFlatTypeDueAmountDetails().get("1200").get(0).getGstAmount());
		assertEquals("2640", response.getFlatTypeDueAmountDetails().get("1200").get(0).getTotalAmount());
		assertEquals("CAM", response.getFlatTypeDueAmountDetails().get("1200").get(0).getPaymentName());
		assertEquals("OPTIONAL", response.getFlatTypeDueAmountDetails().get("1200").get(0).getPaymentType());
		assertEquals("Per Sqft", response.getFlatTypeDueAmountDetails().get("1200").get(0).getPaymentCapita());
		assertEquals(List.of("UPI", "NETBANKING"),
				response.getFlatTypeDueAmountDetails().get("1200").get(0).getAllowedPaymentModes());
		assertEquals(SuccessMessage.SUCC_MESSAGE_28, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_28, response.getMessageCode());
	}

	@Test
	void createPayment_shouldAppendDueAmountDetailsToApplicableFlats() throws Exception {
		CreatePaymentRequest request = new CreatePaymentRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		request.setGenericHeader(header);
		request.setPaymentName("CAM");
		request.setPaymentType("CAM");
		request.setPaymentCapita("PER_FLAT");
		request.setPaymentAmount("1200");
		request.setGst("10");
		request.setCollectionStartDate(Date.valueOf(LocalDate.now()));
		request.setCollectionEndDate(Date.valueOf(LocalDate.now().plusMonths(1)));
		request.setPaymentCollectionCycle("monthly");
		request.setPaymentCollectionMode("pre");
		request.setApplicableFor(List.of("A-101"));
		AddedCharges amountCharge = new AddedCharges();
		amountCharge.setChargeName("Late Fee");
		amountCharge.setChargeType("amount");
		amountCharge.setValue("100");
		request.setAddedCharges(List.of(amountCharge));

		Flat targetFlat = new Flat();
		targetFlat.setFlatNo("A-101");
		Flat ignoredFlat = new Flat();
		ignoredFlat.setFlatNo("A-102");

		when(genericService.getCorrectLocalDateForInputDate(any(Date.class)))
				.thenAnswer(invocation -> ((Date) invocation.getArgument(0)).toLocalDate().atStartOfDay());
		when(genericService.toJson(any())).thenReturn("DUE_JSON");
		when(flatRepository.findByAprmntId("APR-001")).thenReturn(List.of(targetFlat, ignoredFlat));
		when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentServices.createPayment(request);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Flat>> flatCaptor = ArgumentCaptor.forClass((Class) List.class);
		verify(flatRepository, times(1)).saveAll(flatCaptor.capture());
		assertEquals("A-101", flatCaptor.getValue().get(0).getFlatNo());
		assertEquals("DUE_JSON", flatCaptor.getValue().get(0).getFlatPndngPaymntLst());
		ArgumentCaptor<Object> dueListCaptor = ArgumentCaptor.forClass(Object.class);
		verify(genericService, atLeastOnce()).toJson(dueListCaptor.capture());
		List<DueAmountDetails> dueDetails = extractDueAmountDetailsList(dueListCaptor);
		assertTrue(dueDetails.stream().allMatch(d -> d.getDueId() != null && d.getDueId().startsWith("DUE")));
		assertEquals("100", dueDetails.get(0).getAddedCharges().get(0).getFinalChargeValue());
	}

	@Test
	void createPayment_shouldAppendAreaWiseDueAmountDetailsForPerSqftPayments() throws Exception {
		LocalDate today = LocalDate.now();
		CreatePaymentRequest request = new CreatePaymentRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		request.setGenericHeader(header);
		request.setPaymentName("CAM");
		request.setPaymentType("CAM");
		request.setPaymentCapita("Per Sqft");
		request.setPaymentAmount("2");
		request.setGst("10");
		request.setCollectionStartDate(Date.valueOf(today));
		request.setCollectionEndDate(Date.valueOf(today.plusMonths(1)));
		request.setPaymentCollectionCycle("monthly");
		request.setPaymentCollectionMode("pre");
		request.setAddLeftOverPayment(true);

		Flat flat1200 = new Flat();
		flat1200.setFlatNo("A-101");
		flat1200.setFlatArea("1200");
		flat1200.setFlatPndngPaymntLst("EXISTING_JSON_1200");

		Flat flat1000 = new Flat();
		flat1000.setFlatNo("A-102");
		flat1000.setFlatArea("1000");
		flat1000.setFlatPndngPaymntLst("EXISTING_JSON_1000");

		DueAmountDetails existing1200 = new DueAmountDetails();
		existing1200.setDueDate(today.plusDays(5));
		existing1200.setPaymentId("OLD1200");

		DueAmountDetails existing1000 = new DueAmountDetails();
		existing1000.setDueDate(today.plusDays(6));
		existing1000.setPaymentId("OLD1000");

		when(genericService.getCorrectLocalDateForInputDate(any(Date.class)))
				.thenAnswer(invocation -> ((Date) invocation.getArgument(0)).toLocalDate().atStartOfDay());
		when(genericService.fromJson(eq("EXISTING_JSON_1200"), any(TypeReference.class))).thenReturn(List.of(existing1200));
		when(genericService.fromJson(eq("EXISTING_JSON_1000"), any(TypeReference.class))).thenReturn(List.of(existing1000));
		when(genericService.toJson(any())).thenReturn("DUE_JSON");
		when(flatRepository.findByAprmntId("APR-001")).thenReturn(List.of(flat1200, flat1000));
		when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentServices.createPayment(request);

		ArgumentCaptor<Object> dueListCaptor = ArgumentCaptor.forClass(Object.class);
		verify(genericService, atLeastOnce()).toJson(dueListCaptor.capture());
		List<List<DueAmountDetails>> savedDueLists = extractAllDueAmountDetailsLists(dueListCaptor);
		assertEquals(2, savedDueLists.size());
		List<DueAmountDetails> savedListFor1200 = savedDueLists.stream()
				.filter(list -> list.stream().anyMatch(d -> "OLD1200".equals(d.getPaymentId()))).findFirst().orElse(List.of());
		List<DueAmountDetails> savedListFor1000 = savedDueLists.stream()
				.filter(list -> list.stream().anyMatch(d -> "OLD1000".equals(d.getPaymentId()))).findFirst().orElse(List.of());
		assertTrue(savedListFor1200.stream().anyMatch(d -> "2400".equals(d.getAmount()) && "2640".equals(d.getTotalAmount())));
		assertTrue(savedListFor1000.stream().anyMatch(d -> "2000".equals(d.getAmount()) && "2200".equals(d.getTotalAmount())));
		assertTrue(savedDueLists.stream().flatMap(List::stream).filter(d -> !"OLD1200".equals(d.getPaymentId()))
				.filter(d -> !"OLD1000".equals(d.getPaymentId()))
				.allMatch(d -> d.getPaymentId() != null && d.getPaymentId().startsWith("PAYCAM")));
		assertTrue(savedDueLists.stream().flatMap(List::stream).filter(d -> !"OLD1200".equals(d.getPaymentId()))
				.filter(d -> !"OLD1000".equals(d.getPaymentId()))
				.allMatch(d -> d.getDueId() != null && d.getDueId().startsWith("DUE")));
		assertTrue(savedDueLists.stream().flatMap(List::stream).filter(d -> !"OLD1200".equals(d.getPaymentId()))
				.filter(d -> !"OLD1000".equals(d.getPaymentId())).allMatch(DueAmountDetails::isEventPayment));
	}

	@Test
	void createPayment_shouldSetMaintainanceFeeFromCamPaymentFlag() throws Exception {
		CreatePaymentRequest request = new CreatePaymentRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		header.setUserId("USR-001");
		request.setGenericHeader(header);
		request.setPaymentName("CAM");
		request.setPaymentType("CAM");
		request.setPaymentCapita("PER_FLAT");
		request.setPaymentAmount("1200");
		request.setGst("10");
		request.setCollectionStartDate(Date.valueOf(LocalDate.now()));
		request.setCollectionEndDate(Date.valueOf(LocalDate.now().plusMonths(1)));
		request.setPaymentCollectionCycle("half_yearly");
		request.setPaymentCollectionMode("pre");
		request.setCamPayment(true);
		request.setEventPayment(true);
		request.setDiscountCode("DISC10");
		request.setFineCode("FINE5");
		AddedCharges amountCharge = new AddedCharges();
		amountCharge.setChargeName("Late Fee");
		amountCharge.setChargeType("amount");
		amountCharge.setValue("100");
		request.setAddedCharges(List.of(amountCharge));

		when(genericService.getCorrectLocalDateForInputDate(any(Date.class)))
				.thenAnswer(invocation -> ((Date) invocation.getArgument(0)).toLocalDate().atStartOfDay());
		when(genericService.toJson(any())).thenReturn("ADDED_CHARGES_JSON", "DISC_FIN_JSON");
		when(flatRepository.findByAprmntId("APR-001")).thenReturn(List.of());
		when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentServices.createPayment(request);

		ArgumentCaptor<PaymentEntity> paymentCaptor = ArgumentCaptor.forClass(PaymentEntity.class);
		verify(paymentRepository, times(1)).save(paymentCaptor.capture());
		assertTrue(paymentCaptor.getValue().isMaintainanceFee());
		assertTrue(paymentCaptor.getValue().isEventPayment());
		assertEquals("APR-001", paymentCaptor.getValue().getAprmtId());
		assertEquals(SecuraConstants.PAYMENT_CYCLE_HALF_YEARLY, paymentCaptor.getValue().getPaymentCollectionCycle());
		assertEquals(SecuraConstants.PAYMENT_STATUS_ACTIVE, paymentCaptor.getValue().getStatus());
		assertEquals("USR-001", paymentCaptor.getValue().getCreatUsrId());
		assertEquals("ADDED_CHARGES_JSON", paymentCaptor.getValue().getAddedCharges());
		assertEquals("DISC_FIN_JSON", paymentCaptor.getValue().getDiscFin());
	}

	@Test
	void createPayment_shouldSerializeAllowedPaymentModes() throws Exception {
		CreatePaymentRequest request = new CreatePaymentRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		request.setGenericHeader(header);
		request.setPaymentName("CAM");
		request.setPaymentType("CAM");
		request.setPaymentCapita("PER_FLAT");
		request.setPaymentAmount("1200");
		request.setGst("10");
		request.setCollectionStartDate(Date.valueOf(LocalDate.now()));
		request.setCollectionEndDate(Date.valueOf(LocalDate.now().plusMonths(1)));
		request.setPaymentCollectionCycle("monthly");
		request.setPaymentCollectionMode("pre");
		request.setAllowedPaymentModes(List.of("UPI", "CARD"));

		when(genericService.getCorrectLocalDateForInputDate(any(Date.class)))
				.thenAnswer(invocation -> ((Date) invocation.getArgument(0)).toLocalDate().atStartOfDay());
		when(genericService.toJson(eq(List.of("UPI", "CARD")))).thenReturn("ALLOWED_PAYMENT_MODES_JSON");
		when(flatRepository.findByAprmntId("APR-001")).thenReturn(List.of());
		when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentServices.createPayment(request);

		ArgumentCaptor<PaymentEntity> paymentCaptor = ArgumentCaptor.forClass(PaymentEntity.class);
		verify(paymentRepository, times(1)).save(paymentCaptor.capture());
		assertEquals("ALLOWED_PAYMENT_MODES_JSON", paymentCaptor.getValue().getAllowedPaymentModes());
	}

	@Test
	void createPayment_shouldAppendDueAmountDetailsForCommaSeparatedApplicableFlats() throws Exception {
		LocalDate today = LocalDate.now();
		CreatePaymentRequest request = new CreatePaymentRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		request.setGenericHeader(header);
		request.setPaymentName("CAM");
		request.setPaymentType("CAM");
		request.setPaymentCapita("PER_FLAT");
		request.setPaymentAmount("1200");
		request.setGst("10");
		request.setCollectionStartDate(Date.valueOf(today));
		request.setCollectionEndDate(Date.valueOf(today.plusMonths(1)));
		request.setPaymentCollectionCycle("monthly");
		request.setPaymentCollectionMode("pre");
		request.setAddLeftOverPayment(true);
		request.setApplicableFor(List.of("a-101, A-102"));

		Flat targetFlatOne = new Flat();
		targetFlatOne.setFlatNo("A-101");
		targetFlatOne.setFlatPndngPaymntLst("EXISTING_JSON_1");
		Flat targetFlatTwo = new Flat();
		targetFlatTwo.setFlatNo("A-102");
		targetFlatTwo.setFlatPndngPaymntLst("EXISTING_JSON_2");
		Flat ignoredFlat = new Flat();
		ignoredFlat.setFlatNo("A-103");

		DueAmountDetails existingDueOne = new DueAmountDetails();
		existingDueOne.setDueDate(today.plusDays(5));
		existingDueOne.setPaymentId("OLD1");
		DueAmountDetails existingDueTwo = new DueAmountDetails();
		existingDueTwo.setDueDate(today.plusDays(6));
		existingDueTwo.setPaymentId("OLD2");

		when(genericService.getCorrectLocalDateForInputDate(any(Date.class)))
				.thenAnswer(invocation -> ((Date) invocation.getArgument(0)).toLocalDate().atStartOfDay());
		when(genericService.fromJson(eq("EXISTING_JSON_1"), any(TypeReference.class))).thenReturn(List.of(existingDueOne));
		when(genericService.fromJson(eq("EXISTING_JSON_2"), any(TypeReference.class))).thenReturn(List.of(existingDueTwo));
		when(genericService.toJson(any())).thenReturn("DUE_JSON");
		when(flatRepository.findByAprmntId("APR-001")).thenReturn(List.of(targetFlatOne, targetFlatTwo, ignoredFlat));
		when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentServices.createPayment(request);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Flat>> flatCaptor = ArgumentCaptor.forClass((Class) List.class);
		verify(flatRepository, times(1)).saveAll(flatCaptor.capture());
		List<Flat> savedFlats = flatCaptor.getValue();
		assertEquals(2, savedFlats.size());
		assertTrue(savedFlats.stream().anyMatch(flat -> "A-101".equals(flat.getFlatNo())));
		assertTrue(savedFlats.stream().anyMatch(flat -> "A-102".equals(flat.getFlatNo())));

		ArgumentCaptor<Object> dueListCaptor = ArgumentCaptor.forClass(Object.class);
		verify(genericService, atLeastOnce()).toJson(dueListCaptor.capture());
		List<List<DueAmountDetails>> savedDueLists = extractAllDueAmountDetailsLists(dueListCaptor);
		assertEquals(2, savedDueLists.size());
		assertTrue(savedDueLists.stream().allMatch(list -> list.size() >= 2));
		assertTrue(savedDueLists.stream().flatMap(List::stream).anyMatch(d -> "OLD1".equals(d.getPaymentId())));
		assertTrue(savedDueLists.stream().flatMap(List::stream).anyMatch(d -> "OLD2".equals(d.getPaymentId())));
		assertTrue(savedDueLists.stream().flatMap(List::stream)
				.anyMatch(d -> d.getDueId() != null && d.getDueId().startsWith("DUE")));
	}

	@Test
	void createPayment_shouldSupportApplicableForAcrossMultipleListEntriesWithCommaValues() throws Exception {
		LocalDate today = LocalDate.now();
		CreatePaymentRequest request = new CreatePaymentRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		request.setGenericHeader(header);
		request.setPaymentName("CAM");
		request.setPaymentType("CAM");
		request.setPaymentCapita("PER_FLAT");
		request.setPaymentAmount("1200");
		request.setGst("10");
		request.setCollectionStartDate(Date.valueOf(today));
		request.setCollectionEndDate(Date.valueOf(today.plusMonths(1)));
		request.setPaymentCollectionCycle("monthly");
		request.setPaymentCollectionMode("pre");
		request.setApplicableFor(List.of("A-101, A-102", "A-103"));

		Flat flat101 = new Flat();
		flat101.setFlatNo("A-101");
		Flat flat102 = new Flat();
		flat102.setFlatNo("A-102");
		Flat flat103 = new Flat();
		flat103.setFlatNo("A-103");
		Flat flat104 = new Flat();
		flat104.setFlatNo("A-104");

		when(genericService.getCorrectLocalDateForInputDate(any(Date.class)))
				.thenAnswer(invocation -> ((Date) invocation.getArgument(0)).toLocalDate().atStartOfDay());
		when(genericService.toJson(any())).thenReturn("DUE_JSON");
		when(flatRepository.findByAprmntId("APR-001")).thenReturn(List.of(flat101, flat102, flat103, flat104));
		when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentServices.createPayment(request);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Flat>> flatCaptor = ArgumentCaptor.forClass((Class) List.class);
		verify(flatRepository, times(1)).saveAll(flatCaptor.capture());
		List<Flat> savedFlats = flatCaptor.getValue();
		assertEquals(3, savedFlats.size());
		assertTrue(savedFlats.stream().anyMatch(flat -> "A-101".equals(flat.getFlatNo())));
		assertTrue(savedFlats.stream().anyMatch(flat -> "A-102".equals(flat.getFlatNo())));
		assertTrue(savedFlats.stream().anyMatch(flat -> "A-103".equals(flat.getFlatNo())));
	}

	@Test
	void createPayment_shouldDeleteOlderDueObjectsWhenAddLeftOverPaymentFalse() throws Exception {
		LocalDate today = LocalDate.now();
		CreatePaymentRequest request = new CreatePaymentRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		request.setGenericHeader(header);
		request.setPaymentName("CAM");
		request.setPaymentType("CAM");
		request.setPaymentCapita("PER_FLAT");
		request.setPaymentAmount("1200");
		request.setGst("10");
		request.setCollectionStartDate(Date.valueOf(today.minusMonths(1)));
		request.setCollectionEndDate(Date.valueOf(today.plusMonths(1)));
		request.setPaymentCollectionCycle("monthly");
		request.setPaymentCollectionMode("pre");
		request.setApplicableFor(List.of("A-101"));
		request.setAddLeftOverPayment(false);

		Flat targetFlat = new Flat();
		targetFlat.setFlatNo("A-101");
		targetFlat.setFlatPndngPaymntLst("EXISTING_JSON");

		DueAmountDetails oldExisting = new DueAmountDetails();
		oldExisting.setDueDate(today.minusDays(10));
		oldExisting.setPaymentId(null);
		DueAmountDetails futureExisting = new DueAmountDetails();
		futureExisting.setDueDate(today.plusDays(10));
		futureExisting.setPaymentId(null);

		when(genericService.getCorrectLocalDateForInputDate(any(Date.class)))
				.thenAnswer(invocation -> ((Date) invocation.getArgument(0)).toLocalDate().atStartOfDay());
		when(genericService.fromJson(eq("EXISTING_JSON"), any(TypeReference.class)))
				.thenReturn(List.of(oldExisting, futureExisting));
		when(genericService.toJson(any())).thenReturn("DUE_JSON");
		when(flatRepository.findByAprmntId("APR-001")).thenReturn(List.of(targetFlat));
		when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentServices.createPayment(request);

		ArgumentCaptor<Object> dueListCaptor = ArgumentCaptor.forClass(Object.class);
		verify(genericService, atLeastOnce()).toJson(dueListCaptor.capture());
		List<DueAmountDetails> savedDueList = extractDueAmountDetailsList(dueListCaptor);
		assertTrue(savedDueList.stream().allMatch(d -> !d.getDueDate().isBefore(today)));
		assertTrue(savedDueList.stream().allMatch(d -> d.getDueId() != null && d.getDueId().startsWith("DUE")));
	}

	@Test
	void createPayment_shouldApplyDiscountAndFineCodesOnlyToFutureDueDates() throws Exception {
		LocalDate today = LocalDate.now();
		CreatePaymentRequest request = new CreatePaymentRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		request.setGenericHeader(header);
		request.setPaymentName("CAM");
		request.setPaymentType("CAM");
		request.setPaymentCapita("PER_FLAT");
		request.setPaymentAmount("1200");
		request.setGst("10");
		request.setCollectionStartDate(Date.valueOf(today.minusMonths(1)));
		request.setCollectionEndDate(Date.valueOf(today.plusMonths(1)));
		request.setPaymentCollectionCycle("monthly");
		request.setPaymentCollectionMode("pre");
		request.setApplicableFor(List.of("A-101"));
		request.setAddLeftOverPayment(true);
		request.setDiscountCode("DISC10");
		request.setFineCode("FINE5");

		Flat targetFlat = new Flat();
		targetFlat.setFlatNo("A-101");
		targetFlat.setFlatPndngPaymntLst("EXISTING_JSON");

		DueAmountDetails pastExisting = new DueAmountDetails();
		pastExisting.setDueDate(today.minusDays(10));
		pastExisting.setPaymentId("OLD");
		pastExisting.setDueId("DUEOLD001");

		DueAmountDetails futureExisting = new DueAmountDetails();
		futureExisting.setDueDate(today.plusDays(10));
		futureExisting.setPaymentId("OLD");
		futureExisting.setDueId("DUEOLD002");

		when(genericService.getCorrectLocalDateForInputDate(any(Date.class)))
				.thenAnswer(invocation -> ((Date) invocation.getArgument(0)).toLocalDate().atStartOfDay());
		when(genericService.fromJson(eq("EXISTING_JSON"), any(TypeReference.class)))
				.thenReturn(List.of(pastExisting, futureExisting));
		when(genericService.toJson(any())).thenReturn("DUE_JSON");
		when(flatRepository.findByAprmntId("APR-001")).thenReturn(List.of(targetFlat));
		when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentServices.createPayment(request);

		ArgumentCaptor<Object> dueListCaptor = ArgumentCaptor.forClass(Object.class);
		verify(genericService, atLeastOnce()).toJson(dueListCaptor.capture());
		List<DueAmountDetails> savedDueList = extractDueAmountDetailsList(dueListCaptor);

		assertTrue(savedDueList.stream()
				.filter(d -> d.getDueDate() != null && d.getDueDate().isAfter(today) && d.getDueId() != null)
				.allMatch(d -> "DISC10".equals(d.getDiscountCode()) && "FINE5".equals(d.getFineCode())));
		assertTrue(savedDueList.stream()
				.filter(d -> d.getDueDate() != null && !d.getDueDate().isAfter(today))
				.allMatch(d -> d.getDiscountCode() == null && d.getFineCode() == null));
	}

	@Test
	void payDues_shouldCreateOnHoldTransactionAndWorklistForCashTender() throws Exception {
		PayDueRequest request = new PayDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		header.setUserId("USR-001");
		header.setFlatNo("A-101");
		request.setGenericHeader(header);
		request.setPaymentId("PAY1234");
		request.setDueId("DUE-001");
		request.setAmount("1801");
		request.setTender(SecuraConstants.TRANSACTION_TENDER_CASH);
		request.setTransactionStatus("PENDING");
		request.setThirdPartyTransactionId("REF-001");
		request.setNoOfPersons("3");
		request.setFiles(List.of("one.pdf", "two.pdf"));

		DueAmountDetails dueDetails = new DueAmountDetails();
		dueDetails.setDueId("DUE-001");
		GetDueAmountForPerHeadCalculationResponse dueResponse = new GetDueAmountForPerHeadCalculationResponse();
		dueResponse.setDueAmountDetails(dueDetails);

		PaymentEntity paymentEntity = new PaymentEntity();
		paymentEntity.setBankAccountId("BANK-001");
		paymentEntity.setMaintainanceFee(true);
		paymentEntity.setEventPayment(true);

		Worklist worklist = new Worklist();
		worklist.setWorklistTaskId("WL-001");

		when(flatInterface.getDueAmountForPerHeadCalculation(any())).thenReturn(dueResponse);
		when(paymentRepository.findById("PAY1234")).thenReturn(Optional.of(paymentEntity));
		when(genericService.createWorklist(eq(SecuraConstants.WORKLIST_TYPE_TRANSACTION), eq("USR-001"), eq("APR-001"),
				any())).thenReturn(worklist);
		when(genericService.createWorklistAssignmentFlow("WL-001", List.of("admin"))).thenReturn(worklist);
		when(genericService.toJson(any())).thenAnswer(invocation -> invocation.getArgument(0) instanceof DueAmountDetails
				? "DUE_JSON"
				: "FILES_JSON");
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

		PayDueResponse response = paymentServices.payDues(request);

		ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
		verify(transactionRepository).save(transactionCaptor.capture());
		Transaction savedTransaction = transactionCaptor.getValue();

		assertEquals(SuccessMessage.SUCC_MESSAGE_33, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_33, response.getMessageCode());
		assertEquals(savedTransaction.getTrnscId(), response.getTransactionId());
		assertNull(response.getReceipt());
		assertNull(response.getReceiptNumber());
		assertEquals(SecuraConstants.TRANSACTION_STATUS_ON_HOLD, savedTransaction.getTrnsStatus());
		assertEquals(SecuraConstants.TRANSACTION_TYPE_CREDIT, savedTransaction.getTrnsType());
		assertEquals(SecuraConstants.PAYMENT_CURRENCY, savedTransaction.getTrnsCurrency());
		assertEquals(SecuraConstants.TRANSACTION_THIRD_PARTY_RAZOR_PAY, savedTransaction.getThirdPartyName());
		assertEquals("BANK-001", savedTransaction.getTrnsBnkAccnt());
		assertEquals("DUE_JSON", savedTransaction.getDueDetails());
		assertEquals("FILES_JSON", savedTransaction.getTrnsFiles());
		assertEquals(SecuraConstants.TRANSACTION_CAUSE_MAINTENANCE, savedTransaction.getCause());
		assertEquals("WL-001", savedTransaction.getWorkListId());
		assertEquals("APR-001", savedTransaction.getAprmntId());
		assertEquals("USR-001", savedTransaction.getTrnsBy());
		assertTrue(savedTransaction.getTrnscId().contains("PAY1234"));
		verify(flatInterface, never()).getDueAmountForFlat(any());
		verify(genericService).createWorklistAssignmentFlow("WL-001", List.of("admin"));
		verify(receiptServices, never()).createReceipt(any());
	}

	@Test
	void payDues_shouldSetSuccessStatusWithoutWorklistForOnlineTenderAndAttachReceipt() throws Exception {
		PayDueRequest request = new PayDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		header.setUserId("USR-001");
		header.setFlatNo("A-101");
		request.setGenericHeader(header);
		request.setPaymentId("PAY1234");
		request.setDueId("DUE-001");
		request.setAmount("1801");
		request.setTender(SecuraConstants.TRANSACTION_TENDER_ONLINE);
		request.setTransactionStatus(SecuraConstants.TRANSACTION_STATUS_SUCCESS);
		request.setNoOfPersons("3");

		DueAmountDetails dueDetails = new DueAmountDetails();
		dueDetails.setDueId("DUE-001");
		dueDetails.setPaymentName("Maintenance");
		dueDetails.setAmount("1200");
		dueDetails.setPaymentCapita("PER_HEAD");
		dueDetails.setTotalAmount("1350");
		dueDetails.setGstAmount("200");
		dueDetails.setGstPercentage("18");
		dueDetails.setDiscountCode("DISC10");
		dueDetails.setDiscountedAmount("100");
		dueDetails.setFineCode("FINE5");
		dueDetails.setFineAmount("50");
		AddedCharges addedCharge = new AddedCharges();
		addedCharge.setChargeName("Late Fee");
		addedCharge.setChargeType("amount");
		addedCharge.setValue("50");
		addedCharge.setFinalChargeValue("50");
		dueDetails.setAddedCharges(List.of(addedCharge));
		GetDueAmountForPerHeadCalculationResponse dueResponse = new GetDueAmountForPerHeadCalculationResponse();
		dueResponse.setDueAmountDetails(dueDetails);

		PaymentEntity paymentEntity = new PaymentEntity();
		paymentEntity.setBankAccountId("BANK-001");
		paymentEntity.setEventPayment(true);
		CreateReceiptResponse createReceiptResponse = new CreateReceiptResponse();
		createReceiptResponse.setReceipt("RECEIPT_BASE64");
		createReceiptResponse.setReceiptNumber("RCT-1001");

		when(flatInterface.getDueAmountForPerHeadCalculation(any())).thenReturn(dueResponse);
		when(paymentRepository.findById("PAY1234")).thenReturn(Optional.of(paymentEntity));
		when(genericService.toJson(any())).thenReturn("JSON");
		when(receiptServices.createReceipt(any(CreateReceiptRequest.class))).thenReturn(createReceiptResponse);
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

		PayDueResponse response = paymentServices.payDues(request);

		ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
		verify(transactionRepository).save(transactionCaptor.capture());
		Transaction savedTransaction = transactionCaptor.getValue();

		assertEquals(SuccessMessage.SUCC_MESSAGE_33, response.getMessage());
		assertEquals(SecuraConstants.TRANSACTION_STATUS_SUCCESS, savedTransaction.getTrnsStatus());
		assertEquals("RECEIPT_BASE64", response.getReceipt());
		assertEquals("RCT-1001", response.getReceiptNumber());
		assertEquals("RCT-1001", savedTransaction.getReceiptNumber());
		assertEquals(SecuraConstants.TRANSACTION_CAUSE_EVENT, savedTransaction.getCause());
		assertNull(savedTransaction.getWorkListId());
		verify(flatInterface, never()).getDueAmountForFlat(any());
		verify(genericService, never()).createWorklist(any(), any(), any(), any());
		verify(genericService, never()).createWorklistAssignmentFlow(any(), any());
		ArgumentCaptor<CreateReceiptRequest> receiptRequestCaptor = ArgumentCaptor.forClass(CreateReceiptRequest.class);
		verify(receiptServices).createReceipt(receiptRequestCaptor.capture());
		CreateReceiptRequest receiptRequest = receiptRequestCaptor.getValue();
		assertEquals("Payment", receiptRequest.getReceiptType());
		assertTrue(receiptRequest.isPerheadFlag());
		assertTrue(receiptRequest.isUnitPriceRequired());
		assertNull(receiptRequest.getRemarks());
		assertEquals("1801", receiptRequest.getTotalAmount());
		assertEquals(savedTransaction.getTrnscId(), receiptRequest.getTransactionId());
		assertEquals(1, receiptRequest.getItems().size());
		assertEquals("Maintenance", receiptRequest.getItems().get(0).getItemName());
		assertEquals("400", receiptRequest.getItems().get(0).getUnitPrice());
		assertEquals("3", receiptRequest.getItems().get(0).getQuantity());
		assertEquals("1200", receiptRequest.getItems().get(0).getAmount());
		assertEquals("PAYMENT", receiptRequest.getItems().get(0).getType());
		assertEquals(1, receiptRequest.getTenderList().size());
		PaymentTenderData tenderData = receiptRequest.getTenderList().get(0);
		assertEquals(SecuraConstants.TRANSACTION_TENDER_ONLINE, tenderData.getTenderName());
		assertEquals("1801", tenderData.getAmountPaid());
		assertEquals(2, receiptRequest.getAddedCharges().size());
		assertEquals("Late Fee", receiptRequest.getAddedCharges().get(0).getChargeName());
		assertEquals("50", receiptRequest.getAddedCharges().get(0).getFinalChargeValue());
		assertEquals("GST", receiptRequest.getAddedCharges().get(1).getChargeName());
		assertEquals("percentage", receiptRequest.getAddedCharges().get(1).getChargeType());
		assertEquals("18", receiptRequest.getAddedCharges().get(1).getValue());
		assertEquals("200", receiptRequest.getAddedCharges().get(1).getFinalChargeValue());
		DiscFinReceipt discFinReceipt = receiptRequest.getDiscFinReceipt();
		assertNotNull(discFinReceipt);
		assertEquals("DISC10", discFinReceipt.getDiscountCode());
		assertEquals("100", discFinReceipt.getDiscountAmount());
		assertEquals("FINE5", discFinReceipt.getFineCode());
		assertEquals("50", discFinReceipt.getFineAmount());
	}

	@Test
	void payDues_shouldUseDueAmountForNonPerHeadReceiptItem() throws Exception {
		PayDueRequest request = new PayDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		header.setUserId("USR-001");
		header.setFlatNo("A-101");
		request.setGenericHeader(header);
		request.setPaymentId("PAY1234");
		request.setDueId("DUE-001");
		request.setAmount("1180");
		request.setTender(SecuraConstants.TRANSACTION_TENDER_ONLINE);
		request.setTransactionStatus(SecuraConstants.TRANSACTION_STATUS_SUCCESS);

		DueAmountDetails dueDetails = new DueAmountDetails();
		dueDetails.setDueId("DUE-001");
		dueDetails.setPaymentName("Maintenance");
		dueDetails.setAmount("1000");
		dueDetails.setPaymentCapita("PER_FLAT");
		dueDetails.setTotalAmount("1180");
		GetDueAmountForFlatResponse dueResponse = new GetDueAmountForFlatResponse();
		dueResponse.setDuePaymentList(List.of(dueDetails));

		PaymentEntity paymentEntity = new PaymentEntity();
		paymentEntity.setBankAccountId("BANK-001");
		CreateReceiptResponse createReceiptResponse = new CreateReceiptResponse();
		createReceiptResponse.setReceipt("RECEIPT_BASE64");
		createReceiptResponse.setReceiptNumber("RCT-1002");

		when(flatInterface.getDueAmountForFlat(any())).thenReturn(dueResponse);
		when(paymentRepository.findById("PAY1234")).thenReturn(Optional.of(paymentEntity));
		when(genericService.toJson(any())).thenReturn("JSON");
		when(receiptServices.createReceipt(any(CreateReceiptRequest.class))).thenReturn(createReceiptResponse);
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentServices.payDues(request);

		ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
		verify(transactionRepository).save(transactionCaptor.capture());
		assertEquals(SecuraConstants.TRANSACTION_CAUSE_OTHERS, transactionCaptor.getValue().getCause());

		ArgumentCaptor<CreateReceiptRequest> receiptRequestCaptor = ArgumentCaptor.forClass(CreateReceiptRequest.class);
		verify(receiptServices).createReceipt(receiptRequestCaptor.capture());
		CreateReceiptRequest receiptRequest = receiptRequestCaptor.getValue();
		assertFalse(receiptRequest.isPerheadFlag());
		assertFalse(receiptRequest.isUnitPriceRequired());
		assertEquals("1000", receiptRequest.getItems().get(0).getAmount());
		assertNull(receiptRequest.getItems().get(0).getQuantity());
		assertNull(receiptRequest.getItems().get(0).getUnitPrice());
		verify(flatInterface, never()).getDueAmountForPerHeadCalculation(any());
	}

	@Test
	void payDues_shouldFallbackToDueAmountWhenBaseAmountIsMissing() throws Exception {
		PayDueRequest request = new PayDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		header.setUserId("USR-001");
		header.setFlatNo("A-101");
		request.setGenericHeader(header);
		request.setPaymentId("PAY1234");
		request.setDueId("DUE-001");
		request.setAmount("1180");
		request.setTender(SecuraConstants.TRANSACTION_TENDER_ONLINE);
		request.setTransactionStatus(SecuraConstants.TRANSACTION_STATUS_SUCCESS);

		DueAmountDetails dueDetails = new DueAmountDetails();
		dueDetails.setDueId("DUE-001");
		dueDetails.setPaymentName("Maintenance");
		dueDetails.setAmount("1000");
		dueDetails.setTotalAmount("1180");
		GetDueAmountForFlatResponse dueResponse = new GetDueAmountForFlatResponse();
		dueResponse.setDuePaymentList(List.of(dueDetails));

		PaymentEntity paymentEntity = new PaymentEntity();
		paymentEntity.setBankAccountId("BANK-001");
		CreateReceiptResponse createReceiptResponse = new CreateReceiptResponse();
		createReceiptResponse.setReceipt("RECEIPT_BASE64");
		createReceiptResponse.setReceiptNumber("RCT-1003");

		when(flatInterface.getDueAmountForFlat(any())).thenReturn(dueResponse);
		when(paymentRepository.findById("PAY1234")).thenReturn(Optional.of(paymentEntity));
		when(genericService.toJson(any())).thenReturn("JSON");
		when(receiptServices.createReceipt(any(CreateReceiptRequest.class))).thenReturn(createReceiptResponse);
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentServices.payDues(request);

		ArgumentCaptor<CreateReceiptRequest> receiptRequestCaptor = ArgumentCaptor.forClass(CreateReceiptRequest.class);
		verify(receiptServices).createReceipt(receiptRequestCaptor.capture());
		assertEquals("1000", receiptRequestCaptor.getValue().getItems().get(0).getAmount());
	}

	@Test
	void payDues_shouldNotBuildPerHeadReceiptWhenDuePaymentCapitaIsNotPerHead() throws Exception {
		PayDueRequest request = new PayDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		header.setUserId("USR-001");
		header.setFlatNo("A-101");
		request.setGenericHeader(header);
		request.setPaymentId("PAY1234");
		request.setDueId("DUE-001");
		request.setAmount("1180");
		request.setTender(SecuraConstants.TRANSACTION_TENDER_ONLINE);
		request.setTransactionStatus(SecuraConstants.TRANSACTION_STATUS_SUCCESS);
		request.setNoOfPersons("3");

		DueAmountDetails dueDetails = new DueAmountDetails();
		dueDetails.setDueId("DUE-001");
		dueDetails.setPaymentName("Maintenance");
		dueDetails.setAmount("1000");
		dueDetails.setTotalAmount("1180");
		dueDetails.setPaymentCapita("PER_FLAT");
		GetDueAmountForPerHeadCalculationResponse dueResponse = new GetDueAmountForPerHeadCalculationResponse();
		dueResponse.setDueAmountDetails(dueDetails);

		PaymentEntity paymentEntity = new PaymentEntity();
		paymentEntity.setBankAccountId("BANK-001");
		CreateReceiptResponse createReceiptResponse = new CreateReceiptResponse();
		createReceiptResponse.setReceipt("RECEIPT_BASE64");
		createReceiptResponse.setReceiptNumber("RCT-1004");

		when(flatInterface.getDueAmountForPerHeadCalculation(any())).thenReturn(dueResponse);
		when(paymentRepository.findById("PAY1234")).thenReturn(Optional.of(paymentEntity));
		when(genericService.toJson(any())).thenReturn("JSON");
		when(receiptServices.createReceipt(any(CreateReceiptRequest.class))).thenReturn(createReceiptResponse);
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentServices.payDues(request);

		ArgumentCaptor<CreateReceiptRequest> receiptRequestCaptor = ArgumentCaptor.forClass(CreateReceiptRequest.class);
		verify(receiptServices).createReceipt(receiptRequestCaptor.capture());
		CreateReceiptRequest receiptRequest = receiptRequestCaptor.getValue();
		assertFalse(receiptRequest.isPerheadFlag());
		assertFalse(receiptRequest.isUnitPriceRequired());
		assertEquals("1000", receiptRequest.getItems().get(0).getAmount());
		assertNull(receiptRequest.getItems().get(0).getQuantity());
		assertNull(receiptRequest.getItems().get(0).getUnitPrice());
	}

	@Test
	void ledgerEntry_shouldCreateSingleTransactionAndAttachReceipt() throws Exception {
		LedgerEntryRequest request = new LedgerEntryRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		header.setUserId("USR-001");
		request.setGenericHeader(header);
		request.setTrnsDate(Date.valueOf(LocalDate.parse("2026-04-20")));
		request.setLedgerfor("Corpus Fund");
		request.setTrnsTenderList(List.of(SecuraConstants.TRANSACTION_TENDER_ONLINE));
		request.setTrnsType(SecuraConstants.TRANSACTION_TYPE_CREDIT);
		request.setTrnsShrtDesc("Ledger entry");
		request.setTrnsBnkAccnt("BANK-001");
		request.setTrnsAmt("5000");
		request.setTrnsStatus(SecuraConstants.TRANSACTION_STATUS_SUCCESS);
		request.setCause("CAUSE");
		request.setSupportedFile(List.of("one.pdf", "two.pdf"));
		request.setRequiredReceiptFlag(true);

		CreateReceiptResponse createReceiptResponse = new CreateReceiptResponse();
		createReceiptResponse.setReceipt("RECEIPT_BASE64");
		createReceiptResponse.setReceiptNumber("RCT-2001");

		when(genericService.toJson(any())).thenReturn("FILES_JSON");
		when(receiptServices.createReceipt(any(CreateReceiptRequest.class))).thenReturn(createReceiptResponse);
		when(transactionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

		LedgerEntryResponse response = paymentServices.ledgerEntry(request);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Transaction>> transactionsCaptor = ArgumentCaptor.forClass((Class) List.class);
		verify(transactionRepository, times(2)).saveAll(transactionsCaptor.capture());
		List<List<Transaction>> savedBatches = transactionsCaptor.getAllValues();
		assertEquals(2, savedBatches.size());
		assertEquals(1, savedBatches.get(0).size());
		Transaction createdTransaction = savedBatches.get(0).get(0);
		Transaction updatedTransaction = savedBatches.get(1).get(0);
		assertEquals("APR-001", createdTransaction.getAprmntId());
		assertEquals("USR-001", createdTransaction.getTrnsBy());
		assertEquals(SecuraConstants.TRANSACTION_TENDER_ONLINE, createdTransaction.getTrnsTender());
		assertEquals(SecuraConstants.TRANSACTION_TYPE_CREDIT, createdTransaction.getTrnsType());
		assertEquals("Ledger entry", createdTransaction.getTrnsShrtDesc());
		assertEquals("BANK-001", createdTransaction.getTrnsBnkAccnt());
		assertEquals("5000", createdTransaction.getTrnsAmt());
		assertEquals(SecuraConstants.PAYMENT_CURRENCY, createdTransaction.getTrnsCurrency());
		assertEquals(SecuraConstants.TRANSACTION_STATUS_SUCCESS, createdTransaction.getTrnsStatus());
		assertEquals("CAUSE", createdTransaction.getCause());
		assertEquals("FILES_JSON", createdTransaction.getTrnsFiles());
		assertEquals(LocalDate.parse("2026-04-20").atStartOfDay(), createdTransaction.getTrnsDate());
		assertNull(createdTransaction.getParentTransactionId());
		assertEquals("RCT-2001", updatedTransaction.getReceiptNumber());
		assertEquals("RECEIPT_BASE64", response.getReceipt());
		assertEquals(SuccessMessage.SUCC_MESSAGE_40, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_40, response.getMessageCode());

		ArgumentCaptor<CreateReceiptRequest> receiptRequestCaptor = ArgumentCaptor.forClass(CreateReceiptRequest.class);
		verify(receiptServices).createReceipt(receiptRequestCaptor.capture());
		CreateReceiptRequest receiptRequest = receiptRequestCaptor.getValue();
		assertEquals("Ledger Entry", receiptRequest.getReceiptType());
		assertFalse(receiptRequest.isPerheadFlag());
		assertFalse(receiptRequest.isUnitPriceRequired());
		assertEquals("5000", receiptRequest.getTotalAmount());
		assertEquals(createdTransaction.getTrnscId(), receiptRequest.getTransactionId());
		assertEquals("Corpus Fund", receiptRequest.getItems().get(0).getItemName());
		assertEquals("5000", receiptRequest.getItems().get(0).getAmount());
		assertEquals("CAUSE", receiptRequest.getItems().get(0).getType());
		assertEquals(1, receiptRequest.getTenderList().size());
		assertEquals(SecuraConstants.TRANSACTION_TENDER_ONLINE, receiptRequest.getTenderList().get(0).getTenderName());
		assertEquals("5000", receiptRequest.getTenderList().get(0).getAmountPaid());
		assertTrue(receiptRequest.getAddedCharges().isEmpty());
		assertNull(receiptRequest.getDiscFinReceipt());
	}

	@Test
	void ledgerEntry_shouldCreateMultipleTransactionsWithParentAndSharedReceipt() throws Exception {
		LedgerEntryRequest request = new LedgerEntryRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		header.setUserId("USR-001");
		request.setGenericHeader(header);
		request.setLedgerfor("Event Collection");
		request.setTrnsTenderList(List.of(SecuraConstants.TRANSACTION_TENDER_CASH, SecuraConstants.TRANSACTION_TENDER_ONLINE));
		request.setTrnsType(SecuraConstants.TRANSACTION_TYPE_CREDIT);
		request.setTrnsBnkAccnt("BANK-001");
		request.setTrnsAmt("2500");
		request.setTrnsStatus("PENDING");
		request.setCause("EVENT");
		request.setSupportedFile(List.of("receipt.pdf"));
		request.setRequiredReceiptFlag(true);

		CreateReceiptResponse createReceiptResponse = new CreateReceiptResponse();
		createReceiptResponse.setReceipt("RECEIPT_MULTI");
		createReceiptResponse.setReceiptNumber("RCT-2002");

		when(genericService.toJson(any())).thenReturn("FILES_JSON");
		when(receiptServices.createReceipt(any(CreateReceiptRequest.class))).thenReturn(createReceiptResponse);
		when(transactionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

		LedgerEntryResponse response = paymentServices.ledgerEntry(request);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Transaction>> transactionsCaptor = ArgumentCaptor.forClass((Class) List.class);
		verify(transactionRepository, times(2)).saveAll(transactionsCaptor.capture());
		List<List<Transaction>> savedBatches = transactionsCaptor.getAllValues();
		assertEquals(2, savedBatches.size());
		assertEquals(2, savedBatches.get(0).size());
		Transaction parentTransaction = savedBatches.get(0).get(0);
		Transaction childTransaction = savedBatches.get(0).get(1);
		assertNull(parentTransaction.getParentTransactionId());
		assertEquals(parentTransaction.getTrnscId(), childTransaction.getParentTransactionId());
		assertEquals(SecuraConstants.TRANSACTION_TENDER_CASH, parentTransaction.getTrnsTender());
		assertEquals(SecuraConstants.TRANSACTION_TENDER_ONLINE, childTransaction.getTrnsTender());
		assertEquals("FILES_JSON", parentTransaction.getTrnsFiles());
		assertEquals("FILES_JSON", childTransaction.getTrnsFiles());
		assertEquals("RCT-2002", savedBatches.get(1).get(0).getReceiptNumber());
		assertEquals("RCT-2002", savedBatches.get(1).get(1).getReceiptNumber());
		assertEquals("RECEIPT_MULTI", response.getReceipt());

		ArgumentCaptor<CreateReceiptRequest> receiptRequestCaptor = ArgumentCaptor.forClass(CreateReceiptRequest.class);
		verify(receiptServices).createReceipt(receiptRequestCaptor.capture());
		CreateReceiptRequest receiptRequest = receiptRequestCaptor.getValue();
		assertEquals(parentTransaction.getTrnscId(), receiptRequest.getTransactionId());
		assertEquals(2, receiptRequest.getTenderList().size());
		assertEquals(SecuraConstants.TRANSACTION_TENDER_CASH, receiptRequest.getTenderList().get(0).getTenderName());
		assertEquals(SecuraConstants.TRANSACTION_TENDER_ONLINE, receiptRequest.getTenderList().get(1).getTenderName());
		assertNull(receiptRequest.getTenderList().get(0).getAmountPaid());
		assertNull(receiptRequest.getTenderList().get(1).getAmountPaid());
		assertEquals("Event Collection", receiptRequest.getItems().get(0).getItemName());
		assertEquals("2500", receiptRequest.getItems().get(0).getAmount());
		assertEquals("EVENT", receiptRequest.getItems().get(0).getType());
	}

	@SuppressWarnings("unchecked")
	private List<DueAmountDetails> extractDueAmountDetailsList(ArgumentCaptor<Object> dueListCaptor) {
		for (Object captured : dueListCaptor.getAllValues()) {
			if (!(captured instanceof List<?> capturedList) || capturedList.isEmpty()) {
				continue;
			}
			if (capturedList.get(0) instanceof DueAmountDetails) {
				return (List<DueAmountDetails>) capturedList;
			}
		}
		return List.of();
	}

	@SuppressWarnings("unchecked")
	private List<List<DueAmountDetails>> extractAllDueAmountDetailsLists(ArgumentCaptor<Object> dueListCaptor) {
		List<List<DueAmountDetails>> dueLists = new ArrayList<>();
		for (Object captured : dueListCaptor.getAllValues()) {
			if (!(captured instanceof List<?> capturedList) || capturedList.isEmpty()) {
				continue;
			}
			if (capturedList.get(0) instanceof DueAmountDetails) {
				dueLists.add((List<DueAmountDetails>) capturedList);
			}
		}
		return dueLists;
	}
}
