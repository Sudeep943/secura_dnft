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
				.thenReturn(List.of(buildPayment("PAY-1", "Maintenance", SecuraConstants.PAYMENT_STATUS_ACTIVE, "MANDATORY")));
		when(paymentRepository.findByPaymentIdAndAprmtId("PAY-2", "APR-1"))
				.thenReturn(List.of(buildPayment("PAY-2", "Club Fund", SecuraConstants.PAYMENT_STATUS_ACTIVE, "OPTIONAL")));
		when(paymentRepository.findByPaymentIdAndAprmtId("PAY-3", "APR-1"))
				.thenReturn(List.of(buildPayment("PAY-3", "Parking", "INACTIVE", "MANDATORY")));

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
		assertEquals(List.of("John Doe", "Jane Doe"), defaulter.getOwnerNames());
		assertEquals("9999999999, 8888888888", defaulter.getPhoneNumber());
		assertEquals(1, defaulter.getDefaultPaymentList().size());

		DefaultPayment defaultPayment = defaulter.getDefaultPaymentList().get(0);
		assertEquals("PAY-1", defaultPayment.getPaymentId());
		assertEquals("Maintenance", defaultPayment.getPaymentName());
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

	private PaymentEntity buildPayment(String paymentId, String paymentName, String status, String paymentType) {
		PaymentEntity payment = new PaymentEntity();
		payment.setPaymentId(paymentId);
		payment.setPaymentName(paymentName);
		payment.setStatus(status);
		payment.setPaymentType(paymentType);
		return payment;
	}

	private DueAmountDetailsEntity buildDue(String paymentId, String dueId, LocalDate dueDate, String totalAmount,
			String fineAmount, String applicableFlats, String paidFlats) {
		DueAmountDetailsEntity due = new DueAmountDetailsEntity();
		due.setPaymentId(paymentId);
		due.setDueId(dueId);
		due.setCollectionCycle("MONTHLY");
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
