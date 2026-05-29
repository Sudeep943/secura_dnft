package com.secura.dnft.entity;

import java.io.Serializable;
import java.util.Objects;

public class TransactionId implements Serializable {

	private static final long serialVersionUID = 1L;

	private String aprmntId;
	private String trnscId;

	public TransactionId() {
	}

	public TransactionId(String aprmntId, String trnscId) {
		this.aprmntId = aprmntId;
		this.trnscId = trnscId;
	}

	public String getAprmntId() {
		return aprmntId;
	}

	public void setAprmntId(String aprmntId) {
		this.aprmntId = aprmntId;
	}

	public String getTrnscId() {
		return trnscId;
	}

	public void setTrnscId(String trnscId) {
		this.trnscId = trnscId;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof TransactionId other)) {
			return false;
		}
		return Objects.equals(aprmntId, other.aprmntId) && Objects.equals(trnscId, other.trnscId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(aprmntId, trnscId);
	}
}
