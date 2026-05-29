package com.secura.dnft.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonFormat;

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
	@Column(name = "aprmnt_id")
	private String aprmntId;

	@Id
	@Column(name = "due_id")
	private String dueId;

	@Id
	@Column(name = "collection_cycle")
	private String collectionCycle;

	@Id
	@Column(name = "flat_area")
	private String flatArea;

	@Id
	@Column(name = "due_date")
	@JsonFormat(pattern = "d-MMM-yyyy")
	private LocalDate dueDate;

	@Column(name = "payment_id")
	private String paymentId;

	@Column(name = "amount")
	private String amount;

	@Column(name = "gst_amount")
	private String gstAmount;

	@Column(name = "total_amount")
	private String totalAmount;

	@Column(name = "payment_name")
	private String paymentName;

	@Column(name = "payment_type")
	private String paymentType;

	@Column(name = "cause")
	private String cause;

	@Column(name = "payment_capita")
	private String paymentCapita;

	@Column(name = "added_charges", columnDefinition = "TEXT")
	private String addedCharges;

	@Column(name = "amount_per_month")
	private String amountPerMonth;

	@Column(name = "total_added_charges")
	private String totalAddedCharges;

	@Column(name = "estimated_collection_amount")
	private String estimatedCollectionAmount;

	@Column(name = "gst_percentage")
	private String gstPercentage;

	@Column(name = "discount_code")
	private String discountCode;

	@Column(name = "discount_mode")
	private String discountMode;

	@Column(name = "cummilation_cycle")
	private String cummilationCycle;

	@Column(name = "fine_code")
	private String fineCode;

	@Column(name = "disc_value")
	private String discValue;

	@Column(name = "fn_value")
	private String fnValue;

	@Column(name = "discounted_amount")
	private String discountedAmount;

	@Column(name = "fine_amount")
	private String fineAmount;

	@Column(name = "fine_mode")
	private String fineMode;

	@Column(name = "fine_type")
	private String fineType;

	@Column(name = "round_up_amount")
	private String roundUpAmount;

	@Column(name = "already_paid_amount")
	private String alreadyPaidAmount;

	@Column(name = "admin_discount")
	private String adminDiscount;

	@Column(name = "applicable_flats", columnDefinition = "TEXT")
	private String applicableFlats;

	@Column(name = "paid_flats", columnDefinition = "TEXT")
	private String paidFlats;

	@Column(name = "allowed_tenders", columnDefinition = "TEXT")
	private String allowedTenders;

	@Column(name = "payment_status")
	private String paymentStatus;

	@Column(name = "due_end_date")
	@JsonFormat(pattern = "d-MMM-yyyy")
	private LocalDate dueEndDate;

	@Column(name = "due_start_date")
	@JsonFormat(pattern = "d-MMM-yyyy")
	private LocalDate dueStartDate;

	@Column(name = "payment_date")
	@JsonFormat(pattern = "d-MMM-yyyy")
	private LocalDate paymentDate;

	@Column(name = "creat_ts")
	@CreationTimestamp
	private LocalDateTime creatTs;

	@Column(name = "creat_usr_id")
	private String creatUsrId;

	@Column(name = "lst_updt_ts")
	@UpdateTimestamp
	private LocalDateTime lstUpdtTs;

	@Column(name = "lst_updt_usr_id")
	private String lstUpdtUsrId;

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

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public String getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public String getGstAmount() {
		return gstAmount;
	}

	public void setGstAmount(String gstAmount) {
		this.gstAmount = gstAmount;
	}

	public String getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(String totalAmount) {
		this.totalAmount = totalAmount;
	}

	public String getPaymentName() {
		return paymentName;
	}

	public void setPaymentName(String paymentName) {
		this.paymentName = paymentName;
	}

	public String getPaymentType() {
		return paymentType;
	}

	public void setPaymentType(String paymentType) {
		this.paymentType = paymentType;
	}

	public String getFlatArea() {
		return flatArea;
	}

	public void setFlatArea(String flatArea) {
		this.flatArea = flatArea;
	}

	public String getCause() {
		return cause;
	}

	public void setCause(String cause) {
		this.cause = cause;
	}

	public String getPaymentCapita() {
		return paymentCapita;
	}

	public void setPaymentCapita(String paymentCapita) {
		this.paymentCapita = paymentCapita;
	}

	public String getAddedCharges() {
		return addedCharges;
	}

	public void setAddedCharges(String addedCharges) {
		this.addedCharges = addedCharges;
	}

	public String getAmountPerMonth() {
		return amountPerMonth;
	}

	public void setAmountPerMonth(String amountPerMonth) {
		this.amountPerMonth = amountPerMonth;
	}

	public String getTotalAddedCharges() {
		return totalAddedCharges;
	}

	public void setTotalAddedCharges(String totalAddedCharges) {
		this.totalAddedCharges = totalAddedCharges;
	}

	public String getEstimatedCollectionAmount() {
		return estimatedCollectionAmount;
	}

	public void setEstimatedCollectionAmount(String estimatedCollectionAmount) {
		this.estimatedCollectionAmount = estimatedCollectionAmount;
	}

	public String getGstPercentage() {
		return gstPercentage;
	}

	public void setGstPercentage(String gstPercentage) {
		this.gstPercentage = gstPercentage;
	}

	public String getDiscountCode() {
		return discountCode;
	}

	public void setDiscountCode(String discountCode) {
		this.discountCode = discountCode;
	}

	public String getDiscountMode() {
		return discountMode;
	}

	public void setDiscountMode(String discountMode) {
		this.discountMode = discountMode;
	}

	public String getFineCode() {
		return fineCode;
	}

	public void setFineCode(String fineCode) {
		this.fineCode = fineCode;
	}

	public String getCummilationCycle() {
		return cummilationCycle;
	}

	public void setCummilationCycle(String cummilationCycle) {
		this.cummilationCycle = cummilationCycle;
	}

	public String getDiscValue() {
		return discValue;
	}

	public void setDiscValue(String discValue) {
		this.discValue = discValue;
	}

	public String getFnValue() {
		return fnValue;
	}

	public void setFnValue(String fnValue) {
		this.fnValue = fnValue;
	}

	public String getDiscountedAmount() {
		return discountedAmount;
	}

	public void setDiscountedAmount(String discountedAmount) {
		this.discountedAmount = discountedAmount;
	}

	public String getFineAmount() {
		return fineAmount;
	}

	public void setFineAmount(String fineAmount) {
		this.fineAmount = fineAmount;
	}

	public String getFineMode() {
		return fineMode;
	}

	public void setFineMode(String fineMode) {
		this.fineMode = fineMode;
	}

	public String getFineType() {
		return fineType;
	}

	public void setFineType(String fineType) {
		this.fineType = fineType;
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

	public String getApplicableFlats() {
		return applicableFlats;
	}

	public void setApplicableFlats(String applicableFlats) {
		this.applicableFlats = applicableFlats;
	}

	public String getAllowedTenders() {
		return allowedTenders;
	}

	public String getPaidFlats() {
		return paidFlats;
	}

	public void setPaidFlats(String paidFlats) {
		this.paidFlats = paidFlats;
	}

	public void setAllowedTenders(String allowedTenders) {
		this.allowedTenders = allowedTenders;
	}

	public String getPaymentStatus() {
		return paymentStatus;
	}

	public void setPaymentStatus(String paymentStatus) {
		this.paymentStatus = paymentStatus;
	}

	public LocalDate getDueEndDate() {
		return dueEndDate;
	}

	public void setDueEndDate(LocalDate dueEndDate) {
		this.dueEndDate = dueEndDate;
	}

	public LocalDate getDueStartDate() {
		return dueStartDate;
	}

	public void setDueStartDate(LocalDate dueStartDate) {
		this.dueStartDate = dueStartDate;
	}

	public LocalDate getPaymentDate() {
		return paymentDate;
	}

	public void setPaymentDate(LocalDate paymentDate) {
		this.paymentDate = paymentDate;
	}

	public LocalDateTime getCreatTs() {
		return creatTs;
	}

	public void setCreatTs(LocalDateTime creatTs) {
		this.creatTs = creatTs;
	}

	public String getCreatUsrId() {
		return creatUsrId;
	}

	public void setCreatUsrId(String creatUsrId) {
		this.creatUsrId = creatUsrId;
	}

	public LocalDateTime getLstUpdtTs() {
		return lstUpdtTs;
	}

	public void setLstUpdtTs(LocalDateTime lstUpdtTs) {
		this.lstUpdtTs = lstUpdtTs;
	}

	public String getLstUpdtUsrId() {
		return lstUpdtUsrId;
	}

	public void setLstUpdtUsrId(String lstUpdtUsrId) {
		this.lstUpdtUsrId = lstUpdtUsrId;
	}
}
