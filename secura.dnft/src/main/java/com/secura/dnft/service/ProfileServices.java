package com.secura.dnft.service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.bean.ProfileAccountDetails;
import com.secura.dnft.dao.ApartmentRepository;
import com.secura.dnft.dao.OwnerRepository;
import com.secura.dnft.dao.ProfileRepository;
import com.secura.dnft.dao.TenantRepository;
import com.secura.dnft.entity.ApartmentMaster;
import com.secura.dnft.entity.Owner;
import com.secura.dnft.entity.Profile;
import com.secura.dnft.entity.Tenant;
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
import com.secura.dnft.request.response.GetTenantRequest;
import com.secura.dnft.request.response.GetTenantResponse;
import com.secura.dnft.request.response.ManageTenantRequest;
import com.secura.dnft.request.response.ManageTenantResponse;
import com.secura.dnft.request.response.UpdateProfileRequest;
import com.secura.dnft.request.response.UpdateProfileResponse;


@Service
public class ProfileServices {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileServices.class);
	
	@Autowired
	GenericService genericService;
	
	@Autowired
	ProfileRepository profileRepository;
	
	@Autowired
	ApartmentRepository apartmentRepository;
	
	@Autowired
	OwnerRepository ownerRepository;
	
	@Autowired
	TenantRepository tenantRepository;
	
	
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
		List<ProfileAccountDetails> accontDetailsList= new ArrayList<>();
		ProfileAccountDetails accountDetails = new ProfileAccountDetails();
		accountDetails.setApartmentId(request.getHeader().getApartmentId());
		accountDetails.setApartmentName(profileId);
		List<String> flatIds= new ArrayList<>();
		flatIds.add(request.getProfileFlatNo());
		accountDetails.setFlatId(flatIds);
		accountDetails.setPosition(request.getProfilePosition());
		accountDetails.setProfileType(request.getProfileType());
		accountDetails.setStatus(SecuraConstants.PROFILE_STATUS_ACTIVE);
		accontDetailsList.add(accountDetails);
		String name=genericService.toJson(request.getProfileName());
		String profileAccountDetailsJson=genericService.toJson(accontDetailsList);
		String primaryAddres=genericService.toJson(request.getProfilePrimaryPostalAdrss());
		String otherAddress=genericService.toJson(request.getProfileOthrAdrss());
	    Profile profile = new Profile(request, profileId,profileAccountDetailsJson,primaryAddres,otherAddress);
		profile.setPrflName(name);
		profileRepository.save(profile);
		if(request.getProfileType().equals(SecuraConstants.PROFILE_TYPE_OWNER)) {
			Owner owner = new Owner();
			owner.setAprmt_id(request.getHeader().getApartmentId());
			owner.setCreatUsrId(request.getHeader().getUserId());
			owner.setFlatNo(request.getProfileFlatNo());
			List<String> profileIds=new ArrayList<>();
			profileIds.add(profileId);
			owner.setPrflId(genericService.toJson(profileIds));
			owner.setStatus(SecuraConstants.PROFILE_STATUS_ACTIVE);
			ownerRepository.save(owner);
		}
        if(request.getProfileType().equals(SecuraConstants.PROFILE_TYPE_TENANT)) {
			Tenant tenant = new Tenant();
			tenant.setAprmt_id(request.getHeader().getApartmentId());
			tenant.setCreatUsrId(request.getHeader().getUserId());
			tenant.setFlatNo(request.getProfileFlatNo());
			tenant.setVerified(false);
			List<String> profileIds=new ArrayList<>();
			profileIds.add(profileId);
			tenant.setPrflId(genericService.toJson(profileIds));
			tenant.setStatus(SecuraConstants.PROFILE_STATUS_ACTIVE);
			tenantRepository.save(tenant);
		}
		response.setProfileId(profileId);
		response.setMessage(SuccessMessage.SUCC_MESSAGE_09);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_09);
		}
		catch (Exception e) {
			LOGGER.error("Error while creating profile for userId: {}", request.getHeader().getUserId(), e);
			response.setMessage(ErrorMessage.ERR_MESSAGE_25);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_25);
		}
		return response;
	}
	
	public UpdateProfileResponse updateProfile(UpdateProfileRequest request) {
		UpdateProfileResponse response = new UpdateProfileResponse();
		
		try {
		///validation
		Optional<Profile> profile =profileRepository.findById(request.getProfileId());	
		if(profile.isPresent()) {
			
			Profile existingProfile=profile.get();
			String name=genericService.toJson(request.getProfileName());
			existingProfile.setPrflName(name);
			List<ProfileAccountDetails> accountDetails =
     		        genericService.fromJson(
     		        		profile.get().getPrflAcountDetails(),
     		                new TypeReference<List<ProfileAccountDetails>>() {}
     		        );
			if(null!=request.getProfileFlatNo()) {
				if(null==accountDetails || accountDetails.isEmpty()) {
					ProfileAccountDetails details= new ProfileAccountDetails();
					details.setApartmentId(name);
					List<String> flatIds= new ArrayList<>();
					flatIds.add(request.getProfileFlatNo());
					details.setFlatId(flatIds);
					details.setPosition(request.getProfilePosition());
					details.setProfileType(request.getProfileType());
					details.setRole(request.getRole());
					details.setStatus(request.getProfileStatus());
					accountDetails.add(details);
					existingProfile.setPrflAcountDetails(genericService.toJson(accountDetails));
				}
				else {
					Optional<ProfileAccountDetails> details= accountDetails.stream().filter(ac->ac.getApartmentId().equals(request.getHeader().getApartmentId())).findFirst();
					if(details.isPresent()) {
						details.get().setPosition(request.getProfilePosition());
						details.get().setProfileType(request.getProfileType());
						details.get().setRole(request.getRole());
						}
				}
				
			}
			existingProfile.setPrflPhoneNo(request.getContact().getMobileNumber());
			existingProfile.setPrflEmailAdrss(request.getContact().getEmailId());
			existingProfile.setPrflOthrAdrss(genericService.toJson(request.getProfileOthrAdrss()));
			existingProfile.setPrflPrimaryPostalAdrss((genericService.toJson(request.getPrimaryPostalAddress())));
			existingProfile.setProfile_pic(request.getProfilePic());
			existingProfile.setLst_updt_usrId(request.getHeader().getUserId());
			profileRepository.save(existingProfile);
			response.setMessage(SuccessMessage.SUCC_MESSAGE_10);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_10);
			response= new UpdateProfileResponse(request);
		}
		
	    
		response.setMessage(SuccessMessage.SUCC_MESSAGE_10);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_10);
		response= new UpdateProfileResponse(request);
		response.setHeader(request.getHeader());
		}
		catch (Exception e) {
			LOGGER.error("Error while updating profileId: {}", request.getProfileId(), e);
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
			getProfileResponse=new GetProfileResponse(profile.get(),request.getGenericHeader().getApartmentId());
			getProfileResponse.setPrflName(genericService.fromJson(profile.get().getPrflName(),Name.class));
			getProfileResponse.setPrflOthrAdrss(genericService.fromJson(profile.get().getPrflOthrAdrss(),Address.class));
			getProfileResponse.setPrimaryAddress(genericService.fromJson(profile.get().getPrflPrimaryPostalAdrss(),Address.class));
			getProfileResponse.setMessage(SuccessMessage.SUCC_MESSAGE_11);
			getProfileResponse.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_11);
			Optional<ApartmentMaster> apartmentMaster= apartmentRepository.findById(request.getGenericHeader().getApartmentId());
			if(apartmentMaster.isPresent()) {
				getProfileResponse.setApartmentName(apartmentMaster.get().getAprmntName());
			}
			
			
			if(null!=profile.get().getCreat_usr_id()) {
			Optional<Profile> createprofile=profileRepository.findById(profile.get().getCreat_usr_id());
			if(createprofile.isPresent()) {
				StringBuffer createuserName= new StringBuffer();
				Name createuser= genericService.fromJson(createprofile.get().getPrflName(), Name.class);
				createuserName.append(createuser.getFirstName()).append(" ").append(createuser.getLastName()).append(" ").append("(").append(createprofile.get().getCreat_usr_id()).append(")");
				getProfileResponse.setCreatUsrName(createuserName.toString());
			}
			}
			if(null!=profile.get().getLst_updt_usrId()) {
			Optional<Profile> lastUpdateprofile=profileRepository.findById(profile.get().getLst_updt_usrId());
			if(lastUpdateprofile.isPresent()) {
				StringBuffer lastUpdateprofileName= new StringBuffer();
				Name lastUpdateProfileuser= genericService.fromJson(lastUpdateprofile.get().getPrflName(), Name.class);
				lastUpdateprofileName.append(lastUpdateProfileuser.getFirstName()).append(" ").append(lastUpdateProfileuser.getLastName()).append(" ").append("(").append(lastUpdateprofile.get().getLst_updt_usrId()).append(")");
				getProfileResponse.setLstUpdtUsrName(lastUpdateprofileName.toString());
			}}
			
		}
		else {
			getProfileResponse.setMessage(ErrorMessage.ERR_MESSAGE_27);
			getProfileResponse.setMessageCode(ErrorMessageCode.ERR_MESSAGE_27);
		}
		}
		catch (Exception e) {
			LOGGER.error("Error while fetching profile detail for profileId: {}", request.getProfileID(), e);
			getProfileResponse.setMessage(ErrorMessage.ERR_MESSAGE_28);
			getProfileResponse.setMessageCode(ErrorMessageCode.ERR_MESSAGE_28);
		}
		getProfileResponse.setGenericHeader(request.getGenericHeader());
		return getProfileResponse;
	}

	
	public ManageTenantResponse updateTenantDetails(ManageTenantRequest request ) {
		ManageTenantResponse response  = new ManageTenantResponse();
		response.setHeader(request.getHeader());
		GetTenantRequest getTenantRequest= new GetTenantRequest();
		getTenantRequest.setGenericHeader(request.getHeader());
		getTenantRequest.setFlatId(request.getFlatId());
		
		GetTenantResponse getTenantResponse=getTenant(getTenantRequest);
		if(getTenantResponse.getProfile().size()>0) {
            Tenant tenant=getTenantResponse.getTenant();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			if(null!=request.getEndDate()) {
				String formatted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((request.getEndDate()));
				tenant.setEndDate(LocalDateTime.parse(formatted, formatter));
			}
	         if(null!=request.getStartDate()) {
	        	 String formatted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((request.getStartDate()));
	 			tenant.setStartDate(LocalDateTime.parse(formatted, formatter));
			}
			tenant.setStatus(request.getStatus());
			tenant.setVerified(request.isVerified());
			String douments= genericService.toJson(request.getListOfDocuments());
			tenant.setDocument(douments);
			tenant.setLstUpdtUsrId(request.getHeader().getUserId());
			tenantRepository.save(tenant);
			response.setMessage(SuccessMessage.SUCC_MESSAGE_16);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_16);
		}
		else {
			response.setMessage(ErrorMessage.ERR_MESSAGE_36);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_36);
		}
		return response;
	}
	
	public GetTenantResponse getTenant(GetTenantRequest request ) {
		GetTenantResponse getTenantResponse = new GetTenantResponse();
		getTenantResponse.setGenericHeader(request.getGenericHeader());
     	Tenant currentTenantData=getCurrentFlatTenant(request.getFlatId());
     	getTenantResponse.setTenant(currentTenantData);
     	if(currentTenantData!=null) {
    	List<String> tenantProfiles =
 		        genericService.fromJson(
 		        		currentTenantData.getPrflId(),
 		                new TypeReference<List<String>>() {}
 		        );

         List<Optional<Profile>> profileList= tenantProfiles.stream().map(prfl->profileRepository.findById(prfl)).collect(Collectors.toList());
         getTenantResponse.setProfile(profileList.stream().filter(prfl->prfl.isPresent()).map(prfl->prfl.get()).collect(Collectors.toList()));
         
         if(getTenantResponse.getProfile().size()>0) {
        	 getTenantResponse.setMessage(SuccessMessage.SUCC_MESSAGE_15);
        	 getTenantResponse.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_15);
         }
         else {
        	 getTenantResponse.setMessage(ErrorMessage.ERR_MESSAGE_35);
        	 getTenantResponse.setMessageCode(ErrorMessageCode.ERR_MESSAGE_35);
         }
         }
     	else {
     		 getTenantResponse.setMessage(ErrorMessage.ERR_MESSAGE_35);
        	 getTenantResponse.setMessageCode(ErrorMessageCode.ERR_MESSAGE_35);
     	}
    	return getTenantResponse;
	}
	
	public Tenant getCurrentFlatTenant(String flatId ) {
		List<Tenant> tenantList= tenantRepository.findByFlatNo(flatId);
		Optional<Tenant> tenant=tenantList.stream().filter(tnt->null==tnt.getEndDate()).findFirst();
		if(tenant.isPresent()) {
			return tenant.get();
		}
		else {
			return null;
		}
		
	}
	
}
