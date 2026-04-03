package com.secura.dnft.request.response;

public class GetNoticeRequest {
	
	private GenericHeader genericHeader;
	private String noticeId;
	
	public GenericHeader getGenericHeader() {
		return genericHeader;
	}
	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}
	public String getNoticeId() {
		return noticeId;
	}
	public void setNoticeId(String noticeId) {
		this.noticeId = noticeId;
	}
	

	

}
