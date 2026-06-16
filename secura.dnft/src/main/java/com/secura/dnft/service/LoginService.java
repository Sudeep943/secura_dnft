package com.secura.dnft.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.bean.ProfileAccountDetails;
import com.secura.dnft.dao.ApartmentRepository;
import com.secura.dnft.dao.ProfileRepository;
import com.secura.dnft.entity.ApartmentMaster;
import com.secura.dnft.entity.Profile;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.Name;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.LoginRequest;
import com.secura.dnft.request.response.LoginResponse;
import com.secura.dnft.request.response.UpdatePasswordRequest;
import com.secura.dnft.request.response.UpdatePasswordResponse;
import com.secura.dnft.security.AuthCryptoUtil;
import com.secura.dnft.security.JwtUtil;
import com.secura.dnft.security.LoginException;

@Service
public class LoginService {

	@Autowired
	JwtUtil jwtUtil;

	@Autowired
	ProfileRepository profileRepository;

	// @Autowired
	// DataPrivacyService dataPrivacyService;

	@Autowired
	GenericService genericService;
	
	@Autowired
	ProfileServices profileServices;

	@Autowired
	ApartmentRepository apartmentRepository;

	@Autowired
	AuthCryptoUtil authCryptoUtil;
	
	private String apartmentId=null;
	private String flatId=null;
	

	public LoginResponse login(LoginRequest loginRequest) throws Exception {
		 apartmentId=null;
		 flatId=null;
		LoginResponse loginResponse = new LoginResponse();
		// String passwords=authCryptoUtil.decrypt(loginRequest.getPassword());
//		Optional<Profile> profile = java.util.Optional.empty();
//		Optional<Profile> prfl = profileRepository.findById(loginRequest.getUsername());
//		if (prfl.isEmpty()) {
//			List<Profile> profileByphoneList=profileRepository.findByPrflPhoneNo(loginRequest.getUsername());
//			if(null!=profileByphoneList && !profileByphoneList.isEmpty()) {
//				Profile profileByphone=profileByphoneList.get(0);
//				profile = Optional.ofNullable(profileByphone);
//				}else {
//					throw new LoginException(ErrorMessage.ERR_MESSAGE_55, ErrorMessageCode.ERR_MESSAGE_55, null);
//			}
//			
//		} else {
//			profile = prfl;
//		}
    	Optional<Profile> profile = java.util.Optional.empty();
		profile=Optional.of(profileServices.getProfileEntity(loginRequest.getUsername()));
		try {
			if (profile.isPresent()) {
				String password = profile.get().getPassword();
				if (null == password || password.isEmpty()) {
					if (null == loginRequest.getOtp() || loginRequest.getOtp().isEmpty()) {
						generateOTP(profile.get().getPrflPhoneNo());
						loginResponse.setMessage(ErrorMessage.ERR_MESSAGE_29);
						loginResponse.setMessageCode(ErrorMessageCode.ERR_MESSAGE_29);
						return loginResponse;
					} else {
						if (validOTP(profile.get().getPrflPhoneNo(), loginRequest.getOtp())) {
							loginResponse.setMessage(ErrorMessage.ERR_MESSAGE_30);
							loginResponse.setMessageCode(ErrorMessageCode.ERR_MESSAGE_30);
							return loginResponse;
						}
					}
				} else {
					if (authCryptoUtil.decrypt(password).equals(authCryptoUtil.decrypt(loginRequest.getPassword()))) {
						if(loginRequest.getUsername().equalsIgnoreCase("admin")) {
							return getAdminProfile(loginRequest,profile.get(),"APRT001");
						}
						
						List<ProfileAccountDetails> accountDetails = genericService.fromJson(
								profile.get().getPrflAcountDetails(), new TypeReference<List<ProfileAccountDetails>>() {
								});
						ProfileAccountDetails acDetails = new ProfileAccountDetails();
						
//						if(accountDetails.size()>1) {
//							if(null==loginRequest.getFlatID() || null==loginRequest.getApartmentId()) {
//								throw new LoginException(ErrorMessage.ERR_MESSAGE_54, ErrorMessageCode.ERR_MESSAGE_54, createFlatDetailsMap(accountDetails));
//							}
//							else {
//								acDetails=	accountDetails.stream().filter(ad->ad.getApartmentId().equals(loginRequest.getApartmentId())&& ad.getFlatId().contains(loginRequest.getFlatID())).findFirst().get();
//							}
//						}
//						else {
//							 acDetails = accountDetails.get(0);	
//						}
						if(!validateLoginDetils(accountDetails,loginRequest)) {
							throw new LoginException(ErrorMessage.ERR_MESSAGE_54, ErrorMessageCode.ERR_MESSAGE_54, createFlatDetailsMap(accountDetails));
						}
						
						
						GenericHeader genericHeader = new GenericHeader();
						genericHeader.setUserId(loginRequest.getUsername());
						genericHeader.setApartmentId(apartmentId);
						genericHeader.setRole(SecuraConstants.PROFILE_TYPE_OWNER);
						//genericHeader.setRole(getRole(accountDetails,loginRequest.getFlatID(),loginRequest.getApartmentId()));
//						if(genericHeader.getRole().equals(SecuraConstants.PROFILE_TYPE_OWNER)) {
//							genericHeader.setPosition(SecuraConstants.PROFILE_TYPE_MEMBER);
//						}
//						else {
//							genericHeader.setPosition(SecuraConstants.PROFILE_TYPE_TENANT);
//						}
						
						
						genericHeader.setFlatNo(flatId);
						Name name = genericService.fromJson(profile.get().getPrflName(), Name.class);
						StringBuffer newName = new StringBuffer();
						newName.append(name.getFirstName()).append(" ").append(name.getLastName());
						genericHeader.setProfileName(newName.toString());
						Optional<ApartmentMaster> aprtmnt = apartmentRepository.findById(apartmentId);
						if (aprtmnt.isPresent()) {
							genericHeader.setApartmentName(aprtmnt.get().getAprmntName());
						}
						loginResponse.setHeader(genericHeader);
						loginResponse.setToken(jwtUtil.generateToken(loginRequest.getUsername()));
						loginResponse.setMessage(SuccessMessage.SUCC_MESSAGE_12);
						loginResponse.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_12);
					} else {
						loginResponse.setMessage(ErrorMessage.ERR_MESSAGE_31);
						loginResponse.setMessageCode(ErrorMessageCode.ERR_MESSAGE_31);
					}
				}
			}
		}
		catch (LoginException le) {
			le.printStackTrace();
    		loginResponse.setAccountDetails(le.getAccountDetails());
			loginResponse.setMessage(le.getErrorMessage());
			loginResponse.setMessageCode(le.getErrorMessageCode());
		}
		catch (Exception e) {
			e.printStackTrace();
			loginResponse.setMessage(ErrorMessage.ERR_MESSAGE_33);
			loginResponse.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return loginResponse;
	}
	
