package com.secura.dnft.service;

import java.util.List;

import com.secura.dnft.entity.Profile;
import com.secura.dnft.entity.ProfileAccount;
import com.secura.dnft.request.response.AddOwnerRequest;
import com.secura.dnft.request.response.AddOwnerResponse;
import com.secura.dnft.request.response.AddTenantRequest;
import com.secura.dnft.request.response.AddTenantResponse;
import com.secura.dnft.request.response.CreateProfileRequest;
import com.secura.dnft.request.response.CreateProfileResponse;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.GetOwnerRequest;
import com.secura.dnft.request.response.GetOwnerResponse;
import com.secura.dnft.request.response.GetProfileAccountRequest;
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
import com.secura.dnft.request.response.UpdateProfileAccountRequest;
import com.secura.dnft.request.response.UpdateProfileRequest;
import com.secura.dnft.request.response.UpdateProfileResponse;
import com.secura.dnft.security.BusinessException;

public interface ProfileService {
    String createProfileId();
    CreateProfileResponse createProfile(CreateProfileRequest request);
    UpdateProfileResponse updateProfile(UpdateProfileRequest request);
    GetProfileResponse getProfileDetail(GetProfileRequest request);
    ManageTenantResponse updateTenantDetails(ManageTenantRequest request);
    ManageOwnerResponse updateOwnerDetails(ManageOwnerRequest request);
    GetTenantResponse getTenant(GetTenantRequest request);
    GetOwnerResponse getOwner(GetOwnerRequest request);
    String createOwnerProfile(String profileId, String addToExistingProfile, boolean profileExists, String flatId, GenericHeader header) throws BusinessException;
    String createTenantProfile(String profileId, String addToExistingProfile, boolean profileExists, String flatId, GenericHeader header) throws BusinessException;
    String createOwnertenantId(String flatId, String type);
    List<SearchProfileResponse> searchProfile(SearchProfileRequest request);
    AddTenantResponse addTenant(AddTenantRequest request) throws Exception;
    AddOwnerResponse addOwner(AddOwnerRequest request) throws Exception;
    RemoveOwnerTenantProfileResponse removeProfileFromOwnerTenant(RemoveOwnerTenantProfileRequest request);
    Profile getProfileEntity(String id) throws BusinessException;
    ProfileAccount getProfileAccountDetails(GetProfileAccountRequest request);
    ProfileAccount updateProfileAccountDetails(UpdateProfileAccountRequest request, String actor);
}
