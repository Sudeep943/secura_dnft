package com.secura.dnft.validation;

import org.springframework.stereotype.Service;

import com.secura.dnft.generic.bean.Address;
import com.secura.dnft.generic.bean.ContactDetails;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.security.BusinessException;

@Service
public class CommonValidations {
	
	
	public void genericHeaderValidation(GenericHeader genericHeader) throws BusinessException{
		if(genericHeader==null) {
			throw new BusinessException(ErrorMessage.ERR_MESSAGE_04, ErrorMessageCode.ERR_MESSAGE_04);
		}
		if(genericHeader.getApartmentId()==null) {
			throw new BusinessException(ErrorMessage.ERR_MESSAGE_05, ErrorMessageCode.ERR_MESSAGE_05);
		}
		if(genericHeader.getRole()==null) {
			throw new BusinessException(ErrorMessage.ERR_MESSAGE_06, ErrorMessageCode.ERR_MESSAGE_06);
		}
		if(genericHeader.getUserId()==null) {
			throw new BusinessException(ErrorMessage.ERR_MESSAGE_07, ErrorMessageCode.ERR_MESSAGE_07);
		}
	
	}
	
	public void addressValidation(Address address) throws BusinessException {
		if(address==null || address.getAddressLine1()==null || address.getAddressLine2() ==null) {
			throw new BusinessException(ErrorMessage.ERR_MESSAGE_08, ErrorMessageCode.ERR_MESSAGE_08);
		}
		if(address.getCity()==null) {
			throw new BusinessException(ErrorMessage.ERR_MESSAGE_09, ErrorMessageCode.ERR_MESSAGE_09);
		}
		if(address.getState()==null) {
			throw new BusinessException(ErrorMessage.ERR_MESSAGE_10, ErrorMessageCode.ERR_MESSAGE_10);
		}
		if(address.getPin()==null) {
			throw new BusinessException(ErrorMessage.ERR_MESSAGE_11, ErrorMessageCode.ERR_MESSAGE_11);
		}
		}

	public void contactDetailsValidation(ContactDetails contactDetails) throws BusinessException{
		if(contactDetails==null || contactDetails.getMobileNumber()==null) {
			throw new BusinessException(ErrorMessage.ERR_MESSAGE_11, ErrorMessageCode.ERR_MESSAGE_11);
		}
	}
}
