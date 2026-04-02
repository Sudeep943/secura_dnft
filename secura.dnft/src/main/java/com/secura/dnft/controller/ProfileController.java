package com.secura.dnft.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.request.response.AddTenantRequest;
import com.secura.dnft.request.response.AddTenantResponse;
import com.secura.dnft.request.response.AddOwnerRequest;
import com.secura.dnft.request.response.AddOwnerResponse;
import com.secura.dnft.request.response.CreateProfileRequest;
import com.secura.dnft.request.response.CreateProfileResponse;
import com.secura.dnft.request.response.GetOwnerRequest;
import com.secura.dnft.request.response.GetOwnerResponse;
import com.secura.dnft.request.response.GetProfileRequest;
import com.secura.dnft.request.response.GetProfileResponse;
import com.secura.dnft.request.response.GetTenantRequest;
import com.secura.dnft.request.response.GetTenantResponse;
import com.secura.dnft.request.response.ManageOwnerRequest;
import com.secura.dnft.request.response.ManageOwnerResponse;
import com.secura.dnft.request.response.ManageTenantRequest;
import com.secura.dnft.request.response.ManageTenantResponse;
import com.secura.dnft.request.response.RemoveOwnerTenantProfileRequest;
import com.secura.dnft.request.response.RemoveOwnerTenantProfileResponse;
import com.secura.dnft.request.response.SearchProfileRequest;
import com.secura.dnft.request.response.SearchProfileResponse;
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

   @PostMapping("/updateOwnerDetails")
   @CrossOrigin(origins = "*")
   public ManageOwnerResponse updateOwnerDetails(@RequestBody ManageOwnerRequest request) {
	   ManageOwnerResponse response = new ManageOwnerResponse();
	   response=profileServices.updateOwnerDetails(request);
   	return response;
           }
   
   @PostMapping("/getTenant")
   @CrossOrigin(origins = "*")
   public GetTenantResponse getTenant(@RequestBody GetTenantRequest request) {
	   GetTenantResponse response = new GetTenantResponse();
	   response=profileServices.getTenant(request);
   	return response;
           }

   @PostMapping("/getOwner")
   @CrossOrigin(origins = "*")
   public GetOwnerResponse getOwner(@RequestBody GetOwnerRequest request) {
	   GetOwnerResponse response = new GetOwnerResponse();
	   response=profileServices.getOwner(request);
   	return response;
           }
   
   @GetMapping("/validateCurrentOwner/{flatId}/{profileType}")
   @CrossOrigin(origins = "*")
   public boolean validateOwnerTenantExits(@PathVariable   String flatId,@PathVariable  String profileType) {
	   return profileServiceValidation.validateOwnerTenantExits(flatId,profileType);
           }
   
   @PostMapping("/searchProfile")
   @CrossOrigin(origins = "*")
   public List<SearchProfileResponse> searchProfile(@RequestBody SearchProfileRequest request) {
	   List<SearchProfileResponse> response = new ArrayList<>();
	   response=profileServices.searchProfile(request);
   	return response;
           }
   

   @PostMapping("/addTenant")
   @CrossOrigin(origins = "*")
   public AddTenantResponse addTenant(@RequestBody AddTenantRequest request) {
	   AddTenantResponse response = new AddTenantResponse();
	   response=profileServices.addTenant(request);
   	return response;
           }

   @PostMapping("/addOwner")
   @CrossOrigin(origins = "*")
   public AddOwnerResponse addOwner(@RequestBody AddOwnerRequest request) {
	   AddOwnerResponse response = new AddOwnerResponse();
	   response=profileServices.addOwner(request);
   	return response;
           }
   
   @PostMapping("/removeProfileFromOwnerTenant")
   @CrossOrigin(origins = "*")
   public RemoveOwnerTenantProfileResponse removeProfileFromOwnerTenant(@RequestBody RemoveOwnerTenantProfileRequest request) {
	   RemoveOwnerTenantProfileResponse response = profileServices.removeProfileFromOwnerTenant(request);
	   return response;
           }
}
