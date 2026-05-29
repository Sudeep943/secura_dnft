package com.secura.dnft.entity;

import java.io.Serializable;
import java.util.Objects;

public class DueAmountDetailsEntityId implements Serializable {

	private static final long serialVersionUID = 1L;

	private String aprmntId;
	private String dueId;

	public DueAmountDetailsEntityId() {
	}

	public DueAmountDetailsEntityId(String aprmntId, String dueId) {
		this.aprmntId = aprmntId;
		this.dueId = dueId;
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
		if (!(object instanceof DueAmountDetailsEntityId other)) {
			return false;
		}
		return Objects.equals(aprmntId, other.aprmntId) && Objects.equals(dueId, other.dueId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(aprmntId, dueId);
	}
}
