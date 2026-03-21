package com.secura.dnft.validation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.request.response.BookingRequest;
import com.secura.dnft.request.response.CancelBookingRequest;
import com.secura.dnft.request.response.CheckHallAvailablityRequest;
import com.secura.dnft.security.BusinessException;


@Service
public class BookingServiceValidation {

      @Autowired
	 private CommonValidations commonValidations;
	
	public void validateBookingRequest(BookingRequest bookingRequest) throws BusinessException {
	commonValidations.genericHeaderValidation(bookingRequest.getGenericHeader());
	if(bookingRequest.getBookingHallId()==null){
	throw new BusinessException(ErrorMessage.ERR_MESSAGE_12, ErrorMessageCode.ERR_MESSAGE_12);
	}
	
	if(bookingRequest.getBookingPurpose()==null){
	throw new BusinessException(ErrorMessage.ERR_MESSAGE_13, ErrorMessageCode.ERR_MESSAGE_13);
	}
	
	if(bookingRequest.getBookingyType()==null){
	throw new BusinessException(ErrorMessage.ERR_MESSAGE_14, ErrorMessageCode.ERR_MESSAGE_14);
	}
	
	if(bookingRequest.getEventDate()==null){
	throw new BusinessException(ErrorMessage.ERR_MESSAGE_15, ErrorMessageCode.ERR_MESSAGE_15);
	}
	
	if(bookingRequest.getFlatNo()==null){
	throw new BusinessException(ErrorMessage.ERR_MESSAGE_16, ErrorMessageCode.ERR_MESSAGE_16);
	}
	
//	String userid=bookingRequest.getGenericHeader().getUserId();
	
	if(bookingRequest.getBookingyType().equals("Society") && bookingRequest.getGenericHeader().getAccess().equals("Admin"))
	{
	throw new BusinessException(ErrorMessage.ERR_MESSAGE_17, ErrorMessageCode.ERR_MESSAGE_17);
	}
	
	
	
	}
	
	public void validateCancelBookingRequest(CancelBookingRequest cancelBookingRequest) throws BusinessException {
	commonValidations.genericHeaderValidation(cancelBookingRequest.getGenericHeader());
	
	if(cancelBookingRequest.getBookingId()==null){
	throw new BusinessException(ErrorMessage.ERR_MESSAGE_18, ErrorMessageCode.ERR_MESSAGE_18);
	}
	
	if(cancelBookingRequest.getReason()==null){
	throw new BusinessException(ErrorMessage.ERR_MESSAGE_16, ErrorMessageCode.ERR_MESSAGE_19);
	}
		
	}
	
	public void validateCheckAvailablityOfHallRequest(CheckHallAvailablityRequest checkHallAvailablityRequest) throws BusinessException {
	commonValidations.genericHeaderValidation(checkHallAvailablityRequest.getGenericHeader());
	
	if(checkHallAvailablityRequest.getEventDate()==null){
	throw new BusinessException(ErrorMessage.ERR_MESSAGE_15, ErrorMessageCode.ERR_MESSAGE_15);
	}
	
	if(checkHallAvailablityRequest.getHallId()==null){
	throw new BusinessException(ErrorMessage.ERR_MESSAGE_12, ErrorMessageCode.ERR_MESSAGE_12);
	}
	}
	
	
}
