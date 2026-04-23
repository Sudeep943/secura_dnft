package com.secura.dnft.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.dao.TransactionRepository;
import com.secura.dnft.entity.Transaction;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.BankInstrumentTenderDetails;
import com.secura.dnft.request.response.DueAmountDetails;
import com.secura.dnft.request.response.GetTransactionRequest;
import com.secura.dnft.request.response.GetTransactionResponse;
import com.secura.dnft.request.response.PaymentTenderData;
import com.secura.dnft.request.response.TransactionData;

@Service
public class TransactionAndReportsService {

	@Autowired
	TransactionRepository transactionRepository;

	@Autowired
	GenericService genericService;

	public GetTransactionResponse getTransaction(GetTransactionRequest request) {
		GetTransactionResponse response = new GetTransactionResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);

		List<Transaction> transactions = new ArrayList<>();
		if (request == null || request.getTransactionId() == null || request.getTransactionId().isBlank()) {
			transactions = transactionRepository.findAll();
		} else {
			Optional<Transaction> transactionOpt = transactionRepository.findById(request.getTransactionId());
			transactionOpt.ifPresent(transactions::add);
		}

		List<TransactionData> transactionDataList = new ArrayList<>();
		for (Transaction transaction : transactions) {
			transactionDataList.add(mapToTransactionData(transaction));
		}

		response.setTransactionList(transactionDataList);
		response.setMessage(SuccessMessage.SUCC_MESSAGE_41);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_41);
		return response;
	}

	private TransactionData mapToTransactionData(Transaction transaction) {
		TransactionData data = new TransactionData();
		data.setAprmntId(transaction.getAprmntId());
		data.setTrnscId(transaction.getTrnscId());
		data.setTrnsDate(transaction.getTrnsDate());
		data.setTrnsBy(transaction.getTrnsBy());
		data.setTrnsTender(parseTrnsTender(transaction.getTrnsTender()));
		data.setTrnsType(transaction.getTrnsType());
		data.setTrnsShrtDesc(transaction.getTrnsShrtDesc());
		data.setTrnsFiles(parseTrnsFiles(transaction.getTrnsFiles()));
		data.setTrnsBnkAccnt(transaction.getTrnsBnkAccnt());
		data.setTrnsAmt(transaction.getTrnsAmt());
		data.setTrnsCurrency(transaction.getTrnsCurrency());
		data.setPymntId(transaction.getPymntId());
		data.setTrnsStatus(transaction.getTrnsStatus());
		data.setNoOfPerson(transaction.getNoOfPerson());
		data.setThirdPartyTrnsRef(transaction.getThirdPartyTrnsRef());
		data.setThirdPartyName(transaction.getThirdPartyName());
		data.setDueDetails(parseDueDetails(transaction.getDueDetails()));
		data.setCause(transaction.getCause());
		data.setBankInstrumentTenderDetails(parseBankInstrumentTenderDetails(transaction.getBankInstrumentTenderDetails()));
		data.setWorkListId(transaction.getWorkListId());
		data.setReceiptNumber(transaction.getReceiptNumber());
		data.setCreatTs(transaction.getCreatTs());
		data.setCreatUsrId(transaction.getCreatUsrId());
		data.setLstUpdtTs(transaction.getLstUpdtTs());
		data.setLstUpdtUsrId(transaction.getLstUpdtUsrId());
		return data;
	}

	private List<PaymentTenderData> parseTrnsTender(String trnsTender) {
		if (trnsTender == null || trnsTender.isBlank()) {
			return new ArrayList<>();
		}
		try {
			return genericService.fromJson(trnsTender, new TypeReference<List<PaymentTenderData>>() {});
		} catch (RuntimeException e) {
			return new ArrayList<>();
		}
	}

	private List<String> parseTrnsFiles(String trnsFiles) {
		if (trnsFiles == null || trnsFiles.isBlank()) {
			return new ArrayList<>();
		}
		try {
			return genericService.fromJson(trnsFiles, new TypeReference<List<String>>() {});
		} catch (RuntimeException e) {
			return new ArrayList<>();
		}
	}

	private DueAmountDetails parseDueDetails(String dueDetails) {
		if (dueDetails == null || dueDetails.isBlank()) {
			return null;
		}
		try {
			return genericService.fromJson(dueDetails, DueAmountDetails.class);
		} catch (RuntimeException e) {
			return null;
		}
	}

	private List<BankInstrumentTenderDetails> parseBankInstrumentTenderDetails(String bankInstrumentTenderDetails) {
		if (bankInstrumentTenderDetails == null || bankInstrumentTenderDetails.isBlank()) {
			return new ArrayList<>();
		}
		try {
			return genericService.fromJson(bankInstrumentTenderDetails, new TypeReference<List<BankInstrumentTenderDetails>>() {});
		} catch (RuntimeException e) {
			return new ArrayList<>();
		}
	}
}
