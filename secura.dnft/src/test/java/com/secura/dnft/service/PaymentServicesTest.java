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
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.secura.dnft.dao.DocumentRepository;
import com.secura.dnft.dao.DueAmountDetailsRepository;
import com.secura.dnft.dao.FlatRepository;
import com.secura.dnft.dao.PaymentRepository;
import com.secura.dnft.dao.TransactionRepository;
import com.secura.dnft.entity.DocumentEntity;
import com.secura.dnft.entity.DueAmountDetailsEntity;
import com.secura.dnft.entity.DueAmountDetailsEntityId;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.entity.Transaction;
import com.secura.dnft.entity.Worklist;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.AddedCharges;
import com.secura.dnft.request.response.BankInstrumentTenderDetails;
import com.secura.dnft.request.response.CreateReceiptRequest;
import com.secura.dnft.request.response.CreateReceiptResponse;
import com.secura.dnft.request.response.CreatePaymentRequest;
import com.secura.dnft.request.response.DueAmountDetails;
import com.secura.dnft.request.response.DuePaymentAmountDetailsRequest;
import com.secura.dnft.request.response.DuePaymentAmountDetailsResponse;
import com.secura.dnft.request.response.GenericHeader;
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
	private TransactionRepository transactionRepository;

	@Mock
	private DocumentRepository documentRepository;

	@Mock
	private ReceiptServices receiptServices;

	@Mock
	private DueDetailsService dueDetailsService;

	@Mock
	private FlatRepository flatRepository;

	@Mock
	private DueAmountDetailsRepository dueAmountDetailsRepository;

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
		when(paymentRepository.findByPaymentIdAndAprmtId("PAY-1", "APR-1")).thenReturn(List.of(payment));

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
		when(paymentRepository.findByPaymentIdAndAprmtId("PAY-1", "APR-1")).thenReturn(List.of());

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
	void getDuePaymentAmountDetailsForCreatePayment_shouldReturnMapByPaymentCollectionCycleList() {
		CreatePaymentRequest request = new CreatePaymentRequest();
		request.setPaymentAmount("1200");
		request.setGst("10");
		request.setCollectionStartDate(Date.valueOf(LocalDate.parse("2026-05-11")));
		request.setCollectionEndDate(Date.valueOf(LocalDate.parse("2026-05-25")));
		request.setPaymentCollectionCycleList(List.of("halfyearly", "once"));
		request.setPaymentCollectionMode("pre");
		request.setPaymentCapita("PER_FLAT");
		request.setTodayDate(LocalDate.parse("2026-05-12"));
		DueAmountDetails dueAmountDetails = new DueAmountDetails();
		dueAmountDetails.setAmount("1200");
		Map<String, List<Map<String, DueAmountDetails>>> dueAmountDetailsEntityMap = new LinkedHashMap<>();
		dueAmountDetailsEntityMap.put(SecuraConstants.PAYMENT_CYCLE_HALF_YEARLY,
				List.of(new LinkedHashMap<>(Map.of("ALL", dueAmountDetails))));
		dueAmountDetailsEntityMap.put(SecuraConstants.PAYMENT_CYCLE_ONCE,
				List.of(new LinkedHashMap<>(Map.of("ALL", dueAmountDetails))));
		when(dueDetailsService.previewDuesForPayment(any(), any())).thenReturn(dueAmountDetailsEntityMap);

		GetDuePaymentAmountDetailsResponse response = paymentServices.getDuePaymentAmountDetails(request);

		assertNotNull(response.getDuePaymentAmountDetailsMap());
		assertEquals(2, response.getDuePaymentAmountDetailsMap().size());
		assertNotNull(response.getDueAmountDetailsEntityMap());
		assertEquals(2, response.getDueAmountDetailsEntityMap().size());
		assertTrue(response.getDuePaymentAmountDetailsMap().containsKey(SecuraConstants.PAYMENT_CYCLE_HALF_YEARLY));
		assertTrue(response.getDuePaymentAmountDetailsMap().containsKey(SecuraConstants.PAYMENT_CYCLE_ONCE));
		assertTrue(response.getDueAmountDetailsEntityMap().containsKey(SecuraConstants.PAYMENT_CYCLE_HALF_YEARLY));
		assertTrue(response.getDueAmountDetailsEntityMap().containsKey(SecuraConstants.PAYMENT_CYCLE_ONCE));
		assertEquals(LocalDate.parse("2026-05-11"),
				response.getDuePaymentAmountDetailsMap().get(SecuraConstants.PAYMENT_CYCLE_HALF_YEARLY).getDueDate());
		assertEquals(LocalDate.parse("2026-05-11"),
				response.getDuePaymentAmountDetailsMap().get(SecuraConstants.PAYMENT_CYCLE_ONCE).getDueDate());
		assertEquals("1200", response.getDueAmountDetailsEntityMap().get(SecuraConstants.PAYMENT_CYCLE_HALF_YEARLY)
				.get(0).get("ALL").getAmount());
	}

	@Test
	void getDuePaymentAmountDetailsForCreatePayment_shouldSupportLegacySingleCycleAlias() {
		CreatePaymentRequest request = new CreatePaymentRequest();
		request.setPaymentAmount("1200");
		request.setGst("10");
		request.setCollectionStartDate(Date.valueOf(LocalDate.parse("2026-05-11")));
		request.setCollectionEndDate(Date.valueOf(LocalDate.parse("2026-05-25")));
		request.setPaymentCollectionCycle("halfyearly");
		request.setPaymentCollectionMode("pre");
		request.setPaymentCapita("PER_FLAT");
		request.setTodayDate(LocalDate.parse("2026-05-12"));
		DueAmountDetails dueAmountDetails = new DueAmountDetails();
		dueAmountDetails.setAmount("1200");
		Map<String, List<Map<String, DueAmountDetails>>> dueAmountDetailsEntityMap = new LinkedHashMap<>();
		dueAmountDetailsEntityMap.put(SecuraConstants.PAYMENT_CYCLE_HALF_YEARLY,
				List.of(new LinkedHashMap<>(Map.of("ALL", dueAmountDetails))));
		when(dueDetailsService.previewDuesForPayment(any(), any())).thenReturn(dueAmountDetailsEntityMap);

		GetDuePaymentAmountDetailsResponse response = paymentServices.getDuePaymentAmountDetails(request);

		assertEquals(1, response.getDuePaymentAmountDetailsMap().size());
		assertTrue(response.getDuePaymentAmountDetailsMap().containsKey(SecuraConstants.PAYMENT_CYCLE_HALF_YEARLY));
		assertEquals(1, response.getDueAmountDetailsEntityMap().size());
		assertTrue(response.getDueAmountDetailsEntityMap().containsKey(SecuraConstants.PAYMENT_CYCLE_HALF_YEARLY));
	}

	@Test
	void createPayment_shouldSetCauseDirectlyFromRequest() throws Exception {
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
		request.setCause("custom_cause");
		request.setPartialPaymentAllowed(true);
		request.setDiscountCode("DISC10");
		request.setFineCode("FINE5");
		AddedCharges amountCharge = new AddedCharges();
		amountCharge.setChargeName("Late Fee");
		amountCharge.setChargeType("amount");
		amountCharge.setValue("100");
		request.setAddedCharges(List.of(amountCharge));

		when(genericService.toJson(any())).thenReturn("ADDED_CHARGES_JSON", "DISC_FIN_JSON");
		when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentServices.createPayment(request);

		ArgumentCaptor<PaymentEntity> paymentCaptor = ArgumentCaptor.forClass(PaymentEntity.class);
		verify(paymentRepository, times(1)).save(paymentCaptor.capture());
		verify(dueDetailsService, times(1)).calculateDuesForPayment(any(), eq(header));
		assertEquals("custom_cause", paymentCaptor.getValue().getCauseId());
		assertTrue(paymentCaptor.getValue().isPartialPaymentAllowed());
		assertEquals("APR-001", paymentCaptor.getValue().getAprmtId());
		assertEquals(SecuraConstants.PAYMENT_CYCLE_HALF_YEARLY, paymentCaptor.getValue().getPaymentCollectionCycle());
		assertEquals(SecuraConstants.PAYMENT_STATUS_ACTIVE, paymentCaptor.getValue().getStatus());
		assertEquals("USR-001", paymentCaptor.getValue().getCreatUsrId());
		assertEquals("ADDED_CHARGES_JSON", paymentCaptor.getValue().getAddedCharges());
		assertEquals("DISC_FIN_JSON", paymentCaptor.getValue().getDiscFin());
	}

	@Test
	void createPayment_shouldAllowNullCauseFromRequest() throws Exception {
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
		request.setPaymentCollectionCycle("monthly");
		request.setPaymentCollectionMode("pre");
		request.setCause(null);

		when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentServices.createPayment(request);

		ArgumentCaptor<PaymentEntity> paymentCaptor = ArgumentCaptor.forClass(PaymentEntity.class);
		verify(paymentRepository, times(1)).save(paymentCaptor.capture());
		assertNull(paymentCaptor.getValue().getCauseId());
	}

	@Test
	void createPayment_shouldCreateOneEntityPerPaymentCollectionCycle() throws Exception {
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
		request.setPaymentCollectionCycleList(List.of("half_yearly", "YEARLY", "quarterly"));
		request.setPaymentCollectionMode("pre");
		request.setPartialPaymentAllowed(true);

		when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentServices.createPayment(request);

		ArgumentCaptor<PaymentEntity> paymentCaptor = ArgumentCaptor.forClass(PaymentEntity.class);
		verify(paymentRepository, times(3)).save(paymentCaptor.capture());
		List<PaymentEntity> savedPayments = paymentCaptor.getAllValues();
		assertEquals(List.of(SecuraConstants.PAYMENT_CYCLE_HALF_YEARLY, SecuraConstants.PAYMENT_CYCLE_YEARLY,
				SecuraConstants.PAYMENT_CYCLE_QUATERLY),
				savedPayments.stream().map(PaymentEntity::getPaymentCollectionCycle).toList());
		assertTrue(savedPayments.stream().allMatch(PaymentEntity::isPartialPaymentAllowed));
		assertEquals(1L, savedPayments.stream().map(PaymentEntity::getPaymentId).distinct().count());
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

		when(genericService.toJson(eq(List.of("UPI", "CARD")))).thenReturn("ALLOWED_PAYMENT_MODES_JSON");
		when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentServices.createPayment(request);

		ArgumentCaptor<PaymentEntity> paymentCaptor = ArgumentCaptor.forClass(PaymentEntity.class);
		verify(paymentRepository, times(1)).save(paymentCaptor.capture());
		assertEquals("ALLOWED_PAYMENT_MODES_JSON", paymentCaptor.getValue().getAllowedPaymentModes());
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

		when(genericService.toJson(eq(List.of("A-101", "A-102", "A-103"))))
				.thenReturn("[\"A-101\",\"A-102\",\"A-103\"]");
		when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentServices.createPayment(request);

		ArgumentCaptor<PaymentEntity> paymentCaptor = ArgumentCaptor.forClass(PaymentEntity.class);
		verify(paymentRepository, times(1)).save(paymentCaptor.capture());
		assertEquals("[\"A-101\",\"A-102\",\"A-103\"]", paymentCaptor.getValue().getApplicableFor());
	}

	@Test
	void payDues_shouldUseTenderListPersistBankInstrumentAndBuildFormattedReceiptItem() throws Exception {
		PayDueRequest request = new PayDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		header.setUserId("USR-001");
		header.setFlatNo("A-101");
		request.setGenericHeader(header);
		request.setPaymentId("PAY-1001");
		request.setAmount("5000");
		request.setDueId("DUE1001");
		request.setPaymentCycle(SecuraConstants.PAYMENT_CYCLE_MONTHLY);
		request.setDueDate(LocalDate.parse("2027-03-01"));
		request.setPaymentName("CAM 2026-27");
		request.setDueStartDate(LocalDate.parse("2027-03-01"));
		request.setDueEndDate(LocalDate.parse("2028-05-31"));
		request.setTransactionStatus(SecuraConstants.TRANSACTION_STATUS_SUCCESS);
		request.setPaymentTenderDataList(List.of(createTender(SecuraConstants.TRANSACTION_TENDER_ONLINE, "5000")));
		request.setBankInstrumentTenderDetails(List.of(createBankInstrumentTenderDetails("DDPAB-001")));

		PaymentEntity paymentEntity = new PaymentEntity();
		paymentEntity.setPaymentId("PAY-1001");
		paymentEntity.setBankAccountId("BANK-001");
		paymentEntity.setCauseId("EVENT");
		when(paymentRepository.findFirstByPaymentId("PAY-1001")).thenReturn(Optional.of(paymentEntity));
		when(genericService.toJson(any())).thenAnswer(invocation -> {
			Object payload = invocation.getArgument(0);
			if (payload instanceof List<?> list && list.isEmpty()) {
				return "FILES_JSON";
			}
			if (payload instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof PaymentTenderData) {
				return "TRNS_TENDER_JSON";
			}
			if (payload instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof BankInstrumentTenderDetails) {
				return "BANK_INSTR_JSON";
			}
			return "JSON";
		});

		Flat flat = new Flat();
		flat.setFlatArea("1200");
		when(flatRepository.findById("A-101")).thenReturn(Optional.of(flat));

		DueAmountDetailsEntity dueEntity = new DueAmountDetailsEntity();
		dueEntity.setDueId("DUE1001");
		dueEntity.setCollectionCycle(SecuraConstants.PAYMENT_CYCLE_MONTHLY);
		dueEntity.setFlatArea("1200");
		dueEntity.setDueDate(LocalDate.parse("2027-03-01"));
		dueEntity.setAmount("4500");
		dueEntity.setGstAmount("270");
		dueEntity.setTotalAmount("4770");
		dueEntity.setAddedCharges("[]");
		when(dueAmountDetailsRepository.findById(
				new DueAmountDetailsEntityId("DUE1001", SecuraConstants.PAYMENT_CYCLE_MONTHLY, "1200", LocalDate.parse("2027-03-01"))))
				.thenReturn(Optional.of(dueEntity));
		when(genericService.fromJson(any(), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(List.of());

		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
		CreateReceiptResponse createReceiptResponse = new CreateReceiptResponse();
		createReceiptResponse.setReceipt("RECEIPT_BASE64");
		createReceiptResponse.setReceiptNumber("RCT-3001");
		when(receiptServices.createReceipt(any(CreateReceiptRequest.class))).thenReturn(createReceiptResponse);

		PayDueResponse response = paymentServices.payDues(request);

		ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
		verify(transactionRepository).save(transactionCaptor.capture());
		Transaction createdTransaction = transactionCaptor.getValue();
		assertEquals("TRNS_TENDER_JSON", createdTransaction.getTrnsTender());
		assertEquals("BANK_INSTR_JSON", createdTransaction.getBankInstrumentTenderDetails());
		assertEquals("DUE1001_MONTHLY_1200_2027-03-01", createdTransaction.getDueDetails());
		assertEquals(SecuraConstants.TRANSACTION_STATUS_SUCCESS, createdTransaction.getTrnsStatus());
		assertEquals("BANK-001", createdTransaction.getTrnsBnkAccnt());
		assertEquals("EVENT", createdTransaction.getCause());
		assertEquals("RCT-3001", createdTransaction.getReceiptNumber());

		ArgumentCaptor<CreateReceiptRequest> receiptRequestCaptor = ArgumentCaptor.forClass(CreateReceiptRequest.class);
		verify(receiptServices).createReceipt(receiptRequestCaptor.capture());
		CreateReceiptRequest receiptRequest = receiptRequestCaptor.getValue();
		assertEquals(createdTransaction.getTrnscId(), receiptRequest.getTransactionId());
		assertEquals(1, receiptRequest.getPaymentTenderDataList().size());
		assertEquals(SecuraConstants.TRANSACTION_TENDER_ONLINE,
				receiptRequest.getPaymentTenderDataList().get(0).getTenderName());
		assertEquals("5000", receiptRequest.getPaymentTenderDataList().get(0).getAmountPaid());
		assertEquals("CAM 2026-27 (Period: 1-Mar-2027 to 31-May-2028)", receiptRequest.getItems().get(0).getItemName());
		assertEquals("4500", receiptRequest.getItems().get(0).getAmount());
		assertEquals("4770", receiptRequest.getTotalAmount());
		assertEquals(1, receiptRequest.getAddedCharges().size());
		assertEquals("GST", receiptRequest.getAddedCharges().get(0).getChargeName());
		assertEquals("270", receiptRequest.getAddedCharges().get(0).getFinalChargeValue());

		assertEquals(SuccessMessage.SUCC_MESSAGE_33, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_33, response.getMessageCode());
		assertEquals("RCT-3001", response.getReceiptNumber());
		assertEquals("RECEIPT_BASE64", response.getReceipt());
	}

	@Test
	void payDues_shouldCreateWorklistWhenAnyTenderIsCash() throws Exception {
		PayDueRequest request = new PayDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		header.setUserId("USR-001");
		header.setFlatNo("A-101");
		request.setGenericHeader(header);
		request.setPaymentId("PAY-1002");
		request.setAmount("1500");
		request.setTransactionStatus(SecuraConstants.TRANSACTION_STATUS_SUCCESS);
		request.setPaymentTenderDataList(List.of(createTender(SecuraConstants.TRANSACTION_TENDER_CASH, "1500")));

		PaymentEntity paymentEntity = new PaymentEntity();
		paymentEntity.setPaymentId("PAY-1002");
		paymentEntity.setBankAccountId("BANK-002");
		when(paymentRepository.findFirstByPaymentId("PAY-1002")).thenReturn(Optional.of(paymentEntity));
		when(genericService.toJson(any())).thenAnswer(invocation -> {
			Object payload = invocation.getArgument(0);
			if (payload instanceof List<?> list && list.isEmpty()) {
				return "FILES_JSON";
			}
			if (payload instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof PaymentTenderData) {
				return "TRNS_TENDER_JSON";
			}
			return "JSON";
		});
		Worklist worklist = new Worklist();
		worklist.setWorklistTaskId("WL-1001");
		when(genericService.createWorklist(eq(SecuraConstants.WORKLIST_TYPE_TRANSACTION), eq("USR-001"), eq("APR-001"),
				any())).thenReturn(worklist);
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

		PayDueResponse response = paymentServices.payDues(request);

		verify(genericService, times(1)).createWorklist(eq(SecuraConstants.WORKLIST_TYPE_TRANSACTION), eq("USR-001"),
				eq("APR-001"), any());
		verify(genericService, times(1)).createWorklistAssignmentFlow("WL-1001", List.of("admin"));
		verify(receiptServices, never()).createReceipt(any(CreateReceiptRequest.class));
		assertNull(response.getReceipt());
		assertNull(response.getReceiptNumber());
	}

	@Test
	void payDues_shouldOmitPeriodSuffixForOnceCycleReceiptItem() throws Exception {
		PayDueRequest request = new PayDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		header.setUserId("USR-001");
		header.setFlatNo("A-101");
		request.setGenericHeader(header);
		request.setPaymentId("PAY-1003");
		request.setAmount("2500");
		request.setDueId("DUE1003");
		request.setPaymentCycle(SecuraConstants.PAYMENT_CYCLE_ONCE);
		request.setDueDate(LocalDate.parse("2026-05-29"));
		request.setPaymentName("Club House Booking");
		request.setDueStartDate(LocalDate.parse("2026-05-29"));
		request.setDueEndDate(LocalDate.parse("2026-05-31"));
		request.setTransactionStatus(SecuraConstants.TRANSACTION_STATUS_SUCCESS);
		request.setPaymentTenderDataList(List.of(createTender(SecuraConstants.TRANSACTION_TENDER_ONLINE, "2500")));

		PaymentEntity paymentEntity = new PaymentEntity();
		paymentEntity.setPaymentId("PAY-1003");
		paymentEntity.setBankAccountId("BANK-003");
		when(paymentRepository.findFirstByPaymentId("PAY-1003")).thenReturn(Optional.of(paymentEntity));
		when(genericService.toJson(any())).thenAnswer(invocation -> {
			Object payload = invocation.getArgument(0);
			if (payload instanceof List<?> list && list.isEmpty()) {
				return "FILES_JSON";
			}
			if (payload instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof PaymentTenderData) {
				return "TRNS_TENDER_JSON";
			}
			return "JSON";
		});

		Flat flat = new Flat();
		flat.setFlatArea("1200");
		when(flatRepository.findById("A-101")).thenReturn(Optional.of(flat));

		DueAmountDetailsEntity dueEntity = new DueAmountDetailsEntity();
		dueEntity.setDueId("DUE1003");
		dueEntity.setCollectionCycle(SecuraConstants.PAYMENT_CYCLE_ONCE);
		dueEntity.setFlatArea("1200");
		dueEntity.setDueDate(LocalDate.parse("2026-05-29"));
		dueEntity.setAmount("2500");
		dueEntity.setTotalAmount("2500");
		when(dueAmountDetailsRepository.findById(new DueAmountDetailsEntityId("DUE1003",
				SecuraConstants.PAYMENT_CYCLE_ONCE, "1200", LocalDate.parse("2026-05-29"))))
				.thenReturn(Optional.of(dueEntity));

		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
		CreateReceiptResponse createReceiptResponse = new CreateReceiptResponse();
		createReceiptResponse.setReceipt("RECEIPT_BASE64");
		createReceiptResponse.setReceiptNumber("RCT-3003");
		when(receiptServices.createReceipt(any(CreateReceiptRequest.class))).thenReturn(createReceiptResponse);

		PayDueResponse response = paymentServices.payDues(request);

		ArgumentCaptor<CreateReceiptRequest> receiptRequestCaptor = ArgumentCaptor.forClass(CreateReceiptRequest.class);
		verify(receiptServices).createReceipt(receiptRequestCaptor.capture());
		CreateReceiptRequest receiptRequest = receiptRequestCaptor.getValue();
		assertEquals("Club House Booking", receiptRequest.getItems().get(0).getItemName());
		assertEquals("RCT-3003", response.getReceiptNumber());
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
		request.setTrnsTenderList(List.of(createTender(SecuraConstants.TRANSACTION_TENDER_ONLINE, "5000")));
		request.setTrnsType(SecuraConstants.TRANSACTION_TYPE_CREDIT);
		request.setTrnsShrtDesc("Ledger entry");
		request.setTrnsBnkAccnt("BANK-001");
		request.setTrnsAmt("5000");
		request.setTrnsStatus(SecuraConstants.TRANSACTION_STATUS_SUCCESS);
		request.setCause("CAUSE");
		request.setBankInstrumentTenderDetails(List.of(createBankInstrumentTenderDetails("DDPAB-001")));
		request.setSupportedFileList(List.of(createLedgerDocument("PDF", "one.pdf", "PDF_DATA"),
				createLedgerDocument("IMG", "two.pdf", "IMG_DATA")));
		request.setRequiredReceiptFlag(true);

		CreateReceiptResponse createReceiptResponse = new CreateReceiptResponse();
		createReceiptResponse.setReceipt("RECEIPT_BASE64");
		createReceiptResponse.setReceiptNumber("RCT-2001");

		when(genericService.createDocumentId("PDF", SecuraConstants.LEDGER_DOC_FOR)).thenReturn("PDFLEDGER1001");
		when(genericService.createDocumentId("IMG", SecuraConstants.LEDGER_DOC_FOR)).thenReturn("IMGLEDGER1002");
		when(genericService.toJson(any())).thenReturn("JSON");
		when(genericService.toJson(request.getBankInstrumentTenderDetails())).thenReturn("BANK_INSTR_JSON");
		when(genericService.toJson(List.of("PDFLEDGER1001", "IMGLEDGER1002"))).thenReturn("FILES_JSON");
		when(receiptServices.createReceipt(any(CreateReceiptRequest.class))).thenReturn(createReceiptResponse);
		when(documentRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

		LedgerEntryResponse response = paymentServices.ledgerEntry(request);

		ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
		verify(transactionRepository).save(transactionCaptor.capture());
		Transaction createdTransaction = transactionCaptor.getValue();
		assertEquals("APR-001", createdTransaction.getAprmntId());
		assertEquals("USR-001", createdTransaction.getTrnsBy());
		assertEquals("JSON", createdTransaction.getTrnsTender());
		assertEquals(SecuraConstants.TRANSACTION_TYPE_CREDIT, createdTransaction.getTrnsType());
		assertEquals("Ledger entry", createdTransaction.getTrnsShrtDesc());
		assertEquals("BANK-001", createdTransaction.getTrnsBnkAccnt());
		assertEquals("5000", createdTransaction.getTrnsAmt());
		assertEquals(SecuraConstants.PAYMENT_CURRENCY, createdTransaction.getTrnsCurrency());
		assertEquals(SecuraConstants.TRANSACTION_STATUS_SUCCESS, createdTransaction.getTrnsStatus());
		assertEquals("CAUSE", createdTransaction.getCause());
		assertEquals("BANK_INSTR_JSON", createdTransaction.getBankInstrumentTenderDetails());
		assertEquals("FILES_JSON", createdTransaction.getTrnsFiles());
		assertEquals(LocalDate.parse("2026-04-20").atStartOfDay(), createdTransaction.getTrnsDate());
		assertEquals("RCT-2001", createdTransaction.getReceiptNumber());
		assertEquals("RECEIPT_BASE64", response.getReceipt());
		assertEquals(SuccessMessage.SUCC_MESSAGE_40, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_40, response.getMessageCode());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<DocumentEntity>> documentCaptor = ArgumentCaptor.forClass((Class) List.class);
		verify(documentRepository).saveAll(documentCaptor.capture());
		List<DocumentEntity> savedDocuments = documentCaptor.getValue();
		assertEquals(2, savedDocuments.size());
		assertEquals("PDFLEDGER1001", savedDocuments.get(0).getDocumentId());
		assertEquals("IMGLEDGER1002", savedDocuments.get(1).getDocumentId());
		assertEquals("PDF", savedDocuments.get(0).getDocumentType());
		assertEquals("IMG", savedDocuments.get(1).getDocumentType());
		assertEquals("one.pdf", savedDocuments.get(0).getDocumentName());
		assertEquals("two.pdf", savedDocuments.get(1).getDocumentName());

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
		assertEquals(1, receiptRequest.getPaymentTenderDataList().size());
		assertEquals(SecuraConstants.TRANSACTION_TENDER_ONLINE,
				receiptRequest.getPaymentTenderDataList().get(0).getTenderName());
		assertEquals("5000", receiptRequest.getPaymentTenderDataList().get(0).getAmountPaid());
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
		request.setTrnsTenderList(List.of(createTender(SecuraConstants.TRANSACTION_TENDER_CASH, "1000"),
				createTender(SecuraConstants.TRANSACTION_TENDER_ONLINE, "1500")));
		request.setTrnsType(SecuraConstants.TRANSACTION_TYPE_CREDIT);
		request.setTrnsBnkAccnt("BANK-001");
		request.setTrnsAmt("2500");
		request.setTrnsStatus("PENDING");
		request.setCause("EVENT");
		request.setSupportedFileList(List.of(createLedgerDocument("PDF", "receipt.pdf", "RECEIPT_DATA")));
		request.setRequiredReceiptFlag(true);

		CreateReceiptResponse createReceiptResponse = new CreateReceiptResponse();
		createReceiptResponse.setReceipt("RECEIPT_MULTI");
		createReceiptResponse.setReceiptNumber("RCT-2002");

		when(genericService.createDocumentId("PDF", SecuraConstants.LEDGER_DOC_FOR)).thenReturn("PDFLEDGER2001");
		when(genericService.toJson(any())).thenReturn("JSON");
		when(genericService.toJson(List.of("PDFLEDGER2001"))).thenReturn("FILES_JSON");
		when(receiptServices.createReceipt(any(CreateReceiptRequest.class))).thenReturn(createReceiptResponse);
		when(documentRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

		LedgerEntryResponse response = paymentServices.ledgerEntry(request);

		ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
		verify(transactionRepository).save(transactionCaptor.capture());
		Transaction createdTransaction = transactionCaptor.getValue();
		assertEquals("JSON", createdTransaction.getTrnsTender());
		assertEquals("FILES_JSON", createdTransaction.getTrnsFiles());
		assertEquals("RCT-2002", createdTransaction.getReceiptNumber());
		assertEquals("RECEIPT_MULTI", response.getReceipt());

		ArgumentCaptor<CreateReceiptRequest> receiptRequestCaptor = ArgumentCaptor.forClass(CreateReceiptRequest.class);
		verify(receiptServices).createReceipt(receiptRequestCaptor.capture());
		CreateReceiptRequest receiptRequest = receiptRequestCaptor.getValue();
		assertEquals(createdTransaction.getTrnscId(), receiptRequest.getTransactionId());
		assertEquals(2, receiptRequest.getPaymentTenderDataList().size());
		assertEquals(SecuraConstants.TRANSACTION_TENDER_CASH,
				receiptRequest.getPaymentTenderDataList().get(0).getTenderName());
		assertEquals(SecuraConstants.TRANSACTION_TENDER_ONLINE,
				receiptRequest.getPaymentTenderDataList().get(1).getTenderName());
		assertEquals("1000", receiptRequest.getPaymentTenderDataList().get(0).getAmountPaid());
		assertEquals("1500", receiptRequest.getPaymentTenderDataList().get(1).getAmountPaid());
		assertEquals("Event Collection", receiptRequest.getItems().get(0).getItemName());
		assertEquals("2500", receiptRequest.getItems().get(0).getAmount());
		assertEquals("EVENT", receiptRequest.getItems().get(0).getType());
	}

	@Test
	void ledgerEntry_shouldSaveTransactionsOnceWhenReceiptNotRequired() throws Exception {
		LedgerEntryRequest request = new LedgerEntryRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		header.setUserId("USR-001");
		request.setGenericHeader(header);
		request.setLedgerfor("Corpus Fund");
		request.setTrnsTenderList(List.of(createTender(SecuraConstants.TRANSACTION_TENDER_CASH, "1500")));
		request.setTrnsType("DEBIT");
		request.setTrnsAmt("1500");
		request.setSupportedFileList(List.of(createLedgerDocument("PDF", "doc.pdf", "DOC_DATA")));
		request.setRequiredReceiptFlag(false);

		when(genericService.createDocumentId("PDF", SecuraConstants.LEDGER_DOC_FOR)).thenReturn("PDFLEDGER3001");
		when(genericService.toJson(any())).thenReturn("JSON");
		when(genericService.toJson(List.of("PDFLEDGER3001"))).thenReturn("FILES_JSON");
		when(documentRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

		LedgerEntryResponse response = paymentServices.ledgerEntry(request);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<DocumentEntity>> documentCaptor = ArgumentCaptor.forClass((Class) List.class);
		verify(documentRepository).saveAll(documentCaptor.capture());
		assertEquals("doc.pdf", documentCaptor.getValue().get(0).getDocumentName());

		verify(transactionRepository, times(1)).save(any(Transaction.class));
		verify(receiptServices, never()).createReceipt(any(CreateReceiptRequest.class));
		assertNull(response.getReceipt());
		assertEquals(SuccessMessage.SUCC_MESSAGE_40, response.getMessage());
	}

	private DocumentEntity createLedgerDocument(String documentType, String documentName, String documentData) {
		DocumentEntity documentEntity = new DocumentEntity();
		documentEntity.setDocumentType(documentType);
		documentEntity.setDocumentName(documentName);
		documentEntity.setDocumentData(documentData);
		return documentEntity;
	}

	private PaymentTenderData createTender(String tenderName, String amountPaid) {
		PaymentTenderData tenderData = new PaymentTenderData();
		tenderData.setTenderName(tenderName);
		tenderData.setAmountPaid(amountPaid);
		return tenderData;
	}

	private BankInstrumentTenderDetails createBankInstrumentTenderDetails(String ddPayAtBranch) {
		BankInstrumentTenderDetails bankInstrumentTenderDetails = new BankInstrumentTenderDetails();
		bankInstrumentTenderDetails.setTenderType("DD");
		bankInstrumentTenderDetails.setDdPayAtBranch(ddPayAtBranch);
		return bankInstrumentTenderDetails;
	}

}
