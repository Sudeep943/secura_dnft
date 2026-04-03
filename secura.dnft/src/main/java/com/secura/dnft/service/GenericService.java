package com.secura.dnft.service;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secura.dnft.dao.BookingRepository;
import com.secura.dnft.dao.ProfileRepository;
import com.secura.dnft.dao.WorklistRepository;
import com.secura.dnft.entity.Booking;
import com.secura.dnft.entity.Profile;
import com.secura.dnft.entity.Worklist;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.request.response.DashBordDataResponce;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.GetProfileRequest;


@Service
public class GenericService {

	@Autowired
	BookingRepository bookingRepository;
	
	@Autowired
	ProfileRepository profileRepository;
	
	@Autowired
	WorklistRepository worklistRepository;
	
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
	
	public DashBordDataResponce getDashBoardData(GenericHeader header) {
		DashBordDataResponce bordDataResponce= new DashBordDataResponce();
		List<Booking> upcomingBookings=getUpcomingHallBooking();
		long pendingCount=getPendingWorkListCount();
		bordDataResponce.setHeader(header);
		bordDataResponce.setPendingWorklistCount(pendingCount);
		bordDataResponce.setUpcomingBookings(upcomingBookings);
		GetProfileRequest getProfileRequest= new GetProfileRequest();
		getProfileRequest.setGenericHeader(header);
		getProfileRequest.setProfileID(header.getUserId());
		Optional<Profile> profile =profileRepository.findById(header.getUserId());
		if(profile.isPresent()) {
			bordDataResponce.setProfilePic(profile.get().getProfile_pic());	
		}
		
		return bordDataResponce;
	}
	
	public List<Booking> getUpcomingHallBooking() {
		List<Booking> upcomingBookings = bookingRepository
		        .findTop5ByBkngStsAndBkngEvntDtAfterOrderByBkngEvntDtAsc(SecuraConstants.BOOKING_CONST_STATUS_APPROVED,LocalDateTime.now());
		return upcomingBookings;
	}
	
	public long getPendingWorkListCount() {
		long pendingCount = worklistRepository.countByStatus(SecuraConstants.WORKLIST_STATUS_PENDING);
		return pendingCount;
	}
	
	public Worklist createWorklist(String worklistType,String createdBy, String apartmenId) {
		Worklist worklist = new Worklist();
		worklist.setStatus(SecuraConstants.WORKLIST_STATUS_PENDING);
		worklist.setWorklistsType(worklistType);
		worklist.setWorklistTaskId(createWorklistId(worklistType,createdBy));
		worklist.setCreatUsrId(createdBy);
		worklist.setCreatTs( LocalDateTime.now());
		worklist.setAprmtId(apartmenId);
		worklistRepository.save(worklist);
		return worklist;
	}
	
	public void canelWorklist(String worklistId) {
		Optional<Worklist> worklist=worklistRepository.findById(worklistId);
		if(worklist.isPresent()) {
			worklist.get().setStatus(SecuraConstants.WORKLIST_STATUS_CANCELLED);
			worklistRepository.save(worklist.get());
		}
	}
	
	public void getLatestPaymentsCredit() {}

	public String createWorklistId(String worklistType,String createdBy) {
		StringBuffer worklistId= new StringBuffer();
		worklistId.append(worklistType +"_");
		worklistId.append(createdBy +"_");
		Random random = new Random();
		worklistId.append( 1000 + random.nextInt(9000));
		return worklistId.toString();
	}
	
    public  <T> String toJson(T object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Error converting object to JSON", e);
        }
    }
    
    public  <T> T fromJson(String json, Class<T> clazz) {
    	if(null!=json) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Error converting JSON to object", e);
        }
    	}
    	else {
    		return null;
    	}
    }
    
    public <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (Exception e) {
            throw new RuntimeException("Error converting JSON to object", e);
        }
    }
    
    public String createDocumentId(String documentType, String documentFor) {
    	StringBuffer documentId= new StringBuffer();
    	documentId.append(documentType);
        documentId.append(documentFor);
        Random random = new Random();
        documentId.append(1000 + random.nextInt(9000));
		return documentId.toString().toUpperCase();
    }


public LocalDateTime getCorrectLocalDateForInputDate( Date inputDate) {
 	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	String formatted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(inputDate);
	 return LocalDateTime.parse(formatted, formatter);
    }

}
