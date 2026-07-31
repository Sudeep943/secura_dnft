package com.secura.dnft.request.response;

import java.math.BigDecimal;
import java.sql.Date;

public class CreditNoteDetails {

	private String creditNoteNo;
	private Date creditNoteIssueDate;
	private BigDecimal creditNoteAmount;
	private String creditNoteCause;
	private String creditNoteDetails;
	private String creditNoteIssuedBy;

	public String getCreditNoteNo() {
		return creditNoteNo;
	}

	public void setCreditNoteNo(String creditNoteNo) {
		this.creditNoteNo = creditNoteNo;
	}

	public Date getCreditNoteIssueDate() {
		return creditNoteIssueDate;
	}

	public void setCreditNoteIssueDate(Date creditNoteIssueDate) {
		this.creditNoteIssueDate = creditNoteIssueDate;
	}

	public BigDecimal getCreditNoteAmount() {
		return creditNoteAmount;
	}

	public void setCreditNoteAmount(BigDecimal creditNoteAmount) {
		this.creditNoteAmount = creditNoteAmount;
	}

	public String getCreditNoteCause() {
		return creditNoteCause;
	}

	public void setCreditNoteCause(String creditNoteCause) {
		this.creditNoteCause = creditNoteCause;
	}

	public String getCreditNoteDetails() {
		return creditNoteDetails;
	}

	public void setCreditNoteDetails(String creditNoteDetails) {
		this.creditNoteDetails = creditNoteDetails;
	}

	public String getCreditNoteIssuedBy() {
		return creditNoteIssuedBy;
	}

	public void setCreditNoteIssuedBy(String creditNoteIssuedBy) {
		this.creditNoteIssuedBy = creditNoteIssuedBy;
	}
}
