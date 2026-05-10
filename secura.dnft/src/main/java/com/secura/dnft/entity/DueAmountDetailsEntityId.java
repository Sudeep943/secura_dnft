package com.secura.dnft.entity;

import java.io.Serializable;
import java.util.Objects;

public class DueAmountDetailsEntityId implements Serializable {

	private static final long serialVersionUID = 1L;

	private String dueId;
	private String collectionCycle;

	public DueAmountDetailsEntityId() {
	}

	public DueAmountDetailsEntityId(String dueId, String collectionCycle) {
		this.dueId = dueId;
		this.collectionCycle = collectionCycle;
	}

	public String getDueId() {
		return dueId;
	}

	public void setDueId(String dueId) {
		this.dueId = dueId;
	}

	public String getCollectionCycle() {
		return collectionCycle;
	}

	public void setCollectionCycle(String collectionCycle) {
		this.collectionCycle = collectionCycle;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof DueAmountDetailsEntityId other)) {
			return false;
		}
		return Objects.equals(dueId, other.dueId) && Objects.equals(collectionCycle, other.collectionCycle);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dueId, collectionCycle);
	}
}
