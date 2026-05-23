package com.secura.dnft.request.response;

import com.secura.dnft.bean.BankAccountDetails;

public class AddBankDetailsRequest {

	private GenericHeader genericHeader;
	private BankAccountDetails bankAccountDetails;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public BankAccountDetails getBankAccountDetails() {
		return bankAccountDetails;
	}

	public void setBankAccountDetails(BankAccountDetails bankAccountDetails) {
		this.bankAccountDetails = bankAccountDetails;
	}
}
