package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.dao.DueAmountDetailsRepository;
import com.secura.dnft.dao.FlatRepository;
import com.secura.dnft.dao.OwnerRepository;
import com.secura.dnft.dao.PaymentRepository;
import com.secura.dnft.dao.ProfileRepository;
import com.secura.dnft.dao.TransactionRepository;
import com.secura.dnft.entity.DueAmountDetailsEntity;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.Owner;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.entity.Profile;
import com.secura.dnft.entity.Transaction;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.Name;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.DefaultPayment;
import com.secura.dnft.request.response.Defaulter;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.GetDefaulterRequest;
import com.secura.dnft.request.response.GetDefaulterResponse;

@ExtendWith(MockitoExtension.class)
class TransactionAndReportsServiceTest {

	@Mock
	private TransactionRepository transactionRepository;

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private GenericService genericService;

	@Mock
	private DueAmountDetailsRepository dueAmountDetailsRepository;

	@Mock
	private FlatRepository flatRepository;

	@Mock
	private OwnerRepository ownerRepository;

	@Mock
	private ProfileRepository profileRepository;

	@InjectMocks
	private TransactionAndReportsService transactionAndReportsService;

	@Test
	void getDefaulterList_shouldReturnGroupedDefaultersForActiveMandatoryPayments() {
		GetDefaulterRequest request = new GetDefaulterRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		request.setGenericHeader(header);
		request.setPaymentId(List.of("PAY-1", "PAY-2", "PAY-3"));

		when(paymentRepository.findByPaymentIdAndAprmtId("PAY-1", "APR-1"))
				.thenReturn(List.of(buildPayment("PAY-1", "Maintenance", "PER_SQFT",
						SecuraConstants.PAYMENT_STATUS_ACTIVE, "MANDATORY")));
		when(paymentRepository.findByPaymentIdAndAprmtId("PAY-2", "APR-1"))
				.thenReturn(List.of(buildPayment("PAY-2", "Club Fund", "FIXED_AMOUNT",
						SecuraConstants.PAYMENT_STATUS_ACTIVE, "OPTIONAL")));
		when(paymentRepository.findByPaymentIdAndAprmtId("PAY-3", "APR-1"))
				.thenReturn(List.of(buildPayment("PAY-3", "Parking", "PER_HEAD", "INACTIVE", "MANDATORY")));

		LocalDate firstDueDate = LocalDate.now().minusDays(15);
		LocalDate secondDueDate = LocalDate.now().minusDays(2);
		when(dueAmountDetailsRepository.findByPaymentId("PAY-1")).thenReturn(List.of(
				buildDue("PAY-1", "DUE-1", firstDueDate, "100", "10", "[\"F-101\",\"F-102\"]", "[\"F-102\"]"),
				buildDue("PAY-1", "DUE-2", secondDueDate, "70", "5", "[\"F-101\"]", "[]"),
				buildDue("PAY-1", "DUE-3", LocalDate.now().plusDays(10), "90", "0", "[\"F-101\"]", "[]")));

		Owner owner = new Owner();
		owner.setFlatNo("F-101");
		owner.setStatus(SecuraConstants.PROFILE_STATUS_ACTIVE);
		owner.setPrflId("[\"PR-1\",\"PR-2\"]");
		when(ownerRepository.findByFlatNo("F-101")).thenReturn(List.of(owner));

		Profile profileOne = new Profile();
		profileOne.setPrflId("PR-1");
		profileOne.setPrflPhoneNo("9999999999");
		profileOne.setPrflName("{\"firstName\":\"John\",\"lastName\":\"Doe\"}");
		Profile profileTwo = new Profile();
		profileTwo.setPrflId("PR-2");
		profileTwo.setPrflPhoneNo("8888888888");
		profileTwo.setPrflName("{\"firstName\":\"Jane\",\"lastName\":\"Doe\"}");
		when(profileRepository.findById("PR-1")).thenReturn(Optional.of(profileOne));
		when(profileRepository.findById("PR-2")).thenReturn(Optional.of(profileTwo));
		when(flatRepository.findById("F-101")).thenReturn(Optional.of(buildFlat("F-101", "SUPER_BUILT_UP")));

		when(genericService.fromJson(anyString(), any(TypeReference.class))).thenAnswer(invocation -> {
			String json = invocation.getArgument(0);
			if ("[\"F-101\",\"F-102\"]".equals(json)) {
				return List.of("F-101", "F-102");
			}
			if ("[\"F-102\"]".equals(json)) {
				return List.of("F-102");
			}
			if ("[\"F-101\"]".equals(json)) {
				return List.of("F-101");
			}
			if ("[\"PR-1\",\"PR-2\"]".equals(json)) {
				return List.of("PR-1", "PR-2");
			}
			return List.of();
		});
		when(genericService.fromJson(eq("{\"firstName\":\"John\",\"lastName\":\"Doe\"}"), eq(Name.class)))
				.thenReturn(buildName("John", "Doe"));
		when(genericService.fromJson(eq("{\"firstName\":\"Jane\",\"lastName\":\"Doe\"}"), eq(Name.class)))
				.thenReturn(buildName("Jane", "Doe"));

		Transaction paidTransaction = new Transaction();
		paidTransaction.setTrnsAmt("50");
		when(transactionRepository.findByPymntIdAndFlatIdAndTrnsStatus("PAY-1", "F-101", "SUCCESS"))
				.thenReturn(List.of(paidTransaction));

		GetDefaulterResponse response = transactionAndReportsService.getDefaulterList(request);

		assertEquals(SuccessMessage.SUCC_MESSAGE_45, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_45, response.getMessageCode());
		assertEquals(1, response.getTotalDefaulters());
		assertEquals("50", response.getTotalMoneyCollected());
		assertEquals("120", response.getTotalExpectedToBeCollect());
		assertEquals(1, response.getDefaulterList().size());

		Defaulter defaulter = response.getDefaulterList().get(0);
		assertEquals("F-101", defaulter.getFlatId());
		assertEquals("SUPER BUILT UP", defaulter.getBuiltUpArea());
		assertEquals(List.of("John Doe", "Jane Doe"), defaulter.getOwnerNames());
		assertEquals("9999999999, 8888888888", defaulter.getPhoneNumber());
		assertEquals(1, defaulter.getDefaultPaymentList().size());

		DefaultPayment defaultPayment = defaulter.getDefaultPaymentList().get(0);
		assertEquals("PAY-1", defaultPayment.getPaymentId());
		assertEquals("Maintenance", defaultPayment.getPaymentName());
		assertEquals("PER SQFT", defaultPayment.getPaymentCapita());
		assertEquals("170", defaultPayment.getTotalDue());
		assertEquals("50", defaultPayment.getAmountPaid());
		assertEquals("120", defaultPayment.getAmountTobePaid());
		assertEquals("15", defaultPayment.getPenalty());
		assertEquals(secondDueDate, defaultPayment.getLastDueDate());

		verify(dueAmountDetailsRepository, never()).findByPaymentId("PAY-2");
		verify(dueAmountDetailsRepository, never()).findByPaymentId("PAY-3");
	}

