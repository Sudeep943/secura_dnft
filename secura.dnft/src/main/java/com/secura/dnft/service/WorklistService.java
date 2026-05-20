package com.secura.dnft.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

import jakarta.persistence.EntityNotFoundException;

@Service
public class WorklistService {

	private static final String WORKLISTS_FOUND_MESSAGE = "Worklists fetched successfully";
	private static final String WORKLISTS_FOUND_MESSAGE_CODE = "WORKLISTS_FETCHED";
	private static final String NO_WORKLISTS_FOUND_MESSAGE = "No worklists found";
	private static final String NO_WORKLISTS_FOUND_MESSAGE_CODE = "NO_WORKLISTS_FOUND";
	private static final String ACTION_SUCCESS_MESSAGE = "Transaction review updated successfully";
	private static final String ACTION_SUCCESS_MESSAGE_CODE = "TRANSACTION_REVIEW_UPDATED";

	@Autowired
	private WorklistRepository worklistRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private GenericService genericService;

	public Worklist createTransactionReviewWorklist(String transactionId, GenericHeader genericHeader) {
		LocalDateTime now = LocalDateTime.now();
		String userId = genericHeader != null ? genericHeader.getUserId() : null;
		Worklist worklist = new Worklist();
		worklist.setWorklistId(genericService.createWorklistId(SecuraConstants.WORKLIST_TYPE_TRANSACTION_REVIEW, userId));
		worklist.setApartmentId(genericHeader != null ? genericHeader.getApartmentId() : null);
		worklist.setWorklistType(SecuraConstants.WORKLIST_TYPE_TRANSACTION_REVIEW);
		worklist.setStatus(SecuraConstants.WORKLIST_STATUS_PENDING);
		worklist.setReferenceId(transactionId);
		worklist.setCurrentAssignee(userId);
		worklist.setCreatUsrId(userId);
		worklist.setCreatTs(now);
		worklist.setLstUpdtTs(now);
		worklist.setLstUpdtUsrId(userId);
		worklist.setWorklistAssignmentFlow(genericService.toJson(List.of(buildAssignmentFlow(userId, now))));
		return worklistRepository.save(worklist);
	}

	public GetWorkListsResponse getWorkLists(GetWorkListsRequest request) {
		GetWorkListsResponse response = new GetWorkListsResponse();
		List<Worklist> worklists = new ArrayList<>();
		String worklistId = request != null ? request.getWorklistId() : null;
		if (worklistId != null && !worklistId.isBlank()) {
			worklistRepository.findById(worklistId).ifPresent(worklists::add);
		} else {
			String apartmentId = request != null && request.getGenericHeader() != null
					? request.getGenericHeader().getApartmentId()
					: null;
			String userId = request != null && request.getGenericHeader() != null
					? request.getGenericHeader().getUserId()
					: null;
			worklists = worklistRepository.findByApartmentIdAndCurrentAssignee(apartmentId, userId);
		}
		response.setWorklists(worklists);
		if (worklists.isEmpty()) {
			response.setMessage(NO_WORKLISTS_FOUND_MESSAGE);
			response.setMessageCode(NO_WORKLISTS_FOUND_MESSAGE_CODE);
		} else {
			response.setMessage(WORKLISTS_FOUND_MESSAGE);
			response.setMessageCode(WORKLISTS_FOUND_MESSAGE_CODE);
		}
		return response;
	}

	public GenericResponse actionTransactionReviewWorkList(ActionTransactionReviewWorkListRequest request) {
		GenericResponse response = new GenericResponse();
		Worklist worklist = worklistRepository.findById(request.getWorklistId())
				.orElseThrow(() -> new EntityNotFoundException("Worklist not found"));
		if (!SecuraConstants.WORKLIST_TYPE_TRANSACTION_REVIEW.equalsIgnoreCase(worklist.getWorklistType())) {
			response.setMessage(SecuraConstants.ERROR_INVALID_WORKLIST_TYPE);
			response.setMessageCode(SecuraConstants.ERROR_INVALID_WORKLIST_TYPE_CODE);
			return response;
		}
		Transaction transaction = transactionRepository.findById(worklist.getReferenceId())
				.orElseThrow(() -> new EntityNotFoundException("Transaction not found"));
		String action = request.getAction();
		if (SecuraConstants.ACTION_APPROVE.equalsIgnoreCase(action)) {
			transaction.setTrnsStatus(SecuraConstants.TRANSACTION_STATUS_SUCCESS);
		} else if (SecuraConstants.ACTION_REJECT.equalsIgnoreCase(action)) {
			transaction.setTrnsStatus(SecuraConstants.TRANSACTION_STATUS_FAILED);
		} else {
			throw new IllegalArgumentException("Invalid action");
		}
		transaction.setLstUpdtTs(LocalDateTime.now());
		transaction.setLstUpdtUsrId(request.getGenericHeader() != null ? request.getGenericHeader().getUserId() : null);
		transactionRepository.save(transaction);

		worklist.setStatus(SecuraConstants.WORKLIST_STATUS_COMPLETE);
		worklist.setLstUpdtTs(LocalDateTime.now());
		worklist.setLstUpdtUsrId(request.getGenericHeader() != null ? request.getGenericHeader().getUserId() : null);
		worklistRepository.save(worklist);

		response.setMessage(ACTION_SUCCESS_MESSAGE);
		response.setMessageCode(ACTION_SUCCESS_MESSAGE_CODE);
		return response;
	}

	private WorklistAssignmentFlow buildAssignmentFlow(String userId, LocalDateTime now) {
		WorklistAssignmentFlow flow = new WorklistAssignmentFlow();
		flow.setProfileId(userId);
		flow.setAssignmentDateTime(now);
		flow.setStatus(SecuraConstants.WORKLIST_ASSIGNMENT_STATUS_ACTIVE);
		return flow;
	}
}
