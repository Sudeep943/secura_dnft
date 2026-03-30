package com.secura.dnft.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.secura.dnft.dao.BookingRepository;
import com.secura.dnft.dao.HallRepository;
import com.secura.dnft.dao.ProfileRepository;
import com.secura.dnft.entity.Booking;
import com.secura.dnft.entity.Halls;
import com.secura.dnft.entity.Profile;
import com.secura.dnft.entity.Worklist;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.BookingRequest;
import com.secura.dnft.request.response.BookingResponse;
import com.secura.dnft.request.response.UpdateBookingRequest;
import com.secura.dnft.request.response.UpdateBookingResponse;
import com.secura.dnft.request.response.CheckHallAvailablityRequest;
import com.secura.dnft.request.response.CheckHallAvailablityResponse;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.GetBookingRequest;
import com.secura.dnft.request.response.GetBookingResponse;
import com.secura.dnft.request.response.GetHallsReponse;
import com.secura.dnft.security.BusinessException;
import com.secura.dnft.validation.BookingServiceValidation;

import jakarta.persistence.EntityNotFoundException;

@Service
public class BookingService {

	 @Autowired
	 private BookingRepository bookingRepository;
	 
	 @Autowired
	 private ProfileRepository profileRepository;
	 
	 @Autowired
	 HallRepository hallRepository;
	 
	 @Autowired
	 GenericService genericService;
	 
	 @Autowired
	 BookingServiceValidation bookingServiceValidation;
	 
	public CheckHallAvailablityResponse checkAvailabilityOfHall(CheckHallAvailablityRequest checkHallAvailablityRequest) {
		CheckHallAvailablityResponse checkHallAvailablityResponse= new CheckHallAvailablityResponse();
		checkHallAvailablityResponse.setEventDate(checkHallAvailablityRequest.getEventDate());
		checkHallAvailablityResponse.setHallId(checkHallAvailablityRequest.getHallId());
		checkHallAvailablityResponse.setGenericHeader(checkHallAvailablityRequest.getGenericHeader());
		checkHallAvailablityResponse.setAvailable(true);
		checkHallAvailablityResponse.setMessage(SuccessMessage.SUCC_MESSAGE_01);
		checkHallAvailablityResponse.setMessage_code(SuccessMessageCode.SUCC_MESSAGE_01);
		try {
			bookingServiceValidation.validateCheckAvailablityOfHallRequest(checkHallAvailablityRequest);
		
		Halls hall=hallRepository.findById(checkHallAvailablityRequest.getHallId()).orElse(null);
		
		if(null==hall) {
			checkHallAvailablityResponse.setAvailable(false);
			checkHallAvailablityResponse.setMessage_code(ErrorMessageCode.ERR_MESSAGE_02);
			checkHallAvailablityResponse.setMessage(ErrorMessage.ERR_MESSAGE_02);
			return checkHallAvailablityResponse;
					}
		
		
		if(null!=hall && hall.getHallStatus().equals("D")) {
			checkHallAvailablityResponse.setAvailable(false);
			checkHallAvailablityResponse.setMessage_code(ErrorMessageCode.ERR_MESSAGE_01);
			checkHallAvailablityResponse.setMessage(ErrorMessage.ERR_MESSAGE_01);
			return checkHallAvailablityResponse;
					}
		long countOfEntry=bookingRepository.countBookings(checkHallAvailablityRequest.getHallId(), checkHallAvailablityRequest.getEventDate().toLocalDate().atStartOfDay());
		 if(countOfEntry>0) {
			 checkHallAvailablityResponse.setAvailable(false);
			 checkHallAvailablityResponse.setMessage_code(ErrorMessageCode.ERR_MESSAGE_01);
			 checkHallAvailablityResponse.setMessage(ErrorMessage.ERR_MESSAGE_01);
		 }
		} catch (BusinessException be) {
			checkHallAvailablityResponse.setMessage(be.getErrorMessage());
			checkHallAvailablityResponse.setMessage_code(be.getErrorMessageCode());
		}
		return checkHallAvailablityResponse;
		
	}

	
	public BookingResponse createBooking(BookingRequest bookingRequest) {
		BookingResponse bookingResponse = new BookingResponse();
		bookingResponse.setGenericHeader(bookingRequest.getGenericHeader());
		try {
			CheckHallAvailablityRequest checkHallAvailablityRequest = new CheckHallAvailablityRequest();
			checkHallAvailablityRequest.setGenericHeader(bookingRequest.getGenericHeader());
			checkHallAvailablityRequest.setEventDate(bookingRequest.getEventDate());
			checkHallAvailablityRequest.setHallId(bookingRequest.getBookingHallId());
			boolean available=checkAvailabilityOfHall(checkHallAvailablityRequest).isAvailable();
			Optional<Profile> profile = profileRepository.findById(bookingRequest.getGenericHeader().getUserId());
			String mobileNumber= null;
			if(profile.isPresent()) {
				mobileNumber=profile.get().getPrflPhoneNo();
			}
			
			if(available) {
				bookingServiceValidation.validateBookingRequest(bookingRequest);
				String bookingid=createBookingID(bookingRequest);
				Worklist worklist=genericService.createWorklist(SecuraConstants.WORKLIST_TYPE_BOOKING, bookingRequest.getGenericHeader().getUserId(),bookingRequest.getGenericHeader().getApartmentId());
				Booking bookingEnitity= new Booking(bookingRequest,bookingid,worklist.getWorklistTaskId());
				bookingEnitity.setBkngPhnNo(mobileNumber);
				bookingRepository.save(bookingEnitity);
				bookingResponse.setBookingId(bookingid);
				bookingResponse.setBookingStatus(SecuraConstants.BOOKING_CONST_STATUS_REQUEST_RECEIVED);
				bookingResponse.setMessage(SuccessMessage.SUCC_MESSAGE_03);
				bookingResponse.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_03);
				bookingResponse.setWorkLists(worklist.getWorklistTaskId());
			}
			else {
				throw new BusinessException(ErrorMessage.ERR_MESSAGE_01,ErrorMessageCode.ERR_MESSAGE_01);
			}
		}
		catch (BusinessException be) {
			bookingResponse.setMessage(be.getErrorMessage());
			bookingResponse.setMessageCode(be.getErrorMessageCode());
		}
		catch (Exception be) {
			bookingResponse.setMessage(be.getMessage());
		}
		
