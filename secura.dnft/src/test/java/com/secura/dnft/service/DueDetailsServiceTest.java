package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.dao.DiscFinRepository;
import com.secura.dnft.dao.DueAmountDetailsRepository;
import com.secura.dnft.dao.FlatRepository;
import com.secura.dnft.dao.PaymentRepository;
import com.secura.dnft.entity.DiscFin;
import com.secura.dnft.entity.DueAmountDetailsEntity;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.request.response.AddedCharges;
import com.secura.dnft.request.response.DueAmountDetails;
import com.secura.dnft.request.response.GenericHeader;

@ExtendWith(MockitoExtension.class)
class DueDetailsServiceTest {

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private FlatRepository flatRepository;

	@Mock
	private DiscFinRepository discFinRepository;

	@Mock
	private DueAmountDetailsRepository dueAmountDetailsRepository;

	@Mock
	private GenericService genericService;

	@InjectMocks
	private DueDetailsService dueDetailsService;

	@Test
	void calculateDuesForPayment_shouldCreatePerSqftDuesAndPersistEntities() {
		PaymentEntity payment = new PaymentEntity();
		payment.setPaymentId("PAY1001");
		payment.setAprmtId("APR001");
		payment.setPaymentCollectionCycle("QUATERLY");
		payment.setPaymentCollectionMode("PRE");
		payment.setCollectionStartDate(LocalDate.of(2026, 1, 1));
		payment.setCollectionEndDate(LocalDate.of(2026, 3, 31));
		payment.setPaymentAmount("2");
		payment.setPaymentCapita("per Sqft");
		payment.setPaymentName("Maintenance");
		payment.setPaymentType("MAINTENANCE");
		payment.setGst("10");
		payment.setCauseId("COMMON_AREA");

		Flat flat1000 = new Flat();
		flat1000.setFlatNo("A-101");
		flat1000.setFlatArea("1000");
		flat1000.setFlatPndngPaymntLst(null);
		Flat flat1200 = new Flat();
		flat1200.setFlatNo("A-201");
		flat1200.setFlatArea("1200");
		flat1200.setFlatPndngPaymntLst(null);

		GenericHeader header = new GenericHeader();
		header.setUserId("USR001");

		when(paymentRepository.findByPaymentId("PAY1001")).thenReturn(List.of(payment));
		when(flatRepository.findByAprmntId("APR001")).thenReturn(List.of(flat1000, flat1200));
		when(dueAmountDetailsRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(flatRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(genericService.toJson(any())).thenReturn("[\"DUE\"]");

		Map<String, List<Map<String, DueAmountDetails>>> response = dueDetailsService.calculateDuesForPayment("PAY1001", header);

		assertTrue(response.containsKey("QUATERLY"));
		List<Map<String, DueAmountDetails>> dueIntervals = response.get("QUATERLY");
		assertEquals(1, dueIntervals.size());
		Map<String, DueAmountDetails> duesByFlatType = dueIntervals.get(0);
		assertEquals(2, duesByFlatType.size());
		assertTrue(duesByFlatType.containsKey("1000"));
		assertTrue(duesByFlatType.containsKey("1200"));
		assertNotNull(duesByFlatType.get("1000").getEstimatedCollectionAmount());
		assertEquals("COMMON_AREA", duesByFlatType.get("1000").getCause());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<DueAmountDetailsEntity>> dueEntityCaptor = ArgumentCaptor.forClass((Class) List.class);
		verify(dueAmountDetailsRepository, times(1)).saveAll(dueEntityCaptor.capture());
		List<DueAmountDetailsEntity> savedDueEntities = dueEntityCaptor.getValue();
		assertEquals(2, savedDueEntities.size());
		assertEquals("USR001", savedDueEntities.get(0).getCreatUsrId());
		assertEquals("1000", savedDueEntities.get(0).getFlatArea());

		verify(flatRepository, times(1)).saveAll(any());
	}

	@Test
	void calculateDuesForPayment_shouldUseBaseAmountForPercentagesAndPersistCycleSpecificDiscountCodes() {
		PaymentEntity monthlyPayment = createPayment("PAY2001", "APR002", "MONTHLY", "100", "DISC_JSON", "ADDED_JSON");
		monthlyPayment.setCollectionStartDate(LocalDate.of(2026, 1, 1));
		monthlyPayment.setCollectionEndDate(LocalDate.of(2026, 1, 31));

		PaymentEntity halfYearlyPayment = createPayment("PAY2001", "APR002", "HALF YEARLY", "100", "DISC_JSON", "ADDED_JSON");
		halfYearlyPayment.setCollectionStartDate(LocalDate.of(2026, 1, 1));
		halfYearlyPayment.setCollectionEndDate(LocalDate.of(2026, 6, 30));

		PaymentEntity yearlyPayment = createPayment("PAY2001", "APR002", "YEARLY", "100", "DISC_JSON", "ADDED_JSON");
		yearlyPayment.setCollectionStartDate(LocalDate.of(2026, 1, 1));
		yearlyPayment.setCollectionEndDate(LocalDate.of(2026, 12, 31));

		DiscFin halfYearlyDiscount = new DiscFin();
		halfYearlyDiscount.setDiscFnId("DISC10");
		halfYearlyDiscount.setDiscFnCycleType("HALF YEARLY");
		halfYearlyDiscount.setDiscFnMode("PERCENTAGE");
		halfYearlyDiscount.setDiscFinValue("10");
		halfYearlyDiscount.setDiscFnStrtDt(LocalDate.now().minusDays(2));
		halfYearlyDiscount.setDiscFnEndDt(LocalDate.now().plusDays(2));

		DiscFin yearlyDiscount = new DiscFin();
		yearlyDiscount.setDiscFnId("DISC10");
		yearlyDiscount.setDiscFnCycleType("YEARLY");
		yearlyDiscount.setDiscFnMode("PERCENTAGE");
		yearlyDiscount.setDiscFinValue("10");
		yearlyDiscount.setDiscFnStrtDt(LocalDate.now().minusDays(2));
		yearlyDiscount.setDiscFnEndDt(LocalDate.now().plusDays(2));

		Flat flat = new Flat();
		flat.setFlatNo("B-101");
		flat.setFlatArea("900");

		when(paymentRepository.findByPaymentId("PAY2001"))
				.thenReturn(List.of(monthlyPayment, halfYearlyPayment, yearlyPayment));
		when(flatRepository.findByAprmntId("APR002")).thenReturn(List.of(flat));
		when(discFinRepository.findByDiscFnId("DISC10")).thenReturn(List.of(halfYearlyDiscount, yearlyDiscount));
		when(genericService.fromJson(eq("DISC_JSON"), any(TypeReference.class)))
				.thenReturn(List.of(Map.of("DISTFIN_TYPE", "DISCOUNT", "code", "DISC10")));
		when(genericService.fromJson(eq("ADDED_JSON"), any(TypeReference.class))).thenAnswer(invocation -> {
			AddedCharges percentageCharge = new AddedCharges();
			percentageCharge.setChargeName("service-charge");
			percentageCharge.setChargeType("percentage");
			percentageCharge.setValue("10");
			return List.of(percentageCharge);
		});
		when(genericService.toJson(any())).thenReturn("SERIALIZED_JSON");
		when(dueAmountDetailsRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(flatRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

		Map<String, List<Map<String, DueAmountDetails>>> response = dueDetailsService.calculateDuesForPayment("PAY2001");

		assertEquals("0", response.get("MONTHLY").get(0).get("ALL").getDiscountedAmount());
		assertEquals("18", response.get("MONTHLY").get(0).get("ALL").getGstAmount());
		assertEquals("10", response.get("MONTHLY").get(0).get("ALL").getTotalAddedCharges());
		assertEquals("97.2", response.get("HALF YEARLY").get(0).get("ALL").getGstAmount());
		assertEquals("54", response.get("HALF YEARLY").get(0).get("ALL").getTotalAddedCharges());
		assertEquals("DISC10", response.get("HALF YEARLY").get(0).get("ALL").getDiscountCode());
		assertEquals("DISC10", response.get("YEARLY").get(0).get("ALL").getDiscountCode());
		assertNull(response.get("MONTHLY").get(0).get("ALL").getDiscountCode());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<DueAmountDetailsEntity>> dueEntityCaptor = ArgumentCaptor.forClass((Class) List.class);
		verify(dueAmountDetailsRepository, times(1)).saveAll(dueEntityCaptor.capture());
		List<DueAmountDetailsEntity> savedDueEntities = dueEntityCaptor.getValue();
		DueAmountDetailsEntity monthlyEntity = savedDueEntities.stream()
				.filter(entity -> "MONTHLY".equals(entity.getCollectionCycle())).findFirst().orElseThrow();
		assertEquals("0", monthlyEntity.getFineAmount());
		assertEquals("0", monthlyEntity.getAdminDiscount());
		assertEquals("0", monthlyEntity.getAlreadyPaidAmount());
		assertEquals("100", monthlyEntity.getAmountPerMonth());
		assertEquals("SERIALIZED_JSON", monthlyEntity.getAddedCharges());
		assertNull(monthlyEntity.getDiscountCode());
	}

	@Test
	void calculateDuesForPayment_shouldApplyFixedDiscountCodeAcrossAllPaymentCycles() {
		PaymentEntity monthlyPayment = createPayment("PAY3001", "APR003", "MONTHLY", "100", "DISC_JSON", "ADDED_JSON");
		monthlyPayment.setCollectionStartDate(LocalDate.of(2026, 1, 1));
		monthlyPayment.setCollectionEndDate(LocalDate.of(2026, 1, 31));

		PaymentEntity halfYearlyPayment = createPayment("PAY3001", "APR003", "HALF YEARLY", "100", "DISC_JSON", "ADDED_JSON");
		halfYearlyPayment.setCollectionStartDate(LocalDate.of(2026, 1, 1));
		halfYearlyPayment.setCollectionEndDate(LocalDate.of(2026, 6, 30));

		PaymentEntity yearlyPayment = createPayment("PAY3001", "APR003", "YEARLY", "100", "DISC_JSON", "ADDED_JSON");
		yearlyPayment.setCollectionStartDate(LocalDate.of(2026, 1, 1));
		yearlyPayment.setCollectionEndDate(LocalDate.of(2026, 12, 31));

		DiscFin fixedDiscount = new DiscFin();
		fixedDiscount.setDiscFnId("DISC10");
		fixedDiscount.setDiscFnCycleType("FIXED");
		fixedDiscount.setDiscFnMode("PERCENTAGE");
		fixedDiscount.setDiscFinValue("10");
		fixedDiscount.setDiscFnStrtDt(LocalDate.now().minusDays(2));
		fixedDiscount.setDiscFnEndDt(LocalDate.now().plusDays(2));

		Flat flat = new Flat();
		flat.setFlatNo("C-101");
		flat.setFlatArea("950");

		when(paymentRepository.findByPaymentId("PAY3001"))
				.thenReturn(List.of(monthlyPayment, halfYearlyPayment, yearlyPayment));
		when(flatRepository.findByAprmntId("APR003")).thenReturn(List.of(flat));
		when(discFinRepository.findByDiscFnId("DISC10")).thenReturn(List.of(fixedDiscount));
		when(genericService.fromJson(eq("DISC_JSON"), any(TypeReference.class)))
				.thenReturn(List.of(Map.of("DISTFIN_TYPE", "DISCOUNT", "code", "DISC10")));
		when(genericService.fromJson(eq("ADDED_JSON"), any(TypeReference.class))).thenAnswer(invocation -> {
			AddedCharges percentageCharge = new AddedCharges();
			percentageCharge.setChargeName("service-charge");
			percentageCharge.setChargeType("percentage");
			percentageCharge.setValue("10");
			return List.of(percentageCharge);
		});
		when(genericService.toJson(any())).thenReturn("SERIALIZED_JSON");
		when(dueAmountDetailsRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(flatRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

		Map<String, List<Map<String, DueAmountDetails>>> response = dueDetailsService.calculateDuesForPayment("PAY3001");

		assertEquals("DISC10", response.get("MONTHLY").get(0).get("ALL").getDiscountCode());
		assertEquals("DISC10", response.get("HALF YEARLY").get(0).get("ALL").getDiscountCode());
		assertEquals("DISC10", response.get("YEARLY").get(0).get("ALL").getDiscountCode());
		assertEquals("10", response.get("MONTHLY").get(0).get("ALL").getDiscountedAmount());
		assertEquals("60", response.get("HALF YEARLY").get(0).get("ALL").getDiscountedAmount());
		assertEquals("120", response.get("YEARLY").get(0).get("ALL").getDiscountedAmount());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<DueAmountDetailsEntity>> dueEntityCaptor = ArgumentCaptor.forClass((Class) List.class);
		verify(dueAmountDetailsRepository, times(1)).saveAll(dueEntityCaptor.capture());
		List<DueAmountDetailsEntity> savedDueEntities = dueEntityCaptor.getValue();
		DueAmountDetailsEntity monthlyEntity = savedDueEntities.stream()
				.filter(entity -> "MONTHLY".equals(entity.getCollectionCycle())).findFirst().orElseThrow();
		assertEquals("DISC10", monthlyEntity.getDiscountCode());
		assertEquals("PERCENTAGE", monthlyEntity.getDiscountMode());
		assertEquals("10", monthlyEntity.getDiscountedAmount());
	}

	@Test
	void calculateDuesForPayment_shouldMatchQuarterlyDiscountRegardlessOfSpelling() {
		// Payment uses misspelled "QUATERLY"; discount code uses the correct "QUARTERLY"
		// normalizeCycle must treat both as equivalent so the discount is applied
		PaymentEntity quarterlyPayment = createPayment("PAY4001", "APR004", "QUATERLY", "100", "DISC_JSON", null);
		quarterlyPayment.setCollectionStartDate(LocalDate.of(2026, 1, 1));
		quarterlyPayment.setCollectionEndDate(LocalDate.of(2026, 3, 31));

		DiscFin quarterlyDiscount = new DiscFin();
		quarterlyDiscount.setDiscFnId("DISC20");
		quarterlyDiscount.setDiscFnCycleType("QUARTERLY");
		quarterlyDiscount.setDiscFnMode("PERCENTAGE");
		quarterlyDiscount.setDiscFinValue("5");
		quarterlyDiscount.setDiscFnStrtDt(LocalDate.now().minusDays(1));
		quarterlyDiscount.setDiscFnEndDt(LocalDate.now().plusDays(1));

		Flat flat = new Flat();
		flat.setFlatNo("D-101");
		flat.setFlatArea("800");

		when(paymentRepository.findByPaymentId("PAY4001")).thenReturn(List.of(quarterlyPayment));
		when(flatRepository.findByAprmntId("APR004")).thenReturn(List.of(flat));
		when(discFinRepository.findByDiscFnId("DISC20")).thenReturn(List.of(quarterlyDiscount));
		when(genericService.fromJson(eq("DISC_JSON"), any(TypeReference.class)))
				.thenReturn(List.of(Map.of("DISTFIN_TYPE", "DISCOUNT", "code", "DISC20")));
		when(genericService.toJson(any())).thenReturn("SERIALIZED_JSON");
		when(dueAmountDetailsRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(flatRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

		Map<String, List<Map<String, DueAmountDetails>>> response = dueDetailsService.calculateDuesForPayment("PAY4001");

		// The map key retains the original payment cycle spelling ("QUATERLY")
		DueAmountDetails due = response.get("QUATERLY").get(0).get("ALL");
		assertEquals("DISC20", due.getDiscountCode());
		assertEquals("15", due.getDiscountedAmount());
		assertEquals("PERCENTAGE", due.getDiscountMode());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<DueAmountDetailsEntity>> dueEntityCaptor = ArgumentCaptor.forClass((Class) List.class);
		verify(dueAmountDetailsRepository, times(1)).saveAll(dueEntityCaptor.capture());
		List<DueAmountDetailsEntity> savedEntities = dueEntityCaptor.getValue();
		DueAmountDetailsEntity entity = savedEntities.get(0);
		assertEquals("DISC20", entity.getDiscountCode());
		assertEquals("PERCENTAGE", entity.getDiscountMode());
		assertEquals("15", entity.getDiscountedAmount());
	}

	@Test
	void calculateDuesForPayment_shouldCreateMultipleDuesForQuarterlyOverFullYear() {
		// Start: 1 May 2025, End: 30 Apr 2026 → 4 quarterly dues
		PaymentEntity payment = createPayment("PAY5001", "APR005", "QUATERLY", "2000", null, null);
		payment.setCollectionStartDate(LocalDate.of(2025, 5, 1));
		payment.setCollectionEndDate(LocalDate.of(2026, 4, 30));

		Flat flat = new Flat();
		flat.setFlatNo("E-101");
		flat.setFlatArea("500");
		flat.setFlatPndngPaymntLst(null);

		when(paymentRepository.findByPaymentId("PAY5001")).thenReturn(List.of(payment));
		when(flatRepository.findByAprmntId("APR005")).thenReturn(List.of(flat));
		when(dueAmountDetailsRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(flatRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(genericService.toJson(any())).thenReturn("[]");

		Map<String, List<Map<String, DueAmountDetails>>> response = dueDetailsService.calculateDuesForPayment("PAY5001");

		List<Map<String, DueAmountDetails>> quarterlyDues = response.get("QUATERLY");
		assertEquals(4, quarterlyDues.size(), "Should generate 4 quarterly dues for 12-month range");

		// PRE mode: due dates are interval start dates
		assertEquals(LocalDate.of(2025, 5, 1), quarterlyDues.get(0).get("ALL").getDueDate());
		assertEquals(LocalDate.of(2025, 8, 1), quarterlyDues.get(1).get("ALL").getDueDate());
		assertEquals(LocalDate.of(2025, 11, 1), quarterlyDues.get(2).get("ALL").getDueDate());
		assertEquals(LocalDate.of(2026, 2, 1), quarterlyDues.get(3).get("ALL").getDueDate());

		// All 4 dues are full quarters: 2000 * 3 = 6000
		for (Map<String, DueAmountDetails> duesByFlatType : quarterlyDues) {
			assertEquals("6000", duesByFlatType.get("ALL").getAmount());
		}

		// All dues of the same cycle share the same dueId
		long distinctDueIds = quarterlyDues.stream()
				.map(m -> m.get("ALL").getDueId())
				.distinct()
				.count();
		assertEquals(1, distinctDueIds, "All dues of the same cycle should share the same dueId");

		// 4 entities saved (1 flat type × 4 intervals)
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<DueAmountDetailsEntity>> captor = ArgumentCaptor.forClass((Class) List.class);
		verify(dueAmountDetailsRepository, times(1)).saveAll(captor.capture());
		assertEquals(4, captor.getValue().size());
	}

	@Test
	void calculateDuesForPayment_shouldProrateAmountForPartialLastCycle() {
		// Start: 1 May 2025, End: 30 Mar 2026 → 3 full quarters + 1 partial (Feb–Mar)
		PaymentEntity payment = createPayment("PAY6001", "APR006", "QUATERLY", "2000", null, null);
		payment.setCollectionStartDate(LocalDate.of(2025, 5, 1));
		payment.setCollectionEndDate(LocalDate.of(2026, 3, 30));
		payment.setGst("0");

		Flat flat = new Flat();
		flat.setFlatNo("F-101");
		flat.setFlatArea("500");
		flat.setFlatPndngPaymntLst(null);

		when(paymentRepository.findByPaymentId("PAY6001")).thenReturn(List.of(payment));
		when(flatRepository.findByAprmntId("APR006")).thenReturn(List.of(flat));
		when(dueAmountDetailsRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(flatRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(genericService.toJson(any())).thenReturn("[]");

		Map<String, List<Map<String, DueAmountDetails>>> response = dueDetailsService.calculateDuesForPayment("PAY6001");

		List<Map<String, DueAmountDetails>> quarterlyDues = response.get("QUATERLY");
		assertEquals(4, quarterlyDues.size());

		// First 3 dues: full quarter amount = 2000 * 3 = 6000
		assertEquals("6000", quarterlyDues.get(0).get("ALL").getAmount());
		assertEquals("6000", quarterlyDues.get(1).get("ALL").getAmount());
		assertEquals("6000", quarterlyDues.get(2).get("ALL").getAmount());

		// Due 4 (Feb 1 – Mar 30): covers Feb and Mar = 2 months → 2000 * 2 = 4000
		assertEquals("4000", quarterlyDues.get(3).get("ALL").getAmount());
		assertEquals(LocalDate.of(2026, 2, 1), quarterlyDues.get(3).get("ALL").getDueDate());
	}

	@Test
	void calculateDuesForPayment_shouldCreateTwoDuesForHalfYearlyOverFullYear() {
		// Start: 1 May 2025, End: 30 Apr 2026 → 2 half-yearly dues
		PaymentEntity payment = createPayment("PAY7001", "APR007", "HALF YEARLY", "1000", null, null);
		payment.setCollectionStartDate(LocalDate.of(2025, 5, 1));
		payment.setCollectionEndDate(LocalDate.of(2026, 4, 30));
		payment.setGst("0");

		Flat flat = new Flat();
		flat.setFlatNo("G-101");
		flat.setFlatArea("500");
		flat.setFlatPndngPaymntLst(null);

		when(paymentRepository.findByPaymentId("PAY7001")).thenReturn(List.of(payment));
		when(flatRepository.findByAprmntId("APR007")).thenReturn(List.of(flat));
		when(dueAmountDetailsRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(flatRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(genericService.toJson(any())).thenReturn("[]");

		Map<String, List<Map<String, DueAmountDetails>>> response = dueDetailsService.calculateDuesForPayment("PAY7001");

		List<Map<String, DueAmountDetails>> halfYearlyDues = response.get("HALF YEARLY");
		assertEquals(2, halfYearlyDues.size(), "Should generate 2 half-yearly dues for 12-month range");
		assertEquals(LocalDate.of(2025, 5, 1), halfYearlyDues.get(0).get("ALL").getDueDate());
		assertEquals(LocalDate.of(2025, 11, 1), halfYearlyDues.get(1).get("ALL").getDueDate());
		assertEquals("6000", halfYearlyDues.get(0).get("ALL").getAmount());
		assertEquals("6000", halfYearlyDues.get(1).get("ALL").getAmount());
	}

	@Test
	void calculateDuesForPayment_shouldApplyAmountFineAndPersistFineMetadata() {
		PaymentEntity payment = createPayment("PAY8001", "APR008", "MONTHLY", "100", "FINE_JSON", null);
		payment.setCollectionStartDate(LocalDate.now().minusDays(1));
		payment.setCollectionEndDate(LocalDate.now().plusDays(29));
		payment.setGst("0");

		DiscFin fine = new DiscFin();
		fine.setDiscFnId("FINE50");
		fine.setDiscFnType("FINE");
		fine.setDiscFnCycleType("FIXED");
		fine.setDiscFnMode("AMOUNT");
		fine.setDiscFnCumlatonCycle("SIMPLE");
		fine.setDiscFinValue("50");
		fine.setDueDateAsStartDateFlag(Boolean.FALSE);
		fine.setDiscFnStrtDt(LocalDate.now().minusDays(3));
		fine.setDiscFnEndDt(LocalDate.now().plusDays(3));

		Flat flat = new Flat();
		flat.setFlatNo("H-101");
		flat.setFlatArea("500");
		flat.setFlatPndngPaymntLst(null);

		when(paymentRepository.findByPaymentId("PAY8001")).thenReturn(List.of(payment));
		when(flatRepository.findByAprmntId("APR008")).thenReturn(List.of(flat));
		when(discFinRepository.findByDiscFnId("FINE50")).thenReturn(List.of(fine));
		when(genericService.fromJson(eq("FINE_JSON"), any(TypeReference.class)))
				.thenReturn(List.of(Map.of("DISTFIN_TYPE", "FINE", "code", "FINE50")));
		when(genericService.toJson(any())).thenReturn("[]");
		when(dueAmountDetailsRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(flatRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

		Map<String, List<Map<String, DueAmountDetails>>> response = dueDetailsService.calculateDuesForPayment("PAY8001");

		DueAmountDetails due = response.get("MONTHLY").get(0).get("ALL");
		assertEquals("FINE50", due.getFineCode());
		assertEquals("50", due.getFineAmount());
		assertEquals("FINE", due.getFineType());
		assertEquals("AMOUNT", due.getFineMode());
		assertEquals("SIMPLE", due.getCummilationCycle());
		assertEquals("150", due.getTotalAmount());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<DueAmountDetailsEntity>> dueEntityCaptor = ArgumentCaptor.forClass((Class) List.class);
		verify(dueAmountDetailsRepository, times(1)).saveAll(dueEntityCaptor.capture());
		DueAmountDetailsEntity saved = dueEntityCaptor.getValue().get(0);
		assertEquals("50", saved.getFineAmount());
		assertEquals("AMOUNT", saved.getFineMode());
		assertEquals("SIMPLE", saved.getCummilationCycle());
		assertEquals("FINE", saved.getFineType());
	}

	@Test
	void calculateDuesForPayment_shouldApplyCumulativePercentageFineUsingDueDateAsStartDate() {
		LocalDate dueDate = LocalDate.now().minusDays(30);
		PaymentEntity payment = createPayment("PAY9001", "APR009", "MONTHLY", "100", "FINE_JSON", null);
		payment.setCollectionStartDate(dueDate);
		payment.setCollectionEndDate(dueDate.plusMonths(1).minusDays(1));
		payment.setGst("0");

		DiscFin fine = new DiscFin();
		fine.setDiscFnId("FINEPCT10");
		fine.setDiscFnType("FINE");
		fine.setDiscFnCycleType("FIXED");
		fine.setDiscFnMode("PERCENTAGE");
		fine.setDiscFnCumlatonCycle("CUMULATIVE");
		fine.setFnCalculationType("CUMULATIVE");
		fine.setDiscFinValue("10");
		fine.setDueDateAsStartDateFlag(Boolean.TRUE);
		fine.setDiscFnEndDt(LocalDate.now().plusDays(2));

		Flat flat = new Flat();
		flat.setFlatNo("I-101");
		flat.setFlatArea("500");
		flat.setFlatPndngPaymntLst(null);

		when(paymentRepository.findByPaymentId("PAY9001")).thenReturn(List.of(payment));
		when(flatRepository.findByAprmntId("APR009")).thenReturn(List.of(flat));
		when(discFinRepository.findByDiscFnId("FINEPCT10")).thenReturn(List.of(fine));
		when(genericService.fromJson(eq("FINE_JSON"), any(TypeReference.class)))
				.thenReturn(List.of(Map.of("DISTFIN_TYPE", "FINE", "code", "FINEPCT10")));
		when(genericService.toJson(any())).thenReturn("[]");
		when(dueAmountDetailsRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(flatRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

		Map<String, List<Map<String, DueAmountDetails>>> response = dueDetailsService.calculateDuesForPayment("PAY9001");

		long dayDiff = ChronoUnit.DAYS.between(dueDate, LocalDate.now());
		BigDecimal expectedFine = BigDecimal.ZERO;
		if (dayDiff > 0) {
			double compoundedFactor = Math.pow(BigDecimal.valueOf(1.1d).doubleValue(), dayDiff / 365.0d);
			expectedFine = BigDecimal.valueOf(100).multiply(BigDecimal.valueOf(compoundedFactor)).subtract(BigDecimal.valueOf(100))
					.setScale(2, RoundingMode.HALF_UP);
		}
		BigDecimal expectedTotal = BigDecimal.valueOf(100).add(expectedFine);
		BigDecimal expectedRoundedTotal = expectedTotal.setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);

		DueAmountDetails due = response.get("MONTHLY").get(0).get("ALL");
		assertEquals(format(expectedFine), due.getFineAmount());
		assertEquals("CUMULATIVE", due.getCummilationCycle());
		assertEquals("PERCENTAGE", due.getFineMode());
		assertEquals(format(expectedRoundedTotal), due.getTotalAmount());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<DueAmountDetailsEntity>> dueEntityCaptor = ArgumentCaptor.forClass((Class) List.class);
		verify(dueAmountDetailsRepository, times(1)).saveAll(dueEntityCaptor.capture());
		DueAmountDetailsEntity saved = dueEntityCaptor.getValue().get(0);
		assertEquals(format(expectedFine), saved.getFineAmount());
		assertEquals("CUMULATIVE", saved.getCummilationCycle());
		assertEquals("PERCENTAGE", saved.getFineMode());
	}

	private PaymentEntity createPayment(String paymentId, String apartmentId, String cycle, String amount, String discFin,
			String addedCharges) {
		PaymentEntity payment = new PaymentEntity();
		payment.setPaymentId(paymentId);
		payment.setAprmtId(apartmentId);
		payment.setPaymentCollectionCycle(cycle);
		payment.setPaymentCollectionMode("PRE");
		payment.setPaymentAmount(amount);
		payment.setPaymentCapita("FLAT");
		payment.setPaymentName("Maintenance");
		payment.setPaymentType("MAINTENANCE");
		payment.setGst("18");
		payment.setCauseId("COMMON_AREA");
		payment.setDiscFin(discFin);
		payment.setAddedCharges(addedCharges);
		return payment;
	}

	private String format(BigDecimal value) {
		if (value == null) {
			return "0";
		}
		return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
	}
}
