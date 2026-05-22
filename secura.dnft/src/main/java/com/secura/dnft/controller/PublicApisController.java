package com.secura.dnft.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.request.response.GetAllFlatsRequest;
import com.secura.dnft.request.response.GetAllFlatsResponse;
import com.secura.dnft.request.response.GetDueAmountForFlatRequest;
import com.secura.dnft.request.response.GetDueAmountForFlatResponse;
import com.secura.dnft.request.response.PayDueRequest;
import com.secura.dnft.request.response.PayDueResponse;
import com.secura.dnft.service.FlatServices;
import com.secura.dnft.service.PaymentServices;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/publicapis")
public class PublicApisController {

	@Autowired
	private FlatServices flatServices;

	@Autowired
	private PaymentServices paymentServices;

	@PostMapping("/getFlatsPublic")
	@CrossOrigin(origins = "*")
	public GetAllFlatsResponse getFlatsPublic(@RequestBody GetAllFlatsRequest request) {
		return flatServices.getAllFlats(request);
	}

	@PostMapping("/detDueDetailsForFlatPublic")
	@CrossOrigin(origins = "*")
	public GetDueAmountForFlatResponse getDueDetailsForFlatPublic(@RequestBody GetDueAmountForFlatRequest request) {
		try {
			return flatServices.getDueAmountForFlat(request);
		} catch (Exception e) {
			GetDueAmountForFlatResponse response = new GetDueAmountForFlatResponse();
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
			return response;
		}
	}

	@PostMapping("/payduesPublic")
	@CrossOrigin(origins = "*")
	public PayDueResponse payDuesPublic(@RequestBody PayDueRequest request) {
		PayDueResponse response = new PayDueResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		try {
			return paymentServices.payDues(request);
		} catch (Exception e) {
			e.printStackTrace();
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}
}
