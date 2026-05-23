package com.secura.dnft.entity;

import java.io.Serializable;
import java.util.Objects;

public class BankEntityId implements Serializable {

	private static final long serialVersionUID = 1L;

	private String aprmntId;
	private String bankDetailsID;

	public BankEntityId() {
	}

	public BankEntityId(String aprmntId, String bankDetailsID) {
		this.aprmntId = aprmntId;
		this.bankDetailsID = bankDetailsID;
	}

	public String getAprmntId() {
		return aprmntId;
	}

	public void setAprmntId(String aprmntId) {
		this.aprmntId = aprmntId;
	}

	public String getBankDetailsID() {
		return bankDetailsID;
	}

	public void setBankDetailsID(String bankDetailsID) {
		this.bankDetailsID = bankDetailsID;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof BankEntityId other)) {
			return false;
		}
		return Objects.equals(aprmntId, other.aprmntId) && Objects.equals(bankDetailsID, other.bankDetailsID);
	}

	@Override
	public int hashCode() {
		return Objects.hash(aprmntId, bankDetailsID);
	}
}
