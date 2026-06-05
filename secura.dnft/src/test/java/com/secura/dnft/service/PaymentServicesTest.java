package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Date;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.dao.DocumentRepository;
import com.secura.dnft.dao.DueAmountDetailsRepository;
import com.secura.dnft.dao.FlatRepository;
import com.secura.dnft.dao.PaymentRepository;
import com.secura.dnft.dao.TransDueDetailsRepository;
import com.secura.dnft.dao.TransactionRepository;
import com.secura.dnft.entity.DocumentEntity;
import com.secura.dnft.entity.DueAmountDetailsEntity;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.entity.Transaction;
import com.secura.dnft.entity.Worklist;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.AddedCharges;
import com.secura.dnft.request.response.ActionQRPaymentRequest;
import com.secura.dnft.request.response.ActionQRPaymentResponse;
import com.secura.dnft.request.response.ActionTransactionReviewWorkListRequest;
import com.secura.dnft.request.response.BankInstrumentTenderDetails;
import com.secura.dnft.request.response.CreateReceiptRequest;
import com.secura.dnft.request.response.CreateReceiptResponse;
import com.secura.dnft.request.response.CreatePaymentRequest;
import com.secura.dnft.request.response.DueAmountDetails;
import com.secura.dnft.request.response.DuePaymentAmountDetailsRequest;
import com.secura.dnft.request.response.DuePaymentAmountDetailsResponse;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.GetDuePaymentAmountDetailsResponse;
import com.secura.dnft.request.response.GenericResponse;
import com.secura.dnft.request.response.GetPaymentRequest;
import com.secura.dnft.request.response.GetPaymentResponse;
import com.secura.dnft.request.response.LedgerEntryRequest;
import com.secura.dnft.request.response.LedgerEntryResponse;
import com.secura.dnft.request.response.PayDueRequest;
import com.secura.dnft.request.response.PayDueResponse;
import com.secura.dnft.request.response.PaymentEntityModel;
import com.secura.dnft.request.response.PaymentTenderData;
import com.secura.dnft.request.response.ReconcileQRPaymentRequest;
import com.secura.dnft.request.response.ReconcileQRPaymentResponse;
import com.secura.dnft.request.response.UploadPastDueRequest;
import com.secura.dnft.request.response.UploadPastDueResponse;
import com.secura.dnft.request.response.ValidatePriorDuePaymnentRequest;

@ExtendWith(MockitoExtension.class)
class PaymentServicesTest {

	private static final String RECONCILE_ALL_FOUND_MESSAGE = "All QR Payment Transaction Verified. DownLoad The Excell TO Reconsile";
	private static final String RECONCILE_PARTIAL_MESSAGE = "Few QR Payment Transaction Verified. Recheck The Statement or Inputed Date Range For Other Transcations";

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

	@Mock
	private WorklistService worklistService;

	@Mock
	private TransDueDetailsRepository transDueDetailsRepository;

	@InjectMocks
	private PaymentServices paymentServices;

	@BeforeEach
	void setUp() {
		lenient().when(paymentRepository.findFirstByPaymentId(any())).thenReturn(Optional.empty());
		lenient().when(paymentRepository.findFirstByPaymentIdAndAprmtId(any(), any())).thenReturn(Optional.empty());
		lenient().when(paymentRepository.countByAprmtIdAndCauseIdIgnoreCase(any(), any())).thenReturn(0L);
		lenient().when(transactionRepository.findByAprmntIdAndTrnscId(any(), any())).thenReturn(List.of());
		lenient().when(transactionRepository.findByAprmntIdAndFlatIdAndPymntIdOrderByTrnsDateDesc(any(), any(), any()))
				.thenReturn(List.of());
	}

