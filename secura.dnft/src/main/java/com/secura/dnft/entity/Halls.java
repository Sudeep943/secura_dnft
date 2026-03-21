package com.secura.dnft.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "secura_halls")
public class Halls {

    @Id
    @Column(name = "hall_id")
    private String hallId;

    @Column(name = "hall_name")
    private String hallName;

    @Column(name = "hall_location")
    private String hallLocation;

    @Column(name = "hall_area")
    private String hallArea;

    @Column(name = "hall_amount")
    private String hallAmount;

    @Column(name = "hall_sop")
    private String hallSop;

    @Column(name = "hall_status")
    private String hallStatus;

    @Column(name = "hall_images")
    private String hallImages;

    @Column(name = "aprmnt_id")
    private String aprmntId;

    @Column(name = "creat_ts")
    private Timestamp creatTs;

    @Column(name = "creat_usr_id")
    private String creatUsrId;

    @Column(name = "lst_updt_ts")
    private Timestamp lstUpdtTs;

    @Column(name = "lst_updt_usrId")
    private String lstUpdtUsrId;

    // Constructors
    public Halls() {}

    // Getters and Setters

    public String getHallId() {
        return hallId;
    }

    public void setHallId(String hallId) {
        this.hallId = hallId;
    }

    public String getHallName() {
        return hallName;
    }

    public void setHallName(String hallName) {
        this.hallName = hallName;
    }

    public String getHallLocation() {
        return hallLocation;
    }

    public void setHallLocation(String hallLocation) {
        this.hallLocation = hallLocation;
    }

    public String getHallArea() {
        return hallArea;
    }

    public void setHallArea(String hallArea) {
        this.hallArea = hallArea;
    }

    public String getHallAmount() {
        return hallAmount;
    }

    public void setHallAmount(String hallAmount) {
        this.hallAmount = hallAmount;
    }

    public String getHallSop() {
        return hallSop;
    }

    public void setHallSop(String hallSop) {
        this.hallSop = hallSop;
    }

    public String getHallStatus() {
        return hallStatus;
    }

    public void setHallStatus(String hallStatus) {
        this.hallStatus = hallStatus;
    }

    public String getHallImages() {
        return hallImages;
    }

    public void setHallImages(String hallImages) {
        this.hallImages = hallImages;
    }

    public String getAprmntId() {
        return aprmntId;
    }

    public void setAprmntId(String aprmntId) {
        this.aprmntId = aprmntId;
    }

    public Timestamp getCreatTs() {
        return creatTs;
    }

    public void setCreatTs(Timestamp creatTs) {
        this.creatTs = creatTs;
    }

    public Timestamp getLstUpdtTs() {
        return lstUpdtTs;
    }

    public void setLstUpdtTs(Timestamp lstUpdtTs) {
        this.lstUpdtTs = lstUpdtTs;
    }

    public String getLstUpdtUsrId() {
        return lstUpdtUsrId;
    }

    public void setLstUpdtUsrId(String lstUpdtUsrId) {
        this.lstUpdtUsrId = lstUpdtUsrId;
    }
}