package com.secura.dnft.request.response;

public class IssueCreditNoteRequest {

	private GenericHeader genericHeader;
	private String flatId;
	private CreditNoteDetails creditNoteDetails;

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

	public CreditNoteDetails getCreditNoteDetails() {
		return creditNoteDetails;
	}

	public void setCreditNoteDetails(CreditNoteDetails creditNoteDetails) {
		this.creditNoteDetails = creditNoteDetails;
	}
}
