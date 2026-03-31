package com.secura.dnft.request.response;

import java.sql.Date;
import java.util.List;

import com.secura.dnft.generic.bean.Document;

public class ManageTenantRequest {
	private GenericHeader header;
	
	private String tenantId;
	private String status;
	private String flatId;
	private Date startDate;
	private Date endDate;
	private boolean verified;
	private List<Document> listOfDocuments;
	
	public boolean isVerified() {
		return verified;
	}
	public void setVerified(boolean verified) {
		this.verified = verified;
	}
	public GenericHeader getHeader() {
		return header;
	}
	public void setHeader(GenericHeader header) {
		this.header = header;
	}
	public String getTenantId() {
		return tenantId;
	}
	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
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
