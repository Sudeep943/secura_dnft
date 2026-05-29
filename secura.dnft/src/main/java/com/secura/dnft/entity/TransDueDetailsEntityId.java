package com.secura.dnft.entity;

import java.io.Serializable;
import java.util.Objects;

public class TransDueDetailsEntityId implements Serializable {

	private static final long serialVersionUID = 1L;

	private String transactionId;
	private String aprmntId;
	private String dueId;

	public TransDueDetailsEntityId() {
	}

	public TransDueDetailsEntityId(String transactionId, String aprmntId, String dueId) {
		this.transactionId = transactionId;
		this.aprmntId = aprmntId;
		this.dueId = dueId;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public String getAprmntId() {
		return aprmntId;
	}

	public void setAprmntId(String aprmntId) {
		this.aprmntId = aprmntId;
	}

	public String getDueId() {
		return dueId;
	}

	public void setDueId(String dueId) {
		this.dueId = dueId;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof TransDueDetailsEntityId other)) {
			return false;
		}
		return Objects.equals(transactionId, other.transactionId) && Objects.equals(aprmntId, other.aprmntId)
				&& Objects.equals(dueId, other.dueId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(transactionId, aprmntId, dueId);
	}
}
