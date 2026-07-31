package com.secura.dnft.entity;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.secura.dnft.request.response.CreditNoteDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@IdClass(CreditNoteEntityId.class)
@Table(name = "secura_credit_note_details")
public class CreditNoteEntity {

	@Id
	@Column(name = "apartment_id")
	private String apartmentId;

	@Id
	@Column(name = "flat_id")
	private String flatId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "credit_note_details", columnDefinition = "jsonb")
	private List<CreditNoteDetails> creditNoteDetails;

	@Column(name = "total_amount", precision = 19, scale = 4)
	private BigDecimal totalAmount;

	@Column(name = "used_amount", precision = 19, scale = 4)
	private BigDecimal usedAmount;

	@Column(name = "remaining_amount", precision = 19, scale = 4)
	private BigDecimal remainingAmount;

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

	public List<CreditNoteDetails> getCreditNoteDetails() {
		return creditNoteDetails;
	}

	public void setCreditNoteDetails(List<CreditNoteDetails> creditNoteDetails) {
		this.creditNoteDetails = creditNoteDetails;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount;
	}

	public BigDecimal getUsedAmount() {
		return usedAmount;
	}

	public void setUsedAmount(BigDecimal usedAmount) {
		this.usedAmount = usedAmount;
	}

	public BigDecimal getRemainingAmount() {
		return remainingAmount;
	}

	public void setRemainingAmount(BigDecimal remainingAmount) {
		this.remainingAmount = remainingAmount;
	}
}
