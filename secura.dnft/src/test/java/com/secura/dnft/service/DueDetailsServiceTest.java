package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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
		payment.setCollectionStartDate(LocalDateTime.parse("2026-01-01T00:00:00"));
		payment.setCollectionEndDate(LocalDateTime.parse("2026-03-31T00:00:00"));
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

		Map<String, Map<String, DueAmountDetails>> response = dueDetailsService.calculateDuesForPayment("PAY1001", header);

		assertTrue(response.containsKey("QUATERLY"));
		Map<String, DueAmountDetails> duesByFlatType = response.get("QUATERLY");
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
		monthlyPayment.setCollectionStartDate(LocalDateTime.parse("2026-01-01T00:00:00"));
		monthlyPayment.setCollectionEndDate(LocalDateTime.parse("2026-01-31T00:00:00"));

		PaymentEntity halfYearlyPayment = createPayment("PAY2001", "APR002", "HALF YEARLY", "100", "DISC_JSON", "ADDED_JSON");
		halfYearlyPayment.setCollectionStartDate(LocalDateTime.parse("2026-01-01T00:00:00"));
		halfYearlyPayment.setCollectionEndDate(LocalDateTime.parse("2026-06-30T00:00:00"));

		PaymentEntity yearlyPayment = createPayment("PAY2001", "APR002", "YEARLY", "100", "DISC_JSON", "ADDED_JSON");
		yearlyPayment.setCollectionStartDate(LocalDateTime.parse("2026-01-01T00:00:00"));
		yearlyPayment.setCollectionEndDate(LocalDateTime.parse("2026-12-31T00:00:00"));

		DiscFin halfYearlyDiscount = new DiscFin();
		halfYearlyDiscount.setDiscFnId("DISC10");
		halfYearlyDiscount.setDiscFnCycleType("HALF YEARLY");
		halfYearlyDiscount.setDiscFnMode("PERCENTAGE");
		halfYearlyDiscount.setDiscFinValue("10");
		halfYearlyDiscount.setDiscFnStrtDt(LocalDateTime.now().minusDays(2));
		halfYearlyDiscount.setDiscFnEndDt(LocalDateTime.now().plusDays(2));

		DiscFin yearlyDiscount = new DiscFin();
		yearlyDiscount.setDiscFnId("DISC10");
		yearlyDiscount.setDiscFnCycleType("YEARLY");
		yearlyDiscount.setDiscFnMode("PERCENTAGE");
		yearlyDiscount.setDiscFinValue("10");
		yearlyDiscount.setDiscFnStrtDt(LocalDateTime.now().minusDays(2));
		yearlyDiscount.setDiscFnEndDt(LocalDateTime.now().plusDays(2));

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

		Map<String, Map<String, DueAmountDetails>> response = dueDetailsService.calculateDuesForPayment("PAY2001");

		assertEquals("0", response.get("MONTHLY").get("ALL").getDiscountedAmount());
		assertEquals("18", response.get("MONTHLY").get("ALL").getGstAmount());
		assertEquals("10", response.get("MONTHLY").get("ALL").getTotalAddedCharges());
		assertEquals("97.2", response.get("HALF YEARLY").get("ALL").getGstAmount());
		assertEquals("54", response.get("HALF YEARLY").get("ALL").getTotalAddedCharges());
		assertEquals("DISC10", response.get("HALF YEARLY").get("ALL").getDiscountCode());
		assertEquals("DISC10", response.get("YEARLY").get("ALL").getDiscountCode());
		assertEquals(null, response.get("MONTHLY").get("ALL").getDiscountCode());

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
		assertEquals(null, monthlyEntity.getDiscountCode());
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
}
