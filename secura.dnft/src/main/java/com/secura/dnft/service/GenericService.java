package com.secura.dnft.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.secura.dnft.bean.WorkListAssignment;
import com.secura.dnft.dao.BookingRepository;
import com.secura.dnft.dao.OtpRepository;
import com.secura.dnft.dao.ProfileRepository;
import com.secura.dnft.dao.WorklistRepository;
import com.secura.dnft.entity.Booking;
import com.secura.dnft.entity.Profile;
import com.secura.dnft.entity.SecuraOtp;
import com.secura.dnft.entity.Worklist;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.util.OtpEmailUtility;
import com.secura.dnft.request.response.CreateOtpResponse;
import com.secura.dnft.request.response.DashBordDataResponce;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.GetProfileRequest;
import com.secura.dnft.security.BusinessException;

import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityNotFoundException;


@Service
public class GenericService {

	private static final Logger logger = LoggerFactory.getLogger(GenericService.class);

	private static final int OTP_MAX_RESEND_LIMIT = 3;
	private static final int OTP_MAX_ATTEMPT_LIMIT = 3;
	private static final int OTP_EXPIRY_MINUTES = 5;
	private static final int OTP_CLEANUP_HOURS = 4;

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	@Autowired
	BookingRepository bookingRepository;
	
	@Autowired
	ProfileRepository profileRepository;
	
	@Autowired
	WorklistRepository worklistRepository;

	@Autowired
	DataPrivacyService dataPrivacyService;

	@Autowired
	OtpRepository otpRepository;

	@Autowired
	JavaMailSender mailSender;

	@Value("${spring.mail.username}")
	private String senderEmail;
	
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

		 if (email == null || email.isBlank()) {
	            return email;
	        }

	        int atIndex = email.indexOf('@');

	        // Invalid email
	        if (atIndex <= 0) {
	            return email;
	        }

	        String username = email.substring(0, atIndex);
	        String domain = email.substring(atIndex);

	        int length = username.length();

	        String maskedUsername;

	        if (length <= 1) {
	            // a@gmail.com
	            maskedUsername = username;
	        } else if (length < 4) {
	            // Keep all except last character before @
	            // as -> a*
	            // asl -> as*
	            maskedUsername = username.substring(0, length - 1) + "*";
	        } else {
	            // Keep first 3 chars and last char
	            // Mask everything in between
	            StringBuilder sb = new StringBuilder();
	            sb.append(username, 0, 3);

	            for (int i = 0; i < length - 4; i++) {
	                sb.append('*');
	            }

	            sb.append(username.charAt(length - 1));
	            maskedUsername = sb.toString();
	        }

	        return maskedUsername + domain;
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

	// -------------------------------------------------------------------------
	// OTP Methods
	// -------------------------------------------------------------------------

	/**
	 * Generates a single 6-digit OTP, hashes it, persists one OTP record per userId,
	 * and sends the same OTP to the email address associated with each userId in the list.
	 *
	 * @param sessionId  the current session identifier
	 * @param userIdList the list of profile/user identifiers to send the OTP to
	 * @throws BusinessException if the resend limit (3) has been reached for any userId
	 */
	public CreateOtpResponse createOTP(String sessionId, List<String> userIdList, boolean sendMobile, boolean sendMail) throws BusinessException {
		logger.info("createOTP: initiated for userIdList={}, sessionId={}", userIdList, sessionId);

		String rawOtp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
		String hashedOtp = hashOtp(rawOtp);
		LocalDateTime now = LocalDateTime.now();

		// A single shared otpId is generated for this OTP request so that all
		// recipients receive – and can reference – the same identifier.
		String sharedOtpId = generateAlphanumericId(6);
		List<String> maskedMailIds = new ArrayList<>();

		for (String userId : userIdList) {
			if (userId == null || userId.isBlank()) {
				continue;
			}

			List<SecuraOtp> existingOtps = otpRepository.findByUserId(userId);
			long activeOtpCount = existingOtps.stream()
					.filter(o -> o.getExpiryAt().isAfter(LocalDateTime.now()))
					.count();
			if (activeOtpCount >= OTP_MAX_RESEND_LIMIT) {
				logger.warn("createOTP: resend limit reached for userId={}", userId);
				throw new BusinessException(ErrorMessage.ERR_MESSAGE_57, ErrorMessageCode.ERR_MESSAGE_57);
			}

			SecuraOtp otpEntry = new SecuraOtp();
			otpEntry.setOtpId(sharedOtpId);
			otpEntry.setSessionId(sessionId);
			otpEntry.setUserId(userId);
			otpEntry.setOtpHash(hashedOtp);
			otpEntry.setAttempts(0);
			otpEntry.setCreatedAt(now);
			otpEntry.setExpiryAt(now.plusMinutes(OTP_EXPIRY_MINUTES));
			otpRepository.save(otpEntry);
			logger.info("createOTP: OTP record saved for userId={}, otpId={}", userId, sharedOtpId);

			if (sendMail) {
				Profile profile = getProfileEntity(userId);
				String emailId = profile.getPrflEmailAdrss();
				if (emailId == null || emailId.isBlank()) {
					logger.error("createOTP: no email address found for userId={}", userId);
					otpRepository.delete(otpEntry);
					throw new BusinessException("No email address registered for this user. Please contact your society administrator.",
							ErrorMessageCode.ERR_MESSAGE_28);
				}
				try {
					sendOtpEmail(emailId, rawOtp, sharedOtpId);
					logger.info("createOTP: OTP email dispatched to {} for userId={}, otpId={}", maskEmail(emailId), userId, sharedOtpId);
					maskedMailIds.add(maskEmail(emailId));
				} catch (Exception e) {
					logger.error("createOTP: email dispatch failed for userId={}, rolling back OTP record", userId, e);
					otpRepository.delete(otpEntry);
					throw new BusinessException("Failed to send OTP email. Please try again.", ErrorMessageCode.ERR_MESSAGE_33);
				}
			}
		}

		CreateOtpResponse response = new CreateOtpResponse();
		response.setOtpId(sharedOtpId);
		if (!maskedMailIds.isEmpty()) {
			response.setMailIds(maskedMailIds);
			response.setMessage("Please Enter OTP sent to Email id(s): " + String.join(", ", maskedMailIds));
		}
		return response;
	}

