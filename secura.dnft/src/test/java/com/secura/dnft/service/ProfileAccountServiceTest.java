package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.secura.dnft.dao.ProfileAccountRepository;
import com.secura.dnft.entity.ProfileAccount;
import com.secura.dnft.entity.ProfileAccountId;
import com.secura.dnft.request.response.GetProfileAccountRequest;
import com.secura.dnft.request.response.UpdateProfileAccountRequest;

@ExtendWith(MockitoExtension.class)
class ProfileAccountServiceTest {
    @Mock
    private ProfileAccountRepository repository;

    @InjectMocks
    private ProfileServices service;

    @Test
    void createsAccountWithAuthenticatedActor() {
        UpdateProfileAccountRequest request = updateRequest();
        when(repository.findById(new ProfileAccountId("P1", "A1"))).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(ProfileAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProfileAccount account = service.updateProfileAccountDetails(request, "creator");

        assertEquals("P1", account.getProfileId());
        assertEquals("A1", account.getApartmentId());
        assertEquals("F1", account.getFlatId());
        assertEquals("manager", account.getJobeposition());
        assertEquals("creator", account.getCreateBy());
        assertEquals("creator", account.getLastUpdtBy());
    }

    @Test
    void updatesExistingAccountWithoutReplacingCreator() {
        UpdateProfileAccountRequest request = updateRequest();
        ProfileAccount existing = new ProfileAccount();
        existing.setCreateBy("original");
        when(repository.findById(new ProfileAccountId("P1", "A1"))).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(existing)).thenReturn(existing);

        assertSame(existing, service.updateProfileAccountDetails(request, "editor"));
        assertEquals("original", existing.getCreateBy());
        assertEquals("editor", existing.getLastUpdtBy());
        assertEquals("active", existing.getStatus());
        assertEquals("admin", existing.getRole());
        assertEquals("chair", existing.getPosition());
        verify(repository).saveAndFlush(existing);
    }

    @Test
    void getUsesBothKeyPartsAndReportsMissingAccount() {
        GetProfileAccountRequest request = new GetProfileAccountRequest();
        request.setProfileId("P1");
        request.setApartmentId("A2");
        ProfileAccount account = new ProfileAccount();
        when(repository.findById(new ProfileAccountId("P1", "A2")))
                .thenReturn(Optional.of(account), Optional.empty());

        assertSame(account, service.getProfileAccountDetails(request));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.getProfileAccountDetails(request));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    private UpdateProfileAccountRequest updateRequest() {
        UpdateProfileAccountRequest request = new UpdateProfileAccountRequest();
        request.setProfileId("P1");
        request.setApartmentId("A1");
        request.setFlatId("F1");
        request.setPosition("chair");
        request.setRole("admin");
        request.setJobeposition("manager");
        request.setStatus("active");
        return request;
    }
}
