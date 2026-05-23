package com.secura.dnft.request.response;

import java.util.List;

import com.secura.dnft.bean.BankAccountDetails;

public class GetBankDetailsResponse {

	private GenericHeader genericHeader;
	private List<BankAccountDetails> bankAccountDetails;
	private String message;
	private String messageCode;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public List<BankAccountDetails> getBankAccountDetails() {
		return bankAccountDetails;
	}

	public void setBankAccountDetails(List<BankAccountDetails> bankAccountDetails) {
		this.bankAccountDetails = bankAccountDetails;
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
