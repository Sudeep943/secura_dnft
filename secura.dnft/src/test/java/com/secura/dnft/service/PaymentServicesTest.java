package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
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
		long activeCount = response.getListOfDueAmountDetails().stream().filter(d -> "ACTIVE".equals(d.getStatus()))
				.count();
		assertEquals(1, activeCount);
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
		assertEquals("4790.5", preResponse.getAmountIncludingGst());

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
		assertEquals("4790.5", postResponse.getAmountIncludingGst());
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
		long oldActiveCount = response.getListOfDueAmountDetails().stream()
				.filter(d -> d.getDueDate().isBefore(today) && "ACTIVE".equals(d.getStatus())).count();

		assertTrue(oldDueCount > 0);
		assertEquals(oldDueCount, oldActiveCount);
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
		oldExisting.setStatus("NOT ACTIVE");
		DueAmountDetails futureExisting = new DueAmountDetails();
		futureExisting.setDueDate(today.plusDays(10));
		futureExisting.setStatus("NOT ACTIVE");

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
	}
}
