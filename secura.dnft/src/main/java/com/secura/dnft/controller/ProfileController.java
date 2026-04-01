package com.secura.dnft.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
import com.secura.dnft.service.ProfileServices;
import com.secura.dnft.validation.ProfileServiceValidation;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/profile")
public class ProfileController {

	 @Autowired
	 private ProfileServices profileServices;
	 
	 @Autowired
	 private ProfileServiceValidation profileServiceValidation;
	 
    @PostMapping("/createProfile")
    @CrossOrigin(origins = "*")
    public CreateProfileResponse createProfile(@RequestBody CreateProfileRequest request) {
    	CreateProfileResponse response = new CreateProfileResponse();
    	response=profileServices.createProfile(request);
    	return response;
            }
    
    	 
   @PostMapping("/updateProfile")
   @CrossOrigin(origins = "*")
   public UpdateProfileResponse updateProfile(@RequestBody UpdateProfileRequest request) {
	   UpdateProfileResponse response = new UpdateProfileResponse();
	   response=profileServices.updateProfile(request);
   	return response;
           }
   
   @PostMapping("/getProfile")
   @CrossOrigin(origins = "*")
   public GetProfileResponse getProfile(@RequestBody GetProfileRequest request) {
	   GetProfileResponse response = new GetProfileResponse();
	   response=profileServices.getProfileDetail(request);
   	return response;
           }
   
   @PostMapping("/updateTenantDetails")
   @CrossOrigin(origins = "*")
   public ManageTenantResponse updateTenantDetails(@RequestBody ManageTenantRequest request) {
	   ManageTenantResponse response = new ManageTenantResponse();
	   response=profileServices.updateTenantDetails(request);
   	return response;
           }
   
   @PostMapping("/getTenant")
   @CrossOrigin(origins = "*")
   public GetTenantResponse getTenant(@RequestBody GetTenantRequest request) {
	   GetTenantResponse response = new GetTenantResponse();
	   response=profileServices.getTenant(request);
   	return response;
           }
   
   @GetMapping("/validateCurrentOwner/{flatId}/{profileType}")
   @CrossOrigin(origins = "*")
   public boolean validateOwnerTenantExits(@PathVariable   String flatId,@PathVariable  String profileType) {
	   return profileServiceValidation.validateOwnerTenantExits(flatId,profileType);
           }
}
