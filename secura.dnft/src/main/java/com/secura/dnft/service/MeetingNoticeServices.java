package com.secura.dnft.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.secura.dnft.dao.DocumentRepository;
import com.secura.dnft.dao.NoticeRepository;
import com.secura.dnft.entity.DocumentEntity;
import com.secura.dnft.entity.NoticeEntity;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.interfaceservice.MeetingNoticeInterface;
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

@Service
public class MeetingNoticeServices implements MeetingNoticeInterface{

	@Autowired
	GenericService genericService;
	
	@Autowired
	NoticeRepository noticeRepository;
	
	@Autowired
	DocumentRepository documentRepository;
	
	@Override
	public CreateNoticeResponse createNotice(CreateNoticeRequest createNoticeRequest) throws Exception{
		CreateNoticeResponse createNoticeResponse = new CreateNoticeResponse();
		createNoticeResponse.setGenericHeader(createNoticeRequest.getGenericHeader());
		String noticeId= createNoticeId(createNoticeRequest.getGenericHeader().getApartmentId());
		NoticeEntity entity = new NoticeEntity();
		entity.setNoticeId(noticeId);
		entity.setAprmtId(createNoticeRequest.getGenericHeader().getApartmentId());
        entity.setNoticeHeader(createNoticeRequest.getNoticeHeader());
        entity.setShortDetails(createNoticeRequest.getNoticeShortDescription());
        entity.setLetterNumber(createNoticeRequest.getLetterNumber());
        entity.setPublishingDate(genericService.getCorrectLocalDateForInputDate(createNoticeRequest.getPublishingDate()));
        String docId= genericService.createDocumentId(SecuraConstants.NOTICE_DOC_TYPE, SecuraConstants.NOTICE_DOC_TYPE);
        entity.setNoticeDocumentId(noticeId);
        if(createNoticeRequest.getOpeartion().equals(SecuraConstants.NOTICE_OPERATION_SAVE)) {
        	entity.setStatus("DRAFTED");
        }
        if(createNoticeRequest.getOpeartion().equals(SecuraConstants.NOTICE_OPERATION_PUBLISH)) {
        	entity.setStatus("PUBLISHED");
        }
        entity.setCreatUsrId(createNoticeRequest.getGenericHeader().getUserId());
        DocumentEntity documentEntity= new DocumentEntity();
        documentEntity.setDocumentId(docId);
        entity.setNoticeDocumentId(docId);
        documentEntity.setDocumentData(createNoticeRequest.getNoticeDoc());
        documentEntity.setDocumentType(SecuraConstants.NOTICE_DOC_FOR);
        documentEntity.setCreatUsrId(createNoticeRequest.getGenericHeader().getUserId());
        documentRepository.save(documentEntity);
        noticeRepository.save(entity);
        if(createNoticeRequest.getOpeartion().equals(SecuraConstants.NOTICE_OPERATION_SAVE)) {
        	createNoticeResponse.setMessage(SuccessMessage.SUCC_MESSAGE_20);
            createNoticeResponse.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_20 );	
        }
        else {
        createNoticeResponse.setMessage(SuccessMessage.SUCC_MESSAGE_19);
        createNoticeResponse.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_19 );
        }
        createNoticeResponse.setNoticeId(noticeId);
		return createNoticeResponse;
	}

	@Override
	public GetNoticeResponse getNotice(GetNoticeRequest getNoticeRequest)  throws Exception{
		List<NoticeEntity> noticeList=new ArrayList<>();
		if(null==getNoticeRequest.getNoticeId() || getNoticeRequest.getNoticeId().isEmpty()) {
			noticeList= noticeRepository.findAll();

		}
		else {
			noticeList.add(noticeRepository.findById(getNoticeRequest.getNoticeId()).get());
		}
		GetNoticeResponse reponse= new GetNoticeResponse();
		reponse.setGenericHeader(getNoticeRequest.getGenericHeader());
		if(null!=noticeList && !noticeList.isEmpty()) {
			noticeList=noticeList.stream().map(nt->{nt.setNoticeDocumentId(documentRepository.findById(nt.getNoticeDocumentId()).get().getDocumentData()); return nt;}).collect(Collectors.toList());
			reponse.setNoticeList(noticeList);
			reponse.setMessage(SuccessMessage.SUCC_MESSAGE_21);
			reponse.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_21);
		}
		else{
			reponse.setMessage(SuccessMessage.SUCC_MESSAGE_22);
			reponse.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_21);
		}
		return reponse;
	}

	@Override
	public GetLetterHeadResponse getLetterHead(GetLetterHeadRequest getLetterHeadRequest)throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UpdateNoticeResponse updateNotice(UpdateNoticeRequest request) throws Exception{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ScheduleMeetingResponse scheduleMeeting(ScheduleMeetingRequest meetingRequest) throws Exception{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UpdateMeetingResponse updateMeeting(UpdateMeetingRequest updateMeetingRequest) throws Exception{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CreateMOMResponse createMOM(CreateMOMRequest createMOMRequest)  throws Exception{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UpdateMOMResponse updateMOM(UpdateMOMResponse updateMOMResponse) throws Exception{
		// TODO Auto-generated method stub
		return null;
	}

	
	public String createNoticeId(String aprtId) {
		StringBuffer noticeId= new StringBuffer();
		noticeId.append(SecuraConstants.NOTICE_ID_PREFIX);
		SimpleDateFormat dateFormat = new SimpleDateFormat("DDmmmYY");
		String dateString= dateFormat.format(new Date());
		noticeId.append(dateString);
		noticeId.append(aprtId);
		Random random = new Random();
		noticeId.append(1000 + random.nextInt(9000));
		return noticeId.toString().toUpperCase();
	}
}
