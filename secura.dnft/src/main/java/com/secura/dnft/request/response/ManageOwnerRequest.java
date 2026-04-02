package com.secura.dnft.request.response;

import java.sql.Date;
import java.util.List;

import com.secura.dnft.generic.bean.Document;

public class ManageOwnerRequest {
	private GenericHeader header;
	
	private String ownerId;
	private String status;
	private String flatId;
	private Date startDate;
	private Date endDate;
	private List<Document> listOfDocuments;
	
	public GenericHeader getHeader() {
		return header;
	}
	public void setHeader(GenericHeader header) {
		this.header = header;
	}
	public String getOwnerId() {
		return ownerId;
	}
	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}
	public String getFlatId() {
		return flatId;
	}
	public void setFlatId(String flatId) {
		this.flatId = flatId;
	}
	
	public List<Document> getListOfDocuments() {
		return listOfDocuments;
	}
	public void setListOfDocuments(List<Document> listOfDocuments) {
		this.listOfDocuments = listOfDocuments;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public Date getStartDate() {
		return startDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	public Date getEndDate() {
		return endDate;
	}
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
}
