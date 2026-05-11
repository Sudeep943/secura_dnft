package com.secura.dnft.entity;

import java.io.Serializable;
import java.util.Objects;

public class DueAmountDetailsEntityId implements Serializable {

	private static final long serialVersionUID = 1L;

	private String dueId;
	private String collectionCycle;
	private String flatArea;

	public DueAmountDetailsEntityId() {
	}

	public DueAmountDetailsEntityId(String dueId, String collectionCycle, String flatArea) {
		this.dueId = dueId;
		this.collectionCycle = collectionCycle;
		this.flatArea = flatArea;
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

	public String getFlatArea() {
		return flatArea;
	}

	public void setFlatArea(String flatArea) {
		this.flatArea = flatArea;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof DueAmountDetailsEntityId other)) {
			return false;
		}
		return Objects.equals(dueId, other.dueId) && Objects.equals(collectionCycle, other.collectionCycle)
				&& Objects.equals(flatArea, other.flatArea);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dueId, collectionCycle, flatArea);
	}
}
