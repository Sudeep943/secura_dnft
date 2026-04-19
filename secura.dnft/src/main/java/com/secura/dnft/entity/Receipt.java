package com.secura.dnft.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "secura_receipt")
public class Receipt {

	@Column(name = "aprmt_id")
	private String aprmtId;

	@Id
	@Column(name = "receipt_id")
	private String receiptId;

	@Column(name = "receipt_date")
	private LocalDateTime receiptDate;

	@Column(name = "receipt_type")
	private String receiptType;

	@Column(name = "receipt_data", columnDefinition = "TEXT")
	private String receiptData;

	@Column(name = "creat_ts")
	private LocalDateTime creatTs;

	@Column(name = "creat_usr_id")
	private String creatUsrId;

	@Column(name = "lst_updt_ts")
	private LocalDateTime lstUpdtTs;

	@Column(name = "lst_updt_usrid")
	private String lstUpdtUsrId;

	public String getAprmtId() {
		return aprmtId;
	}

	public void setAprmtId(String aprmtId) {
		this.aprmtId = aprmtId;
	}

	public String getReceiptId() {
		return receiptId;
	}

	public void setReceiptId(String receiptId) {
		this.receiptId = receiptId;
	}

	public LocalDateTime getReceiptDate() {
		return receiptDate;
	}

	public void setReceiptDate(LocalDateTime receiptDate) {
		this.receiptDate = receiptDate;
	}

	public String getReceiptType() {
		return receiptType;
	}

	public void setReceiptType(String receiptType) {
		this.receiptType = receiptType;
	}

	public String getReceiptData() {
		return receiptData;
	}

	public void setReceiptData(String receiptData) {
		this.receiptData = receiptData;
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
