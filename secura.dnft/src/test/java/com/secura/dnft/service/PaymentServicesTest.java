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

import java.sql.Date;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.dao.FlatRepository;
import com.secura.dnft.dao.PaymentRepository;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.AddedCharges;
import com.secura.dnft.request.response.CreatePaymentRequest;
import com.secura.dnft.request.response.DueAmountDetails;
import com.secura.dnft.request.response.DuePaymentAmountDetailsRequest;
import com.secura.dnft.request.response.DuePaymentAmountDetailsResponse;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.GetDuePaymentAmountDetailsResponse;

@ExtendWith(MockitoExtension.class)
class PaymentServicesTest {

	@Mock
	private GenericService genericService;

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private FlatRepository flatRepository;

	@InjectMocks
	private PaymentServices paymentServices;

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
		assertEquals("100", response.getListOfDueAmountDetails().get(0).getGstAmount());
		assertEquals("1100", response.getListOfDueAmountDetails().get(0).getTotalAmount());
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
		assertEquals(new BigDecimal("1000").add(chargeTotal).toPlainString(), dueDetails.getAmount());
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

		assertEquals("1100", dueDetails.getAmount());
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
		assertEquals("1200.4", dueDetails.getAmount());
		assertEquals("100", dueDetails.getGstAmount());
		assertEquals("1300", dueDetails.getTotalAmount());
	}

	@Test
	void getDuePaymentAmountDetails_shouldReturnFlatAreaWiseDueAmountsWhenPaymentCapitaIsPerSqft() {
		LocalDate today = LocalDate.now();
		CreatePaymentRequest request = new CreatePaymentRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-001");
		request.setGenericHeader(header);
		request.setPaymentCapita("Per Sqft");
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
		assertEquals("200", response.getFlatTypeDueAmountDetails().get("1000").get(0).getGstAmount());
		assertEquals("2200", response.getFlatTypeDueAmountDetails().get("1000").get(0).getTotalAmount());
		assertEquals("2400", response.getFlatTypeDueAmountDetails().get("1200").get(0).getAmount());
		assertEquals("240", response.getFlatTypeDueAmountDetails().get("1200").get(0).getGstAmount());
		assertEquals("2640", response.getFlatTypeDueAmountDetails().get("1200").get(0).getTotalAmount());
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
		request.setApplicableFor("[\"A-101\"]");
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
		when(genericService.fromJson(eq("[\"A-101\"]"), any(TypeReference.class))).thenReturn(List.of("A-101"));
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
		verify(genericService, times(1)).toJson(dueListCaptor.capture());
		@SuppressWarnings("unchecked")
		List<DueAmountDetails> dueDetails = (List<DueAmountDetails>) dueListCaptor.getValue();
		assertTrue(dueDetails.stream().allMatch(d -> d.getDueId() != null && d.getDueId().startsWith("DUE")));
		assertEquals("100", dueDetails.get(0).getAddedCharges().get(0).getFinalChargeValue());
	}

	@Test
	void createPayment_shouldSetMaintainanceFeeFromCamPaymentFlag() throws Exception {
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
		request.setCamPayment(true);

		when(genericService.getCorrectLocalDateForInputDate(any(Date.class)))
				.thenAnswer(invocation -> ((Date) invocation.getArgument(0)).toLocalDate().atStartOfDay());
		when(flatRepository.findByAprmntId("APR-001")).thenReturn(List.of());
		when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentServices.createPayment(request);

		ArgumentCaptor<PaymentEntity> paymentCaptor = ArgumentCaptor.forClass(PaymentEntity.class);
		verify(paymentRepository, times(1)).save(paymentCaptor.capture());
		assertTrue(paymentCaptor.getValue().isMaintainanceFee());
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
		request.setApplicableFor("[\"A-101\"]");
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
		when(genericService.fromJson(eq("[\"A-101\"]"), any(TypeReference.class))).thenReturn(List.of("A-101"));
		when(genericService.fromJson(eq("EXISTING_JSON"), any(TypeReference.class)))
				.thenReturn(List.of(oldExisting, futureExisting));
		when(genericService.toJson(any())).thenReturn("DUE_JSON");
		when(flatRepository.findByAprmntId("APR-001")).thenReturn(List.of(targetFlat));
		when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentServices.createPayment(request);

		ArgumentCaptor<Object> dueListCaptor = ArgumentCaptor.forClass(Object.class);
		verify(genericService, times(1)).toJson(dueListCaptor.capture());
		@SuppressWarnings("unchecked")
		List<DueAmountDetails> savedDueList = (List<DueAmountDetails>) dueListCaptor.getValue();
		assertTrue(savedDueList.stream().allMatch(d -> !d.getDueDate().isBefore(today)));
		assertTrue(savedDueList.stream().allMatch(d -> d.getDueId() != null && d.getDueId().startsWith("DUE")));
	}
}
