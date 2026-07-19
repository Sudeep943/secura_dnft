package com.secura.dnft.service;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.secura.dnft.bean.WorkListAssignment;
import com.secura.dnft.dao.BookingRepository;
import com.secura.dnft.dao.ProfileRepository;
import com.secura.dnft.dao.WorklistRepository;
import com.secura.dnft.entity.Booking;
import com.secura.dnft.entity.Profile;
import com.secura.dnft.entity.Worklist;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.request.response.DashBordDataResponce;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.GetProfileRequest;
import com.secura.dnft.security.BusinessException;

import jakarta.persistence.EntityNotFoundException;


@Service
public class GenericService {

	@Autowired
	BookingRepository bookingRepository;
	
	@Autowired
	ProfileRepository profileRepository;
	
	@Autowired
	WorklistRepository worklistRepository;

	@Autowired
	DataPrivacyService dataPrivacyService;
	
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.findAndRegisterModules();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
	
	public DashBordDataResponce getDashBoardData(GenericHeader header) throws BusinessException {
		DashBordDataResponce bordDataResponce= new DashBordDataResponce();
		List<Booking> upcomingBookings=getUpcomingHallBooking();
		long pendingCount=getPendingWorkListCount();
		bordDataResponce.setHeader(header);
		bordDataResponce.setPendingWorklistCount(pendingCount);
		bordDataResponce.setUpcomingBookings(upcomingBookings);
		GetProfileRequest getProfileRequest= new GetProfileRequest();
		getProfileRequest.setGenericHeader(header);
		getProfileRequest.setProfileID(header.getUserId());
		//Optional<Profile> profile =profileRepository.findById(header.getUserId());
		Optional<Profile> profile=Optional.of(getProfileEntity(header.getUserId()));
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
	
	public Worklist createWorklist(String worklistType,String createdBy, String apartmenId,String refferenceID) {
		Worklist worklist = new Worklist();
		worklist.setStatus(SecuraConstants.WORKLIST_STATUS_PENDING);
		worklist.setWorklistType(worklistType);
		worklist.setWorklistId(createWorklistId(worklistType,createdBy));
		worklist.setCreatUsrId(createdBy);
		worklist.setCreatTs( LocalDateTime.now());
		worklist.setApartmentId(apartmenId);
		worklist.setReferenceId(refferenceID);
		worklist.setCurrentAssignee(createdBy);
		worklistRepository.save(worklist);
		return worklist;
	}
	
	public void cancelWorklist(String worklistId) {
		Optional<Worklist> worklist=worklistRepository.findById(worklistId);
		if(worklist.isPresent()) {
			worklist.get().setStatus(SecuraConstants.WORKLIST_STATUS_CANCELLED);
			worklistRepository.save(worklist.get());
		}
	}

	public Worklist createWorklistAssignmentFlow(String workListId, List<String> listOfProfileIDs) {
		Worklist worklist = getWorklistById(workListId);
		List<WorkListAssignment> workListAssignments = new ArrayList<>();
		workListAssignments.add(buildWorkListAssignment(listOfProfileIDs, "new"));
		worklist.setWorklistsAssignFlow(toJson(workListAssignments));
		worklistRepository.save(worklist);
		return worklist;
	}

	public Worklist reassignWorklistFlowService(String worklistId, String newAssignee, String currentAssignee) {
		Worklist worklist = getWorklistById(worklistId);
		List<WorkListAssignment> workListAssignments = getWorkListAssignments(worklist.getWorklistsAssignFlow());
		WorkListAssignment activeAssignment = workListAssignments.stream()
				.filter(assignment -> SecuraConstants.WORKLIST_ASSIGNMENT_STATUS_ACTIVE.equals(assignment.getCurrentStatus()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("No Active WorkList Assignment Found"));
		if (activeAssignment.getAssignedPersonList() == null
				|| !activeAssignment.getAssignedPersonList().contains(currentAssignee)) {
			throw new IllegalArgumentException("You Are Not Allowed To Reassign");
		}
		activeAssignment.setCompletedDate(Date.valueOf(LocalDate.now()));
		activeAssignment.setCurrentStatus(SecuraConstants.WORKLIST_ASSIGNMENT_STATUS_TRANSFERRED);
		workListAssignments.add(buildWorkListAssignment(List.of(newAssignee), currentAssignee));
		worklist.setWorklistsAssignFlow(toJson(workListAssignments));
		worklistRepository.save(worklist);
		return worklist;
	}
	
	public void getLatestPaymentsCredit() {}

	public String createWorklistId(String worklistType,String createdBy) {
		StringBuilder worklistId = new StringBuilder("WRK");
		worklistId.append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
		worklistId.append(1000 + ThreadLocalRandom.current().nextInt(9000));
		return worklistId.toString().toUpperCase();
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
		documentId.append(1000 + ThreadLocalRandom.current().nextInt(9000));
		return documentId.toString().toUpperCase();
	}

	public String encrypt(String value) {
		if (value == null) {
			return null;
		}
		try {
			return dataPrivacyService.encrypt(value);
		} catch (Exception e) {
			throw new RuntimeException("Error encrypting data", e);
		}
	}

	public String decrypt(String value) {
		if (value == null) {
			return null;
		}
		try {
			return dataPrivacyService.decrypt(value);
		} catch (Exception e) {
			throw new RuntimeException("Error decrypting data", e);
		}
	}


public LocalDateTime getCorrectLocalDateForInputDate( Date inputDate) {
 	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	String formatted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(inputDate);
	 return LocalDateTime.parse(formatted, formatter);
    }

	private Worklist getWorklistById(String workListId) {
		return worklistRepository.findById(workListId)
				.orElseThrow(() -> new EntityNotFoundException("Worklist not found"));
	}

	private WorkListAssignment buildWorkListAssignment(List<String> assignedPersonList, String assignedBy) {
		WorkListAssignment workListAssignment = new WorkListAssignment();
		workListAssignment.setAssignmentDate(Date.valueOf(LocalDate.now()));
		workListAssignment.setAssignedPersonList(new ArrayList<>(assignedPersonList));
		workListAssignment.setCurrentStatus(SecuraConstants.WORKLIST_ASSIGNMENT_STATUS_ACTIVE);
		workListAssignment.setAssignedBy(assignedBy);
		return workListAssignment;
	}

	private List<WorkListAssignment> getWorkListAssignments(String worklistsAssignFlow) {
		if (worklistsAssignFlow == null || worklistsAssignFlow.isBlank()) {
			return new ArrayList<>();
		}
		List<WorkListAssignment> workListAssignments = fromJson(worklistsAssignFlow,
				new TypeReference<List<WorkListAssignment>>() {
				});
		return workListAssignments == null ? new ArrayList<>() : workListAssignments;
	}
	
	public Profile getProfileEntity(String id) throws BusinessException {
		Optional<Profile> profile = java.util.Optional.empty();
		Optional<Profile> prfl = profileRepository.findById(id);
		if (prfl.isEmpty()) {
			List<Profile> profileByphoneList=profileRepository.findByPrflPhoneNo(id);
			if(null!=profileByphoneList && !profileByphoneList.isEmpty()) {
				Profile profileByphone=profileByphoneList.get(0);
				profile = Optional.ofNullable(profileByphone);
				}
			else {
					throw new BusinessException(ErrorMessage.ERR_MESSAGE_55, ErrorMessageCode.ERR_MESSAGE_55);
			}
			
		} else {
			profile = prfl;
		}
		return profile.get();
		
	}
	
	public static String maskEmail(String email) {

	    if (email == null || email.trim().isEmpty()) {
	        return email;
	    }

	    int atIndex = email.indexOf('@');

	    if (atIndex <= 0 || atIndex == email.length() - 1) {
	        return email;
	    }

	    String localPart = email.substring(0, atIndex);
	    String domain = email.substring(atIndex);

	    // If local part is too short, don't mask
	    if (localPart.length() <= 6) {
	        return email;
	    }

	    String firstPart = localPart.substring(0, 3);
	    String middlePart = localPart.substring(6, 9);

	    StringBuilder masked = new StringBuilder();
	    masked.append(firstPart);
	    masked.append("***");
	    masked.append(middlePart);

	    for (int i = 9; i < localPart.length(); i++) {
	        masked.append('*');
	    }

	    return masked + domain;
	}
	
	  public String maskPhoneNumber(String phoneNumber) {

	        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
	            return phoneNumber;
	        }

	        phoneNumber = phoneNumber.trim();

	        if (phoneNumber.length() <= 4) {
	            return phoneNumber;
	        }

	        StringBuilder masked = new StringBuilder();

	        for (int i = 0; i < phoneNumber.length() - 4; i++) {
	            masked.append('*');
	        }

	        masked.append(phoneNumber.substring(phoneNumber.length() - 4));

	        return masked.toString();
	    }

}
