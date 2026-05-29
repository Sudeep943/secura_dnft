package com.secura.dnft.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.bean.WorklistAssignmentFlow;
import com.secura.dnft.dao.DueAmountDetailsRepository;
import com.secura.dnft.dao.FlatRepository;
import com.secura.dnft.dao.PaymentRepository;
import com.secura.dnft.dao.TransDueDetailsRepository;
import com.secura.dnft.dao.TransactionRepository;
import com.secura.dnft.dao.WorklistRepository;
import com.secura.dnft.entity.DueAmountDetailsEntity;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.entity.Transaction;
import com.secura.dnft.entity.TransDueDetailsEntityId;
import com.secura.dnft.entity.Worklist;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
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
	private static final String ACTION_SUCCESS_MESSAGE = SuccessMessage.SUCC_MESSAGE_47;
	private static final String ACTION_SUCCESS_MESSAGE_CODE = SuccessMessageCode.SUCC_MESSAGE_47;
	private static final String APPROVAL_BLOCKED_MESSAGE = ErrorMessage.ERR_MESSAGE_53;
	private static final String APPROVAL_BLOCKED_MESSAGE_CODE = ErrorMessageCode.ERR_MESSAGE_53;

	@Autowired
	private WorklistRepository worklistRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private GenericService genericService;

	@Autowired
	private DueAmountDetailsRepository dueAmountDetailsRepository;

	@Autowired
	private FlatRepository flatRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private TransDueDetailsRepository transDueDetailsRepository;

	public Worklist createTransactionReviewWorklist(String transactionId, GenericHeader genericHeader) {
		LocalDateTime now = LocalDateTime.now();
		String userId = genericHeader != null ? genericHeader.getUserId() : null;
		Worklist worklist = new Worklist();
		worklist.setWorklistId(genericService.createWorklistId(SecuraConstants.WORKLIST_TYPE_TRANSACTION_REVIEW, userId));
		worklist.setApartmentId(genericHeader != null ? genericHeader.getApartmentId() : null);
		worklist.setWorklistType(SecuraConstants.WORKLIST_TYPE_TRANSACTION_REVIEW);
		worklist.setStatus(SecuraConstants.WORKLIST_STATUS_PENDING);
		worklist.setReferenceId(transactionId);
		worklist.setFlatNo(genericHeader != null ? genericHeader.getFlatNo() : null);
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
			response.setMessage(ErrorMessage.ERR_MESSAGE_52);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_52);
			return response;
		}
		Transaction transaction = transactionRepository
				.findByAprmntIdAndTrnscId(worklist.getApartmentId(), worklist.getReferenceId())
				.stream()
				.findFirst()
				.orElseThrow(() -> new EntityNotFoundException("Transaction not found"));
		String action = request.getAction();
		if (SecuraConstants.ACTION_APPROVE.equalsIgnoreCase(action)
				&& !isTransactionFlatApplicableForDue(worklist, transaction)) {
			response.setMessage(APPROVAL_BLOCKED_MESSAGE);
			response.setMessageCode(APPROVAL_BLOCKED_MESSAGE_CODE);
			return response;
		}
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
		if (SecuraConstants.ACTION_APPROVE.equalsIgnoreCase(action)) {
			processApprovedTransactionDue(worklist, transaction);
		} else if (SecuraConstants.ACTION_REJECT.equalsIgnoreCase(action)) {
			deleteTransactionDueDetails(worklist, transaction);
		}

		worklist.setStatus(SecuraConstants.WORKLIST_STATUS_COMPLETE);
		worklist.setLstUpdtTs(LocalDateTime.now());
		worklist.setLstUpdtUsrId(request.getGenericHeader() != null ? request.getGenericHeader().getUserId() : null);
		worklistRepository.save(worklist);

		response.setMessage(ACTION_SUCCESS_MESSAGE);
		response.setMessageCode(ACTION_SUCCESS_MESSAGE_CODE);
		return response;
	}

	private boolean isTransactionFlatApplicableForDue(Worklist worklist, Transaction transaction) {
		if (transaction == null || !hasText(transaction.getDueDetails())) {
			return true;
		}
		String apartmentId = resolveApartmentId(worklist, transaction);
		PendingDueKey dueEntityId = parsePendingDueKeyToEntityId(transaction.getDueDetails());
		if (!hasText(apartmentId) || dueEntityId == null) {
			return true;
		}
		DueAmountDetailsEntity dueEntity = dueAmountDetailsRepository
				.findByAprmntIdAndDueIdAndCollectionCycleAndFlatAreaAndDueDate(apartmentId, dueEntityId.dueId(),
						dueEntityId.collectionCycle(), dueEntityId.flatArea(), dueEntityId.dueDate())
				.orElse(null);
		if (dueEntity == null) {
			return true;
		}
		String flatNo = resolveFlatNo(worklist, transaction);
		if (!hasText(flatNo)) {
			return false;
		}
		List<String> applicableFlats = parsePendingDueList(dueEntity.getApplicableFlats());
		if (!containsFlatId(applicableFlats, flatNo)) {
			return false;
		}
		List<String> paidFlats = parsePendingDueList(dueEntity.getPaidFlats());
		if (paidFlats.isEmpty()) {
			return true;
		}
		return !containsFlatId(paidFlats, flatNo);
	}

	private void deleteTransactionDueDetails(Worklist worklist, Transaction transaction) {
		if (transaction == null || !hasText(transaction.getDueDetails()) || !hasText(transaction.getTrnscId())) {
			return;
		}
		String apartmentId = resolveApartmentId(worklist, transaction);
		if (!hasText(apartmentId)) {
			return;
		}
		transDueDetailsRepository.deleteById(
				new TransDueDetailsEntityId(transaction.getTrnscId(), apartmentId, transaction.getDueDetails()));
	}

	private void processApprovedTransactionDue(Worklist worklist, Transaction transaction) {
		if (transaction == null || !hasText(transaction.getDueDetails())) {
			return;
		}
		String apartmentId = resolveApartmentId(worklist, transaction);
		PendingDueKey dueEntityId = parsePendingDueKeyToEntityId(transaction.getDueDetails());
		if (!hasText(apartmentId) || dueEntityId == null) {
			return;
		}
		DueAmountDetailsEntity dueEntity = dueAmountDetailsRepository
				.findByAprmntIdAndDueIdAndCollectionCycleAndFlatAreaAndDueDate(apartmentId, dueEntityId.dueId(),
						dueEntityId.collectionCycle(), dueEntityId.flatArea(), dueEntityId.dueDate())
				.orElse(null);
		if (dueEntity == null) {
			return;
		}
		BigDecimal transactionAmount = parseAmount(transaction.getTrnsAmt());
		BigDecimal totalDueAmount = parseAmount(dueEntity.getTotalAmount());
		if (transactionAmount == null || totalDueAmount == null || transactionAmount.compareTo(totalDueAmount) != 0) {
			return;
		}
		String flatNo = resolveFlatNo(worklist, transaction);
		if (!hasText(flatNo)) {
			return;
		}
		String paymentId = resolvePaymentId(dueEntity, transaction);
		LocalDate paidStartDate = dueEntity.getDueStartDate() != null ? dueEntity.getDueStartDate() : dueEntity.getDueDate();
		LocalDate paidEndDate = dueEntity.getDueEndDate() != null ? dueEntity.getDueEndDate() : dueEntity.getDueDate();
		if (paidStartDate == null || paidEndDate == null) {
			return;
		}
		List<DueAmountDetailsEntity> duesForPayment = dueAmountDetailsRepository.findByPaymentId(paymentId);
		if (duesForPayment == null || duesForPayment.isEmpty()) {
			return;
		}
		List<DueAmountDetailsEntity> coveredDues = duesForPayment.stream().filter(Objects::nonNull)
				.filter(due -> isCoveredDue(due, paidStartDate, paidEndDate)).collect(Collectors.toList());
		if (coveredDues.isEmpty()) {
			return;
		}
		List<String> pendingDueList = removeCoveredDueKeysFromFlat(flatNo, coveredDues);
		updateCoveredDuesForFlat(flatNo, coveredDues, dueEntity);
		addFlatToPaymentPaidFlatsWhenNoDuesRemain(apartmentId, flatNo, paymentId, pendingDueList);
	}

	private String resolveFlatNo(Worklist worklist, Transaction transaction) {
		if (worklist != null && hasText(worklist.getFlatNo())) {
			return worklist.getFlatNo().trim();
		}
		return trimValue(transaction != null ? transaction.getFlatId() : null);
	}

	private String resolveApartmentId(Worklist worklist, Transaction transaction) {
		if (worklist != null && hasText(worklist.getApartmentId())) {
			return worklist.getApartmentId().trim();
		}
		return trimValue(transaction != null ? transaction.getAprmntId() : null);
	}

	private String resolvePaymentId(DueAmountDetailsEntity dueEntity, Transaction transaction) {
		String paymentId = trimValue(dueEntity != null ? dueEntity.getPaymentId() : null);
		if (hasText(paymentId)) {
			return paymentId;
		}
		return trimValue(transaction != null ? transaction.getPymntId() : null);
	}

	private PendingDueKey parsePendingDueKeyToEntityId(String pendingDueKey) {
		if (!hasText(pendingDueKey)) {
			return null;
		}
		String normalizedPendingKey = pendingDueKey.trim();
		int firstSeparatorIndex = normalizedPendingKey.indexOf('_');
		int lastSeparatorIndex = normalizedPendingKey.lastIndexOf('_');
		if (firstSeparatorIndex <= 0 || lastSeparatorIndex <= firstSeparatorIndex) {
			return null;
		}
		int secondLastSeparatorIndex = normalizedPendingKey.lastIndexOf('_', lastSeparatorIndex - 1);
		if (secondLastSeparatorIndex <= firstSeparatorIndex) {
			return null;
		}
		String dueId = normalizedPendingKey.substring(0, firstSeparatorIndex);
		String collectionCycle = normalizedPendingKey.substring(firstSeparatorIndex + 1, secondLastSeparatorIndex);
		String flatArea = normalizedPendingKey.substring(secondLastSeparatorIndex + 1, lastSeparatorIndex);
		String dueDateValue = normalizedPendingKey.substring(lastSeparatorIndex + 1);
		if (!hasText(dueId) || !hasText(collectionCycle) || !hasText(flatArea) || !hasText(dueDateValue)) {
			return null;
		}
		try {
			return new PendingDueKey(dueId.trim(), collectionCycle.trim(), flatArea.trim(),
					LocalDate.parse(dueDateValue.trim()));
		} catch (Exception ex) {
			return null;
		}
	}

	private List<String> parsePendingDueList(String pendingDueJson) {
		if (!hasText(pendingDueJson)) {
			return new ArrayList<>();
		}
		try {
			List<String> parsed = genericService.fromJson(pendingDueJson, new TypeReference<List<String>>() {
			});
			return parsed == null ? new ArrayList<>() : new ArrayList<>(parsed);
		} catch (Exception ex) {
			return new ArrayList<>();
		}
	}

	private boolean isCoveredDue(DueAmountDetailsEntity due, LocalDate paidStartDate, LocalDate paidEndDate) {
		if (due == null) {
			return false;
		}
		LocalDate dueStartDate = due.getDueStartDate() != null ? due.getDueStartDate() : due.getDueDate();
		LocalDate dueEndDate = due.getDueEndDate() != null ? due.getDueEndDate() : due.getDueDate();
		if (dueStartDate == null || dueEndDate == null) {
			return false;
		}
		boolean childDueCovered = dueStartDate.compareTo(paidStartDate) >= 0 && dueEndDate.compareTo(paidEndDate) <= 0;
		boolean parentDueCovered = dueStartDate.compareTo(paidStartDate) <= 0 && dueEndDate.compareTo(paidEndDate) >= 0;
		return childDueCovered || parentDueCovered;
	}

	private List<String> removeCoveredDueKeysFromFlat(String flatNo, List<DueAmountDetailsEntity> coveredDues) {
		if (!hasText(flatNo) || coveredDues == null || coveredDues.isEmpty()) {
			return new ArrayList<>();
		}
		Flat flat = flatRepository.findById(flatNo).orElse(null);
		if (flat == null || !hasText(flat.getFlatPndngPaymntLst())) {
			return new ArrayList<>();
		}
		List<String> pendingDueList = parsePendingDueList(flat.getFlatPndngPaymntLst());
		if (pendingDueList.isEmpty()) {
			return pendingDueList;
		}
		Set<String> coveredDueKeys = coveredDues.stream().map(this::buildFlatPendingDueKey).filter(this::hasText).map(String::trim)
				.collect(Collectors.toSet());
		Set<String> perHeadCoveredDueKeys = coveredDues.stream().filter(Objects::nonNull)
				.filter(due -> isPerHeadCapita(due.getPaymentCapita())).map(this::buildFlatPendingDueKey).filter(this::hasText)
				.map(String::trim).collect(Collectors.toSet());
		boolean removed = pendingDueList.removeIf(pendingDue -> hasText(pendingDue) && coveredDueKeys.contains(pendingDue.trim())
				&& !perHeadCoveredDueKeys.contains(pendingDue.trim()));
		if (removed) {
			flat.setFlatPndngPaymntLst(genericService.toJson(pendingDueList));
			flatRepository.save(flat);
		}
		return pendingDueList;
	}

	private void updateCoveredDuesForFlat(String flatNo, List<DueAmountDetailsEntity> coveredDues, DueAmountDetailsEntity paidDue) {
		if (!hasText(flatNo) || coveredDues == null || coveredDues.isEmpty()) {
			return;
		}
		String flatAreaForPerSqft = null;
		boolean flatAreaResolved = false;
		List<DueAmountDetailsEntity> updatedDues = new ArrayList<>();
		for (DueAmountDetailsEntity due : coveredDues) {
			if (due == null) {
				continue;
			}
			boolean modified = false;
			if (isSameDueIdentity(due, paidDue)) {
				List<String> paidFlats = parsePendingDueList(due.getPaidFlats());
				if (!containsFlatId(paidFlats, flatNo)) {
					paidFlats.add(flatNo.trim());
					due.setPaidFlats(genericService.toJson(paidFlats));
					modified = true;
				}
			} else if (isPerSqftCapita(due.getPaymentCapita())) {
				if (!flatAreaResolved) {
					Flat flat = flatRepository.findById(flatNo).orElse(null);
					flatAreaForPerSqft = flat != null ? flat.getFlatArea() : null;
					flatAreaResolved = true;
				}
				if (hasText(flatAreaForPerSqft) && flatAreaForPerSqft.trim().equalsIgnoreCase(trimValue(due.getFlatArea()))) {
					List<String> applicableFlats = parsePendingDueList(due.getApplicableFlats());
					boolean applicableRemoved = applicableFlats.removeIf(
							applicableFlat -> hasText(applicableFlat) && flatNo.trim().equalsIgnoreCase(applicableFlat.trim()));
					if (applicableRemoved) {
						due.setApplicableFlats(genericService.toJson(applicableFlats));
						modified = true;
					}
				}
			} else {
				List<String> paidFlats = parsePendingDueList(due.getPaidFlats());
				if (!containsFlatId(paidFlats, flatNo)) {
					paidFlats.add(flatNo.trim());
					due.setPaidFlats(genericService.toJson(paidFlats));
					modified = true;
				}
				List<String> applicableFlats = parsePendingDueList(due.getApplicableFlats());
				boolean applicableRemoved = applicableFlats.removeIf(
						applicableFlat -> hasText(applicableFlat) && flatNo.trim().equalsIgnoreCase(applicableFlat.trim()));
				if (applicableRemoved) {
					due.setApplicableFlats(genericService.toJson(applicableFlats));
					modified = true;
				}
			}
			if (modified) {
				updatedDues.add(due);
			}
		}
		if (!updatedDues.isEmpty()) {
			dueAmountDetailsRepository.saveAll(updatedDues);
		}
	}

	private boolean isSameDueIdentity(DueAmountDetailsEntity due, DueAmountDetailsEntity otherDue) {
		if (due == null || otherDue == null) {
			return false;
		}
		return Objects.equals(due.getDueId(), otherDue.getDueId())
				&& Objects.equals(due.getCollectionCycle(), otherDue.getCollectionCycle())
				&& Objects.equals(due.getFlatArea(), otherDue.getFlatArea())
				&& Objects.equals(due.getDueDate(), otherDue.getDueDate());
	}

	private boolean isPerHeadCapita(String paymentCapita) {
		if (!hasText(paymentCapita)) {
			return false;
		}
		String normalized = paymentCapita.toUpperCase(Locale.ROOT).replaceAll("[\\s_-]", "");
		return "PERHEAD".equals(normalized);
	}

	private boolean isPerSqftCapita(String paymentCapita) {
		if (!hasText(paymentCapita)) {
			return false;
		}
		String normalized = paymentCapita.toUpperCase(Locale.ROOT).replaceAll("[\\s_-]", "");
		return "PERSQFT".equals(normalized);
	}

	private void addFlatToPaymentPaidFlatsWhenNoDuesRemain(String apartmentId, String flatNo, String paymentId,
			List<String> pendingDueList) {
		if (!hasText(apartmentId) || !hasText(flatNo) || !hasText(paymentId)) {
			return;
		}
		List<DueAmountDetailsEntity> duesForPayment = dueAmountDetailsRepository.findByPaymentId(paymentId);
		if (duesForPayment == null || duesForPayment.isEmpty()) {
			return;
		}
		Set<String> paymentDueKeysForFlat = duesForPayment.stream().filter(Objects::nonNull)
				.filter(due -> containsFlatId(parsePendingDueList(due.getApplicableFlats()), flatNo))
				.map(this::buildFlatPendingDueKey).filter(this::hasText).map(String::trim).collect(Collectors.toSet());
		if (paymentDueKeysForFlat.isEmpty()) {
			return;
		}
		boolean hasRemainingDueForFlat = pendingDueList.stream().filter(this::hasText).map(String::trim)
				.anyMatch(paymentDueKeysForFlat::contains);
		if (hasRemainingDueForFlat) {
			return;
		}
		List<PaymentEntity> paymentEntities = paymentRepository.findByPaymentIdAndAprmtId(paymentId, apartmentId);
		if (paymentEntities == null || paymentEntities.isEmpty()) {
			return;
		}
		List<PaymentEntity> updatedPayments = new ArrayList<>();
		for (PaymentEntity paymentEntity : paymentEntities) {
			if (paymentEntity == null) {
				continue;
			}
			List<String> paidFlats = parsePendingDueList(paymentEntity.getPaidFlats());
			if (containsFlatId(paidFlats, flatNo)) {
				continue;
			}
			paidFlats.add(flatNo.trim());
			paymentEntity.setPaidFlats(genericService.toJson(paidFlats));
			updatedPayments.add(paymentEntity);
		}
		if (!updatedPayments.isEmpty()) {
			paymentRepository.saveAll(updatedPayments);
		}
	}

	private String buildFlatPendingDueKey(DueAmountDetailsEntity dueEntity) {
		if (dueEntity == null || dueEntity.getDueDate() == null || !hasText(dueEntity.getDueId())
				|| !hasText(dueEntity.getCollectionCycle()) || !hasText(dueEntity.getFlatArea())) {
			return null;
		}
		return dueEntity.getDueId() + "_" + dueEntity.getCollectionCycle() + "_" + dueEntity.getFlatArea() + "_"
				+ dueEntity.getDueDate();
	}

	private boolean containsFlatId(List<String> flatIds, String flatId) {
		if (flatIds == null || flatIds.isEmpty() || !hasText(flatId)) {
			return false;
		}
		String trimmedFlatId = flatId.trim();
		return flatIds.stream().anyMatch(item -> hasText(item) && trimmedFlatId.equalsIgnoreCase(item.trim()));
	}

	private BigDecimal parseAmount(String amount) {
		if (!hasText(amount)) {
			return null;
		}
		try {
			return new BigDecimal(amount.trim().replace(",", ""));
		} catch (Exception ex) {
			return null;
		}
	}

	private String trimValue(String value) {
		return value == null ? null : value.trim();
	}

	private record PendingDueKey(String dueId, String collectionCycle, String flatArea, LocalDate dueDate) {
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private WorklistAssignmentFlow buildAssignmentFlow(String userId, LocalDateTime now) {
		WorklistAssignmentFlow flow = new WorklistAssignmentFlow();
		flow.setProfileId(userId);
		flow.setAssignmentDateTime(now);
		flow.setStatus(SecuraConstants.WORKLIST_ASSIGNMENT_STATUS_ACTIVE);
		return flow;
	}
}