		return bookingResponse;
	}
	
	private String createBookingID(BookingRequest bookingRequest) {
		StringBuffer bookingId= new StringBuffer();
		if(bookingRequest.getBookingHallId().startsWith("BAN")) {
			bookingId.append("BAN");
		}
		else {
			bookingId.append("ACT");
		}
		bookingId.append(bookingRequest.getFlatNo());
		SimpleDateFormat sdf =new SimpleDateFormat("ddMMMyyyy");
		String eventDate=sdf.format(bookingRequest.getEventDate());
		bookingId.append(eventDate);
		Random random = new Random();
		bookingId.append( 1000 + random.nextInt(9000));
		return bookingId.toString().toUpperCase();
	}
	
	public UpdateBookingResponse updateBooking(UpdateBookingRequest updateBookingRequest) {
		UpdateBookingResponse bookingResponse= new UpdateBookingResponse();
		bookingResponse.setGenericHeader(updateBookingRequest.getGenericHeader());
		try {
		bookingServiceValidation.validateCancelBookingRequest(updateBookingRequest);
		 Booking booking = bookingRepository.findById(updateBookingRequest.getBookingId())
		            .orElseThrow(() -> new EntityNotFoundException("Booking not found"));
		 if(booking.getBkngSts().equals(SecuraConstants.BOOKING_CONST_STATUS_CANCELLED)) {
			 throw new BusinessException(ErrorMessage.ERR_MESSAGE_21, ErrorMessageCode.ERR_MESSAGE_21); 
		 }
		
		 booking.setBkng_cncld_reason(updateBookingRequest.getReason());
		 booking.setBkngCncldBy(updateBookingRequest.getGenericHeader().getUserId());
		 booking.setBkngSts(updateBookingRequest.getStatus());
		 booking.setLstUpdtUsrId(updateBookingRequest.getGenericHeader().getUserId());
		 String requestUserId=updateBookingRequest.getGenericHeader().getUserId();
		 if(!booking.getBkngBy().equals(requestUserId) && (updateBookingRequest.getGenericHeader().getAccess()==null|| !updateBookingRequest.getGenericHeader().getAccess().equals(SecuraConstants.ACCESS_ADMIN))) {
			 throw new BusinessException(ErrorMessage.ERR_MESSAGE_20, ErrorMessageCode.ERR_MESSAGE_20); 
		 }
		 bookingRepository.save(booking);
		 if(null!=booking.getWorklist() && !booking.getWorklist().isEmpty()) {
			 genericService.canelWorklist(booking.getWorklist());
		 }
		 
		 bookingResponse.setBookingId(updateBookingRequest.getBookingId());
         bookingResponse.setBookingStatus(updateBookingRequest.getStatus());
         if(updateBookingRequest.getStatus().equals(SecuraConstants.BOOKING_CONST_STATUS_APPROVED)) {
        	 bookingResponse.setMessage(SuccessMessage.SUCC_MESSAGE_08);
         }
         if(updateBookingRequest.getStatus().equals(SecuraConstants.BOOKING_CONST_STATUS_REJECTED)) {
        	 bookingResponse.setMessage(SuccessMessage.SUCC_MESSAGE_04);
         }
         if(updateBookingRequest.getStatus().equals(SecuraConstants.BOOKING_CONST_STATUS_CANCELLED)) {
        	 bookingResponse.setMessage(SuccessMessage.SUCC_MESSAGE_02);
         }
         
         bookingResponse.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_01);
		}
		catch (BusinessException be) {
			bookingResponse.setMessage(be.getErrorMessage());
			bookingResponse.setMessageCode(be.getErrorMessageCode());
		}
		catch (Exception e) {
			 bookingResponse.setMessage(ErrorMessage.ERR_MESSAGE_03 + e.getMessage());
	         bookingResponse.setMessageCode(ErrorMessageCode.ERR_MESSAGE_03);
		}
		
		return bookingResponse;
	}
	
	public GetHallsReponse getAllHals(String apartmentId) {
		GetHallsReponse hallsReponse= new GetHallsReponse();
		hallsReponse.setHalls(hallRepository.findAll().stream().filter(hl->hl.getAprmntId().equals(apartmentId)).collect(Collectors.toList()));
		GenericHeader header = new GenericHeader();
		header.setApartmentId(apartmentId);
		hallsReponse.setGenericHeader(header);
		return hallsReponse;
		
	}
	
	public GetBookingResponse getAllBooking(GetBookingRequest getBookingRequest) {
		GetBookingResponse bookingResponse = new GetBookingResponse();
		bookingResponse.setGenericHeader(getBookingRequest.getGenericHeader());
		List<Booking> bookingList= new ArrayList<>();
		
	  if((null!=getBookingRequest.getGenericHeader().getAccess() && getBookingRequest.getGenericHeader().getAccess().equals(SecuraConstants.ACCESS_ADMIN))) {
			bookingList=bookingRepository.findAll();
		}
		else {
			bookingList=bookingRepository.findByCreatUsrId(getBookingRequest.getGenericHeader().getUserId());
		}
		if(null==bookingList || bookingList.isEmpty()) {
			bookingResponse.setMessage(ErrorMessage.ERR_MESSAGE_24);
			bookingResponse.setMessageCode(ErrorMessageCode.ERR_MESSAGE_24);
		}
		else {
			bookingResponse.setMessage(SuccessMessage.SUCC_MESSAGE_05);
			bookingResponse.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_05);
		}
		bookingResponse.setBookingList(bookingList);
		
		return bookingResponse;
	}
	
	public GetBookingResponse getBooking(GetBookingRequest getBookingRequest) {
		GetBookingResponse bookingResponse = new GetBookingResponse();
		bookingResponse.setGenericHeader(getBookingRequest.getGenericHeader());
		Optional<Booking> booking= bookingRepository.findById(getBookingRequest.getBookingId());
		
		if(booking.isPresent()) {
			if((null!=getBookingRequest.getGenericHeader().getAccess() && getBookingRequest.getGenericHeader().getAccess().equals(SecuraConstants.ACCESS_ADMIN)) || (getBookingRequest.getGenericHeader().getUserId().equals(booking.get().getCreatUsrId()))) {
				List<Booking> responseBookingList= new ArrayList<>();
				responseBookingList.add(booking.get());
				bookingResponse.setBookingList(responseBookingList);
				bookingResponse.setMessage(SuccessMessage.SUCC_MESSAGE_05);
				bookingResponse.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_05);
			}
			else {
				bookingResponse.setMessage(ErrorMessage.ERR_MESSAGE_24);
				bookingResponse.setMessageCode(ErrorMessageCode.ERR_MESSAGE_24);
			}
		}
		else {
			bookingResponse.setMessage(ErrorMessage.ERR_MESSAGE_24);
			bookingResponse.setMessageCode(ErrorMessageCode.ERR_MESSAGE_24);
		}
		
		return bookingResponse;
	}

}
