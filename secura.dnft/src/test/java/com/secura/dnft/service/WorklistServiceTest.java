package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.bean.WorklistAssignmentFlow;
import com.secura.dnft.dao.TransactionRepository;
import com.secura.dnft.dao.WorklistRepository;
import com.secura.dnft.entity.Transaction;
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

	@InjectMocks
	private WorklistService worklistService;

	@Test
	void createTransactionReviewWorklist_shouldPersistPendingReviewWorklist() {
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		header.setUserId("USR-1");
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
		assertEquals("Transaction review updated successfully", response.getMessage());
		assertEquals("TRANSACTION_REVIEW_UPDATED", response.getMessageCode());
	}
}
