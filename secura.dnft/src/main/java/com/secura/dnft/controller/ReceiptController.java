package com.secura.dnft.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.request.response.CreateReceiptRequest;
import com.secura.dnft.request.response.CreateReceiptResponse;
import com.secura.dnft.service.ReceiptServices;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/receipt")
public class ReceiptController {

	@Autowired
	private ReceiptServices receiptServices;

	@PostMapping("/createReceipt")
	public CreateReceiptResponse createReceipt(@RequestBody CreateReceiptRequest request) {
		CreateReceiptResponse response = new CreateReceiptResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		try {
			return receiptServices.createReceipt(request);
		} catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}
}
