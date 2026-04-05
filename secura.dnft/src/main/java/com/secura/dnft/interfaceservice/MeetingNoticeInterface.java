package com.secura.dnft.interfaceservice;

import com.secura.dnft.request.response.CreateMOMRequest;
import com.secura.dnft.request.response.CreateMOMResponse;
import com.secura.dnft.request.response.CreateNoticeRequest;
import com.secura.dnft.request.response.CreateNoticeResponse;
import com.secura.dnft.request.response.GetLetterHeadRequest;
import com.secura.dnft.request.response.GetLetterHeadResponse;
import com.secura.dnft.request.response.GetNoticeRequest;
import com.secura.dnft.request.response.GetNoticeResponse;
import com.secura.dnft.request.response.ScheduleEventRequest;
import com.secura.dnft.request.response.ScheduleEventResponse;
import com.secura.dnft.request.response.UpdateMOMResponse;
import com.secura.dnft.request.response.UpdateEventRequest;
import com.secura.dnft.request.response.UpdateEventRespopnse;
import com.secura.dnft.request.response.UpdateNoticeRequest;
import com.secura.dnft.request.response.UpdateNoticeResponse;

public interface MeetingNoticeInterface {

	

	public GetNoticeResponse getNotice(GetNoticeRequest getNoticeRequest) throws Exception;
	
	public CreateNoticeResponse createNotice(CreateNoticeRequest createNoticeRequest) throws Exception;
	
	public GetLetterHeadResponse getLetterHead(GetLetterHeadRequest getLetterHeadRequest)  throws Exception;
	
	public UpdateNoticeResponse updateNotice(UpdateNoticeRequest profileRequest) throws Exception;
	
	public ScheduleEventResponse scheduleEvent(ScheduleEventRequest meetingRequest) throws Exception;
	
	public UpdateEventRespopnse updateEvent(UpdateEventRequest updateMeetingRequest) throws Exception;
	
	public CreateMOMResponse createMOM(CreateMOMRequest createMOMRequest) throws Exception;
	
	public UpdateMOMResponse updateMOM(UpdateMOMResponse updateMOMResponse) throws Exception;
	
}
