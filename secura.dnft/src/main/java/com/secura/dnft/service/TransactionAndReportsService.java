package com.secura.dnft.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.dao.PaymentRepository;
import com.secura.dnft.dao.TransactionRepository;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.entity.Transaction;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.BankInstrumentTenderDetails;
import com.secura.dnft.request.response.DueAmountDetails;
import com.secura.dnft.request.response.GetBalanceSheetRequest;
import com.secura.dnft.request.response.GetBalanceSheetResponse;
import com.secura.dnft.request.response.GetTransactionRequest;
import com.secura.dnft.request.response.GetTransactionResponse;
import com.secura.dnft.request.response.PaymentTenderData;
import com.secura.dnft.request.response.ReportPaymentData;
import com.secura.dnft.request.response.TransactionResponseItem;

@Service
public class TransactionAndReportsService {

	private static final String TRNS_TYPE_DEBIT = "DEBIT";
	private static final String TRNS_TYPE_CREDIT = "CREDIT";
	private static final String TRNS_STATUS_SUCCESS = "SUCCESS";
	private static final String OTHERS_KEY = "Others";

	@Autowired
	TransactionRepository transactionRepository;

	@Autowired
	PaymentRepository paymentRepository;

	@Autowired
	GenericService genericService;

	public GetTransactionResponse getTransaction(GetTransactionRequest request) {
		GetTransactionResponse response = new GetTransactionResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);

		List<Transaction> transactions = new ArrayList<>();
		String transactionId = request != null ? request.getTransactionId() : null;

		if (transactionId == null || transactionId.isBlank()) {
			transactions = transactionRepository.findAll();
		} else {
			Optional<Transaction> transaction = transactionRepository.findById(transactionId);
			transaction.ifPresent(transactions::add);
		}

		List<TransactionResponseItem> transactionList = new ArrayList<>();
		for (Transaction transaction : transactions) {
			transactionList.add(toResponseItem(transaction));
		}

