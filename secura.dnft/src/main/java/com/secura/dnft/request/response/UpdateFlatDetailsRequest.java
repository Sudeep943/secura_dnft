package com.secura.dnft.request.response;

import java.sql.Date;

public class UpdateFlatDetailsRequest {

	private GenericHeader header;
	private String aprmntId;
	private String flatNo;
	private String flatOwnerList;
	private String flatTower;
	private String flatBlock;
	private Date flatPossnDate;
	private String flatOwnerType;
	private String flatArea;

	public GenericHeader getHeader() {
		return header;
	}

	public void setHeader(GenericHeader header) {
		this.header = header;
	}

	public String getAprmntId() {
		return aprmntId;
	}

	public void setAprmntId(String aprmntId) {
		this.aprmntId = aprmntId;
	}

	public String getFlatNo() {
		return flatNo;
	}

	public void setFlatNo(String flatNo) {
		this.flatNo = flatNo;
	}

	public String getFlatOwnerList() {
		return flatOwnerList;
	}

	public void setFlatOwnerList(String flatOwnerList) {
		this.flatOwnerList = flatOwnerList;
	}

	public String getFlatTower() {
		return flatTower;
	}

	public void setFlatTower(String flatTower) {
		this.flatTower = flatTower;
	}

	public String getFlatBlock() {
		return flatBlock;
	}

	public void setFlatBlock(String flatBlock) {
		this.flatBlock = flatBlock;
	}

	public Date getFlatPossnDate() {
		return flatPossnDate;
	}

	public void setFlatPossnDate(Date flatPossnDate) {
		this.flatPossnDate = flatPossnDate;
	}

	public String getFlatOwnerType() {
		return flatOwnerType;
	}

	public void setFlatOwnerType(String flatOwnerType) {
		this.flatOwnerType = flatOwnerType;
	}

	public String getFlatArea() {
		return flatArea;
	}

	public void setFlatArea(String flatArea) {
		this.flatArea = flatArea;
	}
}
