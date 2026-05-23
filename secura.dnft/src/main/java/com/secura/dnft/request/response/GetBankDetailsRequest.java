package com.secura.dnft.request.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GetBankDetailsRequest {

	private GenericHeader genericHeader;
	@JsonProperty("BankDetailsID")
	@JsonAlias("bankDetailsID")
	private String bankDetailsID;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public String getBankDetailsID() {
		return bankDetailsID;
	}

	public void setBankDetailsID(String bankDetailsID) {
		this.bankDetailsID = bankDetailsID;
	}
}