	@Test
	void getDefaulterList_shouldRequireApartmentId() {
		GetDefaulterRequest request = new GetDefaulterRequest();
		request.setPaymentId(List.of("PAY-1"));

		GetDefaulterResponse response = transactionAndReportsService.getDefaulterList(request);

		assertTrue(response.getDefaulterList().isEmpty());
		assertEquals(0, response.getTotalDefaulters());
		assertEquals(ErrorMessage.ERR_MESSAGE_05, response.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_05, response.getMessageCode());
	}

	@Test
	void getDefaulterList_shouldUseHighestCycleDuesPerPayment() {
		GetDefaulterRequest request = new GetDefaulterRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		request.setGenericHeader(header);
		request.setPaymentId(List.of("PAY-1", "PAY-2", "PAY-3"));

		when(paymentRepository.findByPaymentIdAndAprmtId("PAY-1", "APR-1"))
				.thenReturn(List.of(buildPayment("PAY-1", "Maintenance", "FIXED_AMOUNT",
						SecuraConstants.PAYMENT_STATUS_ACTIVE, "MANDATORY")));
		when(paymentRepository.findByPaymentIdAndAprmtId("PAY-2", "APR-1"))
				.thenReturn(List.of(buildPayment("PAY-2", "Sinking Fund", "PER_HEAD",
						SecuraConstants.PAYMENT_STATUS_ACTIVE, "MANDATORY")));
		when(paymentRepository.findByPaymentIdAndAprmtId("PAY-3", "APR-1"))
				.thenReturn(List.of(buildPayment("PAY-3", "Parking", "PER_SQFT",
						SecuraConstants.PAYMENT_STATUS_ACTIVE, "MANDATORY")));

		LocalDate yearlyDueOne = LocalDate.now().minusDays(30);
		LocalDate yearlyDueTwo = LocalDate.now().minusDays(20);
		LocalDate monthlyDue = LocalDate.now().minusDays(5);
		when(dueAmountDetailsRepository.findByPaymentId("PAY-1")).thenReturn(List.of(
				buildDue("PAY-1", "DUE-1", "YEARLY", yearlyDueOne, "100", "8", "[\"F-101\"]", "[]"),
				buildDue("PAY-1", "DUE-2", "YEARLY", yearlyDueTwo, "20", "2", "[\"F-101\"]", "[]"),
				buildDue("PAY-1", "DUE-3", "MONTHLY", monthlyDue, "10", "1", "[\"F-101\"]", "[]")));

		LocalDate quarterlyDue = LocalDate.now().minusDays(18);
		LocalDate monthlyDueForQuarterlyPayment = LocalDate.now().minusDays(2);
		when(dueAmountDetailsRepository.findByPaymentId("PAY-2")).thenReturn(List.of(
				buildDue("PAY-2", "DUE-4", "QUARTERLY", quarterlyDue, "60", "4", "[\"F-101\"]", "[]"),
				buildDue("PAY-2", "DUE-5", "MONTHLY", monthlyDueForQuarterlyPayment, "15", "1", "[\"F-101\"]", "[]")));

		LocalDate onlyMonthlyDue = LocalDate.now().minusDays(7);
		when(dueAmountDetailsRepository.findByPaymentId("PAY-3")).thenReturn(List.of(
				buildDue("PAY-3", "DUE-6", "MONTHLY", onlyMonthlyDue, "25", "3", "[\"F-101\"]", "[]")));

		Owner owner = new Owner();
		owner.setFlatNo("F-101");
		owner.setStatus(SecuraConstants.PROFILE_STATUS_ACTIVE);
		owner.setPrflId("[\"PR-1\"]");
		when(ownerRepository.findByFlatNo("F-101")).thenReturn(List.of(owner));

		Profile profile = new Profile();
		profile.setPrflId("PR-1");
		profile.setPrflPhoneNo("9999999999");
		profile.setPrflName("{\"firstName\":\"John\",\"lastName\":\"Doe\"}");
		when(profileRepository.findById("PR-1")).thenReturn(Optional.of(profile));
		when(flatRepository.findById("F-101")).thenReturn(Optional.of(buildFlat("F-101", "LARGE_2_BHK")));

		when(genericService.fromJson(anyString(), any(TypeReference.class))).thenAnswer(invocation -> {
			String json = invocation.getArgument(0);
			if ("[\"F-101\"]".equals(json)) {
				return List.of("F-101");
			}
			if ("[]".equals(json)) {
				return List.of();
			}
			if ("[\"PR-1\"]".equals(json)) {
				return List.of("PR-1");
			}
			return List.of();
		});
		when(genericService.fromJson(eq("{\"firstName\":\"John\",\"lastName\":\"Doe\"}"), eq(Name.class)))
				.thenReturn(buildName("John", "Doe"));

		GetDefaulterResponse response = transactionAndReportsService.getDefaulterList(request);

		assertEquals(SuccessMessage.SUCC_MESSAGE_45, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_45, response.getMessageCode());
		assertEquals(1, response.getTotalDefaulters());
		assertEquals("0", response.getTotalMoneyCollected());
		assertEquals("205", response.getTotalExpectedToBeCollect());
		assertEquals(1, response.getDefaulterList().size());

		Defaulter defaulter = response.getDefaulterList().get(0);
		assertEquals("F-101", defaulter.getFlatId());
		assertEquals("LARGE 2 BHK", defaulter.getBuiltUpArea());
		assertEquals(3, defaulter.getDefaultPaymentList().size());

		DefaultPayment yearlyPayment = defaulter.getDefaultPaymentList().get(0);
		assertEquals("PAY-1", yearlyPayment.getPaymentId());
		assertEquals("FIXED AMOUNT", yearlyPayment.getPaymentCapita());
		assertEquals("120", yearlyPayment.getTotalDue());
		assertEquals("0", yearlyPayment.getAmountPaid());
		assertEquals("120", yearlyPayment.getAmountTobePaid());
		assertEquals("10", yearlyPayment.getPenalty());
		assertEquals(yearlyDueTwo, yearlyPayment.getLastDueDate());

		DefaultPayment quarterlyPayment = defaulter.getDefaultPaymentList().get(1);
		assertEquals("PAY-2", quarterlyPayment.getPaymentId());
		assertEquals("PER HEAD", quarterlyPayment.getPaymentCapita());
		assertEquals("60", quarterlyPayment.getTotalDue());
		assertEquals("0", quarterlyPayment.getAmountPaid());
		assertEquals("60", quarterlyPayment.getAmountTobePaid());
		assertEquals("4", quarterlyPayment.getPenalty());
		assertEquals(quarterlyDue, quarterlyPayment.getLastDueDate());

		DefaultPayment monthlyPayment = defaulter.getDefaultPaymentList().get(2);
		assertEquals("PAY-3", monthlyPayment.getPaymentId());
		assertEquals("PER SQFT", monthlyPayment.getPaymentCapita());
		assertEquals("25", monthlyPayment.getTotalDue());
		assertEquals("0", monthlyPayment.getAmountPaid());
		assertEquals("25", monthlyPayment.getAmountTobePaid());
		assertEquals("3", monthlyPayment.getPenalty());
		assertEquals(onlyMonthlyDue, monthlyPayment.getLastDueDate());
	}

