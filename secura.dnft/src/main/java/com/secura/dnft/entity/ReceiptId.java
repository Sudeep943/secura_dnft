package com.secura.dnft.entity;

import java.io.Serializable;
import java.util.Objects;

public class ReceiptId implements Serializable {

	private static final long serialVersionUID = 1L;

	private String aprmtId;
	private String receiptId;

	public ReceiptId() {
	}

	public ReceiptId(String aprmtId, String receiptId) {
		this.aprmtId = aprmtId;
		this.receiptId = receiptId;
	}

	public String getAprmtId() {
		return aprmtId;
	}

	public void setAprmtId(String aprmtId) {
		this.aprmtId = aprmtId;
	}

	public String getReceiptId() {
		return receiptId;
	}

	public void setReceiptId(String receiptId) {
		this.receiptId = receiptId;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof ReceiptId other)) {
			return false;
		}
		return Objects.equals(aprmtId, other.aprmtId) && Objects.equals(receiptId, other.receiptId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(aprmtId, receiptId);
	}
}
