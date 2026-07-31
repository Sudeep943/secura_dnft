package com.secura.dnft.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.request.response.CreditNoteAvailableRequest;
import com.secura.dnft.request.response.CreditNoteAvailableResponse;
import com.secura.dnft.request.response.DeleteCreditNoteRequest;
import com.secura.dnft.request.response.DeleteCreditNoteResponse;
import com.secura.dnft.request.response.IssueCreditNoteRequest;
import com.secura.dnft.request.response.IssueCreditNoteResponse;
import com.secura.dnft.request.response.ReedemCreditNoteRequest;
import com.secura.dnft.request.response.ReedemCreditNoteResponse;
import com.secura.dnft.request.response.ValidateCreditNoteRequest;
import com.secura.dnft.request.response.ValidateCreditNoteResponse;
import com.secura.dnft.request.response.ViewCreditNoteDetailsRequest;
import com.secura.dnft.request.response.ViewCreditNoteDetailsResponse;
import com.secura.dnft.service.CreditNoteServiceImpl;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/creditnote")
public class CreditNoteOperations {

	@Autowired
	private CreditNoteServiceImpl creditNoteServiceImpl;

	@PostMapping("/issueCreditNote")
	public IssueCreditNoteResponse issueCreditNote(@RequestBody IssueCreditNoteRequest request) {
		IssueCreditNoteResponse response = new IssueCreditNoteResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		try {
			response = creditNoteServiceImpl.issueCreditNote(request);
		} catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}

	@PostMapping("/validateCreditNote")
	public ValidateCreditNoteResponse validateCreditNote(@RequestBody ValidateCreditNoteRequest request) {
		ValidateCreditNoteResponse response = new ValidateCreditNoteResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		try {
			response = creditNoteServiceImpl.validateCreditNote(request);
		} catch (Exception e) {
			response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		}
		return response;
	}

	@PostMapping("/reedemCreditNote")
	public ReedemCreditNoteResponse reedemCreditNote(@RequestBody ReedemCreditNoteRequest request) {
		ReedemCreditNoteResponse response = new ReedemCreditNoteResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		try {
			response = creditNoteServiceImpl.reedemCreditNote(request);
		} catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}

	@PostMapping("/viewCreditNoteDetails")
	public ViewCreditNoteDetailsResponse viewCreditNoteDetails(@RequestBody ViewCreditNoteDetailsRequest request) {
		ViewCreditNoteDetailsResponse response = new ViewCreditNoteDetailsResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		try {
			response = creditNoteServiceImpl.viewCreditNoteDetails(request);
		} catch (Exception e) {
			response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		}
		return response;
	}

	@PostMapping("/deleteCreditNote")
	public DeleteCreditNoteResponse deleteCreditNote(@RequestBody DeleteCreditNoteRequest request) {
		DeleteCreditNoteResponse response = new DeleteCreditNoteResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		try {
			response = creditNoteServiceImpl.deleteCreditNote(request);
		} catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}

	@PostMapping("/creditNoteAvailable")
	public CreditNoteAvailableResponse creditNoteAvailable(@RequestBody CreditNoteAvailableRequest request) {
		CreditNoteAvailableResponse response = new CreditNoteAvailableResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		try {
			response = creditNoteServiceImpl.creditNoteAvailable(request);
		} catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}
}