		response.setTransactionList(transactionList);
		if (transactionList.isEmpty()) {
			response.setMessage(SuccessMessage.SUCC_MESSAGE_42);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_42);
		} else {
			response.setMessage(SuccessMessage.SUCC_MESSAGE_41);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_41);
		}
		return response;
	}

	public GetBalanceSheetResponse getBalanceSheet(GetBalanceSheetRequest request) {
		GetBalanceSheetResponse response = new GetBalanceSheetResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);

		LocalDate toDate = LocalDate.now();
		LocalDate fromDate = toDate.minusYears(1);
		response.setFromDate(fromDate);
		response.setToDate(toDate);

		String aprmntId = request != null && request.getGenericHeader() != null
				? request.getGenericHeader().getApartmentId()
				: null;

		LocalDateTime fromDateTime = fromDate.atStartOfDay();
		LocalDateTime toDateTime = toDate.atTime(LocalTime.MAX);

		response.setDebitPaymentData(buildDebitPaymentData(aprmntId, fromDateTime, toDateTime));
		response.setCreditPaymentData(buildCreditPaymentData(aprmntId, fromDateTime, toDateTime));

		response.setMessage(SuccessMessage.SUCC_MESSAGE_41);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_41);
		return response;
	}

	private List<ReportPaymentData> buildDebitPaymentData(String aprmntId, LocalDateTime from, LocalDateTime to) {
		List<Transaction> debitTrns = aprmntId != null
				? transactionRepository.findByAprmntIdAndTrnsTypeAndTrnsDateBetween(aprmntId, TRNS_TYPE_DEBIT, from, to)
				: new ArrayList<>();

		Map<String, BigDecimal> causeAmountMap = new LinkedHashMap<>();
		for (Transaction trns : debitTrns) {
			String cause = trns.getCause() != null && !trns.getCause().isBlank() ? trns.getCause() : OTHERS_KEY;
			BigDecimal amt = parseBigDecimal(trns.getTrnsAmt());
			causeAmountMap.merge(cause, amt, BigDecimal::add);
		}

		List<ReportPaymentData> result = new ArrayList<>();
		for (Map.Entry<String, BigDecimal> entry : causeAmountMap.entrySet()) {
			ReportPaymentData data = new ReportPaymentData();
			data.setPaymentName(entry.getKey());
			data.setTotalAmountIncludingTax(entry.getValue().toPlainString());
			result.add(data);
		}
		return result;
	}

	private List<ReportPaymentData> buildCreditPaymentData(String aprmntId, LocalDateTime from, LocalDateTime to) {
		List<Transaction> creditTrns = aprmntId != null
				? transactionRepository.findByAprmntIdAndTrnsTypeAndTrnsStatusAndTrnsDateBetween(
						aprmntId, TRNS_TYPE_CREDIT, TRNS_STATUS_SUCCESS, from, to)
				: new ArrayList<>();

		// Accumulate per-row sums keyed by paymentId (null -> "Others")
		Map<String, BigDecimal[]> accumulator = new LinkedHashMap<>();
		// index: 0=totalAddedCharges, 1=totalAmountExcludingTax, 2=totalAmountIncludingTax, 3=taxCollected

		for (Transaction trns : creditTrns) {
			String paymentId = trns.getPymntId() != null && !trns.getPymntId().isBlank()
					? trns.getPymntId()
					: OTHERS_KEY;

			BigDecimal trnsAmt = parseBigDecimal(trns.getTrnsAmt());
			DueAmountDetails due = genericService.fromJson(trns.getDueDetails(), DueAmountDetails.class);

			BigDecimal totalAddedCharges = due != null ? parseBigDecimal(due.getTotalAddedCharges()) : BigDecimal.ZERO;
			BigDecimal gstAmount = due != null ? parseBigDecimal(due.getGstAmount()) : BigDecimal.ZERO;

			BigDecimal totalAmountIncludingTax = trnsAmt;
			BigDecimal totalAmountExcludingTax = roundUp(trnsAmt.subtract(totalAddedCharges).subtract(gstAmount));
			BigDecimal taxCollected = totalAddedCharges.add(gstAmount);

			BigDecimal[] sums = accumulator.computeIfAbsent(paymentId, k -> new BigDecimal[]{
					BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
			sums[0] = sums[0].add(totalAddedCharges);
			sums[1] = sums[1].add(totalAmountExcludingTax);
			sums[2] = sums[2].add(totalAmountIncludingTax);
			sums[3] = sums[3].add(taxCollected);
		}

		List<ReportPaymentData> result = new ArrayList<>();
		for (Map.Entry<String, BigDecimal[]> entry : accumulator.entrySet()) {
			String paymentId = entry.getKey();
			BigDecimal[] sums = entry.getValue();

			ReportPaymentData data = new ReportPaymentData();
			if (OTHERS_KEY.equals(paymentId)) {
				data.setPaymentId(OTHERS_KEY);
			} else {
				data.setPaymentId(paymentId);
				Optional<PaymentEntity> paymentOpt = paymentRepository.findById(paymentId);
				if (paymentOpt.isPresent()) {
					data.setPaymentName(paymentOpt.get().getPaymentName());
					data.setPaymentAmount(paymentOpt.get().getPaymentAmount());
				}
			}
			data.setTotalAddedCharges(sums[0].toPlainString());
			data.setTotalAmountExcludingTax(sums[1].toPlainString());
			data.setTotalAmountIncludingTax(sums[2].toPlainString());
			data.setTaxCollected(sums[3].toPlainString());
			result.add(data);
		}
		return result;
	}

	private BigDecimal parseBigDecimal(String value) {
		if (value == null || value.isBlank()) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(value.trim());
		} catch (NumberFormatException e) {
			return BigDecimal.ZERO;
		}
	}

	private BigDecimal roundUp(BigDecimal value) {
		return value.setScale(0, RoundingMode.CEILING);
	}

	private TransactionResponseItem toResponseItem(Transaction transaction) {
		TransactionResponseItem item = new TransactionResponseItem();
		item.setAprmntId(transaction.getAprmntId());
		item.setTrnscId(transaction.getTrnscId());
		item.setTrnsDate(transaction.getTrnsDate());
		item.setTrnsBy(transaction.getTrnsBy());
		item.setTrnsTender(parseList(transaction.getTrnsTender(), new TypeReference<List<PaymentTenderData>>() {}));
		item.setTrnsType(transaction.getTrnsType());
		item.setTrnsShrtDesc(transaction.getTrnsShrtDesc());
		item.setTrnsFiles(parseList(transaction.getTrnsFiles(), new TypeReference<List<String>>() {}));
		item.setTrnsBnkAccnt(transaction.getTrnsBnkAccnt());
		item.setTrnsAmt(transaction.getTrnsAmt());
		item.setTrnsCurrency(transaction.getTrnsCurrency());
		item.setPymntId(transaction.getPymntId());
		item.setTrnsStatus(transaction.getTrnsStatus());
		item.setNoOfPerson(transaction.getNoOfPerson());
		item.setThirdPartyTrnsRef(transaction.getThirdPartyTrnsRef());
		item.setThirdPartyName(transaction.getThirdPartyName());
		item.setDueDetails(genericService.fromJson(transaction.getDueDetails(), DueAmountDetails.class));
		item.setCause(transaction.getCause());
		item.setBankInstrumentTenderDetails(
				parseList(transaction.getBankInstrumentTenderDetails(),
						new TypeReference<List<BankInstrumentTenderDetails>>() {}));
		item.setWorkListId(transaction.getWorkListId());
		item.setReceiptNumber(transaction.getReceiptNumber());
		item.setCreatTs(transaction.getCreatTs());
		item.setCreatUsrId(transaction.getCreatUsrId());
		item.setLstUpdtTs(transaction.getLstUpdtTs());
		item.setLstUpdtUsrId(transaction.getLstUpdtUsrId());
		return item;
	}

	private <T> List<T> parseList(String json, TypeReference<List<T>> typeReference) {
		if (json == null || json.isBlank()) {
			return new ArrayList<>();
		}
		try {
			return genericService.fromJson(json, typeReference);
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}
}

