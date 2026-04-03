package com.secura.dnft.request.response;

import java.sql.Date;

public class CreateNoticeRequest {
	private GenericHeader genericHeader;
	private String noticeShortDescription;
	private String noticeHeader;
	private Date publishingDate;
	private String letterNumber;
	private String noticeDoc;
	private String opeartion;
	
	public GenericHeader getGenericHeader() {
		return genericHeader;
	}
	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}
	public String getNoticeShortDescription() {
		return noticeShortDescription;
	}
	public void setNoticeShortDescription(String noticeShortDescription) {
		this.noticeShortDescription = noticeShortDescription;
	}
	public String getNoticeHeader() {
		return noticeHeader;
	}
	public void setNoticeHeader(String noticeHeader) {
		this.noticeHeader = noticeHeader;
	}
	public Date getPublishingDate() {
		return publishingDate;
	}
	public void setPublishingDate(Date publishingDate) {
		this.publishingDate = publishingDate;
	}
	public String getLetterNumber() {
		return letterNumber;
	}
	public void setLetterNumber(String letterNumber) {
		this.letterNumber = letterNumber;
	}
	public String getNoticeDoc() {
		return noticeDoc;
	}
	public void setNoticeDoc(String noticeDoc) {
		this.noticeDoc = noticeDoc;
	}
	public String getOpeartion() {
		return opeartion;
	}
	public void setOpeartion(String opeartion) {
		this.opeartion = opeartion;
	}



}
