package com.secura.dnft.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "secura_payments")
public class SecuraPayment {

    @Id
    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "payment_name")
    private String paymentName;

    @Column(name = "short_details")
    private String shortDetails;

    @Column(name = "payment_capita")
    private String paymentCapita;

    @Column(name = "payment_amount")
    private String paymentAmount;

    @Column(name = "gst")
    private String gst;

    @Column(name = "collection_start_date")
    private LocalDateTime collectionStartDate;

    @Column(name = "collection_end_date")
    private LocalDateTime collectionEndDate;

    @Column(name = "payment_collection_cycle")
    private String paymentCollectionCycle;

    @Column(name = "payment_collection_mode")
    private String paymentCollectionMode;

    @Column(name = "applicable_for", columnDefinition = "TEXT")
    private String applicableFor;

    @Column(name = "payment_type")
    private String paymentType;

    @Column(name = "bank_account_id")
    private String bankAccountId;

    @Column(name = "status")
    private String status;

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

    public SecuraPayment() {
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getPaymentName() {
        return paymentName;
    }

    public void setPaymentName(String paymentName) {
        this.paymentName = paymentName;
    }

    public String getShortDetails() {
        return shortDetails;
    }

    public void setShortDetails(String shortDetails) {
        this.shortDetails = shortDetails;
    }

    public String getPaymentCapita() {
        return paymentCapita;
    }

    public void setPaymentCapita(String paymentCapita) {
        this.paymentCapita = paymentCapita;
    }

    public String getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(String paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public String getGst() {
        return gst;
    }

    public void setGst(String gst) {
        this.gst = gst;
    }

    public LocalDateTime getCollectionStartDate() {
        return collectionStartDate;
    }

    public void setCollectionStartDate(LocalDateTime collectionStartDate) {
        this.collectionStartDate = collectionStartDate;
    }

    public LocalDateTime getCollectionEndDate() {
        return collectionEndDate;
    }

    public void setCollectionEndDate(LocalDateTime collectionEndDate) {
        this.collectionEndDate = collectionEndDate;
    }

    public String getPaymentCollectionCycle() {
        return paymentCollectionCycle;
    }

    public void setPaymentCollectionCycle(String paymentCollectionCycle) {
        this.paymentCollectionCycle = paymentCollectionCycle;
    }

    public String getPaymentCollectionMode() {
        return paymentCollectionMode;
    }

    public void setPaymentCollectionMode(String paymentCollectionMode) {
        this.paymentCollectionMode = paymentCollectionMode;
    }

    public String getApplicableFor() {
        return applicableFor;
    }

    public void setApplicableFor(String applicableFor) {
        this.applicableFor = applicableFor;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(String bankAccountId) {
        this.bankAccountId = bankAccountId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
