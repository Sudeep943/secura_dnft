package com.secura.dnft.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.secura.dnft.dao.ApartmentRepository;
import com.secura.dnft.dao.ProfileRepository;
import com.secura.dnft.entity.ApartmentMaster;
import com.secura.dnft.entity.Profile;
import com.secura.dnft.generic.bean.Address;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.Name;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.CreateProfileRequest;
import com.secura.dnft.request.response.CreateProfileResponse;
import com.secura.dnft.request.response.GetProfileRequest;
import com.secura.dnft.request.response.GetProfileResponse;
import com.secura.dnft.request.response.UpdateProfileRequest;
import com.secura.dnft.request.response.UpdateProfileResponse;


@Service
public class ProfileServices {
	
	@Autowired
	GenericService genericService;
	
	@Autowired
	ProfileRepository profileRepository;
	
	@Autowired
	ApartmentRepository apartmentRepository;
	
	
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
	
	public GetProfileResponse getProfileDetail(GetProfileRequest request) {
		GetProfileResponse getProfileResponse = new GetProfileResponse();
	
		try {
		Optional<Profile> profile=profileRepository.findById(request.getProfileID());
		
		if(profile.isPresent()) {
			getProfileResponse=new GetProfileResponse(profile.get());
			getProfileResponse.setPrflName(genericService.fromJson(profile.get().getPrflName(),Name.class));
			getProfileResponse.setPrflOthrAdrss(genericService.fromJson(profile.get().getPrflOthrAdrss(),Address.class));
			getProfileResponse.setMessage(SuccessMessage.SUCC_MESSAGE_11);
			getProfileResponse.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_11);
			Optional<ApartmentMaster> apartmentMaster= apartmentRepository.findById(request.getGenericHeader().getApartmentId());
			if(apartmentMaster.isPresent()) {
				getProfileResponse.setApartmentName(apartmentMaster.get().getAprmntName());
			}
			
			
			if(null!=profile.get().getCreatUsrId()) {
			Optional<Profile> createprofile=profileRepository.findById(profile.get().getCreatUsrId());
			if(createprofile.isPresent()) {
				StringBuffer createuserName= new StringBuffer();
				Name createuser= genericService.fromJson(createprofile.get().getPrflName(), Name.class);
				createuserName.append(createuser.getFirstName()).append(" ").append(createuser.getLastName()).append(" ").append("(").append(createprofile.get().getCreatUsrId()).append(")");
				getProfileResponse.setCreatUsrName(createuserName.toString());
			}
			}
			if(null!=profile.get().getLstUpdtUsrId()) {
			Optional<Profile> lastUpdateprofile=profileRepository.findById(profile.get().getLstUpdtUsrId());
			if(lastUpdateprofile.isPresent()) {
				StringBuffer lastUpdateprofileName= new StringBuffer();
				Name lastUpdateProfileuser= genericService.fromJson(lastUpdateprofile.get().getPrflName(), Name.class);
				lastUpdateprofileName.append(lastUpdateProfileuser.getFirstName()).append(" ").append(lastUpdateProfileuser.getLastName()).append(" ").append("(").append(lastUpdateprofile.get().getCreatUsrId()).append(")");
				getProfileResponse.setLstUpdtUsrName(lastUpdateprofileName.toString());
			}}
			
		}
		else {
			getProfileResponse.setMessage(ErrorMessage.ERR_MESSAGE_27);
			getProfileResponse.setMessageCode(ErrorMessageCode.ERR_MESSAGE_27);
		}
		}
		catch (Exception e) {
			getProfileResponse.setMessage(ErrorMessage.ERR_MESSAGE_28);
			getProfileResponse.setMessageCode(ErrorMessageCode.ERR_MESSAGE_28);
		}
		getProfileResponse.setGenericHeader(request.getGenericHeader());
		return getProfileResponse;
	}

}
