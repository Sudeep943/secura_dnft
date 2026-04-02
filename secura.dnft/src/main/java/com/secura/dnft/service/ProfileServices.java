package com.secura.dnft.service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
import com.secura.dnft.request.response.AddTenantRequest;
import com.secura.dnft.request.response.AddTenantResponse;
import com.secura.dnft.request.response.CreateProfileRequest;
import com.secura.dnft.request.response.CreateProfileResponse;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.GetProfileRequest;
import com.secura.dnft.request.response.GetProfileResponse;
import com.secura.dnft.request.response.GetTenantRequest;
import com.secura.dnft.request.response.GetTenantResponse;
import com.secura.dnft.request.response.ManageTenantRequest;
import com.secura.dnft.request.response.ManageTenantResponse;
import com.secura.dnft.request.response.SearchProfileRequest;
import com.secura.dnft.request.response.SearchProfileResponse;
import com.secura.dnft.request.response.UpdateProfileRequest;
import com.secura.dnft.request.response.UpdateProfileResponse;
import com.secura.dnft.security.BusinessException;
import com.secura.dnft.validation.ProfileServiceValidation;

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

	@Autowired
	ProfileServiceValidation profileValidation;

	public String createProfileId() {
		StringBuffer profilId = new StringBuffer();
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMHHmm");
		String currentDate = sdf.format(new Date());
		profilId.append(SecuraConstants.PROFILE_ID_PREFIX);
		profilId.append(currentDate);
		Random random = new Random();
		profilId.append(1000 + random.nextInt(9000));
		return profilId.toString();
	}

	public CreateProfileResponse createProfile(CreateProfileRequest request) {
		CreateProfileResponse response = new CreateProfileResponse();
		response.setHeader(request.getHeader());
		try {
			/// validation
			boolean exits = profileValidation.validateOwnerTenantExits(request.getProfileFlatNo(),
					request.getProfileType());

			if (exits && null == request.getAddToExistingProfileType()) {
				throw new BusinessException(
						request.getProfileType() + " " + ErrorMessage.ERR_MESSAGE_37 + " " + request.getProfileType(),
						ErrorMessageCode.ERR_MESSAGE_37);
			}
			String profileId = createProfileId();
			List<ProfileAccountDetails> accontDetailsList = new ArrayList<>();
			ProfileAccountDetails accountDetails = new ProfileAccountDetails();
			accountDetails.setApartmentId(request.getHeader().getApartmentId());
			accountDetails.setApartmentName(profileId);
			List<String> flatIds = new ArrayList<>();
			flatIds.add(request.getProfileFlatNo());
			accountDetails.setFlatId(flatIds);
			accountDetails.setPosition(request.getProfilePosition());
			accountDetails.setProfileType(request.getProfileType());
			accountDetails.setStatus(SecuraConstants.PROFILE_STATUS_ACTIVE);
			accontDetailsList.add(accountDetails);
			String name = genericService.toJson(request.getProfileName());
			String profileAccountDetailsJson = genericService.toJson(accontDetailsList);
			String primaryAddres = genericService.toJson(request.getProfilePrimaryPostalAdrss());
			String otherAddress = genericService.toJson(request.getProfileOthrAdrss());
			Profile profile = new Profile(request, profileId, profileAccountDetailsJson, primaryAddres, otherAddress);
			profile.setPrflName(name);
			profileRepository.save(profile);
			if (request.getProfileType().equals(SecuraConstants.PROFILE_TYPE_OWNER)) {
				createOwnerProfile(profileId, request.getAddToExistingProfileType(), exits,request.getProfileFlatNo(),request.getHeader());
			}
			if (request.getProfileType().equals(SecuraConstants.PROFILE_TYPE_TENANT)) {
                createTenantProfile(profileId, request.getAddToExistingProfileType(), exits,request.getProfileFlatNo(),request.getHeader());
			}
			response.setProfileId(profileId);
			response.setMessage(SuccessMessage.SUCC_MESSAGE_09);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_09);
		}
		catch (BusinessException be) {
			LOGGER.error("Error while creating profile for userId: {}", request.getHeader().getUserId(), be);
			response.setMessage(be.getErrorMessage().toUpperCase());
			response.setMessageCode(be.getErrorMessageCode());
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
			/// validation
			Optional<Profile> profile = profileRepository.findById(request.getProfileId());
			if (profile.isPresent()) {

				Profile existingProfile = profile.get();
				String name = genericService.toJson(request.getProfileName());
				existingProfile.setPrflName(name);
				List<ProfileAccountDetails> accountDetails = genericService.fromJson(
						profile.get().getPrflAcountDetails(), new TypeReference<List<ProfileAccountDetails>>() {
						});
				if (null != request.getProfileFlatNo()) {
					if (null == accountDetails || accountDetails.isEmpty()) {
						ProfileAccountDetails details = new ProfileAccountDetails();
						details.setApartmentId(name);
						List<String> flatIds = new ArrayList<>();
						flatIds.add(request.getProfileFlatNo());
						details.setFlatId(flatIds);
						details.setPosition(request.getProfilePosition());
						details.setProfileType(request.getProfileType());
						details.setRole(request.getRole());
						details.setStatus(request.getProfileStatus());
						accountDetails.add(details);
						existingProfile.setPrflAcountDetails(genericService.toJson(accountDetails));
					} else {
						Optional<ProfileAccountDetails> details = accountDetails.stream()
								.filter(ac -> ac.getApartmentId().equals(request.getHeader().getApartmentId()))
								.findFirst();
						if (details.isPresent()) {
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
				response = new UpdateProfileResponse(request);
			}

			response.setMessage(SuccessMessage.SUCC_MESSAGE_10);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_10);
			response = new UpdateProfileResponse(request);
			response.setHeader(request.getHeader());
		} catch (Exception e) {
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
			Optional<Profile> profile = profileRepository.findById(request.getProfileID());

			if (profile.isPresent()) {
				getProfileResponse = new GetProfileResponse(profile.get(), request.getGenericHeader().getApartmentId());
				getProfileResponse.setPrflName(genericService.fromJson(profile.get().getPrflName(), Name.class));
				getProfileResponse
						.setPrflOthrAdrss(genericService.fromJson(profile.get().getPrflOthrAdrss(), Address.class));
				getProfileResponse.setPrimaryAddress(
						genericService.fromJson(profile.get().getPrflPrimaryPostalAdrss(), Address.class));
				getProfileResponse.setMessage(SuccessMessage.SUCC_MESSAGE_11);
				getProfileResponse.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_11);
				Optional<ApartmentMaster> apartmentMaster = apartmentRepository
						.findById(request.getGenericHeader().getApartmentId());
				if (apartmentMaster.isPresent()) {
					getProfileResponse.setApartmentName(apartmentMaster.get().getAprmntName());
				}

				if (null != profile.get().getCreat_usr_id()) {
					Optional<Profile> createprofile = profileRepository.findById(profile.get().getCreat_usr_id());
					if (createprofile.isPresent()) {
						StringBuffer createuserName = new StringBuffer();
						Name createuser = genericService.fromJson(createprofile.get().getPrflName(), Name.class);
						createuserName.append(createuser.getFirstName()).append(" ").append(createuser.getLastName())
								.append(" ").append("(").append(createprofile.get().getCreat_usr_id()).append(")");
						getProfileResponse.setCreatUsrName(createuserName.toString());
					}
				}
				if (null != profile.get().getLst_updt_usrId()) {
					Optional<Profile> lastUpdateprofile = profileRepository.findById(profile.get().getLst_updt_usrId());
					if (lastUpdateprofile.isPresent()) {
						StringBuffer lastUpdateprofileName = new StringBuffer();
						Name lastUpdateProfileuser = genericService.fromJson(lastUpdateprofile.get().getPrflName(),
								Name.class);
						lastUpdateprofileName.append(lastUpdateProfileuser.getFirstName()).append(" ")
								.append(lastUpdateProfileuser.getLastName()).append(" ").append("(")
								.append(lastUpdateprofile.get().getLst_updt_usrId()).append(")");
						getProfileResponse.setLstUpdtUsrName(lastUpdateprofileName.toString());
					}
				}

			} else {
				getProfileResponse.setMessage(ErrorMessage.ERR_MESSAGE_27);
				getProfileResponse.setMessageCode(ErrorMessageCode.ERR_MESSAGE_27);
			}
		} catch (Exception e) {
			LOGGER.error("Error while fetching profile detail for profileId: {}", request.getProfileID(), e);
			getProfileResponse.setMessage(ErrorMessage.ERR_MESSAGE_28);
			getProfileResponse.setMessageCode(ErrorMessageCode.ERR_MESSAGE_28);
		}
		getProfileResponse.setGenericHeader(request.getGenericHeader());
		return getProfileResponse;
	}

	public ManageTenantResponse updateTenantDetails(ManageTenantRequest request) {
		ManageTenantResponse response = new ManageTenantResponse();
		response.setHeader(request.getHeader());
		GetTenantRequest getTenantRequest = new GetTenantRequest();
		getTenantRequest.setGenericHeader(request.getHeader());
		getTenantRequest.setFlatId(request.getFlatId());

		GetTenantResponse getTenantResponse = getTenant(getTenantRequest);
		if (getTenantResponse.getProfile().size() > 0) {
			Tenant tenant = getTenantResponse.getTenant();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			if (null != request.getEndDate()) {
				String formatted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((request.getEndDate()));
				tenant.setEndDate(LocalDateTime.parse(formatted, formatter));
			}
			if (null != request.getStartDate()) {
				String formatted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((request.getStartDate()));
				tenant.setStartDate(LocalDateTime.parse(formatted, formatter));
			}
			tenant.setStatus(request.getStatus());
			tenant.setVerified(request.isVerified());
			String douments = genericService.toJson(request.getListOfDocuments());
			tenant.setDocument(douments);
			tenant.setLstUpdtUsrId(request.getHeader().getUserId());
			tenantRepository.save(tenant);
			response.setMessage(SuccessMessage.SUCC_MESSAGE_16);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_16);
		} else {
			response.setMessage(ErrorMessage.ERR_MESSAGE_36);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_36);
		}
		return response;
	}

	public GetTenantResponse getTenant(GetTenantRequest request) {
		GetTenantResponse getTenantResponse = new GetTenantResponse();
		getTenantResponse.setGenericHeader(request.getGenericHeader());
		Tenant currentTenantData = profileValidation.getCurrentFlatTenant(request.getFlatId());
		getTenantResponse.setTenant(currentTenantData);
		if (currentTenantData != null) {
			List<String> tenantProfiles = genericService.fromJson(currentTenantData.getPrflId(),
					new TypeReference<List<String>>() {
					});

			List<Optional<Profile>> profileList = tenantProfiles.stream().map(prfl -> profileRepository.findById(prfl))
					.collect(Collectors.toList());
			getTenantResponse.setProfile(profileList.stream().filter(prfl -> prfl.isPresent()).map(prfl -> prfl.get())
					.collect(Collectors.toList()));

			if (getTenantResponse.getProfile().size() > 0) {
				getTenantResponse.setMessage(SuccessMessage.SUCC_MESSAGE_15);
				getTenantResponse.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_15);
			} else {
				getTenantResponse.setMessage(ErrorMessage.ERR_MESSAGE_35);
				getTenantResponse.setMessageCode(ErrorMessageCode.ERR_MESSAGE_35);
			}
		} else {
			getTenantResponse.setMessage(ErrorMessage.ERR_MESSAGE_35);
			getTenantResponse.setMessageCode(ErrorMessageCode.ERR_MESSAGE_35);
		}
		return getTenantResponse;
	}



	public String createOwnerProfile(String profileID, String addtoExistingProfile, boolean profileExits,String flatId,GenericHeader header) throws BusinessException {
		String ownerId = null;
		if(profileExits) {
			List<Owner> ownerList= ownerRepository.findByFlatNo(flatId);
			Optional<Owner> currentOwner =ownerList.stream().filter(ow->ow.getEndDate()==null).findFirst();
			if(addtoExistingProfile.equals("Y")) {
				if(currentOwner.isPresent()) {
					List<String> ownerProfiles = genericService.fromJson(currentOwner.get().getPrflId(),
							new TypeReference<List<String>>() {
							});
					if(ownerProfiles.contains(profileID)) {
						throw new BusinessException(ErrorMessage.ERR_MESSAGE_39, ErrorMessageCode.ERR_MESSAGE_39);
					}
					ownerProfiles.add(profileID);
					currentOwner.get().setPrflId(genericService.toJson(ownerProfiles));
					ownerRepository.save(currentOwner.get());
					return currentOwner.get().getOwnerId();
				}
				
			}
			else {
				currentOwner.get().setEndDate(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
				currentOwner.get().setStatus(SecuraConstants.PROFILE_STATUS_INACTIVE);
				Owner owner = new Owner();
				owner.setOwnerId(createOwnertenantId(flatId,SecuraConstants.PROFILE_TYPE_OWNER));
				owner.setAprmt_id(header.getApartmentId());
				owner.setCreatUsrId(header.getUserId());
				owner.setFlatNo(flatId);
				List<String> profileIds = new ArrayList<>();
				profileIds.add(profileID);
				owner.setPrflId(genericService.toJson(profileIds));
				owner.setStatus(SecuraConstants.PROFILE_STATUS_ACTIVE);
				ownerRepository.save(owner);
				return owner.getOwnerId();
			}
			
		}
		else {
			Owner owner = new Owner();
			owner.setOwnerId(createOwnertenantId(flatId,SecuraConstants.PROFILE_TYPE_OWNER));
			owner.setAprmt_id(header.getApartmentId());
			owner.setCreatUsrId(header.getUserId());
			owner.setFlatNo(flatId);
			List<String> profileIds = new ArrayList<>();
			profileIds.add(profileID);
			owner.setPrflId(genericService.toJson(profileIds));
			owner.setStatus(SecuraConstants.PROFILE_STATUS_ACTIVE);
			ownerRepository.save(owner);
			return owner.getOwnerId();
		}
		
		return ownerId;
	}
	
	public String createTenantProfile(String profileID, String addtoExistingProfile, boolean profileExits,String flatId,GenericHeader header) throws BusinessException {
		String tenantId = null;
		if(profileExits) {
			List<Tenant> tenantList= tenantRepository.findByFlatNo(flatId);
			Optional<Tenant> currentTenant =tenantList.stream().filter(ow->ow.getEndDate()==null).findFirst();
			if(null!=addtoExistingProfile && addtoExistingProfile.equals("Y")) {
				if(currentTenant.isPresent()) {
					List<String> tenantrProfiles = genericService.fromJson(currentTenant.get().getPrflId(),
							new TypeReference<List<String>>() {
							});
					if(tenantrProfiles.contains(profileID)) {
						throw new BusinessException(ErrorMessage.ERR_MESSAGE_39, ErrorMessageCode.ERR_MESSAGE_39);
					}
					tenantrProfiles.add(profileID);
					currentTenant.get().setPrflId(genericService.toJson(tenantrProfiles));
					tenantRepository.save(currentTenant.get());
					return currentTenant.get().getTenantId(); 
				}
				
			}
			else if(null!=addtoExistingProfile && addtoExistingProfile.equals("N")) {
				currentTenant.get().setEndDate(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
				currentTenant.get().setStatus(SecuraConstants.PROFILE_STATUS_INACTIVE);
				Tenant tenant = new Tenant();
				tenant.setTenantId(createOwnertenantId(flatId,SecuraConstants.PROFILE_TYPE_TENANT));
				tenant.setAprmt_id(header.getApartmentId());
				tenant.setCreatUsrId(header.getUserId());
				tenant.setFlatNo(flatId);
				List<String> profileIds = new ArrayList<>();
				profileIds.add(profileID);
				tenant.setPrflId(genericService.toJson(profileIds));
				tenant.setStatus(SecuraConstants.PROFILE_STATUS_ACTIVE);
				tenantRepository.save(tenant);
				return tenant.getTenantId();
			}
			else {
				throw new BusinessException(ErrorMessage.ERR_MESSAGE_38, ErrorMessageCode.ERR_MESSAGE_38);
			}
			
		}
		else {
			Tenant tenant = new Tenant();
			tenant.setTenantId(createOwnertenantId(flatId,SecuraConstants.PROFILE_TYPE_TENANT));
			tenant.setAprmt_id(header.getApartmentId());
			tenant.setCreatUsrId(header.getUserId());
			tenant.setFlatNo(flatId);
			List<String> profileIds = new ArrayList<>();
			profileIds.add(profileID);
			tenant.setPrflId(genericService.toJson(profileIds));
			tenant.setStatus(SecuraConstants.PROFILE_STATUS_ACTIVE);
			tenantRepository.save(tenant);
			return tenant.getTenantId();
		}
		
		return tenantId;
	}

	public String createOwnertenantId(String flatID,String type) {
		StringBuilder id= new StringBuilder();
		id.append(type);
		id.append(flatID);
		Random ran = new Random();
		id.append(ran.nextInt());
		return id.toString();
	}
	
	public List<SearchProfileResponse> searchProfile(SearchProfileRequest request) {
		List<SearchProfileResponse> responseList= new ArrayList<>();
		List<Profile> profiles=profileRepository.searchProfiles(request.getInputKey(), request.getGenericHeader().getApartmentId());
		if(null!=profiles && !profiles.isEmpty()) {
			responseList=profiles.stream().map(rs->{SearchProfileResponse profileResponse= new SearchProfileResponse();
			profileResponse.setProfileId(rs.getPrflId());
			StringBuilder dispalyName= new StringBuilder();
			Name name=genericService.fromJson(rs.getPrflName(), Name.class);
			dispalyName.append(name.getFirstName()).append(" ").append(name.getMiddleName()).append(" ").append(name.getLastName()).append(" (").append(rs.getPrflId()).append(")");
			profileResponse.setDisplayName(dispalyName.toString());
			profileResponse.setProfilePic(rs.getProfile_pic());
			return profileResponse;
			}).collect(Collectors.toList());
		}
		return responseList;
	}
	
	public AddTenantResponse addTenant(AddTenantRequest request) {
		AddTenantResponse response = new AddTenantResponse();
		response.setHeader(request.getHeader());
		boolean profileExits = profileValidation.validateOwnerTenantExits(request.getFlatId(),
				SecuraConstants.PROFILE_TYPE_TENANT);
		String tenantId;
		try {
			tenantId = createTenantProfile(request.getProfileId(), request.getAddtoExisting(), profileExits,request.getFlatId(),request.getHeader());
			if(null!=tenantId) {
				response.setMessage(SuccessMessage.SUCC_MESSAGE_16);
				response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_16);
				response.setTenantId(tenantId);
			}
		} catch (BusinessException e) {
			response.setMessage( e.getErrorMessage());
			response.setMessageCode( e.getErrorMessageCode());
		}
		
		return response;
	}
}
