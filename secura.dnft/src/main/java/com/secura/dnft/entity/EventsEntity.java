package com.secura.dnft.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "secura_events")
public class EventsEntity {

    @Column(name = "aprmt_id")
    private String aprmtId;

    @Id
    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "event_date_time")
    private LocalDateTime eventDateTime;

    @Column(name = "location")
    private String location;

    @Column(name = "duration")
    private String duration;

    @Column(name = "occurance")
    private String occurance;

    @Column(name = "till_date")
    private LocalDateTime tillDate;

    @Column(name = "header", columnDefinition = "TEXT")
    private String header;

    @Column(name = "short_details", columnDefinition = "TEXT")
    private String shortDetails;

    @Column(name = "paid")
    private Boolean paid;

    @Column(name = "payment_type")
    private String paymentType;

    @Column(name = "collection_start_date")
    private LocalDateTime collectionStartDate;

    @Column(name = "collection_end_date")
    private LocalDateTime collectionEndDate;

    @Column(name = "registration_form_link", columnDefinition = "TEXT")
    private String registrationFormLink;

    @Column(name = "invitees", columnDefinition = "TEXT")
    private String invitees;

    @Column(name = "required_coupon_creation_for_paid_member")
    private Boolean requiredCouponCreationForPaidMember;

    @Column(name = "bank_account_id")
    private String bankAccountId;

    @Column(name = "payment_amount")
    private String paymentAmount;

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

    public String getAprmtId() {
        return aprmtId;
    }

    public void setAprmtId(String aprmtId) {
        this.aprmtId = aprmtId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getEventDateTime() {
        return eventDateTime;
    }

    public void setEventDateTime(LocalDateTime eventDateTime) {
        this.eventDateTime = eventDateTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getOccurance() {
        return occurance;
    }

    public void setOccurance(String occurance) {
        this.occurance = occurance;
    }

    public LocalDateTime getTillDate() {
        return tillDate;
    }

    public void setTillDate(LocalDateTime tillDate) {
        this.tillDate = tillDate;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getShortDetails() {
        return shortDetails;
    }

    public void setShortDetails(String shortDetails) {
        this.shortDetails = shortDetails;
    }

    public Boolean getPaid() {
        return paid;
    }

    public void setPaid(Boolean paid) {
        this.paid = paid;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
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

    public String getRegistrationFormLink() {
        return registrationFormLink;
    }

    public void setRegistrationFormLink(String registrationFormLink) {
        this.registrationFormLink = registrationFormLink;
    }

    public String getInvitees() {
        return invitees;
    }

    public void setInvitees(String invitees) {
        this.invitees = invitees;
    }

    public Boolean getRequiredCouponCreationForPaidMember() {
        return requiredCouponCreationForPaidMember;
    }

    public void setRequiredCouponCreationForPaidMember(Boolean requiredCouponCreationForPaidMember) {
        this.requiredCouponCreationForPaidMember = requiredCouponCreationForPaidMember;
    }

    public String getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(String bankAccountId) {
        this.bankAccountId = bankAccountId;
    }

    public String getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(String paymentAmount) {
        this.paymentAmount = paymentAmount;
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
