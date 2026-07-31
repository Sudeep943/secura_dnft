package com.secura.dnft.request.response;

import java.math.BigDecimal;

public class ValidateCreditNoteResponse {

	private GenericHeader genericHeader;
	private boolean valid;
	private BigDecimal balanceAmount;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public BigDecimal getBalanceAmount() {
		return balanceAmount;
	}

	public void setBalanceAmount(BigDecimal balanceAmount) {
		this.balanceAmount = balanceAmount;
	}
}