	@Test
	void getPayments_shouldReturnApartmentPaymentsWhenPaymentIdMissing() throws Exception {
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		GetPaymentRequest request = new GetPaymentRequest();
		request.setGenericHeader(header);

		PaymentEntity payment = new PaymentEntity();
		payment.setPaymentId("PAY-1");
		payment.setAprmtId("APR-1");
		payment.setPaymentCollectionCycle("[\"MONTHLY\",\"QUARTERLY\"]");
		when(paymentRepository.findByAprmtId("APR-1")).thenReturn(List.of(payment));
		when(genericService.fromJson(eq("[\"MONTHLY\",\"QUARTERLY\"]"), any(TypeReference.class)))
				.thenReturn(List.of("MONTHLY", "QUARTERLY"));

		GetPaymentResponse response = paymentServices.getPayments(request);

		assertEquals(1, response.getPaymentList().size());
		assertEquals("PAY-1", response.getPaymentList().get(0).getPaymentId());
		assertEquals(List.of("MONTHLY", "QUARTERLY"), response.getPaymentList().get(0).getPaymentCollectionCycleList());
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
		payment.setPaymentCollectionCycle("MONTHLY");
		when(paymentRepository.findByPaymentIdAndAprmtId("PAY-1", "APR-1")).thenReturn(List.of(payment));

		GetPaymentResponse response = paymentServices.getPayments(request);

		assertEquals(1, response.getPaymentList().size());
		assertEquals("PAY-1", response.getPaymentList().get(0).getPaymentId());
		assertEquals(List.of("MONTHLY"), response.getPaymentList().get(0).getPaymentCollectionCycleList());
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
	void getPayments_shouldGroupByPaymentIdAndMapDiscountFineCodes() throws Exception {
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		GetPaymentRequest request = new GetPaymentRequest();
		request.setGenericHeader(header);

		PaymentEntity monthly = new PaymentEntity();
		monthly.setPaymentId("PAY-1");
		monthly.setAprmtId("APR-1");
		monthly.setPaymentName("Maintenance");
		monthly.setPaymentCollectionCycle("MONTHLY");
		String discFinJson = "[{\"DISTFIN_TYPE\":\"DISCOUNT\",\"code\":\"DISC1\",\"Status\":\"Active\"},"
				+ "{\"DISTFIN_TYPE\":\"FINE\",\"code\":\"FINE1\",\"Status\":\"Active\"}]";
		monthly.setDiscFin(discFinJson);

		PaymentEntity quarterly = new PaymentEntity();
		quarterly.setPaymentId("PAY-1");
		quarterly.setAprmtId("APR-1");
		quarterly.setPaymentCollectionCycle("QUARTERLY");

		Map<String, String> discount = new LinkedHashMap<>();
		discount.put("DISTFIN_TYPE", "DISCOUNT");
		discount.put("code", "DISC1");
		Map<String, String> fine = new LinkedHashMap<>();
		fine.put("DISTFIN_TYPE", "FINE");
		fine.put("code", "FINE1");
		when(paymentRepository.findByAprmtId("APR-1")).thenReturn(List.of(monthly, quarterly));
		when(genericService.fromJson(eq(discFinJson), any(TypeReference.class)))
				.thenReturn(List.of(discount, fine));

		GetPaymentResponse response = paymentServices.getPayments(request);

		assertEquals(1, response.getPaymentList().size());
		PaymentEntityModel payment = response.getPaymentList().get(0);
		assertEquals("PAY-1", payment.getPaymentId());
		assertEquals("Maintenance", payment.getPaymentName());
		assertEquals(List.of("MONTHLY", "QUARTERLY"), payment.getPaymentCollectionCycleList());
		assertEquals("DISC1", payment.getDiscountCode());
		assertEquals("FINE1", payment.getFineCode());
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
	void getDuePaymentAmountDetailsForCreatePayment_shouldPassApplicableForToPreviewDues() {
		CreatePaymentRequest request = new CreatePaymentRequest();
		request.setPaymentAmount("1200");
		request.setGst("10");
		request.setCollectionStartDate(Date.valueOf(LocalDate.parse("2026-05-11")));
		request.setCollectionEndDate(Date.valueOf(LocalDate.parse("2026-05-25")));
		request.setPaymentCollectionCycle("monthly");
		request.setPaymentCollectionMode("pre");
		request.setPaymentCapita("PER_FLAT");
		request.setApplicableFor(List.of("A-101, A-102", "A-201"));
		when(genericService.toJson(eq(List.of("A-101", "A-102", "A-201"))))
				.thenReturn("[\"A-101\",\"A-102\",\"A-201\"]");
		when(dueDetailsService.previewDuesForPayment(any(), any())).thenReturn(new LinkedHashMap<>());

		paymentServices.getDuePaymentAmountDetails(request);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<PaymentEntity>> paymentEntitiesCaptor = ArgumentCaptor.forClass((Class) List.class);
		verify(dueDetailsService).previewDuesForPayment(paymentEntitiesCaptor.capture(), any());
		assertEquals("[\"A-101\",\"A-102\",\"A-201\"]",
				paymentEntitiesCaptor.getValue().get(0).getApplicableFor());
	}

	@Test
	void getPaymentId_shouldUseCauseCodeForKnownCause() {
		String paymentId = paymentServices.getPaymentId("FESTIVAL_FUND", "APT-1");

		assertEquals("EVN001", paymentId);
	}

	@Test
	void getPaymentId_shouldUseOtherCodeForUnknownCause() {
		String paymentId = paymentServices.getPaymentId("unknown-cause", "APT-1");

		assertEquals("OTH001", paymentId);
	}

	@Test
	void getPaymentId_shouldUseOtherCodeForNullCause() {
		String paymentId = paymentServices.getPaymentId(null, "APT-1");

		assertEquals("OTH001", paymentId);
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

		when(genericService.toJson(argThat(payload -> payload instanceof List<?> list
				&& !list.isEmpty() && list.get(0) instanceof String))).thenReturn("[\"HALFYEARLY\"]");
		when(genericService.toJson(argThat(payload -> payload instanceof List<?> list
				&& !list.isEmpty() && list.get(0) instanceof AddedCharges))).thenReturn("ADDED_CHARGES_JSON");
		when(genericService.toJson(argThat(payload -> payload instanceof List<?> list
				&& !list.isEmpty() && list.get(0) instanceof Map))).thenReturn("DISC_FIN_JSON");
		when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentServices.createPayment(request);

		ArgumentCaptor<PaymentEntity> paymentCaptor = ArgumentCaptor.forClass(PaymentEntity.class);
		verify(paymentRepository, times(1)).save(paymentCaptor.capture());
		verify(dueDetailsService, times(1)).calculateDuesForPaymentWithoutDiscFine(any(), eq(header));
		assertEquals("custom_cause", paymentCaptor.getValue().getCauseId());
		assertTrue(paymentCaptor.getValue().isPartialPaymentAllowed());
		assertEquals("APR-001", paymentCaptor.getValue().getAprmtId());
		assertEquals("[\"HALFYEARLY\"]", paymentCaptor.getValue().getPaymentCollectionCycle());
		assertEquals(SecuraConstants.PAYMENT_STATUS_ACTIVE, paymentCaptor.getValue().getStatus());
		assertEquals("USR-001", paymentCaptor.getValue().getCreatUsrId());
		assertEquals("ADDED_CHARGES_JSON", paymentCaptor.getValue().getAddedCharges());
		assertEquals("DISC_FIN_JSON", paymentCaptor.getValue().getDiscFin());
		assertEquals("N", paymentCaptor.getValue().getEmailSentflag());
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
	void createPayment_shouldCreateSingleEntityWithPaymentCollectionCycleJson() throws Exception {
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

		when(genericService.toJson(eq(List.of(SecuraConstants.PAYMENT_CYCLE_HALF_YEARLY,
				SecuraConstants.PAYMENT_CYCLE_YEARLY, SecuraConstants.PAYMENT_CYCLE_QUATERLY))))
						.thenReturn("[\"HALFYEARLY\",\"YEARLY\",\"QUARTERLY\"]");
		when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentServices.createPayment(request);

		ArgumentCaptor<PaymentEntity> paymentCaptor = ArgumentCaptor.forClass(PaymentEntity.class);
		verify(paymentRepository, times(1)).save(paymentCaptor.capture());
		PaymentEntity savedPayment = paymentCaptor.getValue();
		assertEquals("[\"HALFYEARLY\",\"YEARLY\",\"QUARTERLY\"]", savedPayment.getPaymentCollectionCycle());
		assertTrue(savedPayment.isPartialPaymentAllowed());
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
		request.setTransactionStatus("success");
		request.setPaymentTenderDataList(List.of(createTender(SecuraConstants.TRANSACTION_TENDER_ONLINE, "5000")));
		request.setBankInstrumentTenderDetails(List.of(createBankInstrumentTenderDetails("DDPAB-001")));

		PaymentEntity paymentEntity = new PaymentEntity();
		paymentEntity.setPaymentId("PAY-1001");
		paymentEntity.setBankAccountId("BANK-001");
		paymentEntity.setCauseId("EVENT");
		paymentEntity.setPaymentCapita("PER_SQFT");
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
		when(flatRepository.findByAprmntIdAndFlatNo(any(), "A-101")).thenReturn(Optional.of(flat));

		DueAmountDetailsEntity dueEntity = new DueAmountDetailsEntity();
		dueEntity.setDueId("DUE1001");
		dueEntity.setCollectionCycle(SecuraConstants.PAYMENT_CYCLE_MONTHLY);
		dueEntity.setFlatArea("1200");
		dueEntity.setDueDate(LocalDate.parse("2027-03-01"));
		dueEntity.setAmount("4500");
		dueEntity.setGstAmount("270");
		dueEntity.setTotalAmount("4770");
		dueEntity.setAddedCharges("[]");
		when(dueAmountDetailsRepository.findByAprmntIdAndDueIdAndCollectionCycleAndFlatAreaAndDueDate(
				"APR-001", "DUE1001", SecuraConstants.PAYMENT_CYCLE_MONTHLY, "1200", LocalDate.parse("2027-03-01")))
				.thenReturn(Optional.of(dueEntity));
		when(genericService.fromJson(any(), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(List.of());

		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
		CreateReceiptResponse createReceiptResponse = new CreateReceiptResponse();
		createReceiptResponse.setReceipt("RECEIPT_BASE64");
		createReceiptResponse.setReceiptNumber("RCT-3001");
		when(receiptServices.createReceipt(any(CreateReceiptRequest.class))).thenReturn(createReceiptResponse);

		PayDueResponse response = paymentServices.payDues(request);

		ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
		verify(transactionRepository, atLeastOnce()).save(transactionCaptor.capture());
		List<Transaction> savedTransactions = transactionCaptor.getAllValues();
		Transaction createdTransaction = savedTransactions.get(savedTransactions.size() - 1);
		assertEquals("TRNS_TENDER_JSON", createdTransaction.getTrnsTender());
		assertEquals("BANK_INSTR_JSON", createdTransaction.getBankInstrumentTenderDetails());
		assertEquals("DUE1001_MONTHLY_1200_2027-03-01", createdTransaction.getDueDetails());
		assertEquals(SecuraConstants.TRANSACTION_STATUS_SUCCESS, createdTransaction.getTrnsStatus());
		assertEquals("BANK-001", createdTransaction.getTrnsBnkAccnt());
		assertEquals(SecuraConstants.TRANSACTION_THIRD_PARTY_RAZOR_PAY, createdTransaction.getThirdPartyName());
		assertEquals("EVENT", createdTransaction.getCause());
		assertEquals("RCT-3001", createdTransaction.getReceiptNumber());
		assertEquals("N", createdTransaction.getEmailSentflag());
		assertTransactionIdFormat(createdTransaction.getTrnscId(), "APR-001");

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
		assertNull(response.getQrIdentifier());
	}

	@Test
	void payDues_shouldNotSetReceiptInResponseWhenOnlinePaymentFails() throws Exception {
		PayDueRequest request = new PayDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		header.setUserId("USR-001");
		header.setFlatNo("A-101");
		request.setGenericHeader(header);
		request.setPaymentId("PAY-1002");
		request.setAmount("5000");
		request.setDueId("DUE1002");
		request.setPaymentCycle(SecuraConstants.PAYMENT_CYCLE_MONTHLY);
		request.setDueDate(LocalDate.parse("2027-03-01"));
		request.setPaymentName("CAM 2026-27");
		request.setTransactionStatus(SecuraConstants.TRANSACTION_STATUS_FAILED);
		request.setPaymentTenderDataList(List.of(createTender(SecuraConstants.TRANSACTION_TENDER_ONLINE, "5000")));

		PaymentEntity paymentEntity = new PaymentEntity();
		paymentEntity.setPaymentId("PAY-1002");
		paymentEntity.setBankAccountId("BANK-001");
		paymentEntity.setCauseId("EVENT");
		paymentEntity.setPaymentCapita("PER_SQFT");
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

		Flat flat = new Flat();
		flat.setFlatArea("1200");
		when(flatRepository.findByAprmntIdAndFlatNo(any(), "A-101")).thenReturn(Optional.of(flat));

		DueAmountDetailsEntity dueEntity = new DueAmountDetailsEntity();
		dueEntity.setDueId("DUE1002");
		dueEntity.setCollectionCycle(SecuraConstants.PAYMENT_CYCLE_MONTHLY);
		dueEntity.setFlatArea("1200");
		dueEntity.setDueDate(LocalDate.parse("2027-03-01"));
		dueEntity.setAmount("4500");
		dueEntity.setGstAmount("270");
		dueEntity.setTotalAmount("4770");
		dueEntity.setAddedCharges("[]");
		when(dueAmountDetailsRepository.findByAprmntIdAndDueIdAndCollectionCycleAndFlatAreaAndDueDate(
				"APR-001", "DUE1002", SecuraConstants.PAYMENT_CYCLE_MONTHLY, "1200", LocalDate.parse("2027-03-01")))
				.thenReturn(Optional.of(dueEntity));
		when(genericService.fromJson(any(), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(List.of());

		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
		CreateReceiptResponse createReceiptResponse = new CreateReceiptResponse();
		createReceiptResponse.setReceipt("RECEIPT_BASE64");
		createReceiptResponse.setReceiptNumber("RCT-3002");
		when(receiptServices.createReceipt(any(CreateReceiptRequest.class))).thenReturn(createReceiptResponse);

		PayDueResponse response = paymentServices.payDues(request);

		assertEquals(SuccessMessage.SUCC_MESSAGE_33, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_33, response.getMessageCode());
		assertNull(response.getReceiptNumber());
		assertNull(response.getReceipt());
	}

	@Test
	void payDues_shouldGenerateQrIdentifierForPendingQrTransactions() throws Exception {
		PayDueRequest request = new PayDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		header.setUserId("USR-001");
		header.setFlatNo("A-101");
		request.setGenericHeader(header);
		request.setPaymentId("PAY-QR-1");
		request.setAmount("5000");
		request.setDueId("DUE-QR-1");
		request.setPaymentCycle(SecuraConstants.PAYMENT_CYCLE_MONTHLY);
		request.setDueDate(LocalDate.parse("2027-03-01"));
		request.setPaymentName("CAM 2026-27");
		request.setTransactionStatus(SecuraConstants.TRANSACTION_STATUS_PENDING);
		request.setPaymentTenderDataList(List.of(createTender("SOCIETY_QR", "5000")));

		PaymentEntity paymentEntity = new PaymentEntity();
		paymentEntity.setPaymentId("PAY-QR-1");
		paymentEntity.setBankAccountId("BANK-001");
		paymentEntity.setCauseId("EVENT");
		paymentEntity.setPaymentCapita("PER_SQFT");
		when(paymentRepository.findFirstByPaymentId("PAY-QR-1")).thenReturn(Optional.of(paymentEntity));
		when(genericService.toJson(any())).thenAnswer(invocation -> {
			Object payload = invocation.getArgument(0);
			if (payload instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof PaymentTenderData) {
				return "[{\"tenderName\":\"SOCIETY_QR\"}]";
			}
			return "JSON";
		});
		when(genericService.fromJson(any(), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(List.of());

		Flat flat = new Flat();
		flat.setFlatArea("1200");
		when(flatRepository.findByAprmntIdAndFlatNo(any(), "A-101")).thenReturn(Optional.of(flat));

		DueAmountDetailsEntity dueEntity = new DueAmountDetailsEntity();
		dueEntity.setDueId("DUE-QR-1");
		dueEntity.setCollectionCycle(SecuraConstants.PAYMENT_CYCLE_MONTHLY);
		dueEntity.setFlatArea("1200");
		dueEntity.setDueDate(LocalDate.parse("2027-03-01"));
		dueEntity.setAmount("4500");
		dueEntity.setTotalAmount("4500");
		when(dueAmountDetailsRepository.findByAprmntIdAndDueIdAndCollectionCycleAndFlatAreaAndDueDate(any(), any(), any(), any(), any()))
				.thenReturn(Optional.of(dueEntity));

		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(receiptServices.createReceipt(any(CreateReceiptRequest.class))).thenReturn(new CreateReceiptResponse());
		Worklist worklist = new Worklist();
		worklist.setWorklistId("WL-001");
		when(worklistService.createTransactionReviewWorklist(any(), any())).thenReturn(worklist);

		PayDueResponse response = paymentServices.payDues(request);

		ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
		verify(transactionRepository, atLeastOnce()).save(transactionCaptor.capture());
		Transaction createdTransaction = transactionCaptor.getAllValues().get(0);
		assertNotNull(createdTransaction.getQrIdentifier());
		assertEquals(5, createdTransaction.getQrIdentifier().length());
		assertTrue(createdTransaction.getQrIdentifier().matches("^[A-Z0-9]{5}$"));
		assertEquals(createdTransaction.getQrIdentifier(), response.getQrIdentifier());
		assertEquals("N", createdTransaction.getEmailSentflag());
	}

	@Test
	void payDues_shouldUseAllInDueDetailsWhenPaymentCapitaIsNotPerSqft() throws Exception {
		PayDueRequest request = new PayDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		header.setUserId("USR-001");
		header.setFlatNo("A-101");
		request.setGenericHeader(header);
		request.setPaymentId("PAY-1004");
		request.setAmount("2500");
		request.setDueId("DUE1004");
		request.setPaymentCycle(SecuraConstants.PAYMENT_CYCLE_MONTHLY);
		request.setDueDate(LocalDate.parse("2026-06-01"));
		request.setPaymentName("Water");
		request.setTransactionStatus(SecuraConstants.TRANSACTION_STATUS_SUCCESS);
		request.setPaymentTenderDataList(List.of(createTender(SecuraConstants.TRANSACTION_TENDER_ONLINE, "2500")));

		PaymentEntity paymentEntity = new PaymentEntity();
		paymentEntity.setPaymentId("PAY-1004");
		paymentEntity.setBankAccountId("BANK-003");
		paymentEntity.setPaymentCapita("PER_HEAD");
		when(paymentRepository.findFirstByPaymentId("PAY-1004")).thenReturn(Optional.of(paymentEntity));
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
		when(flatRepository.findByAprmntIdAndFlatNo(any(), "A-101")).thenReturn(Optional.of(flat));

		DueAmountDetailsEntity dueEntity = new DueAmountDetailsEntity();
		dueEntity.setDueId("DUE1004");
		dueEntity.setCollectionCycle(SecuraConstants.PAYMENT_CYCLE_MONTHLY);
		dueEntity.setFlatArea("1200");
		dueEntity.setDueDate(LocalDate.parse("2026-06-01"));
		dueEntity.setAmount("2500");
		dueEntity.setTotalAmount("2500");
		when(dueAmountDetailsRepository.findByAprmntIdAndDueIdAndCollectionCycleAndFlatAreaAndDueDate(any(), any(), any(), any(), any()))
				.thenReturn(Optional.of(dueEntity));

		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
		CreateReceiptResponse createReceiptResponse = new CreateReceiptResponse();
		createReceiptResponse.setReceipt("RECEIPT_BASE64");
		createReceiptResponse.setReceiptNumber("RCT-5004");
		when(receiptServices.createReceipt(any(CreateReceiptRequest.class))).thenReturn(createReceiptResponse);

		paymentServices.payDues(request);

		ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
		verify(transactionRepository, atLeastOnce()).save(transactionCaptor.capture());
		List<Transaction> savedTransactions = transactionCaptor.getAllValues();
		assertEquals("DUE1004_MONTHLY_ALL_2026-06-01",
				savedTransactions.get(savedTransactions.size() - 1).getDueDetails());
	}

	@Test
	void payDues_shouldUsePaidDueDetailsFromRequestWithoutRepositoryLookup() throws Exception {
		PayDueRequest request = new PayDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		header.setUserId("USR-001");
		header.setFlatNo("A-101");
		request.setGenericHeader(header);
		request.setPaymentId("PAY-1005");
		request.setAmount("2500");
		request.setDueId("DUE1005");
		request.setPaymentCycle(SecuraConstants.PAYMENT_CYCLE_MONTHLY);
		request.setDueDate(LocalDate.parse("2026-07-01"));
		request.setPaymentName("Water");
		// Keep the transaction unsuccessful so the test only covers the request-provided due lookup path.
		request.setTransactionStatus(SecuraConstants.TRANSACTION_STATUS_FAILED);
		request.setPaymentTenderDataList(List.of(createTender(SecuraConstants.TRANSACTION_TENDER_ONLINE, "2500")));

		DueAmountDetailsEntity dueEntity = new DueAmountDetailsEntity();
		dueEntity.setDueId("DUE1005");
		dueEntity.setCollectionCycle(SecuraConstants.PAYMENT_CYCLE_MONTHLY);
		dueEntity.setFlatArea("1200");
		dueEntity.setDueDate(LocalDate.parse("2026-07-01"));
		dueEntity.setAmount("2200");
		dueEntity.setGstAmount("300");
		dueEntity.setTotalAmount("2500");
		request.setPaidDueDetails(dueEntity);

		PaymentEntity paymentEntity = new PaymentEntity();
		paymentEntity.setPaymentId("PAY-1005");
		paymentEntity.setBankAccountId("BANK-005");
		paymentEntity.setPaymentCapita("PER_SQFT");
		when(paymentRepository.findFirstByPaymentId("PAY-1005")).thenReturn(Optional.of(paymentEntity));
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
		when(flatRepository.findByAprmntIdAndFlatNo(any(), "A-101")).thenReturn(Optional.of(flat));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
		CreateReceiptResponse createReceiptResponse = new CreateReceiptResponse();
		createReceiptResponse.setReceipt("RECEIPT_BASE64");
		createReceiptResponse.setReceiptNumber("RCT-3005");
		when(receiptServices.createReceipt(any(CreateReceiptRequest.class))).thenReturn(createReceiptResponse);

		PayDueResponse response = paymentServices.payDues(request);

		verify(dueAmountDetailsRepository, never()).findByAprmntIdAndDueIdAndCollectionCycleAndFlatAreaAndDueDate(any(), any(), any(), any(), any());
		ArgumentCaptor<CreateReceiptRequest> receiptRequestCaptor = ArgumentCaptor.forClass(CreateReceiptRequest.class);
		verify(receiptServices).createReceipt(receiptRequestCaptor.capture());
		assertEquals("2200", receiptRequestCaptor.getValue().getItems().get(0).getAmount());
		assertEquals("2500", receiptRequestCaptor.getValue().getTotalAmount());
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
		worklist.setWorklistId("WL-1001");
		when(worklistService.createTransactionReviewWorklist(any(), eq(header))).thenReturn(worklist);
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
		CreateReceiptResponse createReceiptResponse = new CreateReceiptResponse();
		createReceiptResponse.setReceipt("RECEIPT_BASE64");
		createReceiptResponse.setReceiptNumber("RCT-1002");
		when(receiptServices.createReceipt(any(CreateReceiptRequest.class))).thenReturn(createReceiptResponse);

		PayDueResponse response = paymentServices.payDues(request);

		verify(worklistService, times(1)).createTransactionReviewWorklist(any(), eq(header));
		ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
		verify(transactionRepository, atLeastOnce()).save(transactionCaptor.capture());
		assertEquals("", transactionCaptor.getAllValues().get(0).getThirdPartyName());
		verify(receiptServices, times(1)).createReceipt(any(CreateReceiptRequest.class));
		assertNull(response.getReceipt());
		assertNull(response.getReceiptNumber());
	}

	@Test
	void payDues_shouldSetUnitPriceQuantityAndPerHeadFlagsForPerHeadPayments() throws Exception {
		PayDueRequest request = new PayDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		header.setUserId("USR-001");
		header.setFlatNo("A-101");
		request.setGenericHeader(header);
		request.setPaymentId("PAY-2001");
		request.setAmount("3000");
		request.setDueId("DUE2001");
		request.setPaymentCycle(SecuraConstants.PAYMENT_CYCLE_MONTHLY);
		request.setDueDate(LocalDate.parse("2027-04-01"));
		request.setPaymentName("Water");
		request.setNoOfPersons("3");
		request.setTransactionStatus(SecuraConstants.TRANSACTION_STATUS_SUCCESS);
		request.setPaymentTenderDataList(List.of(createTender(SecuraConstants.TRANSACTION_TENDER_ONLINE, "3000")));

		PaymentEntity paymentEntity = new PaymentEntity();
		paymentEntity.setPaymentId("PAY-2001");
		paymentEntity.setBankAccountId("BANK-010");
		when(paymentRepository.findFirstByPaymentId("PAY-2001")).thenReturn(Optional.of(paymentEntity));
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
		when(flatRepository.findByAprmntIdAndFlatNo(any(), "A-101")).thenReturn(Optional.of(flat));

		DueAmountDetailsEntity dueEntity = new DueAmountDetailsEntity();
		dueEntity.setDueId("DUE2001");
		dueEntity.setCollectionCycle(SecuraConstants.PAYMENT_CYCLE_MONTHLY);
		dueEntity.setFlatArea("1200");
		dueEntity.setDueDate(LocalDate.parse("2027-04-01"));
		dueEntity.setPaymentCapita("PER_HEAD");
		dueEntity.setAmount("1000");
		dueEntity.setGstAmount("120");
		dueEntity.setAddedCharges("[{\"chargeName\":\"Maintenance\",\"chargeType\":\"amount\",\"value\":\"50\",\"finalChargeValue\":\"50\"}]");
		dueEntity.setDiscountCode("DISC-1");
		dueEntity.setDiscValue("2");
		dueEntity.setDiscountMode("amount");
		dueEntity.setDiscountedAmount("20");
		dueEntity.setFineCode("FINE-1");
		dueEntity.setFnValue("1");
		dueEntity.setFineType("amount");
		dueEntity.setFineMode("monthly");
		dueEntity.setFineAmount("30");
		dueEntity.setTotalAmount("1180");
		when(dueAmountDetailsRepository.findByAprmntIdAndDueIdAndCollectionCycleAndFlatAreaAndDueDate(any(), any(), any(), any(), any())).thenReturn(Optional.of(dueEntity));
		when(genericService.fromJson(any(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
				.thenReturn(List.of(new AddedCharges() {{
					setChargeName("Maintenance");
					setChargeType("amount");
					setValue("50");
					setFinalChargeValue("50");
				}}));

		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
		CreateReceiptResponse createReceiptResponse = new CreateReceiptResponse();
		createReceiptResponse.setReceipt("RECEIPT_BASE64");
		createReceiptResponse.setReceiptNumber("RCT-5001");
		when(receiptServices.createReceipt(any(CreateReceiptRequest.class))).thenReturn(createReceiptResponse);

		paymentServices.payDues(request);

		ArgumentCaptor<CreateReceiptRequest> receiptRequestCaptor = ArgumentCaptor.forClass(CreateReceiptRequest.class);
		verify(receiptServices).createReceipt(receiptRequestCaptor.capture());
		CreateReceiptRequest receiptRequest = receiptRequestCaptor.getValue();
		assertTrue(receiptRequest.isPerheadFlag());
		assertTrue(receiptRequest.isUnitPriceRequired());
		assertEquals("1000", receiptRequest.getItems().get(0).getUnitPrice());
		assertEquals("3", receiptRequest.getItems().get(0).getQuantity());
		assertEquals("3000", receiptRequest.getItems().get(0).getAmount());
		assertEquals("3540", receiptRequest.getTotalAmount());
		assertEquals(2, receiptRequest.getAddedCharges().size());
		assertEquals("150", receiptRequest.getAddedCharges().get(0).getValue());
		assertEquals("150", receiptRequest.getAddedCharges().get(0).getFinalChargeValue());
		assertEquals("360", receiptRequest.getAddedCharges().get(1).getValue());
		assertEquals("360", receiptRequest.getAddedCharges().get(1).getFinalChargeValue());
		assertNotNull(receiptRequest.getDiscFinReceipt());
		assertEquals("60", receiptRequest.getDiscFinReceipt().getDiscountAmount());
		assertEquals("90", receiptRequest.getDiscFinReceipt().getFineAmount());
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
		when(flatRepository.findByAprmntIdAndFlatNo(any(), "A-101")).thenReturn(Optional.of(flat));

		DueAmountDetailsEntity dueEntity = new DueAmountDetailsEntity();
		dueEntity.setDueId("DUE1003");
		dueEntity.setCollectionCycle(SecuraConstants.PAYMENT_CYCLE_ONCE);
		dueEntity.setFlatArea("1200");
		dueEntity.setDueDate(LocalDate.parse("2026-05-29"));
		dueEntity.setAmount("2500");
		dueEntity.setTotalAmount("2500");
		when(dueAmountDetailsRepository.findByAprmntIdAndDueIdAndCollectionCycleAndFlatAreaAndDueDate(any(), any(), any(), any(), any()))
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
	void payDues_shouldRemoveCoveredDuesAndTrackPaidFlatsOnSuccessfulPayment() throws Exception {
		PayDueRequest request = new PayDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		header.setUserId("USR-001");
		header.setFlatNo("A-101");
		request.setGenericHeader(header);
		request.setPaymentId("PAY-9001");
		request.setPaymentName("Maintenance");
		request.setAmount("9000");
		request.setDueId("DUE-Q1");
		request.setPaymentCycle(SecuraConstants.PAYMENT_CYCLE_QUATERLY);
		request.setDueDate(LocalDate.parse("2025-01-01"));
		request.setTransactionStatus(SecuraConstants.TRANSACTION_STATUS_SUCCESS);
		request.setPaymentTenderDataList(List.of(createTender(SecuraConstants.TRANSACTION_TENDER_ONLINE, "9000")));

		PaymentEntity paymentEntity = new PaymentEntity();
		paymentEntity.setPaymentId("PAY-9001");
		paymentEntity.setBankAccountId("BANK-9001");
		paymentEntity.setPaymentCapita("PER_SQFT");
		paymentEntity.setApplicableFor("PAYMENT_APPLICABLE");
		when(paymentRepository.findFirstByPaymentId("PAY-9001")).thenReturn(Optional.of(paymentEntity));

		Flat flat = new Flat();
		flat.setFlatNo("A-101");
		flat.setFlatArea("1200");
		flat.setFlatPndngPaymntLst("FLAT_PENDING");
		when(flatRepository.findByAprmntIdAndFlatNo(any(), "A-101")).thenReturn(Optional.of(flat));

		DueAmountDetailsEntity paidQuarterlyDue = createDueEntity("DUE-Q1", SecuraConstants.PAYMENT_CYCLE_QUATERLY, "1200",
				LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-01"), LocalDate.parse("2025-03-31"), "PAY-9001",
				"APPL_Q1");
		paidQuarterlyDue.setPaymentCapita("PER_SQFT");
		DueAmountDetailsEntity monthly1 = createDueEntity("DUE-M1", SecuraConstants.PAYMENT_CYCLE_MONTHLY, "1200",
				LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"), "PAY-9001",
				"APPL_M1");
		DueAmountDetailsEntity monthly2 = createDueEntity("DUE-M2", SecuraConstants.PAYMENT_CYCLE_MONTHLY, "1200",
				LocalDate.parse("2025-02-01"), LocalDate.parse("2025-02-01"), LocalDate.parse("2025-02-28"), "PAY-9001",
				"APPL_M2");
		DueAmountDetailsEntity monthly3 = createDueEntity("DUE-M3", SecuraConstants.PAYMENT_CYCLE_MONTHLY, "1200",
				LocalDate.parse("2025-03-01"), LocalDate.parse("2025-03-01"), LocalDate.parse("2025-03-31"), "PAY-9001",
				"APPL_M3");
		DueAmountDetailsEntity halfYearly = createDueEntity("DUE-H1", SecuraConstants.PAYMENT_CYCLE_HALF_YEARLY, "1200",
				LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-01"), LocalDate.parse("2025-06-30"), "PAY-9001",
				"APPL_H1");

		when(dueAmountDetailsRepository.findByAprmntIdAndDueIdAndCollectionCycleAndFlatAreaAndDueDate(any(), any(), any(), any(), any())).thenReturn(Optional.of(paidQuarterlyDue));
		when(dueAmountDetailsRepository.findByPaymentId("PAY-9001"))
				.thenReturn(new ArrayList<>(List.of(paidQuarterlyDue, monthly1, monthly2, monthly3, halfYearly)));
		when(dueAmountDetailsRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

		when(genericService.fromJson(any(), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenAnswer(invocation -> {
			String marker = invocation.getArgument(0);
			if (marker != null && marker.startsWith("[") && marker.endsWith("]")) {
				String content = marker.substring(1, marker.length() - 1).trim();
				if (content.isEmpty()) {
					return new ArrayList<>();
				}
				return new ArrayList<>(List.of(content.split(",\\s*")));
			}
			return switch (marker) {
			case "FLAT_PENDING" -> new ArrayList<>(List.of("DUE-Q1_QUATERLY_1200_2025-01-01", "DUE-M1_MONTHLY_1200_2025-01-01",
					"DUE-M2_MONTHLY_1200_2025-02-01", "DUE-M3_MONTHLY_1200_2025-03-01", "DUE-H1_HALF_YEARLY_1200_2025-01-01",
					"OTHER_DUE_MONTHLY_1200_2025-04-01"));
			case "APPL_Q1", "APPL_M1", "APPL_M2", "APPL_M3", "APPL_H1" -> new ArrayList<>(List.of("A-101", "A-102", "A-101"));
			case "PAYMENT_APPLICABLE" -> new ArrayList<>(List.of("A-101", "A-102"));
			default -> new ArrayList<>();
			};
		});
		when(genericService.toJson(any())).thenAnswer(invocation -> {
			Object value = invocation.getArgument(0, Object.class);
			return value == null ? null : value.toString();
		});

		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
		CreateReceiptResponse createReceiptResponse = new CreateReceiptResponse();
		createReceiptResponse.setReceipt("RECEIPT");
		createReceiptResponse.setReceiptNumber("RCT-9001");
		when(receiptServices.createReceipt(any(CreateReceiptRequest.class))).thenReturn(createReceiptResponse);

		paymentServices.payDues(request);

		ArgumentCaptor<Flat> flatCaptor = ArgumentCaptor.forClass(Flat.class);
		verify(flatRepository).save(flatCaptor.capture());
		assertFalse(flatCaptor.getValue().getFlatPndngPaymntLst().contains("DUE-Q1_QUATERLY_1200_2025-01-01"));
		assertTrue(flatCaptor.getValue().getFlatPndngPaymntLst().contains("OTHER_DUE_MONTHLY_1200_2025-04-01"));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<DueAmountDetailsEntity>> dueSaveCaptor = ArgumentCaptor.forClass((Class) List.class);
		verify(dueAmountDetailsRepository).saveAll(dueSaveCaptor.capture());
		assertEquals(5, dueSaveCaptor.getValue().size());
		DueAmountDetailsEntity savedQuarterlyDue = dueSaveCaptor.getValue().stream().filter(d -> "DUE-Q1".equals(d.getDueId()))
				.findFirst().orElseThrow();
		DueAmountDetailsEntity savedMonthly1 = dueSaveCaptor.getValue().stream().filter(d -> "DUE-M1".equals(d.getDueId()))
				.findFirst().orElseThrow();
		assertEquals("[A-101]", savedQuarterlyDue.getPaidFlats());
		assertEquals("APPL_Q1", savedQuarterlyDue.getApplicableFlats());
		assertEquals("[A-101]", savedMonthly1.getPaidFlats());
		assertEquals("[A-102]", savedMonthly1.getApplicableFlats());

		verify(paymentRepository, never()).saveAll(any());
	}

	@Test
	void payDues_shouldAddFlatToPaymentPaidFlatsWhenAllDuesCleared() throws Exception {
		PayDueRequest request = new PayDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		header.setUserId("USR-001");
		header.setFlatNo("A-101");
		request.setGenericHeader(header);
		request.setPaymentId("PAY-9001");
		request.setPaymentName("Maintenance");
		request.setAmount("9000");
		request.setDueId("DUE-Q1");
		request.setPaymentCycle(SecuraConstants.PAYMENT_CYCLE_QUATERLY);
		request.setDueDate(LocalDate.parse("2025-01-01"));
		request.setTransactionStatus(SecuraConstants.TRANSACTION_STATUS_SUCCESS);
		request.setPaymentTenderDataList(List.of(createTender(SecuraConstants.TRANSACTION_TENDER_ONLINE, "9000")));

		PaymentEntity paymentEntity = new PaymentEntity();
		paymentEntity.setPaymentId("PAY-9001");
		paymentEntity.setBankAccountId("BANK-9001");
		paymentEntity.setPaymentCapita("PER_SQFT");
		paymentEntity.setApplicableFor("PAYMENT_APPLICABLE");
		when(paymentRepository.findFirstByPaymentId("PAY-9001")).thenReturn(Optional.of(paymentEntity));

		Flat flat = new Flat();
		flat.setFlatNo("A-101");
		flat.setFlatArea("1200");
		flat.setFlatPndngPaymntLst("FLAT_PENDING_ONLY_Q1");
		when(flatRepository.findByAprmntIdAndFlatNo(any(), "A-101")).thenReturn(Optional.of(flat));

		DueAmountDetailsEntity paidQuarterlyDue = createDueEntity("DUE-Q1", SecuraConstants.PAYMENT_CYCLE_QUATERLY, "1200",
				LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-01"), LocalDate.parse("2025-03-31"), "PAY-9001",
				"APPL_Q1");
		paidQuarterlyDue.setPaymentCapita("PER_SQFT");

		when(dueAmountDetailsRepository.findByAprmntIdAndDueIdAndCollectionCycleAndFlatAreaAndDueDate(any(), any(), any(), any(), any())).thenReturn(Optional.of(paidQuarterlyDue));
		when(dueAmountDetailsRepository.findByPaymentId("PAY-9001"))
				.thenReturn(new ArrayList<>(List.of(paidQuarterlyDue)));
		when(dueAmountDetailsRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

		PaymentEntity paymentForAprmtId = new PaymentEntity();
		paymentForAprmtId.setPaymentId("PAY-9001");
		when(paymentRepository.findByPaymentIdAndAprmtId("PAY-9001", "APR-001"))
				.thenReturn(new ArrayList<>(List.of(paymentForAprmtId)));
		when(paymentRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

		when(genericService.fromJson(any(), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenAnswer(invocation -> {
			String marker = invocation.getArgument(0);
			if (marker != null && marker.startsWith("[") && marker.endsWith("]")) {
				String content = marker.substring(1, marker.length() - 1).trim();
				if (content.isEmpty()) {
					return new ArrayList<>();
				}
				return new ArrayList<>(List.of(content.split(",\\s*")));
			}
			return switch (marker) {
			case "FLAT_PENDING_ONLY_Q1" -> new ArrayList<>(List.of("DUE-Q1_QUATERLY_1200_2025-01-01"));
			case "APPL_Q1" -> new ArrayList<>(List.of("A-101", "A-102"));
			case "PAYMENT_APPLICABLE" -> new ArrayList<>(List.of("A-101", "A-102"));
			default -> new ArrayList<>();
			};
		});
		when(genericService.toJson(any())).thenAnswer(invocation -> {
			Object value = invocation.getArgument(0, Object.class);
			return value == null ? null : value.toString();
		});

		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
		CreateReceiptResponse createReceiptResponse = new CreateReceiptResponse();
		createReceiptResponse.setReceipt("RECEIPT");
		createReceiptResponse.setReceiptNumber("RCT-9001");
		when(receiptServices.createReceipt(any(CreateReceiptRequest.class))).thenReturn(createReceiptResponse);

		paymentServices.payDues(request);

		ArgumentCaptor<Flat> flatCaptor = ArgumentCaptor.forClass(Flat.class);
		verify(flatRepository).save(flatCaptor.capture());
		assertFalse(flatCaptor.getValue().getFlatPndngPaymntLst().contains("DUE-Q1_QUATERLY_1200_2025-01-01"));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<PaymentEntity>> paymentSaveCaptor = ArgumentCaptor.forClass((Class) List.class);
		verify(paymentRepository).saveAll(paymentSaveCaptor.capture());
		assertEquals(1, paymentSaveCaptor.getValue().size());
		assertTrue(paymentSaveCaptor.getValue().get(0).getPaidFlats().contains("A-101"));
	}

	@Test
	void payDues_shouldNotRemovePendingDueKeyWhenCoveredDueIsPerHead() throws Exception {
		PayDueRequest request = new PayDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		header.setUserId("USR-001");
		header.setFlatNo("A-101");
		request.setGenericHeader(header);
		request.setPaymentId("PAY-9003");
		request.setPaymentName("Water");
		request.setAmount("900");
		request.setDueId("DUE-PH");
		request.setPaymentCycle(SecuraConstants.PAYMENT_CYCLE_MONTHLY);
		request.setDueDate(LocalDate.parse("2025-01-01"));
		request.setTransactionStatus(SecuraConstants.TRANSACTION_STATUS_SUCCESS);
		request.setPaymentTenderDataList(List.of(createTender(SecuraConstants.TRANSACTION_TENDER_ONLINE, "900")));

		PaymentEntity paymentEntity = new PaymentEntity();
		paymentEntity.setPaymentId("PAY-9003");
		paymentEntity.setBankAccountId("BANK-9003");
		paymentEntity.setPaymentCapita("PER_HEAD");
		paymentEntity.setApplicableFor("PAYMENT_APPLICABLE");
		when(paymentRepository.findFirstByPaymentId("PAY-9003")).thenReturn(Optional.of(paymentEntity));

		Flat flat = new Flat();
		flat.setFlatNo("A-101");
		flat.setFlatArea("1200");
		flat.setFlatPndngPaymntLst("FLAT_PENDING_PER_HEAD");
		when(flatRepository.findByAprmntIdAndFlatNo(any(), "A-101")).thenReturn(Optional.of(flat));

		DueAmountDetailsEntity perHeadDue = createDueEntity("DUE-PH", SecuraConstants.PAYMENT_CYCLE_MONTHLY, "ALL",
				LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"), "PAY-9003",
				"APPL_PH");
		perHeadDue.setPaymentCapita("PER_HEAD");
		when(dueAmountDetailsRepository.findByAprmntIdAndDueIdAndCollectionCycleAndFlatAreaAndDueDate(any(), any(), any(), any(), any())).thenReturn(Optional.of(perHeadDue));
		when(dueAmountDetailsRepository.findByPaymentId("PAY-9003")).thenReturn(new ArrayList<>(List.of(perHeadDue)));
		when(dueAmountDetailsRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

		when(genericService.fromJson(any(), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenAnswer(invocation -> {
			String marker = invocation.getArgument(0);
			if (marker != null && marker.startsWith("[") && marker.endsWith("]")) {
				String content = marker.substring(1, marker.length() - 1).trim();
				if (content.isEmpty()) {
					return new ArrayList<>();
				}
				return new ArrayList<>(List.of(content.split(",\\s*")));
			}
			return switch (marker) {
			case "FLAT_PENDING_PER_HEAD" -> new ArrayList<>(List.of("DUE-PH_MONTHLY_ALL_2025-01-01"));
			case "APPL_PH", "PAYMENT_APPLICABLE" -> new ArrayList<>(List.of("A-101", "A-102"));
			default -> new ArrayList<>();
			};
		});
		when(genericService.toJson(any())).thenAnswer(invocation -> {
			Object value = invocation.getArgument(0, Object.class);
			return value == null ? null : value.toString();
		});

		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
		CreateReceiptResponse createReceiptResponse = new CreateReceiptResponse();
		createReceiptResponse.setReceipt("RECEIPT");
		createReceiptResponse.setReceiptNumber("RCT-9003");
		when(receiptServices.createReceipt(any(CreateReceiptRequest.class))).thenReturn(createReceiptResponse);

		paymentServices.payDues(request);

		verify(flatRepository, never()).save(any(Flat.class));
	}

	@Test
	void payDues_shouldNotAppendFlatAgainWhenAlreadyPresentInPaidFlats() throws Exception {
		PayDueRequest request = new PayDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		header.setUserId("USR-001");
		header.setFlatNo("A-101");
		request.setGenericHeader(header);
		request.setPaymentId("PAY-9001");
		request.setPaymentName("Maintenance");
		request.setAmount("9000");
		request.setDueId("DUE-Q1");
		request.setPaymentCycle(SecuraConstants.PAYMENT_CYCLE_QUATERLY);
		request.setDueDate(LocalDate.parse("2025-01-01"));
		request.setTransactionStatus(SecuraConstants.TRANSACTION_STATUS_SUCCESS);
		request.setPaymentTenderDataList(List.of(createTender(SecuraConstants.TRANSACTION_TENDER_ONLINE, "9000")));

		PaymentEntity paymentEntity = new PaymentEntity();
		paymentEntity.setPaymentId("PAY-9001");
		paymentEntity.setBankAccountId("BANK-9001");
		paymentEntity.setPaymentCapita("PER_SQFT");
		paymentEntity.setApplicableFor("PAYMENT_APPLICABLE");
		when(paymentRepository.findFirstByPaymentId("PAY-9001")).thenReturn(Optional.of(paymentEntity));

		Flat flat = new Flat();
		flat.setFlatNo("A-101");
		flat.setFlatArea("1200");
		flat.setFlatPndngPaymntLst("FLAT_PENDING_ONLY_Q1");
		when(flatRepository.findByAprmntIdAndFlatNo(any(), "A-101")).thenReturn(Optional.of(flat));

		DueAmountDetailsEntity paidQuarterlyDue = createDueEntity("DUE-Q1", SecuraConstants.PAYMENT_CYCLE_QUATERLY, "1200",
				LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-01"), LocalDate.parse("2025-03-31"), "PAY-9001",
				"APPL_Q1");
		paidQuarterlyDue.setPaymentCapita("PER_SQFT");
		paidQuarterlyDue.setPaidFlats("DUE_PAID_FLATS");

		when(dueAmountDetailsRepository.findByAprmntIdAndDueIdAndCollectionCycleAndFlatAreaAndDueDate(any(), any(), any(), any(), any())).thenReturn(Optional.of(paidQuarterlyDue));
		when(dueAmountDetailsRepository.findByPaymentId("PAY-9001"))
				.thenReturn(new ArrayList<>(List.of(paidQuarterlyDue)));

		PaymentEntity paymentForAprmtId = new PaymentEntity();
		paymentForAprmtId.setPaymentId("PAY-9001");
		paymentForAprmtId.setPaidFlats("PAYMENT_PAID_FLATS");
		when(paymentRepository.findByPaymentIdAndAprmtId("PAY-9001", "APR-001"))
				.thenReturn(new ArrayList<>(List.of(paymentForAprmtId)));

		when(genericService.fromJson(any(), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenAnswer(invocation -> {
			String marker = invocation.getArgument(0);
			if (marker != null && marker.startsWith("[") && marker.endsWith("]")) {
				String content = marker.substring(1, marker.length() - 1).trim();
				if (content.isEmpty()) {
					return new ArrayList<>();
				}
				return new ArrayList<>(List.of(content.split(",\\s*")));
			}
			return switch (marker) {
			case "FLAT_PENDING_ONLY_Q1" -> new ArrayList<>(List.of("DUE-Q1_QUATERLY_1200_2025-01-01"));
			case "APPL_Q1", "PAYMENT_APPLICABLE", "DUE_PAID_FLATS", "PAYMENT_PAID_FLATS" -> new ArrayList<>(
					List.of("A-101", "A-102"));
			default -> new ArrayList<>();
			};
		});
		when(genericService.toJson(any())).thenAnswer(invocation -> {
			Object value = invocation.getArgument(0, Object.class);
			return value == null ? null : value.toString();
		});

		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
		CreateReceiptResponse createReceiptResponse = new CreateReceiptResponse();
		createReceiptResponse.setReceipt("RECEIPT");
		createReceiptResponse.setReceiptNumber("RCT-9001");
		when(receiptServices.createReceipt(any(CreateReceiptRequest.class))).thenReturn(createReceiptResponse);

		paymentServices.payDues(request);

		verify(dueAmountDetailsRepository, never()).saveAll(any());
		verify(paymentRepository, never()).saveAll(any());
	}

	@Test
	void payDues_shouldNotRunCoveredDueRemovalWhenRequestStatusIsNotSuccess() throws Exception {
		PayDueRequest request = new PayDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		header.setUserId("USR-001");
		header.setFlatNo("A-101");
		request.setGenericHeader(header);
		request.setPaymentId("PAY-9002");
		request.setAmount("1000");
		request.setTransactionStatus("FAILED");
		request.setPaymentTenderDataList(List.of(createTender(SecuraConstants.TRANSACTION_TENDER_ONLINE, "1000")));

		PaymentEntity paymentEntity = new PaymentEntity();
		paymentEntity.setPaymentId("PAY-9002");
		paymentEntity.setBankAccountId("BANK-9002");
		when(paymentRepository.findFirstByPaymentId("PAY-9002")).thenReturn(Optional.of(paymentEntity));
		when(genericService.toJson(any())).thenReturn("JSON");
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentServices.payDues(request);

		verify(dueAmountDetailsRepository, never()).findByPaymentId("PAY-9002");
		verify(flatRepository, never()).save(any(Flat.class));
		verify(paymentRepository, never()).saveAll(any());
	}

	@Test
	void payDues_shouldHandlePerSqftRelatedDuesOnSuccessfulPayment() throws Exception {
		PayDueRequest request = new PayDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		header.setUserId("USR-001");
		header.setFlatNo("A-101");
		request.setGenericHeader(header);
		request.setPaymentId("PAY-9010");
		request.setPaymentName("Maintenance");
		request.setAmount("9000");
		request.setDueId("DUE-Q1");
		request.setPaymentCycle(SecuraConstants.PAYMENT_CYCLE_QUATERLY);
		request.setDueDate(LocalDate.parse("2025-01-01"));
		request.setTransactionStatus(SecuraConstants.TRANSACTION_STATUS_SUCCESS);
		request.setPaymentTenderDataList(List.of(createTender(SecuraConstants.TRANSACTION_TENDER_ONLINE, "9000")));

		PaymentEntity paymentEntity = new PaymentEntity();
		paymentEntity.setPaymentId("PAY-9010");
		paymentEntity.setBankAccountId("BANK-9010");
		paymentEntity.setPaymentCapita("PER_SQFT");
		paymentEntity.setApplicableFor("PAYMENT_APPLICABLE");
		when(paymentRepository.findFirstByPaymentId("PAY-9010")).thenReturn(Optional.of(paymentEntity));

		Flat flat = new Flat();
		flat.setFlatNo("A-101");
		flat.setFlatArea("1200");
		flat.setFlatPndngPaymntLst("FLAT_PENDING_P10");
		when(flatRepository.findByAprmntIdAndFlatNo(any(), "A-101")).thenReturn(Optional.of(flat));

		// Paid quarterly due (PER_SQFT, flatArea=1200)
		DueAmountDetailsEntity paidQuarterlyDue = createDueEntity("DUE-Q1", SecuraConstants.PAYMENT_CYCLE_QUATERLY, "1200",
				LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-01"), LocalDate.parse("2025-03-31"), "PAY-9010",
				"APPL_Q1_P10");
		paidQuarterlyDue.setPaymentCapita("PER_SQFT");

		// Related monthly due with same flatArea (1200) — should have flat removed from applicableFlats, NOT added to paidFlats
		DueAmountDetailsEntity relatedDueSameArea = createDueEntity("DUE-M1", SecuraConstants.PAYMENT_CYCLE_MONTHLY, "1200",
				LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"), "PAY-9010",
				"APPL_M1_P10");
		relatedDueSameArea.setPaymentCapita("PER_SQFT");

		// Related monthly due with different flatArea (1500) — should be left untouched
		DueAmountDetailsEntity relatedDueDiffArea = createDueEntity("DUE-M2", SecuraConstants.PAYMENT_CYCLE_MONTHLY, "1500",
				LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"), "PAY-9010",
				"APPL_M2_P10");
		relatedDueDiffArea.setPaymentCapita("PER_SQFT");

		when(dueAmountDetailsRepository.findByAprmntIdAndDueIdAndCollectionCycleAndFlatAreaAndDueDate(any(), any(), any(), any(), any())).thenReturn(Optional.of(paidQuarterlyDue));
		when(dueAmountDetailsRepository.findByPaymentId("PAY-9010"))
				.thenReturn(new ArrayList<>(List.of(paidQuarterlyDue, relatedDueSameArea, relatedDueDiffArea)));
		when(dueAmountDetailsRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

		when(genericService.fromJson(any(), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenAnswer(invocation -> {
			String marker = invocation.getArgument(0);
			if (marker != null && marker.startsWith("[") && marker.endsWith("]")) {
				String content = marker.substring(1, marker.length() - 1).trim();
				if (content.isEmpty()) {
					return new ArrayList<>();
				}
				return new ArrayList<>(List.of(content.split(",\\s*")));
			}
			return switch (marker) {
			case "FLAT_PENDING_P10" -> new ArrayList<>(List.of("DUE-Q1_QUATERLY_1200_2025-01-01",
					"DUE-M1_MONTHLY_1200_2025-01-01", "DUE-M2_MONTHLY_1500_2025-01-01"));
			case "APPL_Q1_P10", "APPL_M1_P10" -> new ArrayList<>(List.of("A-101", "A-102"));
			case "APPL_M2_P10" -> new ArrayList<>(List.of("A-103", "A-104"));
			case "PAYMENT_APPLICABLE" -> new ArrayList<>(List.of("A-101", "A-102"));
			default -> new ArrayList<>();
			};
		});
		when(genericService.toJson(any())).thenAnswer(invocation -> {
			Object value = invocation.getArgument(0, Object.class);
			return value == null ? null : value.toString();
		});

		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
		CreateReceiptResponse createReceiptResponse = new CreateReceiptResponse();
		createReceiptResponse.setReceipt("RECEIPT");
		createReceiptResponse.setReceiptNumber("RCT-9010");
		when(receiptServices.createReceipt(any(CreateReceiptRequest.class))).thenReturn(createReceiptResponse);

		paymentServices.payDues(request);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<DueAmountDetailsEntity>> dueSaveCaptor = ArgumentCaptor.forClass((Class) List.class);
		verify(dueAmountDetailsRepository).saveAll(dueSaveCaptor.capture());

		DueAmountDetailsEntity savedPaidDue = dueSaveCaptor.getValue().stream()
				.filter(d -> "DUE-Q1".equals(d.getDueId())).findFirst().orElseThrow();
		// Paid due: flat added to paidFlats
		assertEquals("[A-101]", savedPaidDue.getPaidFlats());

		DueAmountDetailsEntity savedRelatedDueSameArea = dueSaveCaptor.getValue().stream()
				.filter(d -> "DUE-M1".equals(d.getDueId())).findFirst().orElseThrow();
		// PER_SQFT related due with matching flatArea: flat removed from applicableFlats, NOT added to paidFlats
		assertNull(savedRelatedDueSameArea.getPaidFlats());
		assertEquals("[A-102]", savedRelatedDueSameArea.getApplicableFlats());

		// PER_SQFT related due with different flatArea: not modified at all
		boolean diffAreaDueSaved = dueSaveCaptor.getValue().stream().anyMatch(d -> "DUE-M2".equals(d.getDueId()));
		assertFalse(diffAreaDueSaved);
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
		assertTransactionIdFormat(createdTransaction.getTrnscId(), "APR-001");
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

	@Test
	void uploadPastDue_shouldCreatePaymentForValidRowsAndReturnErrorFileForInvalidRows() throws Exception {
		UploadPastDueRequest request = new UploadPastDueRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		header.setUserId("USR-1");
		request.setGenericHeader(header);
		request.setFile(buildPastDueWorkbookBase64(
				List.of(
						List.of("A-101", "1-Mar-2026", "31-Mar-2026", "March Due", "1200.00", "18.0", "1416", "MAINTENANCE",
								"BANK-123"),
						List.of("A-999", "1-Mar-2026", "31-Mar-2026", "Bad Flat", "1300", "18", "1534", "EVENT",
								"BANK-999"))));

		Flat flat = new Flat();
		flat.setFlatNo("A-101");
		when(flatRepository.findByAprmntId("APR-1")).thenReturn(List.of(flat));
		when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UploadPastDueResponse response = paymentServices.uploadPastDue(request);

		ArgumentCaptor<PaymentEntity> paymentCaptor = ArgumentCaptor.forClass(PaymentEntity.class);
		verify(paymentRepository, atLeastOnce()).save(paymentCaptor.capture());
		verify(dueDetailsService, times(1)).calculateDuesForPaymentWithoutDiscFine(any(), eq(header));
		assertEquals("March Due", paymentCaptor.getValue().getPaymentName());
		assertEquals("1200", paymentCaptor.getValue().getPaymentAmount());
		assertEquals("18", paymentCaptor.getValue().getGst());
		assertEquals("MAINTENANCE", paymentCaptor.getValue().getCauseId());
		assertEquals("BANK-123", paymentCaptor.getValue().getBankAccountId());
		assertEquals(ErrorMessage.ERR_MESSAGE_42, response.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_42, response.getMessageCode());
		assertEquals(1, response.getSuccessRows());
		assertEquals(1, response.getFailedRows());
		assertTrue(response.getFile() != null && !response.getFile().isBlank());

		try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(Base64.getDecoder().decode(response.getFile())))) {
			Sheet failedSheet = workbook.getSheetAt(0);
			Row headerRow = failedSheet.getRow(0);
			Row failedRow = failedSheet.getRow(1);
			assertEquals("Cause", headerRow.getCell(7).getStringCellValue());
			assertEquals("BankAccountID", headerRow.getCell(8).getStringCellValue());
			assertEquals("EVENT", failedRow.getCell(7).getStringCellValue());
			assertEquals("BANK-999", failedRow.getCell(8).getStringCellValue());
			assertEquals("Flat Id not found for apartment", failedRow.getCell(9).getStringCellValue());
		}
	}

	@Test
	void uploadPastDue_shouldReturnServiceErrorForInvalidInput() throws Exception {
		UploadPastDueRequest request = new UploadPastDueRequest();
		request.setFile("");

		UploadPastDueResponse response = paymentServices.uploadPastDue(request);

		assertEquals(ErrorMessage.ERR_MESSAGE_33, response.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_33, response.getMessageCode());
		assertEquals(0, response.getSuccessRows());
		assertEquals(0, response.getFailedRows());
	}

	@Test
	void validatePriorDuePaymnent_shouldReturnSuccessWhenMatchingTransactionsAreFailed() throws Exception {
		ValidatePriorDuePaymnentRequest request = buildValidatePriorDuePaymentRequest();
		Transaction failedTransaction = new Transaction();
		failedTransaction.setDueDetails("DUE-1_MONTHLY_1200_2026-03-01");
		failedTransaction.setTrnsStatus(SecuraConstants.TRANSACTION_STATUS_FAILED);
		when(transactionRepository.findByAprmntIdAndFlatIdAndPymntIdOrderByTrnsDateDesc("APR-1", "A-101", "PAY-1"))
				.thenReturn(List.of(failedTransaction));

		GenericResponse response = paymentServices.validatePriorDuePaymnent(request);

		assertEquals(SuccessMessage.SUCC_MESSAGE_46, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_46, response.getMessageCode());
	}

	@Test
	void validatePriorDuePaymnent_shouldReturnErrorWhenMatchingTransactionIsSuccessful() throws Exception {
		ValidatePriorDuePaymnentRequest request = buildValidatePriorDuePaymentRequest();
		Transaction successTransaction = new Transaction();
		successTransaction.setDueDetails("DUE-1_MONTHLY_1200_2026-03-01");
		successTransaction.setTrnsStatus(SecuraConstants.TRANSACTION_STATUS_SUCCESS);
		when(transactionRepository.findByAprmntIdAndFlatIdAndPymntIdOrderByTrnsDateDesc("APR-1", "A-101", "PAY-1"))
				.thenReturn(List.of(successTransaction));

		GenericResponse response = paymentServices.validatePriorDuePaymnent(request);

		assertEquals(ErrorMessage.ERR_MESSAGE_50, response.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_50, response.getMessageCode());
	}

	@Test
	void validatePriorDuePaymnent_shouldReturnPendingErrorWhenMatchingTransactionIsNotFailedOrSuccessful() throws Exception {
		ValidatePriorDuePaymnentRequest request = buildValidatePriorDuePaymentRequest();
		Transaction pendingTransaction = new Transaction();
		pendingTransaction.setDueDetails("DUE-1_MONTHLY_1200_2026-03-01");
		pendingTransaction.setTrnsStatus(SecuraConstants.TRANSACTION_STATUS_PENDING);
		when(transactionRepository.findByAprmntIdAndFlatIdAndPymntIdOrderByTrnsDateDesc("APR-1", "A-101", "PAY-1"))
				.thenReturn(List.of(pendingTransaction));

		GenericResponse response = paymentServices.validatePriorDuePaymnent(request);

		assertEquals(ErrorMessage.ERR_MESSAGE_51, response.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_51, response.getMessageCode());
	}

	@Test
	void reconcileQRPayment_shouldHighlightMatchedRowsAndReturnAllFoundMessage() throws Exception {
		ReconcileQRPaymentRequest request = buildReconcileQrPaymentRequest();
		request.setBase64EncodedSatementFile(buildReconcileWorkbookBase64(List.of(
				"UPI/SUDEEP KUM/9658733181@axl/QR1A2/AXIS BANK",
				"UPI/SUDEEP KUM/9658733181@axl/QR1A3/AXIS BANK")));
		Transaction transaction = new Transaction();
		transaction.setTrnscId("CAMTRNM1FH");
		transaction.setQrIdentifier("QR1A2");
		transaction.setFlatId("A-101");
		transaction.setTrnsStatus(SecuraConstants.TRANSACTION_STATUS_PENDING);
		transaction.setTrnsTender("[{\"tenderName\":\"SOCIETY_QR\"}]");
		transaction.setCreatTs(LocalDateTime.of(2026, 3, 10, 9, 30));
		when(transactionRepository.findByAprmntId("APR-1")).thenReturn(List.of(transaction));

		ReconcileQRPaymentResponse response = paymentServices.reconcileQRPayment(request);

		assertEquals(1, response.getFoundCount());
		assertEquals(0, response.getNotFoundCount());
		assertEquals(1, response.getFoundTransactionsList().size());
		assertEquals(RECONCILE_ALL_FOUND_MESSAGE, response.getMessage());
		assertTrue(response.getHighlithedBase64EncodedFile() != null && !response.getHighlithedBase64EncodedFile().isBlank());
		try (Workbook workbook = WorkbookFactory.create(
				new ByteArrayInputStream(Base64.getDecoder().decode(response.getHighlithedBase64EncodedFile())))) {
			Sheet sheet = workbook.getSheetAt(0);
			assertEquals("Flat Id", sheet.getRow(0).getCell(0).getStringCellValue());
			assertEquals("Transaction ID", sheet.getRow(0).getCell(1).getStringCellValue());
			assertEquals("QR Identifier", sheet.getRow(0).getCell(2).getStringCellValue());
			Row matchedRow = sheet.getRow(1);
			assertEquals("A-101", matchedRow.getCell(0).getStringCellValue());
			assertEquals("CAMTRNM1FH", matchedRow.getCell(1).getStringCellValue());
			assertEquals("QR1A2", matchedRow.getCell(2).getStringCellValue());
			assertReconcileHighlightStyle(workbook, matchedRow.getCell(0).getCellStyle());
			assertReconcileHighlightStyle(workbook, matchedRow.getCell(1).getCellStyle());
			assertReconcileHighlightStyle(workbook, matchedRow.getCell(2).getCellStyle());
		}
	}

	@Test
	void reconcileQRPayment_shouldSupportXlsStatements() throws Exception {
		ReconcileQRPaymentRequest request = buildReconcileQrPaymentRequest();
		request.setBase64EncodedSatementFile(buildReconcileWorkbookBase64(List.of("UPI/.../QR1A2/..."), false));
		Transaction transaction = new Transaction();
		transaction.setTrnscId("CAMTRNM1FH");
		transaction.setQrIdentifier("QR1A2");
		transaction.setFlatId("A-101");
		transaction.setTrnsStatus(SecuraConstants.TRANSACTION_STATUS_PENDING);
		transaction.setTrnsTender("[{\"tenderName\":\"SOCIETY_QR\"}]");
		transaction.setCreatTs(LocalDateTime.of(2026, 3, 10, 9, 30));
		when(transactionRepository.findByAprmntId("APR-1")).thenReturn(List.of(transaction));

		ReconcileQRPaymentResponse response = paymentServices.reconcileQRPayment(request);

		assertEquals(1, response.getFoundCount());
		assertEquals(0, response.getNotFoundCount());
		assertTrue(response.getHighlithedBase64EncodedFile() != null && !response.getHighlithedBase64EncodedFile().isBlank());
		try (Workbook workbook = WorkbookFactory.create(
				new ByteArrayInputStream(Base64.getDecoder().decode(response.getHighlithedBase64EncodedFile())))) {
			assertTrue(workbook instanceof HSSFWorkbook);
			Row matchedRow = workbook.getSheetAt(0).getRow(1);
			assertEquals("A-101", matchedRow.getCell(0).getStringCellValue());
			assertEquals("CAMTRNM1FH", matchedRow.getCell(1).getStringCellValue());
			assertEquals("QR1A2", matchedRow.getCell(2).getStringCellValue());
			assertReconcileHighlightStyle(workbook, matchedRow.getCell(0).getCellStyle());
			assertReconcileHighlightStyle(workbook, matchedRow.getCell(1).getCellStyle());
			assertReconcileHighlightStyle(workbook, matchedRow.getCell(2).getCellStyle());
		}
	}

	@Test
	void reconcileQRPayment_shouldSupportXlsStatementsWithMultipleColumns() throws Exception {
		ReconcileQRPaymentRequest request = buildReconcileQrPaymentRequest();
		request.setBase64EncodedSatementFile(buildMultiColumnReconcileWorkbookBase64(false));
		Transaction transaction = new Transaction();
		transaction.setTrnscId("CAMTRNM1FH");
		transaction.setQrIdentifier("QR1A2");
		transaction.setFlatId("A-101");
		transaction.setTrnsStatus(SecuraConstants.TRANSACTION_STATUS_PENDING);
		transaction.setTrnsTender("[{\"tenderName\":\"SOCIETY_QR\"}]");
		transaction.setCreatTs(LocalDateTime.of(2026, 3, 10, 9, 30));
		when(transactionRepository.findByAprmntId("APR-1")).thenReturn(List.of(transaction));

		ReconcileQRPaymentResponse response = paymentServices.reconcileQRPayment(request);

		assertEquals(1, response.getFoundCount());
		assertEquals(0, response.getNotFoundCount());
		assertTrue(response.getHighlithedBase64EncodedFile() != null && !response.getHighlithedBase64EncodedFile().isBlank());
		try (Workbook workbook = WorkbookFactory.create(
				new ByteArrayInputStream(Base64.getDecoder().decode(response.getHighlithedBase64EncodedFile())))) {
			assertTrue(workbook instanceof HSSFWorkbook, "Output must be a valid HSSF workbook");
			Sheet sheet = workbook.getSheetAt(0);
			Row headerRow = sheet.getRow(0);
			assertEquals("Flat Id", headerRow.getCell(0).getStringCellValue());
			assertEquals("Transaction ID", headerRow.getCell(1).getStringCellValue());
			assertEquals("QR Identifier", headerRow.getCell(2).getStringCellValue());
			assertEquals("Date", headerRow.getCell(3).getStringCellValue());
			assertEquals("Narration", headerRow.getCell(4).getStringCellValue());
			assertEquals("Amount", headerRow.getCell(5).getStringCellValue());
			assertEquals("Balance", headerRow.getCell(6).getStringCellValue());
			Row matchedRow = sheet.getRow(1);
			assertEquals("A-101", matchedRow.getCell(0).getStringCellValue());
			assertEquals("CAMTRNM1FH", matchedRow.getCell(1).getStringCellValue());
			assertEquals("QR1A2", matchedRow.getCell(2).getStringCellValue());
			assertEquals("10-Mar-2026", matchedRow.getCell(3).getStringCellValue());
			assertEquals("UPI/.../QR1A2/...", matchedRow.getCell(4).getStringCellValue());
			assertEquals(5000.0, matchedRow.getCell(5).getNumericCellValue(), 0.001);
			assertEquals(15000.0, matchedRow.getCell(6).getNumericCellValue(), 0.001);
			assertReconcileHighlightStyle(workbook, matchedRow.getCell(0).getCellStyle());
			assertReconcileHighlightStyle(workbook, matchedRow.getCell(1).getCellStyle());
			assertReconcileHighlightStyle(workbook, matchedRow.getCell(2).getCellStyle());
		}
	}


	@Test
	void reconcileQRPayment_shouldReturnMixedMessageWhenSomeTransactionsAreNotFound() throws Exception {
		ReconcileQRPaymentRequest request = buildReconcileQrPaymentRequest();
		request.setBase64EncodedSatementFile(buildReconcileWorkbookBase64(List.of("UPI/.../QR1A2/...")));
		Transaction foundTransaction = new Transaction();
		foundTransaction.setTrnscId("CAMTRNM1FH");
		foundTransaction.setQrIdentifier("QR1A2");
		foundTransaction.setFlatId("A-101");
		foundTransaction.setTrnsStatus(SecuraConstants.TRANSACTION_STATUS_PENDING);
		foundTransaction.setTrnsTender("[{\"tenderName\":\"SOCIETY_QR\"}]");
		foundTransaction.setCreatTs(LocalDateTime.of(2026, 3, 10, 10, 30));
		Transaction notFoundTransaction = new Transaction();
		notFoundTransaction.setTrnscId("CAMTRNM2FH");
		notFoundTransaction.setQrIdentifier("QR1A3");
		notFoundTransaction.setFlatId("A-102");
		notFoundTransaction.setTrnsStatus(SecuraConstants.TRANSACTION_STATUS_PENDING);
		notFoundTransaction.setTrnsTender("[{\"tenderName\":\"SOCIETY_QR\"}]");
		notFoundTransaction.setCreatTs(LocalDateTime.of(2026, 3, 11, 10, 30));
		Transaction outOfRangeTransaction = new Transaction();
		outOfRangeTransaction.setTrnscId("CAMTRNM3FH");
		outOfRangeTransaction.setQrIdentifier("QR1A4");
		outOfRangeTransaction.setFlatId("A-103");
		outOfRangeTransaction.setTrnsStatus(SecuraConstants.TRANSACTION_STATUS_PENDING);
		outOfRangeTransaction.setTrnsTender("[{\"tenderName\":\"SOCIETY_QR\"}]");
		outOfRangeTransaction.setCreatTs(LocalDateTime.of(2026, 2, 1, 10, 30));
		when(transactionRepository.findByAprmntId("APR-1"))
				.thenReturn(List.of(foundTransaction, notFoundTransaction, outOfRangeTransaction));

		ReconcileQRPaymentResponse response = paymentServices.reconcileQRPayment(request);

		assertEquals(1, response.getFoundCount());
		assertEquals(1, response.getNotFoundCount());
		assertEquals(1, response.getNotFoundTransactionsList().size());
		assertEquals("CAMTRNM2FH", response.getNotFoundTransactionsList().get(0).getTrnscId());
		assertEquals(RECONCILE_PARTIAL_MESSAGE, response.getMessage());
	}

	@Test
	void actionQRPayment_shouldApproveTransactionsAndReturnAllSuccess() throws Exception {
		ActionQRPaymentRequest request = buildActionQrPaymentRequest(SecuraConstants.ACTION_APPROVE);
		Transaction transaction = null;//request.getFoundTransactionsList().get(0);
		GenericResponse successResponse = new GenericResponse();
		successResponse.setMessage(SuccessMessage.SUCC_MESSAGE_47);
		successResponse.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_47);
		when(worklistService.actionTransactionReviewWorkList(any(ActionTransactionReviewWorkListRequest.class)))
				.thenReturn(successResponse);

		ActionQRPaymentResponse response = paymentServices.actionQRPayment(request);

		assertTrue(response.getNotCompletedTransactionList().isEmpty());
		assertNull(response.getFailedWorklistActionFileBase64Encoded());
		assertEquals("SUCC_ACTION_QR_PAYMENT_ALL", response.getMessageCode());
		ArgumentCaptor<ActionTransactionReviewWorkListRequest> captor = ArgumentCaptor
				.forClass(ActionTransactionReviewWorkListRequest.class);
		verify(worklistService).actionTransactionReviewWorkList(captor.capture());
		assertEquals(SecuraConstants.ACTION_APPROVE, captor.getValue().getAction());
		assertEquals(transaction.getWorkListId(), captor.getValue().getWorklistId());
	}

	@Test
	void actionQRPayment_shouldRejectAndCreateFailureWorkbookForNonSuccessWorklist() throws Exception {
		ActionQRPaymentRequest request = buildActionQrPaymentRequest(SecuraConstants.ACTION_REJECT);
		Transaction failedTransaction = new Transaction();
		failedTransaction.setTrnscId("TRNS-2");
		failedTransaction.setFlatId("A-102");
		failedTransaction.setTrnsAmt("950.50");
		failedTransaction.setWorkListId("WL-2");
		failedTransaction.setCreatTs(LocalDateTime.of(2026, 3, 1, 21, 45));
		//request.setFoundTransactionsList(List.of(request.getFoundTransactionsList().get(0), failedTransaction));

		GenericResponse successResponse = new GenericResponse();
		successResponse.setMessage(SuccessMessage.SUCC_MESSAGE_47);
		successResponse.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_47);
		GenericResponse failedResponse = new GenericResponse();
		failedResponse.setMessage("approval blocked");
		failedResponse.setMessageCode(ErrorMessageCode.ERR_MESSAGE_53);
		when(worklistService.actionTransactionReviewWorkList(any(ActionTransactionReviewWorkListRequest.class)))
				.thenReturn(successResponse, failedResponse);

		ActionQRPaymentResponse response = paymentServices.actionQRPayment(request);

		assertEquals(1, response.getNotCompletedTransactionList().size());
		assertEquals("TRNS-2", response.getNotCompletedTransactionList().get(0).getTrnscId());
		assertEquals("ERR_ACTION_QR_PAYMENT_PARTIAL", response.getMessageCode());
		assertNotNull(response.getFailedWorklistActionFileBase64Encoded());
		try (Workbook workbook = new XSSFWorkbook(
				new ByteArrayInputStream(Base64.getDecoder().decode(response.getFailedWorklistActionFileBase64Encoded())))) {
			Sheet sheet = workbook.getSheetAt(0);
			assertEquals("Transaction Id", sheet.getRow(0).getCell(0).getStringCellValue());
			assertEquals("TRNS-2", sheet.getRow(1).getCell(0).getStringCellValue());
			assertEquals("A-102", sheet.getRow(1).getCell(1).getStringCellValue());
			assertEquals("950.50", sheet.getRow(1).getCell(2).getStringCellValue());
			assertEquals("1-Mar-2026 21:45", sheet.getRow(1).getCell(3).getStringCellValue());
			assertEquals("approval blocked", sheet.getRow(1).getCell(4).getStringCellValue());
		}
		ArgumentCaptor<ActionTransactionReviewWorkListRequest> captor = ArgumentCaptor
				.forClass(ActionTransactionReviewWorkListRequest.class);
		verify(worklistService, times(2)).actionTransactionReviewWorkList(captor.capture());
		assertEquals(SecuraConstants.ACTION_REJECT, captor.getAllValues().get(0).getAction());
		assertEquals(SecuraConstants.ACTION_REJECT, captor.getAllValues().get(1).getAction());
	}

	private String buildPastDueWorkbookBase64(List<List<String>> rows) throws Exception {
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("past_due");
			Row headerRow = sheet.createRow(0);
			String[] headers = { "Flat Id", "Due From", "Due Till", "Due Cause", "Due Amount", "GST%", "Total Due Amount",
					"Cause", "BankAccountID" };
			for (int i = 0; i < headers.length; i++) {
				headerRow.createCell(i).setCellValue(headers[i]);
			}
			for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
				Row row = sheet.createRow(rowIndex + 1);
				List<String> values = rows.get(rowIndex);
				for (int col = 0; col < values.size(); col++) {
					row.createCell(col).setCellValue(values.get(col));
				}
			}
			workbook.write(outputStream);
			return Base64.getEncoder().encodeToString(outputStream.toByteArray());
		}
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

	private DueAmountDetailsEntity createDueEntity(String dueId, String cycle, String flatArea, LocalDate dueDate,
			LocalDate dueStartDate, LocalDate dueEndDate, String paymentId, String applicableFlatsMarker) {
		DueAmountDetailsEntity due = new DueAmountDetailsEntity();
		due.setDueId(dueId);
		due.setCollectionCycle(cycle);
		due.setFlatArea(flatArea);
		due.setDueDate(dueDate);
		due.setDueStartDate(dueStartDate);
		due.setDueEndDate(dueEndDate);
		due.setPaymentId(paymentId);
		due.setApplicableFlats(applicableFlatsMarker);
		return due;
	}

	private ValidatePriorDuePaymnentRequest buildValidatePriorDuePaymentRequest() {
		ValidatePriorDuePaymnentRequest request = new ValidatePriorDuePaymnentRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		header.setFlatNo("A-101");
		request.setGenericHeader(header);
		request.setPaymentId("PAY-1");
		request.setDueId("DUE-1");
		request.setPaymentCycle("MONTHLY");
		request.setDueDate(LocalDate.of(2026, 3, 1));
		return request;
	}

	private ReconcileQRPaymentRequest buildReconcileQrPaymentRequest() {
		ReconcileQRPaymentRequest request = new ReconcileQRPaymentRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		request.setGenericHeader(header);
		request.setFromDate(LocalDate.of(2026, 3, 1));
		request.setToDate(LocalDate.of(2026, 3, 31));
		return request;
	}

	private ActionQRPaymentRequest buildActionQrPaymentRequest(String action) {
		ActionQRPaymentRequest request = new ActionQRPaymentRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		header.setUserId("USR-1");
		request.setGenericHeader(header);
		request.setAction(action);
		Transaction transaction = new Transaction();
		transaction.setTrnscId("TRNS-1");
		transaction.setFlatId("A-101");
		transaction.setTrnsAmt("1200");
		transaction.setWorkListId("WL-1");
		transaction.setCreatTs(LocalDateTime.of(2026, 3, 2, 11, 15));
		//request.setFoundTransactionsList(List.of(transaction));
		return request;
	}

	private String buildReconcileWorkbookBase64(List<String> statementRows) throws Exception {
		return buildReconcileWorkbookBase64(statementRows, true);
	}

	private String buildReconcileWorkbookBase64(List<String> statementRows, boolean xlsxFormat) throws Exception {
		try (Workbook workbook = xlsxFormat ? new XSSFWorkbook() : new HSSFWorkbook();
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("statement");
			Row headerRow = sheet.createRow(0);
			headerRow.createCell(0).setCellValue("Narration");
			for (int rowIndex = 0; rowIndex < statementRows.size(); rowIndex++) {
				Row row = sheet.createRow(rowIndex + 1);
				row.createCell(0).setCellValue(statementRows.get(rowIndex));
			}
			workbook.write(outputStream);
			return Base64.getEncoder().encodeToString(outputStream.toByteArray());
		}
	}

	private String buildMultiColumnReconcileWorkbookBase64(boolean xlsxFormat) throws Exception {
		try (Workbook workbook = xlsxFormat ? new XSSFWorkbook() : new HSSFWorkbook();
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("statement");
			Row headerRow = sheet.createRow(0);
			headerRow.createCell(0).setCellValue("Date");
			headerRow.createCell(1).setCellValue("Narration");
			headerRow.createCell(2).setCellValue("Amount");
			headerRow.createCell(3).setCellValue("Balance");
			Row dataRow = sheet.createRow(1);
			dataRow.createCell(0).setCellValue("10-Mar-2026");
			dataRow.createCell(1).setCellValue("UPI/.../QR1A2/...");
			dataRow.createCell(2).setCellValue(5000.0);
			dataRow.createCell(3).setCellValue(15000.0);
			workbook.write(outputStream);
			return Base64.getEncoder().encodeToString(outputStream.toByteArray());
		}
	}


	private void assertReconcileHighlightStyle(Workbook workbook, CellStyle style) {
		assertEquals(BorderStyle.THIN, style.getBorderTop());
		assertEquals(BorderStyle.THIN, style.getBorderBottom());
		assertEquals(BorderStyle.THIN, style.getBorderLeft());
		assertEquals(BorderStyle.THIN, style.getBorderRight());
		if (style instanceof XSSFCellStyle xssfCellStyle) {
			XSSFColor fillColor = xssfCellStyle.getFillForegroundColorColor();
			assertNotNull(fillColor);
			assertEquals("FFF5FF96", fillColor.getARGBHex());
			return;
		}
		assertEquals(IndexedColors.LIGHT_YELLOW.getIndex(), style.getFillForegroundColor());
	}

	private void assertTransactionIdFormat(String transactionId, String apartmentId) {
		assertNotNull(transactionId);
		assertTrue(transactionId.matches("^[A-Z0-9]{3}TRN[A-Z0-9]{4}$"),
				() -> "Unexpected transaction id format: " + transactionId);
	}

}