	/**
	 * Validates the supplied OTP against the latest stored entry for the given session.
	 * <ol>
	 *   <li>Rejects if the attempt count has already reached the limit (3).</li>
	 *   <li>Rejects if the latest OTP has expired.</li>
	 *   <li>Compares the SHA-256 hash of the supplied OTP with the latest stored hash.</li>
	 *   <li>On match: deletes all OTP records for the session and returns {@code true}.</li>
	 *   <li>On mismatch: increments the attempt counter and throws a {@link BusinessException}.</li>
	 * </ol>
	 *
	 * @param sessionId the session identifier used when the OTP was created
	 * @param otp       the raw 6-digit OTP entered by the user
	 * @return {@code true} when the OTP is valid
	 * @throws BusinessException on attempt-limit breach, expiry, or OTP mismatch
	 */
	public boolean validateOTP(String sessionId, String otpId, String otp) throws BusinessException {
		logger.info("validateOTP: initiated for sessionId={}, otpId={}", sessionId, otpId);

		List<SecuraOtp> otpEntries = otpRepository.findByOtpIdOrderByCreatedAtDesc(otpId);
		if (otpEntries == null || otpEntries.isEmpty()) {
			logger.warn("validateOTP: no OTP record found for sessionId={}, otpId={}", sessionId, otpId);
			throw new BusinessException("No active OTP found for this session. Please request a new OTP.",
					ErrorMessageCode.ERR_MESSAGE_59);
		}

		SecuraOtp latestOtp = otpEntries.get(0);

		if (latestOtp.getAttempts() >= OTP_MAX_ATTEMPT_LIMIT) {
			logger.warn("validateOTP: attempt limit reached for sessionId={}, otpId={}", sessionId, otpId);
			throw new BusinessException(ErrorMessage.ERR_MESSAGE_58, ErrorMessageCode.ERR_MESSAGE_58);
		}

		if (latestOtp.getExpiryAt().isBefore(LocalDateTime.now())) {
			logger.warn("validateOTP: OTP has expired for sessionId={}, otpId={}", sessionId, otpId);
			otpRepository.deleteAll(otpEntries);
			throw new BusinessException("The OTP has expired. Please request a new OTP.",
					ErrorMessageCode.ERR_MESSAGE_59);
		}

		String hashedInput = hashOtp(otp);
		if (hashedInput.equals(latestOtp.getOtpHash())) {
			otpRepository.deleteAll(otpEntries);
			logger.info("validateOTP: OTP matched, all matching OTP records deleted for sessionId={}, otpId={}", sessionId, otpId);
			return true;
		} else {
			latestOtp.setAttempts(latestOtp.getAttempts() + 1);
			otpRepository.save(latestOtp);
			logger.warn("validateOTP: OTP mismatch for sessionId={}, otpId={}, attempts now={}", sessionId, otpId, latestOtp.getAttempts());
			throw new BusinessException(ErrorMessage.ERR_MESSAGE_59, ErrorMessageCode.ERR_MESSAGE_59);
		}
	}

	/**
	 * Scheduled job that runs every 5 minutes and purges OTP records whose
	 * {@code expiry_at} timestamp is more than {@value #OTP_CLEANUP_HOURS} hours in the past.
	 */
	//@Scheduled(cron = "0 */5 * * * *")
	public void deleteExpiredOTP() {
		logger.info("deleteExpiredOTP: scheduled cleanup started");
		try {
			LocalDateTime cutoff = LocalDateTime.now().minusHours(OTP_CLEANUP_HOURS);
			List<SecuraOtp> expiredOtps = otpRepository.findByExpiryAtBefore(cutoff);
			if (!expiredOtps.isEmpty()) {
				otpRepository.deleteAll(expiredOtps);
				logger.info("deleteExpiredOTP: deleted {} expired OTP record(s)", expiredOtps.size());
			} else {
				logger.info("deleteExpiredOTP: no expired OTP records found");
			}
		} catch (Exception e) {
			logger.error("deleteExpiredOTP: error during cleanup", e);
		}
		logger.info("deleteExpiredOTP: scheduled cleanup completed");
	}

	private static final String ALPHANUMERIC_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

	private String generateAlphanumericId(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(ALPHANUMERIC_CHARS.charAt(SECURE_RANDOM.nextInt(ALPHANUMERIC_CHARS.length())));
		}
		return sb.toString();
	}

	private String hashOtp(String otp) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hashBytes);
		} catch (Exception e) {
			throw new RuntimeException("SHA-256 algorithm not available on this platform", e);
		}
	}

	private void sendOtpEmail(String toEmail, String otp, String otpId) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setFrom(senderEmail);
			helper.setTo(toEmail);
			helper.setSubject("Your Secura Verification OTP");
			String htmlBody = OtpEmailUtility.buildOtpEmailBody(otp, otpId);
			helper.setText(htmlBody, true);
			mailSender.send(message);
		} catch (Exception e) {
			logger.error("sendOtpEmail: failed to send OTP email to {}", maskEmail(toEmail), e);
			throw new RuntimeException("Failed to dispatch OTP email. Please try again.", e);
		}
	}

}
