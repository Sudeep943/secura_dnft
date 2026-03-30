package com.secura.dnft.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.secura.dnft.bean.ProfileAccountDetails;
import com.secura.dnft.dao.ApartmentRepository;
import com.secura.dnft.dao.ProfileRepository;
import com.secura.dnft.entity.ApartmentMaster;
import com.secura.dnft.entity.Profile;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.Name;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.LoginRequest;
import com.secura.dnft.request.response.LoginResponse;
import com.secura.dnft.request.response.UpdatePasswordRequest;
import com.secura.dnft.request.response.UpdatePasswordResponse;
import com.secura.dnft.security.AuthCryptoUtil;
import com.secura.dnft.security.JwtUtil;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class LoginService {

	 @Autowired
	 JwtUtil jwtUtil;
	 
	@Autowired
	ProfileRepository profileRepository;
	
	//@Autowired
	//DataPrivacyService dataPrivacyService;
	
	@Autowired
	GenericService genericService;
	
	@Autowired
	ApartmentRepository apartmentRepository;
	
	@Autowired
	AuthCryptoUtil authCryptoUtil;
	
	public LoginResponse login(LoginRequest loginRequest) {
		LoginResponse loginResponse = new LoginResponse();
		//String passwords=authCryptoUtil.decrypt(loginRequest.getPassword());
	  Optional<Profile> profile = profileRepository.findById(loginRequest.getUsername());
	  try{
	   if(profile.isPresent()) {
		  String password=profile.get().getPassword();
		  if(null==password || password.isEmpty()) {
			  if(null==loginRequest.getOtp() ||loginRequest.getOtp().isEmpty()) {
				  generateOTP(profile.get().getPrflPhoneNo());
				  loginResponse.setMessage(ErrorMessage.ERR_MESSAGE_29);
				  loginResponse.setMessageCode(ErrorMessageCode.ERR_MESSAGE_29);
				  return loginResponse;
			  }
			  else {
				  if(validOTP(profile.get().getPrflPhoneNo(),loginRequest.getOtp())) {
					  loginResponse.setMessage(ErrorMessage.ERR_MESSAGE_30);
					  loginResponse.setMessageCode(ErrorMessageCode.ERR_MESSAGE_30);
					  return loginResponse; 
				  }
			  }
		  }
		  else {
           if(authCryptoUtil.decrypt(password).equals(authCryptoUtil.decrypt(loginRequest.getPassword()))) {
       	           	   List<ProfileAccountDetails> accountDetails =
        		        genericService.fromJson(
        		        		profile.get().getPrflAcountDetails(),
        		                new TypeReference<List<ProfileAccountDetails>>() {}
        		        );
        	   ProfileAccountDetails acDetails=accountDetails.get(0);
        	   GenericHeader genericHeader = new GenericHeader();
             	genericHeader.setUserId(loginRequest.getUsername());
            	genericHeader.setApartmentId(acDetails.getApartmentId());
           	    genericHeader.setRole(acDetails.getRole());
           	    genericHeader.setPosition(acDetails.getPosition());
           	    genericHeader.setFlatNo(acDetails.getRole());
           	    Name name= genericService.fromJson(profile.get().getPrflName(),Name.class);
           	    StringBuffer newName= new StringBuffer();
           	    newName.append(name.getFirstName()).append(" ").append(name.getLastName());
                genericHeader.setProfileName(newName.toString());
                Optional<ApartmentMaster> aprtmnt=apartmentRepository.findById(acDetails.getApartmentId());
                if(aprtmnt.isPresent()) {
                	genericHeader.setApartmentName(aprtmnt.get().getAprmntName());	
                }
                loginResponse.setHeader(genericHeader);
               loginResponse.setToken( jwtUtil.generateToken(loginRequest.getUsername()));
        	   loginResponse.setMessage(SuccessMessage.SUCC_MESSAGE_12);
			   loginResponse.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_12);
	}
           else {
        	   loginResponse.setMessage(ErrorMessage.ERR_MESSAGE_31);
				  loginResponse.setMessageCode(ErrorMessageCode.ERR_MESSAGE_31);
           }
}
	   }}
	   catch(Exception e){
		   e.printStackTrace();
		   loginResponse.setMessage(ErrorMessage.ERR_MESSAGE_33);
		  loginResponse.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
	   }
		return loginResponse;
	}
	
	
	public UpdatePasswordResponse updatePassword(UpdatePasswordRequest profileRequest) {
		UpdatePasswordResponse response = new UpdatePasswordResponse();
		 Optional<Profile> profile = profileRepository.findById(profileRequest.getProfileId());
		 try {
		 if(profile.isPresent()) {
			 if(profileRequest.isOtpVerified()) {
				 profile.get().setPassword(profileRequest.getNewPassword());
				 profileRepository.save(profile.get());
				 response.setMessage(SuccessMessage.SUCC_MESSAGE_13);
				 response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_13);
			 }
			 else {
				 if(authCryptoUtil.decrypt(profile.get().getPassword()).equals(profileRequest.getOldPassword())) {
					 profile.get().setPassword(profileRequest.getNewPassword());
					 profileRepository.save(profile.get());
					 response.setMessage(SuccessMessage.SUCC_MESSAGE_13);
					 response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_13);
				 }
				 else {
					 response.setMessage(ErrorMessage.ERR_MESSAGE_32);
					 response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_32);
				 }
			 }
		 }
		 } catch (Exception e) {
			 e.printStackTrace();
			 response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			 response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
			}
		return response; 
	}
	
	
	public void generateOTP(String mobileNumber) {
		
	}
	
    public boolean validOTP(String mobileNumber,String OTP) {
    	if(OTP.equals("1234")) {
    		return true;
    	}
    	return false;
	}
}
