package com.secura.dnft.entity;

import java.io.Serializable;
import java.util.Objects;

public class CreditNoteEntityId implements Serializable {

	private static final long serialVersionUID = 1L;

	private String apartmentId;
	private String flatId;

	public CreditNoteEntityId() {
	}

	public CreditNoteEntityId(String apartmentId, String flatId) {
		this.apartmentId = apartmentId;
		this.flatId = flatId;
	}

	public String getApartmentId() {
		return apartmentId;
	}

	public void setApartmentId(String apartmentId) {
		this.apartmentId = apartmentId;
	}

	public String getFlatId() {
		return flatId;
	}

	public void setFlatId(String flatId) {
		this.flatId = flatId;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof CreditNoteEntityId other)) {
			return false;
		}
		return Objects.equals(apartmentId, other.apartmentId) && Objects.equals(flatId, other.flatId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(apartmentId, flatId);
	}
}
