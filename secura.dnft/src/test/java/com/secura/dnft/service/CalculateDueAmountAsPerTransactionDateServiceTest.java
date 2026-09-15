package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secura.dnft.dao.BankEntityRepository;
import com.secura.dnft.dao.DiscFinRepository;
import com.secura.dnft.dao.DueAmountDetailsRepository;
import com.secura.dnft.dao.FlatRepository;
import com.secura.dnft.dao.PaymentRepository;
import com.secura.dnft.dao.TransactionRepository;
import com.secura.dnft.entity.DiscFin;
import com.secura.dnft.entity.DueAmountDetailsEntity;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.entity.Transaction;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.GetDueAmountForFlatRequest;
import com.secura.dnft.request.response.GetDueAmountForFlatResponse;
import com.secura.dnft.request.response.PaymentDetail;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CalculateDueAmountAsPerTransactionDateServiceTest {

	@Mock
	private FlatRepository flatRepository;

	@Mock
	private DueAmountDetailsRepository dueAmountDetailsRepository;

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private BankEntityRepository bankEntityRepository;

	@Mock
	private DiscFinRepository discFinRepository;

	@Mock
	private TransactionRepository transactionRepository;

	@Mock
	private GenericService genericService;

	@InjectMocks
	private CalculateDueAmountAsPerTransactionDateService service;

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	@BeforeEach
	void setUpJsonSupport() throws Exception {
		when(genericService.toJson(any())).thenAnswer(invocation -> objectMapper.writeValueAsString(invocation.getArgument(0)));
		when(genericService.fromJson(anyString(), any(TypeReference.class)))
				.thenAnswer(invocation -> objectMapper.readValue((String) invocation.getArgument(0),
						(TypeReference<Object>) invocation.getArgument(1)));
	}

	@Test
	void getDueDetailsAsTransactionDate_shouldHaveZeroPenaltyBeforePenaltyDate() {
		mockSingleDueScenario();

		GetDueAmountForFlatResponse response = service.getDueDetailsAsTransactionDate(buildRequest(LocalDate.of(2026, 7, 31)));

		DueAmountDetailsEntity due = firstDue(response);
		assertEquals("0", due.getFineAmount());
		assertEquals("5", due.getDiscountedAmount());
		assertEquals("95", response.getTotalDue());
	}

	@Test
	void getDueDetailsAsTransactionDate_shouldApplyPenaltyAfterPenaltyDate() {
		mockSingleDueScenario();

		GetDueAmountForFlatResponse response = service.getDueDetailsAsTransactionDate(buildRequest(LocalDate.of(2026, 8, 10)));

		DueAmountDetailsEntity due = firstDue(response);
		assertTrue(Double.parseDouble(due.getFineAmount()) > 0d);
		assertEquals("0", due.getDiscountedAmount());
	}

	@Test
	void getDueDetailsAsTransactionDate_shouldApplyDiscountWithinDiscountPeriod() {
		mockSingleDueScenario();

		GetDueAmountForFlatResponse response = service.getDueDetailsAsTransactionDate(buildRequest(LocalDate.of(2026, 8, 3)));

		assertEquals("5", firstDue(response).getDiscountedAmount());
	}

	@Test
	void getDueDetailsAsTransactionDate_shouldNotApplyDiscountAfterDiscountExpiry() {
		mockSingleDueScenario();

		GetDueAmountForFlatResponse response = service.getDueDetailsAsTransactionDate(buildRequest(LocalDate.of(2026, 8, 10)));

		assertEquals("0", firstDue(response).getDiscountedAmount());
	}

	@Test
	void getDueDetailsAsTransactionDate_shouldChangeAmountsForDifferentTransactionDates() {
		mockSingleDueScenario();

		GetDueAmountForFlatResponse augustResponse = service
				.getDueDetailsAsTransactionDate(buildRequest(LocalDate.of(2026, 8, 1)));
		GetDueAmountForFlatResponse septemberResponse = service
				.getDueDetailsAsTransactionDate(buildRequest(LocalDate.of(2026, 9, 1)));
		GetDueAmountForFlatResponse octoberResponse = service
				.getDueDetailsAsTransactionDate(buildRequest(LocalDate.of(2026, 10, 1)));

		double augustPenalty = Double.parseDouble(firstDue(augustResponse).getFineAmount());
		double septemberPenalty = Double.parseDouble(firstDue(septemberResponse).getFineAmount());
		double octoberPenalty = Double.parseDouble(firstDue(octoberResponse).getFineAmount());

		assertEquals("5", firstDue(augustResponse).getDiscountedAmount());
		assertEquals("0", firstDue(septemberResponse).getDiscountedAmount());
		assertEquals("0", firstDue(octoberResponse).getDiscountedAmount());
		assertTrue(septemberPenalty > augustPenalty);
		assertTrue(octoberPenalty > septemberPenalty);
	}

	@Test
	void getDueDetailsAsTransactionDate_shouldUseTransactionsUpToTransactionDateForTotals() {
		mockSingleDueScenario();
		Transaction transaction = new Transaction();
		transaction.setTrnsAmt("20");
		transaction.setTrnsDate(LocalDateTime.of(2026, 8, 5, 10, 0));
		when(transactionRepository.findByPymntIdAndFlatIdAndTrnsStatus("PAY1", "A-101", "SUCCESS"))
				.thenReturn(List.of(transaction));

		GetDueAmountForFlatResponse beforePaymentResponse = service
				.getDueDetailsAsTransactionDate(buildRequest(LocalDate.of(2026, 8, 3)));
		GetDueAmountForFlatResponse afterPaymentResponse = service
				.getDueDetailsAsTransactionDate(buildRequest(LocalDate.of(2026, 8, 10)));

		assertEquals("96", beforePaymentResponse.getTotalDue());
		assertEquals("83", afterPaymentResponse.getTotalDue());
	}

	@Test
	void getDueDetailsAsTransactionDate_shouldReturnErrorForMissingTransactionDate() {
		mockSingleDueScenario();
		GetDueAmountForFlatRequest request = buildRequest(null);

		GetDueAmountForFlatResponse response = service.getDueDetailsAsTransactionDate(request);

		assertEquals("ERR_MESSAGE_43", response.getMessageCode());
	}

	private void mockSingleDueScenario() {
		Flat flat = new Flat();
		flat.setFlatNo("A-101");
		flat.setFlatArea("1200");
		flat.setFlatPndngPaymntLst("[\"DUE1_MONTHLY_ALL_2026-08-01\"]");

		DueAmountDetailsEntity dueEntity = new DueAmountDetailsEntity();
		dueEntity.setAprmntId("APRT001");
		dueEntity.setDueId("DUE1");
		dueEntity.setCollectionCycle("MONTHLY");
		dueEntity.setFlatArea("ALL");
		dueEntity.setDueDate(LocalDate.of(2026, 8, 1));
		dueEntity.setDueStartDate(LocalDate.of(2026, 8, 1));
		dueEntity.setDueEndDate(LocalDate.of(2026, 8, 31));
		dueEntity.setPaymentId("PAY1");
		dueEntity.setAmount("100");
		dueEntity.setAmountPerMonth("100");
		dueEntity.setGstAmount("0");
		dueEntity.setTotalAmount("100");
		dueEntity.setPaymentName("Maintenance");
		dueEntity.setPaymentType("MANDATORY");
		dueEntity.setPaymentCapita("PER_FLAT");
		dueEntity.setAddedCharges("[]");
		dueEntity.setApplicableFlats("[\"A-101\"]");
		dueEntity.setAllowedTenders("[\"UPI\"]");
		dueEntity.setDiscountedAmount("0");
		dueEntity.setFineAmount("0");
		dueEntity.setTotalAddedCharges("0");
		dueEntity.setGstPercentage("0");
		dueEntity.setAlreadyPaidAmount("0");
		dueEntity.setAdminDiscount("0");
		dueEntity.setRoundUpAmount("0");

		PaymentEntity paymentEntity = new PaymentEntity();
		paymentEntity.setPaymentId("PAY1");
		paymentEntity.setAprmtId("APRT001");
		paymentEntity.setPaymentName("Maintenance");
		paymentEntity.setPaymentType("MANDATORY");
		paymentEntity.setPaymentCapita("PER_FLAT");
		paymentEntity.setPaymentAmount("100");
		paymentEntity.setGst("0");
		paymentEntity.setAllowedPaymentModes("[\"UPI\"]");
		paymentEntity.setAddedCharges("[]");
		paymentEntity.setPaymentCollectionMode("PRE");
		paymentEntity.setPaymentCollectionCycle("[\"MONTHLY\"]");
		paymentEntity.setApplicableFor("ALL");
		paymentEntity.setCollectionStartDate(LocalDate.of(2026, 8, 1));
		paymentEntity.setCollectionEndDate(LocalDate.of(2026, 8, 31));
		paymentEntity.setDiscFin(
				"[{\"DISTFIN_TYPE\":\"DISCOUNT\",\"code\":\"DISC10\",\"Status\":\"Active\"},{\"DISTFIN_TYPE\":\"FINE\",\"code\":\"FINE10\",\"Status\":\"Active\"}]");

		DiscFin discountDiscFin = new DiscFin();
		discountDiscFin.setDiscFnId("DISC10");
		discountDiscFin.setDiscFnCycleType("FIXED");
		discountDiscFin.setDiscFnMode("PERCENTAGE");
		discountDiscFin.setDiscFinValue("5");
		discountDiscFin.setDiscFnStrtDt(LocalDate.of(2026, 7, 1));
		discountDiscFin.setDiscFnEndDt(LocalDate.of(2026, 8, 5));
		discountDiscFin.setMinimumPaymentAmount("0");

		DiscFin fineDiscFin = new DiscFin();
		fineDiscFin.setDiscFnId("FINE10");
		fineDiscFin.setDiscFnCycleType("FIXED");
		fineDiscFin.setDiscFnMode("PERCENTAGE");
		fineDiscFin.setDiscFinValue("10");
		fineDiscFin.setDueDateAsStartDateFlag(Boolean.TRUE);
		fineDiscFin.setBufferTime("0");
		fineDiscFin.setBufferTimeUnit("DAYS");
		fineDiscFin.setFnCalculationType("SIMPLE");
		fineDiscFin.setSimpleFineCycle("MONTHLY");
		fineDiscFin.setDiscFnCumlatonCycle("MONTHLY");

		when(flatRepository.findByAprmntIdAndFlatNo("APRT001", "A-101")).thenReturn(Optional.of(flat));
		when(flatRepository.findByAprmntId("APRT001")).thenReturn(List.of(flat));
		when(dueAmountDetailsRepository.findByDueIdIn(List.of("DUE1"))).thenReturn(List.of(dueEntity));
		when(dueAmountDetailsRepository.findByPaymentId("PAY1")).thenReturn(List.of(dueEntity));
		when(paymentRepository.findFirstByPaymentIdAndAprmtId("PAY1", "APRT001")).thenReturn(Optional.of(paymentEntity));
		when(paymentRepository.findByPaymentIdAndAprmtId("PAY1", "APRT001")).thenReturn(List.of(paymentEntity));
		when(discFinRepository.findByDiscFnId("DISC10")).thenReturn(List.of(discountDiscFin));
		when(discFinRepository.findByDiscFnId("FINE10")).thenReturn(List.of(fineDiscFin));
		when(transactionRepository.findByPymntIdAndTrnsStatusOrderByTrnsDateAsc("PAY1", "SUCCESS")).thenReturn(List.of());
		when(transactionRepository.findByPymntIdAndFlatIdAndTrnsStatus("PAY1", "A-101", "SUCCESS")).thenReturn(List.of());
	}

	private GetDueAmountForFlatRequest buildRequest(LocalDate transactionDate) {
		GetDueAmountForFlatRequest request = new GetDueAmountForFlatRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APRT001");
		request.setGenericHeader(header);
		request.setFlatId("A-101");
		request.setTransactionDate(transactionDate);
		return request;
	}

	private DueAmountDetailsEntity firstDue(GetDueAmountForFlatResponse response) {
		assertNotNull(response.getDueDetails());
		Map.Entry<PaymentDetail, List<DueAmountDetailsEntity>> entry = response.getDueDetails().entrySet().iterator().next();
		assertEquals("PAY1", entry.getKey().getPaymentId());
		return entry.getValue().get(0);
	}
}
