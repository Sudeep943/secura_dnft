package com.secura.dnft.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.bean.BankAccountDetails;
import com.secura.dnft.bean.ExecutiveMember;
import com.secura.dnft.dao.ApartmentRepository;
import com.secura.dnft.entity.ApartmentMaster;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.GetApartmentDetailsRequest;
import com.secura.dnft.request.response.GetApartmentDetailsResponse;
import com.secura.dnft.request.response.UpdateApartmentDetailsRequest;
import com.secura.dnft.request.response.UpdateApartmentDetailsResponse;
import com.secura.dnft.security.BusinessException;
import com.secura.dnft.validation.CommonValidations;

@Service
public class ApartmentService {

	
	
	 @Autowired
	    private ApartmentRepository repository;

	 @Autowired
	 private GenericService genericService;

	 @Autowired
	 private CommonValidations commonValidations;

	    public List<ApartmentMaster> getAllApartments() {
	        return repository.findAll();
	    }

	    public UpdateApartmentDetailsResponse updateApartmentDetails(UpdateApartmentDetailsRequest request) {
	    	UpdateApartmentDetailsResponse response = new UpdateApartmentDetailsResponse();
	    	response.setGenericHeader(request != null ? request.getGenericHeader() : null);
	    	try {
	    		commonValidations.genericHeaderValidation(request.getGenericHeader());
	    		ApartmentMaster apartment = repository.findById(request.getGenericHeader().getApartmentId())
	    				.orElseGet(ApartmentMaster::new);
	    		if (apartment.getAprmntId() == null) {
	    			apartment.setAprmntId(request.getGenericHeader().getApartmentId());
	    			apartment.setCreat_ts(LocalDateTime.now());
	    			apartment.setCreat_usr_id(request.getGenericHeader().getUserId());
	    		}
	    		if (request.getGenericHeader().getApartmentName() != null) {
	    			apartment.setAprmntName(request.getGenericHeader().getApartmentName());
	    		}
	    		apartment.setAprmnt_logo(request.getApartmentLogo());
	    		apartment.setAprmntAddress(genericService.toJson(request.getAddress()));
	    		apartment.setAprmnt_bank_acccount_list(
	    				genericService.encrypt(genericService.toJson(defaultIfNull(request.getBankAccountDetails()))));
	    		apartment.setAprmnt_executive_role_list(genericService.toJson(defaultIfNull(request.getExecutiveMemberList())));
	    		apartment.setAprmntLetterHead(request.getApartmentLetterHead());
	    		apartment.setLst_updt_ts(LocalDateTime.now());
	    		apartment.setLst_updt_usrid(request.getGenericHeader().getUserId());
	    		repository.save(apartment);
	    		response.setMessage(SuccessMessage.SUCC_MESSAGE_35);
	    		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_35);
	    	} catch (BusinessException e) {
	    		response.setMessage(e.getErrorMessage());
	    		response.setMessageCode(e.getErrorMessageCode());
	    	}
	    	return response;
	    }

	    public GetApartmentDetailsResponse getApartmentDetails(GetApartmentDetailsRequest request) {
	    	GetApartmentDetailsResponse response = new GetApartmentDetailsResponse();
	    	response.setGenericHeader(request != null ? request.getGenericHeader() : null);
	    	try {
	    		commonValidations.genericHeaderValidation(request.getGenericHeader());
	    		Optional<ApartmentMaster> apartmentOptional = repository.findById(request.getGenericHeader().getApartmentId());
	    		if (apartmentOptional.isEmpty()) {
	    			response.setMessage(ErrorMessage.ERR_MESSAGE_47);
	    			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_47);
	    			return response;
	    		}
	    		ApartmentMaster apartment = apartmentOptional.get();
	    		response.setApartmentLogo(apartment.getAprmnt_logo());
	    		response.setAddress(genericService.fromJson(apartment.getAprmntAddress(),
	    				com.secura.dnft.generic.bean.Address.class));
	    		response.setExecutiveMemberList(readExecutiveMembers(apartment.getAprmnt_executive_role_list()));
	    		response.setBankAccountDetails(readBankAccounts(apartment.getAprmnt_bank_acccount_list()));
	    		response.setApartmentLetterHead(apartment.getAprmntLetterHead());
	    		response.setMessage(SuccessMessage.SUCC_MESSAGE_36);
	    		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_36);
	    	} catch (BusinessException e) {
	    		response.setMessage(e.getErrorMessage());
	    		response.setMessageCode(e.getErrorMessageCode());
	    	}
	    	return response;
	    }

	    private List<BankAccountDetails> readBankAccounts(String value) {
	    	if (value == null || value.isBlank()) {
	    		return new ArrayList<>();
	    	}
	    	return genericService.fromJson(genericService.decrypt(value), new TypeReference<List<BankAccountDetails>>() {
	    	});
	    }

	    private List<ExecutiveMember> readExecutiveMembers(String value) {
	    	if (value == null || value.isBlank()) {
	    		return new ArrayList<>();
	    	}
	    	return genericService.fromJson(value, new TypeReference<List<ExecutiveMember>>() {
	    	});
	    }

	    private <T> List<T> defaultIfNull(List<T> value) {
	    	return value == null ? new ArrayList<>() : value;
	    }

}
