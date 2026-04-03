package com.secura.dnft.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "secura_notice")
public class SecuraNotice {

    @Column(name = "aprmt_id")
    private String aprmtId;

    @Id
    @Column(name = "notice_id", nullable = false)
    private String noticeId;

    @Column(name = "publishing_date")
    private LocalDateTime publishingDate;

    @Column(name = "letter_number")
    private String letterNumber;

    @Column(name = "header", columnDefinition = "TEXT")
    private String header;

    @Column(name = "short_details", columnDefinition = "TEXT")
    private String shortDetails;

    @Column(name = "status")
    private String status;

    @Column(name = "notice_document_id")
    private String noticeDocumentId;

    @Column(name = "creat_ts")
    @CreationTimestamp
    private LocalDateTime creatTs;

    @Column(name = "creat_usr_id")
    private String creatUsrId;

    @Column(name = "lst_updt_ts")
    @UpdateTimestamp
    private LocalDateTime lstUpdtTs;

    @Column(name = "lst_updt_usrId")
    private String lstUpdtUsrId;

    public String getAprmtId() {
        return aprmtId;
    }

    public void setAprmtId(String aprmtId) {
        this.aprmtId = aprmtId;
    }

    public String getNoticeId() {
        return noticeId;
    }

    public void setNoticeId(String noticeId) {
        this.noticeId = noticeId;
    }

    public LocalDateTime getPublishingDate() {
        return publishingDate;
    }

    public void setPublishingDate(LocalDateTime publishingDate) {
        this.publishingDate = publishingDate;
    }

    public String getLetterNumber() {
        return letterNumber;
    }

    public void setLetterNumber(String letterNumber) {
        this.letterNumber = letterNumber;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNoticeDocumentId() {
        return noticeDocumentId;
    }

    public void setNoticeDocumentId(String noticeDocumentId) {
        this.noticeDocumentId = noticeDocumentId;
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
