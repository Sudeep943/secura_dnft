package com.secura.dnft.controller;

import java.security.Principal;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.entity.ProfileAccount;
import com.secura.dnft.request.response.GetProfileAccountRequest;
import com.secura.dnft.request.response.UpdateProfileAccountRequest;
import com.secura.dnft.service.ProfileService;

import jakarta.validation.Valid;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/profile")
public class ProfileAccountController {
    private final ProfileService profileService;

    public ProfileAccountController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @PostMapping("/getProfileAccountDetails")
    public ProfileAccount getProfileAccountDetails(@Valid @RequestBody GetProfileAccountRequest request) {
        return profileService.getProfileAccountDetails(request);
    }

    @PostMapping("/updateProfileAccountDetails")
    public ProfileAccount updateProfileAccountDetails(@Valid @RequestBody UpdateProfileAccountRequest request,
            Principal principal) {
        return profileService.updateProfileAccountDetails(request, principal.getName());
    }
}
