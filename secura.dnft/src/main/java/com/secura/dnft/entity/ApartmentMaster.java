package com.secura.dnft.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "secura_aprmnt")
public class ApartmentMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "aprmnt_id")
    private String aprmntId;

    @Column(name = "aprmnt_name")
    private String aprmntName;

    @Column(name = "aprmnt_address")
    private String aprmntAddress;
    
    @Column(name = "creat_ts")
    private LocalDateTime  creat_ts;
    
    @Column(name = "lst_updt_ts")
    private LocalDateTime  lst_updt_ts;
    
    @Column(name = "creat_usr_id")
    private String creat_usr_id;
    
    @Column(name = "lst_updt_usrid")
    private String lst_updt_usrid;

	public String getAprmntId() {
		return aprmntId;
	}

	public void setAprmntId(String aprmntId) {
		this.aprmntId = aprmntId;
	}

	public String getAprmntName() {
		return aprmntName;
	}

	public void setAprmntName(String aprmntName) {
		this.aprmntName = aprmntName;
	}

	public String getAprmntAddress() {
		return aprmntAddress;
	}

	public void setAprmntAddress(String aprmntAddress) {
		this.aprmntAddress = aprmntAddress;
	}

	public LocalDateTime getCreat_ts() {
		return creat_ts;
	}

	public void setCreat_ts(LocalDateTime creat_ts) {
		this.creat_ts = creat_ts;
	}

	public LocalDateTime getLst_updt_ts() {
		return lst_updt_ts;
	}

	public void setLst_updt_ts(LocalDateTime lst_updt_ts) {
		this.lst_updt_ts = lst_updt_ts;
	}

	public String getCreat_usr_id() {
		return creat_usr_id;
	}

	public void setCreat_usr_id(String creat_usr_id) {
		this.creat_usr_id = creat_usr_id;
	}

	public String getLst_updt_usrid() {
		return lst_updt_usrid;
	}

	public void setLst_updt_usrid(String lst_updt_usrid) {
		this.lst_updt_usrid = lst_updt_usrid;
	}
    
    
    
    
}