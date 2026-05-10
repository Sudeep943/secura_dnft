package com.secura.dnft.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@IdClass(DueAmountDetailsEntityId.class)
@Table(name = "secura_due_amount_details")
public class DueAmountDetailsEntity {

	@Id
	@Column(name = "due_id")
	private String dueId;

	@Id
	@Column(name = "collection_cycle")
	private String collectionCycle;

	@Column(name = "round_up_amount")
	private String roundUpAmount;

	@Column(name = "already_paid_amount")
	private String alreadyPaidAmount;

	@Column(name = "admin_discount")
	private String adminDiscount;

	@Column(name = "payment_status")
	private String paymentStatus;

	@Column(name = "payment_date")
	private LocalDate paymentDate;

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

	public String getRoundUpAmount() {
		return roundUpAmount;
	}

	public void setRoundUpAmount(String roundUpAmount) {
		this.roundUpAmount = roundUpAmount;
	}

	public String getAlreadyPaidAmount() {
		return alreadyPaidAmount;
	}

	public void setAlreadyPaidAmount(String alreadyPaidAmount) {
		this.alreadyPaidAmount = alreadyPaidAmount;
	}

	public String getAdminDiscount() {
		return adminDiscount;
	}

	public void setAdminDiscount(String adminDiscount) {
		this.adminDiscount = adminDiscount;
	}

	public String getPaymentStatus() {
		return paymentStatus;
	}

	public void setPaymentStatus(String paymentStatus) {
		this.paymentStatus = paymentStatus;
	}

	public LocalDate getPaymentDate() {
		return paymentDate;
	}

	public void setPaymentDate(LocalDate paymentDate) {
		this.paymentDate = paymentDate;
	}
}
