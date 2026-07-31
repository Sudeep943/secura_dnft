package com.secura.dnft.request.response;

import java.util.List;

public class ViewCreditNoteDetailsResponse {

	private GenericHeader genericHeader;
	private String flatId;
	private List<CreditNoteDetails> creditNoteDetails;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public String getFlatId() {
		return flatId;
	}

	public void setFlatId(String flatId) {
		this.flatId = flatId;
	}

	public List<CreditNoteDetails> getCreditNoteDetails() {
		return creditNoteDetails;
	}

	public void setCreditNoteDetails(List<CreditNoteDetails> creditNoteDetails) {
		this.creditNoteDetails = creditNoteDetails;
	}
}
