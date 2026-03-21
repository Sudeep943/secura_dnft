package com.secura.dnft.service;

import java.text.SimpleDateFormat;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.secura.dnft.dao.BookingRepository;
import com.secura.dnft.dao.HallRepository;
import com.secura.dnft.entity.Booking;
import com.secura.dnft.entity.Halls;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.BookingRequest;
import com.secura.dnft.request.response.BookingResponse;
import com.secura.dnft.request.response.CancelBookingRequest;
import com.secura.dnft.request.response.CancelBookingResponse;
import com.secura.dnft.request.response.CheckHallAvailablityRequest;
import com.secura.dnft.request.response.CheckHallAvailablityResponse;
import com.secura.dnft.security.BusinessException;
import com.secura.dnft.validation.BookingServiceValidation;

import jakarta.persistence.EntityNotFoundException;

@Service
public class BookingService {

	 @Autowired
	 private BookingRepository bookingRepository;
	 
	 @Autowired
	 HallRepository hallRepository;
	 
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
			
			if(available) {
				bookingServiceValidation.validateBookingRequest(bookingRequest);
				//RazorPayPaymentServices paymentServices= new RazorPayPaymentServices();
				//paymentServices.pay(null);
				String bookingid=createBookingID(bookingRequest);
				Booking bookingEnitity= new Booking(bookingRequest,bookingid);
				bookingRepository.save(bookingEnitity);
				bookingResponse.setBookingId(bookingid);
				bookingResponse.setBookingStatus(SecuraConstants.BOOKING_CONST_STATUS_REQUEST_RECEIVED);
				bookingResponse.setMessage(SuccessMessage.SUCC_MESSAGE_03);
				bookingResponse.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_03);
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
	
	public CancelBookingResponse cancelBooking(CancelBookingRequest cancelBookingRequest) {
		CancelBookingResponse bookingResponse= new CancelBookingResponse();
		bookingResponse.setGenericHeader(cancelBookingRequest.getGenericHeader());
		try {
		bookingServiceValidation.validateCancelBookingRequest(cancelBookingRequest);
		 Booking booking = bookingRepository.findById(cancelBookingRequest.getBookingId())
		            .orElseThrow(() -> new EntityNotFoundException("Booking not found"));
		 if(booking.getBkngSts().equals(SecuraConstants.BOOKING_CONST_STATUS_CANCELED)) {
			 throw new BusinessException(ErrorMessage.ERR_MESSAGE_21, ErrorMessageCode.ERR_MESSAGE_21); 
		 }
		 
		 booking.setBkng_cncld_reason(cancelBookingRequest.getReason());
		 booking.setBkngCncldBy(cancelBookingRequest.getGenericHeader().getUserId());
		 booking.setBkngSts(SecuraConstants.BOOKING_CONST_STATUS_CANCELED);
		 booking.setLstUpdtUsrId(cancelBookingRequest.getGenericHeader().getUserId());
		 String requestUserId=cancelBookingRequest.getGenericHeader().getUserId();
		 if(!booking.getBkngBy().equals(requestUserId) && (cancelBookingRequest.getGenericHeader().getAccess()==null|| !cancelBookingRequest.getGenericHeader().getAccess().equals(SecuraConstants.ACCESS_ADMIN))) {
			 throw new BusinessException(ErrorMessage.ERR_MESSAGE_20, ErrorMessageCode.ERR_MESSAGE_20); 
		 }
		 bookingRepository.save(booking);
		 bookingResponse.setBookingId(cancelBookingRequest.getBookingId());
         bookingResponse.setBookingStatus("Cancled");
         bookingResponse.setMessage(SuccessMessage.SUCC_MESSAGE_02);
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

}
