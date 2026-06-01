package com.secura.dnft.entity;

public class FailedEmailCause {

    private String referenceUniqueId;
    private String type;
    private String cause;
    private String failedEmailId;

    public FailedEmailCause() {
    }

    public FailedEmailCause(String referenceUniqueId, String type, String cause, String failedEmailId) {
        this.referenceUniqueId = referenceUniqueId;
        this.type = type;
        this.cause = cause;
        this.failedEmailId = failedEmailId;
    }

    public String getReferenceUniqueId() {
        return referenceUniqueId;
    }

    public void setReferenceUniqueId(String referenceUniqueId) {
        this.referenceUniqueId = referenceUniqueId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }

    public String getFailedEmailId() {
        return failedEmailId;
    }

    public void setFailedEmailId(String failedEmailId) {
        this.failedEmailId = failedEmailId;
    }
}
