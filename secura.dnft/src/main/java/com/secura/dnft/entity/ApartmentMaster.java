package com.secura.dnft.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "secura_aprmnt")
public class ApartmentMaster {

    @Id
    @Column(name = "aprmnt_id")
    private String aprmntId;

    @Column(name = "aprmnt_name")
    private String aprmntName;

    @Column(name = "aprmnt_address", columnDefinition = "TEXT")
    private String aprmntAddress;
    
    @Column(name = "aprmnt_bank_acccount_list", columnDefinition = "TEXT")
    private String aprmnt_bank_acccount_list;
    
    @Column(name = "aprmnt_executive_role_list", columnDefinition = "TEXT")
    private String aprmnt_executive_role_list;
    
    @Column(name = "aprmnt_logo", columnDefinition = "TEXT")
    private String aprmnt_logo;

    @Column(name = "aprmnt_letter_head", columnDefinition = "TEXT")
    private String aprmntLetterHead;
    
    @Column(name = "creat_ts")
    private LocalDateTime  creat_ts;
    
    @Column(name = "lst_updt_ts")
    private LocalDateTime  lst_updt_ts;
    
    @Column(name = "creat_usr_id")
    private String creat_usr_id;
    
    @Column(name = "lst_updt_usr_id")
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

	public String getAprmnt_bank_acccount_list() {
		return aprmnt_bank_acccount_list;
	}

	public void setAprmnt_bank_acccount_list(String aprmnt_bank_acccount_list) {
		this.aprmnt_bank_acccount_list = aprmnt_bank_acccount_list;
	}

	public String getAprmnt_executive_role_list() {
		return aprmnt_executive_role_list;
	}

	public void setAprmnt_executive_role_list(String aprmnt_executive_role_list) {
		this.aprmnt_executive_role_list = aprmnt_executive_role_list;
	}

	public String getAprmnt_logo() {
		return aprmnt_logo;
	}

	public void setAprmnt_logo(String aprmnt_logo) {
		this.aprmnt_logo = aprmnt_logo;
	}

	public String getAprmntLetterHead() {
		return aprmntLetterHead;
	}

	public void setAprmntLetterHead(String aprmntLetterHead) {
		this.aprmntLetterHead = aprmntLetterHead;
	}
     
     
     
     
}
