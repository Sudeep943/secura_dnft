package com.secura.dnft.request.response;

import java.time.LocalDate;

import com.secura.dnft.entity.DueAmountDetailsEntity;

public class GetDueAmountForFlatRequest {

	private GenericHeader genericHeader;
	private String flatId;
	private LocalDate transactionDate;
	private DueAmountDetailsEntity specificDue;

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

	public LocalDate getTransactionDate() {
		return transactionDate;
	}

	public void setTransactionDate(LocalDate transactionDate) {
		this.transactionDate = transactionDate;
	}

	public DueAmountDetailsEntity getSpecificDue() {
		return specificDue;
	}

	public void setSpecificDue(DueAmountDetailsEntity specificDue) {
		this.specificDue = specificDue;
	}
	
	
}
