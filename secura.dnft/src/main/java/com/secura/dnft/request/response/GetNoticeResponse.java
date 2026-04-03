package com.secura.dnft.request.response;

import java.util.List;

import com.secura.dnft.entity.NoticeEntity;

public class GetNoticeResponse {
	private GenericHeader genericHeader;
	private String message;
    private String messageCode;
    private List<NoticeEntity> noticeList;
    
	public GenericHeader getGenericHeader() {
		return genericHeader;
	}
	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getMessageCode() {
		return messageCode;
	}
	public void setMessageCode(String messageCode) {
		this.messageCode = messageCode;
	}
	public List<NoticeEntity> getNoticeList() {
		return noticeList;
	}
	public void setNoticeList(List<NoticeEntity> noticeList) {
		this.noticeList = noticeList;
	}

    
}
