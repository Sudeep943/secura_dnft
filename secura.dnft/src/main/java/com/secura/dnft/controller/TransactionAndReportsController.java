package com.secura.dnft.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.request.response.GetBalanceSheetRequest;
import com.secura.dnft.request.response.GetBalanceSheetResponse;
import com.secura.dnft.request.response.GetTransactionRequest;
import com.secura.dnft.request.response.GetTransactionResponse;
import com.secura.dnft.service.TransactionAndReportsService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/transactionAndReports")
public class TransactionAndReportsController {

	@Autowired
	TransactionAndReportsService transactionAndReportsService;

	@PostMapping("/getTransaction")
	@CrossOrigin(origins = "*")
	public GetTransactionResponse getTransaction(@RequestBody GetTransactionRequest request) {
		GetTransactionResponse response = new GetTransactionResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		try {
			return transactionAndReportsService.getTransaction(request);
		} catch (Exception e) {
			e.printStackTrace();
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}

	@PostMapping("/getBalanceSheet")
	@CrossOrigin(origins = "*")
	public GetBalanceSheetResponse getBalanceSheet(@RequestBody GetBalanceSheetRequest request) {
		GetBalanceSheetResponse response = new GetBalanceSheetResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		try {
			return transactionAndReportsService.getBalanceSheet(request);
		} catch (Exception e) {
			e.printStackTrace();
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}
}

