package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.secura.dnft.dao.DueAmountDetailsRepository;
import com.secura.dnft.dao.FlatRepository;
import com.secura.dnft.bean.WorklistAssignmentFlow;
import com.secura.dnft.dao.PaymentRepository;
import com.secura.dnft.dao.TransactionRepository;
import com.secura.dnft.dao.WorklistRepository;
import com.secura.dnft.entity.DueAmountDetailsEntity;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.entity.Transaction;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.Worklist;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.request.response.ActionTransactionReviewWorkListRequest;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.GenericResponse;
import com.secura.dnft.request.response.GetWorkListsRequest;
import com.secura.dnft.request.response.GetWorkListsResponse;

@ExtendWith(MockitoExtension.class)
class WorklistServiceTest {

	@Mock
	private WorklistRepository worklistRepository;

	@Mock
	private TransactionRepository transactionRepository;

	@Mock
	private GenericService genericService;

	@Mock
	private DueAmountDetailsRepository dueAmountDetailsRepository;

	@Mock
	private FlatRepository flatRepository;

	@Mock
	private PaymentRepository paymentRepository;

	@InjectMocks
	private WorklistService worklistService;

	@Test
	void createTransactionReviewWorklist_shouldPersistPendingReviewWorklist() {
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		header.setUserId("USR-1");
		header.setFlatNo("A-101");
		when(genericService.createWorklistId(any(), eq("USR-1"))).thenReturn("WRK202605201234561001");
		when(genericService.toJson(any())).thenCallRealMethod();
		when(worklistRepository.save(any(Worklist.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Worklist worklist = worklistService.createTransactionReviewWorklist("TRN-1", header);

		assertEquals("WRK202605201234561001", worklist.getWorklistId());
		assertEquals("APR-1", worklist.getApartmentId());
		assertEquals(SecuraConstants.WORKLIST_TYPE_TRANSACTION_REVIEW, worklist.getWorklistType());
		assertEquals(SecuraConstants.WORKLIST_STATUS_PENDING, worklist.getStatus());
		assertEquals("TRN-1", worklist.getReferenceId());
		assertEquals("USR-1", worklist.getCurrentAssignee());
		assertEquals("A-101", worklist.getFlatNo());
		List<WorklistAssignmentFlow> flow = new GenericService().fromJson(worklist.getWorklistAssignmentFlow(),
				new TypeReference<List<WorklistAssignmentFlow>>() {
				});
		assertEquals(1, flow.size());
		assertEquals("USR-1", flow.get(0).getProfileId());
		assertEquals(SecuraConstants.WORKLIST_ASSIGNMENT_STATUS_ACTIVE, flow.get(0).getStatus());
	}

	@Test
	void getWorkLists_shouldReturnWorklistByIdWhenProvided() {
		GetWorkListsRequest request = new GetWorkListsRequest();
		request.setWorklistId("WL-1");
		Worklist worklist = new Worklist();
		worklist.setWorklistId("WL-1");
		when(worklistRepository.findById("WL-1")).thenReturn(Optional.of(worklist));

		GetWorkListsResponse response = worklistService.getWorkLists(request);

		assertEquals(1, response.getWorklists().size());
		assertEquals("WL-1", response.getWorklists().get(0).getWorklistId());
	}

	@Test
	void getWorkLists_shouldReturnAssignedWorklistsWhenIdMissing() {
		GetWorkListsRequest request = new GetWorkListsRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		header.setUserId("USR-1");
		request.setGenericHeader(header);
		Worklist worklist = new Worklist();
		worklist.setWorklistId("WL-2");
		when(worklistRepository.findByApartmentIdAndCurrentAssignee("APR-1", "USR-1")).thenReturn(List.of(worklist));

		GetWorkListsResponse response = worklistService.getWorkLists(request);

		assertEquals(1, response.getWorklists().size());
		assertEquals("WL-2", response.getWorklists().get(0).getWorklistId());
	}

	@Test
	void actionTransactionReviewWorkList_shouldReturnErrorForInvalidType() {
		ActionTransactionReviewWorkListRequest request = new ActionTransactionReviewWorkListRequest();
		request.setWorklistId("WL-3");
		Worklist worklist = new Worklist();
		worklist.setWorklistId("WL-3");
		worklist.setWorklistType(SecuraConstants.WORKLIST_TYPE_BOOKING);
		when(worklistRepository.findById("WL-3")).thenReturn(Optional.of(worklist));

		GenericResponse response = worklistService.actionTransactionReviewWorkList(request);

		assertEquals(SecuraConstants.ERROR_INVALID_WORKLIST_TYPE, response.getMessage());
		assertEquals(SecuraConstants.ERROR_INVALID_WORKLIST_TYPE_CODE, response.getMessageCode());
		verify(transactionRepository, never()).save(any(Transaction.class));
	}

	@Test
	void actionTransactionReviewWorkList_shouldApproveTransactionAndCompleteWorklist() {
		ActionTransactionReviewWorkListRequest request = new ActionTransactionReviewWorkListRequest();
		request.setWorklistId("WL-4");
		request.setAction(SecuraConstants.ACTION_APPROVE);
		GenericHeader header = new GenericHeader();
		header.setUserId("USR-2");
		request.setGenericHeader(header);

		Worklist worklist = new Worklist();
		worklist.setWorklistId("WL-4");
		worklist.setWorklistType(SecuraConstants.WORKLIST_TYPE_TRANSACTION_REVIEW);
		worklist.setReferenceId("TRN-4");
		Transaction transaction = new Transaction();
		transaction.setTrnscId("TRN-4");

		when(worklistRepository.findById("WL-4")).thenReturn(Optional.of(worklist));
		when(transactionRepository.findById("TRN-4")).thenReturn(Optional.of(transaction));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(worklistRepository.save(any(Worklist.class))).thenAnswer(invocation -> invocation.getArgument(0));

		GenericResponse response = worklistService.actionTransactionReviewWorkList(request);

		ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
		verify(transactionRepository).save(transactionCaptor.capture());
		assertEquals(SecuraConstants.TRANSACTION_STATUS_SUCCESS, transactionCaptor.getValue().getTrnsStatus());
		assertEquals("USR-2", transactionCaptor.getValue().getLstUpdtUsrId());

		ArgumentCaptor<Worklist> worklistCaptor = ArgumentCaptor.forClass(Worklist.class);
		verify(worklistRepository).save(worklistCaptor.capture());
		assertEquals(SecuraConstants.WORKLIST_STATUS_COMPLETE, worklistCaptor.getValue().getStatus());
		assertEquals("USR-2", worklistCaptor.getValue().getLstUpdtUsrId());
		verify(flatRepository, never()).save(any(Flat.class));
		assertEquals("Transaction review updated successfully", response.getMessage());
		assertEquals("TRANSACTION_REVIEW_UPDATED", response.getMessageCode());
	}

	@Test
	void actionTransactionReviewWorkList_shouldRemovePendingDueOnApproveWhenAmountMatches() {
		ActionTransactionReviewWorkListRequest request = new ActionTransactionReviewWorkListRequest();
		request.setWorklistId("WL-5");
		request.setAction(SecuraConstants.ACTION_APPROVE);
		GenericHeader header = new GenericHeader();
		header.setUserId("USR-2");
		request.setGenericHeader(header);

		Worklist worklist = new Worklist();
		worklist.setWorklistId("WL-5");
		worklist.setWorklistType(SecuraConstants.WORKLIST_TYPE_TRANSACTION_REVIEW);
		worklist.setReferenceId("TRN-5");
		worklist.setFlatNo("A-101");
		Transaction transaction = new Transaction();
		transaction.setTrnscId("TRN-5");
		transaction.setTrnsAmt("1500.00");
		transaction.setDueDetails("DUE1001_MONTHLY_ALL_2026-06-01");
		DueAmountDetailsEntity due = new DueAmountDetailsEntity();
		due.setDueId("DUE1001");
		due.setCollectionCycle(SecuraConstants.PAYMENT_CYCLE_MONTHLY);
		due.setFlatArea("ALL");
		due.setDueDate(LocalDate.parse("2026-06-01"));
		due.setTotalAmount("1500");
		due.setPaymentId("PAY-1001");
		due.setApplicableFlats("[\"A-101\",\"A-102\"]");
		Flat flat = new Flat();
		flat.setFlatNo("A-101");
		flat.setFlatPndngPaymntLst("[\"DUE1001_MONTHLY_ALL_2026-06-01\",\"DUE1002_MONTHLY_ALL_2026-07-01\"]");

		when(worklistRepository.findById("WL-5")).thenReturn(Optional.of(worklist));
		when(transactionRepository.findById("TRN-5")).thenReturn(Optional.of(transaction));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(worklistRepository.save(any(Worklist.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(dueAmountDetailsRepository.findById(any())).thenReturn(Optional.of(due));
		when(flatRepository.findById("A-101")).thenReturn(Optional.of(flat));
		when(genericService.fromJson(eq(flat.getFlatPndngPaymntLst()), any(TypeReference.class)))
				.thenReturn(new ArrayList<>(List.of("DUE1001_MONTHLY_ALL_2026-06-01", "DUE1002_MONTHLY_ALL_2026-07-01")));
		when(genericService.toJson(any())).thenCallRealMethod();
		when(flatRepository.save(any(Flat.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(dueAmountDetailsRepository.findByPaymentId("PAY-1001")).thenReturn(List.of(due));

		worklistService.actionTransactionReviewWorkList(request);

		ArgumentCaptor<Flat> flatCaptor = ArgumentCaptor.forClass(Flat.class);
		verify(flatRepository).save(flatCaptor.capture());
		assertEquals("[\"DUE1002_MONTHLY_ALL_2026-07-01\"]", flatCaptor.getValue().getFlatPndngPaymntLst());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<DueAmountDetailsEntity>> dueCaptor = ArgumentCaptor.forClass((Class) List.class);
		verify(dueAmountDetailsRepository).saveAll(dueCaptor.capture());
		assertEquals(1, dueCaptor.getValue().size());
		assertEquals("[\"A-101\"]", dueCaptor.getValue().get(0).getPaidFlats());
		verify(paymentRepository, never()).saveAll(any());
	}

	@Test
	void actionTransactionReviewWorkList_shouldAddFlatToPaymentPaidFlatsWhenNoDuesRemainForPayment() {
		ActionTransactionReviewWorkListRequest request = new ActionTransactionReviewWorkListRequest();
		request.setWorklistId("WL-6");
		request.setAction(SecuraConstants.ACTION_APPROVE);
		GenericHeader header = new GenericHeader();
		header.setUserId("USR-2");
		request.setGenericHeader(header);

		Worklist worklist = new Worklist();
		worklist.setWorklistId("WL-6");
		worklist.setApartmentId("APR-1");
		worklist.setWorklistType(SecuraConstants.WORKLIST_TYPE_TRANSACTION_REVIEW);
		worklist.setReferenceId("TRN-6");
		worklist.setFlatNo("A-101");
		Transaction transaction = new Transaction();
		transaction.setTrnscId("TRN-6");
		transaction.setTrnsAmt("1500.00");
		transaction.setDueDetails("DUE1001_MONTHLY_ALL_2026-06-01");
		DueAmountDetailsEntity due = new DueAmountDetailsEntity();
		due.setDueId("DUE1001");
		due.setCollectionCycle(SecuraConstants.PAYMENT_CYCLE_MONTHLY);
		due.setFlatArea("ALL");
		due.setDueDate(LocalDate.parse("2026-06-01"));
		due.setTotalAmount("1500");
		due.setPaymentId("PAY-1001");
		due.setApplicableFlats("[\"A-101\",\"A-102\"]");
		Flat flat = new Flat();
		flat.setFlatNo("A-101");
		flat.setFlatPndngPaymntLst("[\"DUE1001_MONTHLY_ALL_2026-06-01\"]");
		PaymentEntity paymentEntity = new PaymentEntity();
		paymentEntity.setPaymentId("PAY-1001");

		when(worklistRepository.findById("WL-6")).thenReturn(Optional.of(worklist));
		when(transactionRepository.findById("TRN-6")).thenReturn(Optional.of(transaction));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(worklistRepository.save(any(Worklist.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(dueAmountDetailsRepository.findById(any())).thenReturn(Optional.of(due));
		when(flatRepository.findById("A-101")).thenReturn(Optional.of(flat));
		when(genericService.fromJson(eq(flat.getFlatPndngPaymntLst()), any(TypeReference.class)))
				.thenReturn(new ArrayList<>(List.of("DUE1001_MONTHLY_ALL_2026-06-01")));
		when(genericService.fromJson(eq(due.getApplicableFlats()), any(TypeReference.class)))
				.thenReturn(new ArrayList<>(List.of("A-101", "A-102")));
		when(genericService.toJson(any())).thenCallRealMethod();
		when(flatRepository.save(any(Flat.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(dueAmountDetailsRepository.findByPaymentId("PAY-1001")).thenReturn(List.of(due));
		when(paymentRepository.findByPaymentIdAndAprmtId("PAY-1001", "APR-1")).thenReturn(List.of(paymentEntity));
		when(paymentRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

		worklistService.actionTransactionReviewWorkList(request);

		ArgumentCaptor<List> paymentCaptor = ArgumentCaptor.forClass(List.class);
		verify(paymentRepository).saveAll(paymentCaptor.capture());
		PaymentEntity savedPayment = (PaymentEntity) paymentCaptor.getValue().get(0);
		assertEquals("[\"A-101\"]", savedPayment.getPaidFlats());
	}

	@Test
	void actionTransactionReviewWorkList_shouldUpdateRelatedCoveredDuesForApprovedTransaction() {
		ActionTransactionReviewWorkListRequest request = new ActionTransactionReviewWorkListRequest();
		request.setWorklistId("WL-7");
		request.setAction(SecuraConstants.ACTION_APPROVE);
		GenericHeader header = new GenericHeader();
		header.setUserId("USR-2");
		request.setGenericHeader(header);

		Worklist worklist = new Worklist();
		worklist.setWorklistId("WL-7");
		worklist.setApartmentId("APR-1");
		worklist.setWorklistType(SecuraConstants.WORKLIST_TYPE_TRANSACTION_REVIEW);
		worklist.setReferenceId("TRN-7");
		worklist.setFlatNo("A-101");
		Transaction transaction = new Transaction();
		transaction.setTrnscId("TRN-7");
		transaction.setTrnsAmt("9000.00");
		transaction.setDueDetails("DUE1001_QUATERLY_ALL_2026-01-01");

		DueAmountDetailsEntity paidDue = new DueAmountDetailsEntity();
		paidDue.setDueId("DUE1001");
		paidDue.setCollectionCycle(SecuraConstants.PAYMENT_CYCLE_QUATERLY);
		paidDue.setFlatArea("ALL");
		paidDue.setDueDate(LocalDate.parse("2026-01-01"));
		paidDue.setDueStartDate(LocalDate.parse("2026-01-01"));
		paidDue.setDueEndDate(LocalDate.parse("2026-03-31"));
		paidDue.setTotalAmount("9000");
		paidDue.setPaymentId("PAY-1001");
		paidDue.setApplicableFlats("[\"A-101\",\"A-102\"]");

		DueAmountDetailsEntity relatedDue = new DueAmountDetailsEntity();
		relatedDue.setDueId("DUE2001");
		relatedDue.setCollectionCycle(SecuraConstants.PAYMENT_CYCLE_MONTHLY);
		relatedDue.setFlatArea("ALL");
		relatedDue.setDueDate(LocalDate.parse("2026-02-01"));
		relatedDue.setDueStartDate(LocalDate.parse("2026-02-01"));
		relatedDue.setDueEndDate(LocalDate.parse("2026-02-28"));
		relatedDue.setPaymentId("PAY-1001");
		relatedDue.setApplicableFlats("[\"A-101\",\"A-102\"]");

		Flat flat = new Flat();
		flat.setFlatNo("A-101");
		flat.setFlatPndngPaymntLst("[\"DUE1001_QUATERLY_ALL_2026-01-01\",\"DUE2001_MONTHLY_ALL_2026-02-01\",\"OTHER_DUE_MONTHLY_ALL_2026-04-01\"]");

		when(worklistRepository.findById("WL-7")).thenReturn(Optional.of(worklist));
		when(transactionRepository.findById("TRN-7")).thenReturn(Optional.of(transaction));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(worklistRepository.save(any(Worklist.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(dueAmountDetailsRepository.findById(any())).thenReturn(Optional.of(paidDue));
		when(dueAmountDetailsRepository.findByPaymentId("PAY-1001")).thenReturn(List.of(paidDue, relatedDue));
		when(flatRepository.findById("A-101")).thenReturn(Optional.of(flat));
		when(genericService.fromJson(eq(flat.getFlatPndngPaymntLst()), any(TypeReference.class))).thenReturn(
				new ArrayList<>(List.of("DUE1001_QUATERLY_ALL_2026-01-01", "DUE2001_MONTHLY_ALL_2026-02-01",
						"OTHER_DUE_MONTHLY_ALL_2026-04-01")));
		when(genericService.fromJson(eq(paidDue.getApplicableFlats()), any(TypeReference.class)))
				.thenReturn(new ArrayList<>(List.of("A-101", "A-102")));
		when(genericService.fromJson(eq(relatedDue.getApplicableFlats()), any(TypeReference.class)))
				.thenReturn(new ArrayList<>(List.of("A-101", "A-102")));
		when(genericService.toJson(any())).thenCallRealMethod();
		when(flatRepository.save(any(Flat.class))).thenAnswer(invocation -> invocation.getArgument(0));

		worklistService.actionTransactionReviewWorkList(request);

		ArgumentCaptor<Flat> flatCaptor = ArgumentCaptor.forClass(Flat.class);
		verify(flatRepository).save(flatCaptor.capture());
		assertEquals("[\"OTHER_DUE_MONTHLY_ALL_2026-04-01\"]", flatCaptor.getValue().getFlatPndngPaymntLst());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<DueAmountDetailsEntity>> dueCaptor = ArgumentCaptor.forClass((Class) List.class);
		verify(dueAmountDetailsRepository).saveAll(dueCaptor.capture());
		assertEquals(2, dueCaptor.getValue().size());
		DueAmountDetailsEntity savedPaidDue = dueCaptor.getValue().stream().filter(d -> "DUE1001".equals(d.getDueId())).findFirst()
				.orElseThrow();
		DueAmountDetailsEntity savedRelatedDue = dueCaptor.getValue().stream().filter(d -> "DUE2001".equals(d.getDueId())).findFirst()
				.orElseThrow();
		assertEquals("[\"A-101\"]", savedPaidDue.getPaidFlats());
		assertEquals("[\"A-101\"]", savedRelatedDue.getPaidFlats());
		assertEquals("[\"A-102\"]", savedRelatedDue.getApplicableFlats());
	}
}
