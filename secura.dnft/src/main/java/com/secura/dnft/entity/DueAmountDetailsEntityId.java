package com.secura.dnft.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class DueAmountDetailsEntityId implements Serializable {

	private static final long serialVersionUID = 1L;

	private String aprmntId;
	private String dueId;
	private String collectionCycle;
	private String flatArea;
	private LocalDate dueDate;

	public DueAmountDetailsEntityId() {
	}

	public DueAmountDetailsEntityId(String aprmntId, String dueId, String collectionCycle, String flatArea,
			LocalDate dueDate) {
		this.aprmntId = aprmntId;
		this.dueId = dueId;
		this.collectionCycle = collectionCycle;
		this.flatArea = flatArea;
		this.dueDate = dueDate;
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

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof DueAmountDetailsEntityId other)) {
			return false;
		}
		return Objects.equals(aprmntId, other.aprmntId) && Objects.equals(dueId, other.dueId)
				&& Objects.equals(collectionCycle, other.collectionCycle)
				&& Objects.equals(flatArea, other.flatArea) && Objects.equals(dueDate, other.dueDate);
	}

	@Override
	public int hashCode() {
		return Objects.hash(aprmntId, dueId, collectionCycle, flatArea, dueDate);
	}
}