	private PaymentEntity buildPayment(String paymentId, String paymentName, String paymentCapita, String status,
			String paymentType) {
		PaymentEntity payment = new PaymentEntity();
		payment.setPaymentId(paymentId);
		payment.setPaymentName(paymentName);
		payment.setPaymentCapita(paymentCapita);
		payment.setStatus(status);
		payment.setPaymentType(paymentType);
		return payment;
	}

	private Flat buildFlat(String flatId, String flatArea) {
		Flat flat = new Flat();
		flat.setFlatNo(flatId);
		flat.setFlatArea(flatArea);
		return flat;
	}

	private DueAmountDetailsEntity buildDue(String paymentId, String dueId, LocalDate dueDate, String totalAmount,
			String fineAmount, String applicableFlats, String paidFlats) {
		return buildDue(paymentId, dueId, "MONTHLY", dueDate, totalAmount, fineAmount, applicableFlats, paidFlats);
	}

	private DueAmountDetailsEntity buildDue(String paymentId, String dueId, String cycle, LocalDate dueDate, String totalAmount,
			String fineAmount, String applicableFlats, String paidFlats) {
		DueAmountDetailsEntity due = new DueAmountDetailsEntity();
		due.setPaymentId(paymentId);
		due.setDueId(dueId);
		due.setCollectionCycle(cycle);
		due.setFlatArea("ALL");
		due.setDueDate(dueDate);
		due.setTotalAmount(totalAmount);
		due.setFineAmount(fineAmount);
		due.setApplicableFlats(applicableFlats);
		due.setPaidFlats(paidFlats);
		due.setPaymentName("Maintenance");
		return due;
	}

	private Name buildName(String firstName, String lastName) {
		Name name = new Name();
		name.setFirstName(firstName);
		name.setLastName(lastName);
		return name;
	}
}
