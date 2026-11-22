package com.secura.dnft.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.secura.dnft.entity.ProfileAccount;
import com.secura.dnft.request.response.GetProfileAccountRequest;
import com.secura.dnft.request.response.UpdateProfileAccountRequest;
import com.secura.dnft.service.ProfileService;

import static org.mockito.Mockito.mock;

class ProfileAccountControllerTest {
    private final ProfileService service = mock(ProfileService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ProfileAccountController(service)).build();
    }

    @Test
    void updateUsesPrincipalAndReturnsAccount() throws Exception {
        ProfileAccount account = new ProfileAccount();
        account.setProfileId("P1");
        account.setApartmentId("A1");
        when(service.updateProfileAccountDetails(any(UpdateProfileAccountRequest.class), eq("alice")))
                .thenReturn(account);

        mvc.perform(post("/profile/updateProfileAccountDetails").principal(() -> "alice")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"profileId\":\"P1\",\"apartmentId\":\"A1\",\"status\":\"active\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileId").value("P1"))
                .andExpect(jsonPath("$.apartmentId").value("A1"));
        verify(service).updateProfileAccountDetails(any(UpdateProfileAccountRequest.class), eq("alice"));
    }

    @Test
    void rejectsMissingOrBlankIdentity() throws Exception {
        mvc.perform(post("/profile/updateProfileAccountDetails").principal(() -> "alice")
                .contentType(MediaType.APPLICATION_JSON).content("{\"profileId\":\" \",\"apartmentId\":\"A1\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/profile/getProfileAccountDetails")
                .contentType(MediaType.APPLICATION_JSON).content("{\"profileId\":\"P1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReturnsNotFoundForMissingAccount() throws Exception {
        when(service.getProfileAccountDetails(any(GetProfileAccountRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile account not found"));
        mvc.perform(post("/profile/getProfileAccountDetails")
                .contentType(MediaType.APPLICATION_JSON).content("{\"profileId\":\"P1\",\"apartmentId\":\"A1\"}"))
                .andExpect(status().isNotFound());
    }
}
