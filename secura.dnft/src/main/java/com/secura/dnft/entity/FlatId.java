package com.secura.dnft.entity;

import java.io.Serializable;
import java.util.Objects;

public class FlatId implements Serializable {

	private static final long serialVersionUID = 1L;

	private String aprmntId;
	private String flatNo;

	public FlatId() {
	}

	public FlatId(String aprmntId, String flatNo) {
		this.aprmntId = aprmntId;
		this.flatNo = flatNo;
	}

	public String getAprmntId() {
		return aprmntId;
	}

	public void setAprmntId(String aprmntId) {
		this.aprmntId = aprmntId;
	}

	public String getFlatNo() {
		return flatNo;
	}

	public void setFlatNo(String flatNo) {
		this.flatNo = flatNo;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof FlatId other)) {
			return false;
		}
		return Objects.equals(aprmntId, other.aprmntId) && Objects.equals(flatNo, other.flatNo);
	}

	@Override
	public int hashCode() {
		return Objects.hash(aprmntId, flatNo);
	}
}
