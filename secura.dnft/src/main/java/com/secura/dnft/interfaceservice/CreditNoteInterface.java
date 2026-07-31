package com.secura.dnft.interfaceservice;

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

public interface CreditNoteInterface {

	IssueCreditNoteResponse issueCreditNote(IssueCreditNoteRequest request) throws Exception;

	ValidateCreditNoteResponse validateCreditNote(ValidateCreditNoteRequest request) throws Exception;

	ReedemCreditNoteResponse reedemCreditNote(ReedemCreditNoteRequest request) throws Exception;

	ViewCreditNoteDetailsResponse viewCreditNoteDetails(ViewCreditNoteDetailsRequest request) throws Exception;

	DeleteCreditNoteResponse deleteCreditNote(DeleteCreditNoteRequest request) throws Exception;

	CreditNoteAvailableResponse creditNoteAvailable(CreditNoteAvailableRequest request) throws Exception;
}