	private LoginResponse getAdminProfile(LoginRequest loginRequest,Profile profile, String apartmentid){
		LoginResponse loginResponse = new LoginResponse();
		GenericHeader genericHeader = new GenericHeader();
		genericHeader.setUserId(loginRequest.getUsername());
		genericHeader.setApartmentId(apartmentid);
		//genericHeader.setRole(getRole(accountDetails,loginRequest.getFlatID(),loginRequest.getApartmentId()));
		genericHeader.setPosition(SecuraConstants.PROFILE_TYPE_MEMBER);
		genericHeader.setFlatNo("2054");
		Name name = genericService.fromJson(profile.getPrflName(), Name.class);
		StringBuffer newName = new StringBuffer();
		newName.append(name.getFirstName()).append(" ").append(name.getLastName());
		genericHeader.setProfileName(newName.toString());
		Optional<ApartmentMaster> aprtmnt = apartmentRepository.findById(apartmentid);
		if (aprtmnt.isPresent()) {
			genericHeader.setApartmentName(aprtmnt.get().getAprmntName());
		}
		loginResponse.setHeader(genericHeader);
		loginResponse.setToken(jwtUtil.generateToken(loginRequest.getUsername()));
		loginResponse.setMessage(SuccessMessage.SUCC_MESSAGE_12);
		loginResponse.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_12);
		return loginResponse;
	}
	
	private boolean validateLoginDetils(List<ProfileAccountDetails> accountDetails,LoginRequest loginRequest) {
		boolean validate =true;
		if(accountDetails.size()>1) {
			if(loginRequest.getApartmentId()==null || loginRequest.getFlatID()==null) {
				return false;
			}
			else {
				apartmentId=loginRequest.getApartmentId();
				flatId=loginRequest.getFlatID();
			}
		}
		else if(accountDetails.size()==1) {
			Map<String,List<String>> flatDetailsMap=accountDetails.get(0).getFlatDetailsMap();
			boolean hasMultipleValues= flatDetailsMap.values()
			        .stream()
			        .anyMatch(list -> list.size() > 1);	
			if(hasMultipleValues && (loginRequest.getApartmentId()==null || loginRequest.getFlatID()==null)){
				return false;
			}
			else if(loginRequest.getApartmentId()!=null && loginRequest.getFlatID()!=null) {
				flatId=loginRequest.getFlatID();
				apartmentId=loginRequest.getApartmentId();
			}
			else {
				flatId=flatDetailsMap.get(SecuraConstants.PROFILE_TYPE_OWNER).get(0);
				if(null==flatId) {
					flatId=flatDetailsMap.get(SecuraConstants.PROFILE_TYPE_TENANT).get(0);
				}
				apartmentId=accountDetails.get(0).getApartmentId();

			}
		}
		
//		
//		if(accountDetails.size()>1) {
//			if(loginRequest.getApartmentId()==null || loginRequest.getFlatID()==null) {
//				return false;
//			}
//		}
//		else if(accountDetails.size()==1) {
//			if(flatDetailsMap.keySet().size()>1 && loginRequest.getFlatID()==null) {
//				return false;
//			}
//			else {
//				boolean hasMultipleValues= flatDetailsMap.values()
//				        .stream()
//				        .anyMatch(list -> list.size() > 1);	
//				if(hasMultipleValues && loginRequest.getApartmentId()==null && loginRequest.getFlatID()==null){
//					return false;
//				}
//			}
//			
//		}
		return validate;
	}
	
	private String getRole(List<ProfileAccountDetails> accountDetailList, String flatId, String apartmentId) {
		ProfileAccountDetails accountDetail=accountDetailList.stream().filter(ac->ac.getApartmentId().equals(apartmentId)).findFirst().get(); 
		Map<String,List<String>> flatDetailsMap=accountDetail.getFlatDetailsMap();
		List<String> keys = flatDetailsMap.entrySet()
		        .stream()
		        .filter(entry -> entry.getValue().contains(flatId))
		        .map(Map.Entry::getKey)
		        .collect(Collectors.toList());
		return keys.get(0);
	}
	
	private Map<String,List<String>>createFlatDetailsMap(List<ProfileAccountDetails> accountDetailsList) {
		Map<String,List<String>> flatDetailsMap= new HashMap<>();
		
	for(ProfileAccountDetails accountDetails:accountDetailsList) {
		String apartmentIdKey=getKey(accountDetails.getApartmentId());
		if(flatDetailsMap.get(apartmentIdKey)== null || flatDetailsMap.get(apartmentIdKey).isEmpty() ) {
			flatDetailsMap.put(accountDetails.getApartmentId(), accountDetails.getFlatDetailsMap().values()
			        .stream()
			        .flatMap(List::stream)
			        .distinct()
			        .collect(Collectors.toList()));
		}
		else {
			List<String>flatIdList=flatDetailsMap.get(accountDetails.getApartmentId());
			flatIdList.addAll(accountDetails.getFlatDetailsMap().values()
			        .stream()
			        .flatMap(List::stream)
			        .distinct()
			        .collect(Collectors.toList()));
			flatDetailsMap.put(apartmentIdKey, flatIdList);
		}
	}

	return flatDetailsMap;
	}
	
	private String getKey(String apartmentId) {
		StringBuilder apartmentKey = new StringBuilder();
		Optional<ApartmentMaster> aprtmnt = apartmentRepository.findById(apartmentId);
		if(aprtmnt.isPresent()) {
			apartmentKey=apartmentKey.append(aprtmnt.get().getAprmntName());
			apartmentKey=apartmentKey.append("_");
			apartmentKey=apartmentKey.append(apartmentId);
			
		}
		return apartmentKey.toString();
	}

	public UpdatePasswordResponse updatePassword(UpdatePasswordRequest profileRequest) throws LoginException {
		UpdatePasswordResponse response = new UpdatePasswordResponse();
		Optional<Profile> profile = java.util.Optional.empty();
		//Optional<Profile> profile = profileRepository.findById(profileRequest.getProfileId());
		Optional<Profile> prfl = profileRepository.findById(profileRequest.getProfileId());
		if (prfl.isEmpty()) {
			List<Profile> profileByphoneList=profileRepository.findByPrflPhoneNo(profileRequest.getProfileId());
			if(null!=profileByphoneList && !profileByphoneList.isEmpty()) {
				Profile profileByphone=profileByphoneList.get(0);
				profile = Optional.ofNullable(profileByphone);
				}else {
					throw new LoginException(ErrorMessage.ERR_MESSAGE_55, ErrorMessageCode.ERR_MESSAGE_55, null);
			}
			
		} else {
			profile = prfl;
		}
		
		try {
			if (profile.isPresent()) {
				if (profileRequest.isOtpVerified()) {
					profile.get().setPassword(profileRequest.getNewPassword());
					profileRepository.save(profile.get());
					response.setMessage(SuccessMessage.SUCC_MESSAGE_13);
					response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_13);
				} else {
					if (authCryptoUtil.decrypt(profile.get().getPassword()).equals(profileRequest.getOldPassword())) {
						profile.get().setPassword(profileRequest.getNewPassword());
						profileRepository.save(profile.get());
						response.setMessage(SuccessMessage.SUCC_MESSAGE_13);
						response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_13);
					} else {
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

	public boolean validOTP(String mobileNumber, String OTP) {
		if (OTP.equals("1234")) {
			return true;
		}
		return false;
	}
}
