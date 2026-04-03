package com.secura.dnft.interfaceservice;

import com.secura.dnft.request.response.CreateMOMRequest;
import com.secura.dnft.request.response.CreateMOMResponse;
import com.secura.dnft.request.response.CreateNoticeRequest;
import com.secura.dnft.request.response.CreateNoticeResponse;
import com.secura.dnft.request.response.GetLetterHeadRequest;
import com.secura.dnft.request.response.GetLetterHeadResponse;
import com.secura.dnft.request.response.GetNoticeRequest;
import com.secura.dnft.request.response.GetNoticeResponse;
import com.secura.dnft.request.response.ScheduleMeetingRequest;
import com.secura.dnft.request.response.ScheduleMeetingResponse;
import com.secura.dnft.request.response.UpdateMOMResponse;
import com.secura.dnft.request.response.UpdateMeetingRequest;
import com.secura.dnft.request.response.UpdateMeetingResponse;
import com.secura.dnft.request.response.UpdateNoticeRequest;
import com.secura.dnft.request.response.UpdateNoticeResponse;

public interface MeetingNoticeInterface {

	

	public GetNoticeResponse getNotice(GetNoticeRequest getNoticeRequest) throws Exception;
	
	public CreateNoticeResponse createNotice(CreateNoticeRequest createNoticeRequest) throws Exception;
	
	public GetLetterHeadResponse getLetterHead(GetLetterHeadRequest getLetterHeadRequest)  throws Exception;
	
	public UpdateNoticeResponse updateNotice(UpdateNoticeRequest profileRequest) throws Exception;
	
	public ScheduleMeetingResponse scheduleMeeting(ScheduleMeetingRequest meetingRequest) throws Exception;
	
	public UpdateMeetingResponse updateMeeting(UpdateMeetingRequest updateMeetingRequest) throws Exception;
	
	public CreateMOMResponse createMOM(CreateMOMRequest createMOMRequest) throws Exception;
	
	public UpdateMOMResponse updateMOM(UpdateMOMResponse updateMOMResponse) throws Exception;
	
}
