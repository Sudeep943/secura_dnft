package com.secura.dnft.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
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
import com.secura.dnft.service.MeetingNoticeServices;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/meetingnotice")
public class MeetingNoticeController {

	@Autowired
	MeetingNoticeServices meetingNoticeServices;
	
	@PostMapping("/getNotice")
	@CrossOrigin(origins = "*")
	public GetNoticeResponse getNotice(@RequestBody GetNoticeRequest getNoticeRequest) {
		GetNoticeResponse response = new GetNoticeResponse();
		try {
			response=meetingNoticeServices.getNotice(getNoticeRequest);
		}
		catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
		
	};
	
	
	@PostMapping("/createNotice")
	@CrossOrigin(origins = "*")
	public CreateNoticeResponse createNotice(@RequestBody CreateNoticeRequest createNoticeRequest) {
		CreateNoticeResponse response = new CreateNoticeResponse();
		try {
			response=meetingNoticeServices.createNotice(createNoticeRequest);
		}
		catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
		
	};
	
	
	@PostMapping("/getLetterHead")
	@CrossOrigin(origins = "*")
	public GetLetterHeadResponse getLetterHead(GetLetterHeadRequest getLetterHeadRequest) {
		GetLetterHeadResponse response = new GetLetterHeadResponse();
		try {
			//response=meetingNoticeServices.getNotice(getNoticeRequest);
		}
		catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
		
	};
	
	@PostMapping("/updateNotice")
	@CrossOrigin(origins = "*")
	public UpdateNoticeResponse updateNotice(UpdateNoticeRequest profileRequest) {
		
		UpdateNoticeResponse response = new UpdateNoticeResponse();
		try {
		}
		catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	};
	
	@PostMapping("/scheduleMeeting")
	@CrossOrigin(origins = "*")
	public ScheduleEventResponse scheduleMeeting(ScheduleEventRequest meetingRequest) {
		ScheduleEventResponse response = new ScheduleEventResponse();
		try {
		}
		catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	};
	
	@PostMapping("/updateMeeting")
	@CrossOrigin(origins = "*")
	public UpdateEventRespopnse updateMeeting(UpdateEventRequest updateMeetingRequest) {
		UpdateEventRespopnse response = new UpdateEventRespopnse();
		try {
		}
		catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	};
	
	
	@PostMapping("/createMOM")
	@CrossOrigin(origins = "*")
	public CreateMOMResponse createMOM(CreateMOMRequest createMOMRequest) {
		CreateMOMResponse response = new CreateMOMResponse();
		try {
			//response=meetingNoticeServices.getNotice(getNoticeRequest);
		}
		catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	};
	
	@PostMapping("/updateMOM")
	@CrossOrigin(origins = "*")
	public UpdateMOMResponse updateMOM(UpdateMOMResponse updateMOMResponse) {
		UpdateMOMResponse response = new UpdateMOMResponse();
		try {
			//response=meetingNoticeServices.getNotice(getNoticeRequest);
		}
		catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	};
	
	
	
	
	
}
