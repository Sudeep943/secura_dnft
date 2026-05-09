package com.secura.dnft.entity;

import java.io.Serializable;
import java.util.Objects;

public class DiscFinId implements Serializable {

	private static final long serialVersionUID = 1L;

	private String discFnId;
	private String discFnCycleType;

	public DiscFinId() {
	}

	public DiscFinId(String discFnId, String discFnCycleType) {
		this.discFnId = discFnId;
		this.discFnCycleType = discFnCycleType;
	}

	public String getDiscFnId() {
		return discFnId;
	}

	public void setDiscFnId(String discFnId) {
		this.discFnId = discFnId;
	}

	public String getDiscFnCycleType() {
		return discFnCycleType;
	}

	public void setDiscFnCycleType(String discFnCycleType) {
		this.discFnCycleType = discFnCycleType;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof DiscFinId other)) {
			return false;
		}
		return Objects.equals(discFnId, other.discFnId)
				&& Objects.equals(discFnCycleType, other.discFnCycleType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(discFnId, discFnCycleType);
	}
}
