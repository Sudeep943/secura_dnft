package com.secura.dnft.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.request.response.BookingRequest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "secura_bkng")
public class Booking {

    @Id
    @Column(name = "bkng_id")
    private String bkngId;

    @Column(name = "bkng_date")
    private LocalDateTime bkngDate;

    @Column(name = "bkng_by")
    private String bkngBy;

    @Column(name = "bkng_hall_id")
    private String bkngHallId;

    @Column(name = "bkng_evnt_dt")
    private LocalDateTime bkngEvntDt;

    @Column(name = "bkng_flt_no")
    private String bkngFltNo;

    @Column(name = "bkng_phn_no")
    private String bkngPhnNo;

    @Column(name = "bkng_pros")
    private String bkngPros;

    @Column(name = "bkng_expt_gest")
    private String bkngExptGest;

    @Column(name = "bkng_type")
    private String bkngType;

    @Column(name = "bkng_sts")
    private String bkngSts;

    @Column(name = "bkng_aprvd_by")
    private String bkngAprvdBy;

    @Column(name = "bkng_cncld_by")
    private String bkngCncldBy;

    @Column(name = "bkng_cncld_reason")
    private String bkng_cncld_reason;
    
    @Column(name = "creat_ts")
    private LocalDateTime creatTs;

    @Column(name = "creat_usr_id")
    private String creatUsrId;

    @Column(name = "lst_updt_ts")
    @UpdateTimestamp
    private LocalDateTime lstUpdtTs;

    @Column(name = "lst_updt_usr_id")
    private String lstUpdtUsrId;

//    @ManyToOne
//    @JoinColumn(name = "bkng_trnsc_id")
//    private Transaction transaction;

    // Getters and Setters

    public Booking() {} 
    
    public String getBkngId() {
        return bkngId;
    }

    public void setBkngId(String bkngId) {
        this.bkngId = bkngId;
    }

    public LocalDateTime getBkngDate() {
        return bkngDate;
    }

    public void setBkngDate(LocalDateTime bkngDate) {
        this.bkngDate = bkngDate;
    }

    public String getBkngBy() {
        return bkngBy;
    }

    public void setBkngBy(String bkngBy) {
        this.bkngBy = bkngBy;
    }

    public String getBkngHallId() {
        return bkngHallId;
    }

    public void setBkngHallId(String bkngHallId) {
        this.bkngHallId = bkngHallId;
    }

    public LocalDateTime getBkngEvntDt() {
        return bkngEvntDt;
    }

    public void setBkngEvntDt(LocalDateTime bkngEvntDt) {
        this.bkngEvntDt = bkngEvntDt;
    }

    public String getBkngFltNo() {
        return bkngFltNo;
    }

    public void setBkngFltNo(String bkngFltNo) {
        this.bkngFltNo = bkngFltNo;
    }

    public String getBkngPhnNo() {
        return bkngPhnNo;
    }

    public void setBkngPhnNo(String bkngPhnNo) {
        this.bkngPhnNo = bkngPhnNo;
    }

    public String getBkngPros() {
        return bkngPros;
    }

    public void setBkngPros(String bkngPros) {
        this.bkngPros = bkngPros;
    }

    public String getBkngExptGest() {
        return bkngExptGest;
    }

    public void setBkngExptGest(String bkngExptGest) {
        this.bkngExptGest = bkngExptGest;
    }

    public String getBkngType() {
        return bkngType;
    }

    public void setBkngType(String bkngType) {
        this.bkngType = bkngType;
    }

    public String getBkngSts() {
        return bkngSts;
    }

    public void setBkngSts(String bkngSts) {
        this.bkngSts = bkngSts;
    }

    public String getBkngAprvdBy() {
        return bkngAprvdBy;
    }

    public void setBkngAprvdBy(String bkngAprvdBy) {
        this.bkngAprvdBy = bkngAprvdBy;
    }

    public String getBkngCncldBy() {
        return bkngCncldBy;
    }

    public void setBkngCncldBy(String bkngCncldBy) {
        this.bkngCncldBy = bkngCncldBy;
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
//
//    public Transaction getTransaction() {
//        return transaction;
//    }
//
//    public void setTransaction(Transaction transaction) {
//        this.transaction = transaction;
//    }
    
    
    public Booking(BookingRequest request,String bookingId) {
        this.bkngId = bookingId;
        this.bkngDate = LocalDateTime.now();
        if(request.getGenericHeader() != null) {
            this.bkngBy = request.getGenericHeader().getUserId();
            this.creatUsrId = request.getGenericHeader().getUserId();
        }
        this.bkngHallId = request.getBookingHallId();
        if(request.getEventDate() != null) {
            this.bkngEvntDt = request.getEventDate()
            		.toLocalDate().atStartOfDay();
        }

        this.bkngFltNo = request.getFlatNo();
        this.bkngPhnNo = null;
        this.bkngPros = request.getBookingPurpose();
        this.bkngExptGest = request.getExpectedGuest();
        this.bkngType = request.getBookingyType();
        this.bkngSts = SecuraConstants.BOOKING_CONST_STATUS_REQUEST_RECEIVED;
        this.creatTs = LocalDateTime.now();
        this.lstUpdtTs = LocalDateTime.now();
    }

	public String getBkng_cncld_reason() {
		return bkng_cncld_reason;
	}

	public void setBkng_cncld_reason(String bkng_cncld_reason) {
		this.bkng_cncld_reason = bkng_cncld_reason;
	}
}