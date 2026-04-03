package com.secura.dnft.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
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
import com.secura.dnft.request.response.ScheduleMeetingRequest;
import com.secura.dnft.request.response.ScheduleMeetingResponse;
import com.secura.dnft.request.response.UpdateMOMResponse;
import com.secura.dnft.request.response.UpdateMeetingRequest;
import com.secura.dnft.request.response.UpdateMeetingResponse;
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
	public GetNoticeResponse getNotice(GetNoticeRequest getNoticeRequest) {
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
	public CreateNoticeResponse createNotice(CreateNoticeRequest createNoticeRequest) {
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
	public ScheduleMeetingResponse scheduleMeeting(ScheduleMeetingRequest meetingRequest) {
		ScheduleMeetingResponse response = new ScheduleMeetingResponse();
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
	public UpdateMeetingResponse updateMeeting(UpdateMeetingRequest updateMeetingRequest) {
		UpdateMeetingResponse response = new UpdateMeetingResponse();
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
