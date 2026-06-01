package com.secura.dnft.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "secura_email_log")
public class SecuraEmailLog {

    @Id
    @Column(name = "log_id")
    private String logId;

    /**
     * Type: Payment / Transaction / Notice / Booking
     */
    @Column(name = "type")
    private String type;

    /**
     * Reference unique ID of the source entity (payment_id / trnsc_id / notice_id / booking_id)
     */
    @Column(name = "reference_unique_id")
    private String referenceUniqueId;

    @Column(name = "attempt")
    private Integer attempt;

    @Column(name = "total_applicable")
    private Integer totalApplicable;

    @Column(name = "email_sent")
    private Integer emailSent;

    /**
     * JSON list of flat/applicable IDs for which email sending failed
     */
    @Column(name = "failed_applicable_list", columnDefinition = "TEXT")
    private String failedApplicableList;

    /**
     * JSON list of FailedEmailCause objects describing each failure
     */
    @Column(name = "failed_email_cause", columnDefinition = "TEXT")
    private String failedEmailCause;

    @Column(name = "create_ts")
    @CreationTimestamp
    private LocalDateTime createTs;

    @Column(name = "last_update_ts")
    @UpdateTimestamp
    private LocalDateTime lastUpdateTs;

    public SecuraEmailLog() {
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReferenceUniqueId() {
        return referenceUniqueId;
    }

    public void setReferenceUniqueId(String referenceUniqueId) {
        this.referenceUniqueId = referenceUniqueId;
    }

    public Integer getAttempt() {
        return attempt;
    }

    public void setAttempt(Integer attempt) {
        this.attempt = attempt;
    }

    public Integer getTotalApplicable() {
        return totalApplicable;
    }

    public void setTotalApplicable(Integer totalApplicable) {
        this.totalApplicable = totalApplicable;
    }

    public Integer getEmailSent() {
        return emailSent;
    }

    public void setEmailSent(Integer emailSent) {
        this.emailSent = emailSent;
    }

    public String getFailedApplicableList() {
        return failedApplicableList;
    }

    public void setFailedApplicableList(String failedApplicableList) {
        this.failedApplicableList = failedApplicableList;
    }

    public String getFailedEmailCause() {
        return failedEmailCause;
    }

    public void setFailedEmailCause(String failedEmailCause) {
        this.failedEmailCause = failedEmailCause;
    }

    public LocalDateTime getCreateTs() {
        return createTs;
    }

    public void setCreateTs(LocalDateTime createTs) {
        this.createTs = createTs;
    }

    public LocalDateTime getLastUpdateTs() {
        return lastUpdateTs;
    }

    public void setLastUpdateTs(LocalDateTime lastUpdateTs) {
        this.lastUpdateTs = lastUpdateTs;
    }
}
