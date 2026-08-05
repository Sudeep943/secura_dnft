package com.secura.dnft.request.response;

import java.math.BigDecimal;
import java.util.List;

public class ViewCreditNoteDetailsResponse {

	private GenericHeader genericHeader;
	private String flatId;
	private List<CreditNoteDetails> creditNoteDetails;
	private BigDecimal totalAmount;
	private BigDecimal usedAmount;
	private BigDecimal remainingAmount;
	private String message;
	private String messageCode;

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

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount;
	}

	public BigDecimal getUsedAmount() {
		return usedAmount;
	}

	public void setUsedAmount(BigDecimal usedAmount) {
		this.usedAmount = usedAmount;
	}

	public BigDecimal getRemainingAmount() {
		return remainingAmount;
	}

	public void setRemainingAmount(BigDecimal remainingAmount) {
		this.remainingAmount = remainingAmount;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessageCode() {
		return messageCode;
	}

	public void setMessageCode(String messageCode) {
		this.messageCode = messageCode;
	}
}
