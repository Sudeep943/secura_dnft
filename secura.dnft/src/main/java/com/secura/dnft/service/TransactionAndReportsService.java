package com.secura.dnft.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.dao.TransactionRepository;
import com.secura.dnft.entity.Transaction;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.BankInstrumentTenderDetails;
import com.secura.dnft.request.response.DueAmountDetails;
import com.secura.dnft.request.response.GetTransactionRequest;
import com.secura.dnft.request.response.GetTransactionResponse;
import com.secura.dnft.request.response.PaymentTenderData;
import com.secura.dnft.request.response.TransactionResponseItem;

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
