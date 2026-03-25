package com.secura.dnft.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.secura.dnft.dao.ProfileRepository;
import com.secura.dnft.entity.Profile;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.CreateProfileRequest;
import com.secura.dnft.request.response.CreateProfileResponse;
import com.secura.dnft.request.response.UpdateProfileRequest;
import com.secura.dnft.request.response.UpdateProfileResponse;


@Service
public class ProfileServices {
	
	@Autowired
	GenericService genericService;
	
	@Autowired
	ProfileRepository profileRepository;
	
	public String createProfileId() {
		StringBuffer profilId= new StringBuffer();
		SimpleDateFormat sdf= new SimpleDateFormat("ddMMHHmm");
		String currentDate= sdf.format(new Date()); 
		profilId.append(SecuraConstants.PROFILE_ID_PREFIX);
		profilId.append(currentDate);
		Random random = new Random();
		profilId.append( 1000 + random.nextInt(9000));
		return profilId.toString();
	}
	
	public CreateProfileResponse createProfile(CreateProfileRequest request) {
		CreateProfileResponse response = new CreateProfileResponse();
		response.setHeader(request.getHeader());
		try {
		///validation
		String profileId= createProfileId();
		
		String name=genericService.toJson(request.getProfileName());
		Profile profile = new Profile(request, profileId);
		profile.setPrflName(name);
		profile.setPrflOthrAdrss(genericService.toJson(request.getProfileOthrAdrss()));
		profileRepository.save(profile);
		response.setProfileId(profileId);
		response.setMessage(SuccessMessage.SUCC_MESSAGE_09);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_09);
		}
		catch (Exception e) {
			e.printStackTrace();
			response.setMessage(ErrorMessage.ERR_MESSAGE_25);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_25);
		}
		return response;
	}
	
	public UpdateProfileResponse updateProfile(UpdateProfileRequest request) {
		UpdateProfileResponse response = new UpdateProfileResponse();
		
		try {
		///validation
		Profile profile = new Profile(request, request.getProfileId());
		
		if(null!=request.getProfileName()) {
			profile.setPrflName(genericService.toJson(request.getProfileName()));
		}
		
		profile.setPrflOthrAdrss(genericService.toJson(request.getProfileOthrAdrss()));
		profile.setPassword(request.getPassword());
		profile.setProfilePic(request.getProfilePic());
		profileRepository.save(profile);
		response.setMessage(SuccessMessage.SUCC_MESSAGE_10);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_10);
		response= new UpdateProfileResponse(request);
		response.setHeader(request.getHeader());
		}
		catch (Exception e) {
			response.setProfileId(request.getProfileId());
			response.setMessage(ErrorMessage.ERR_MESSAGE_26);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_26);
		}
		return response;
	}

}
