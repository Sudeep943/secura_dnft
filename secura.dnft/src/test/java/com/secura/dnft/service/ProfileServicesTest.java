package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.dao.FlatRepository;
import com.secura.dnft.dao.OwnerRepository;
import com.secura.dnft.dao.ProfileRepository;
import com.secura.dnft.dao.TenantRepository;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.Owner;
import com.secura.dnft.entity.Profile;
import com.secura.dnft.entity.Tenant;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.GetOwnerRequest;
import com.secura.dnft.request.response.GetOwnerResponse;
import com.secura.dnft.request.response.RemoveOwnerTenantProfileRequest;
import com.secura.dnft.request.response.RemoveOwnerTenantProfileResponse;
import com.secura.dnft.validation.ProfileServiceValidation;

@ExtendWith(MockitoExtension.class)
class ProfileServicesTest {

	@Mock
	private OwnerRepository ownerRepository;

	@Mock
	private FlatRepository flatRepository;

	@Mock
	private ProfileRepository profileRepository;

	@Mock
	private TenantRepository tenantRepository;

	@Mock
	private ProfileServiceValidation profileValidation;

	@Mock
	private GenericService genericService;

	@InjectMocks
	private ProfileServices profileServices;

	@Test
	void hasClassLogger() throws NoSuchFieldException {
		Field loggerField = ProfileServices.class.getDeclaredField("LOGGER");
		assertEquals(Logger.class, loggerField.getType());
		assertTrue(Modifier.isStatic(loggerField.getModifiers()));
		assertTrue(Modifier.isFinal(loggerField.getModifiers()));
	}

	@Test
	void removeProfileFromOwnerTenant_shouldReturnBusinessError_whenOnlyOneOwnerProfileExists() {
		RemoveOwnerTenantProfileRequest request = buildRequest(SecuraConstants.PROFILE_TYPE_OWNER, "FLAT-1", "PRFL-1");
		Owner owner = new Owner();
		owner.setOwnerId("OWNER-1");
		owner.setFlatNo("FLAT-1");
		owner.setPrflId("[\"PRFL-1\"]");

		when(ownerRepository.findByFlatNo("FLAT-1")).thenReturn(List.of(owner));
		when(genericService.fromJson(any(String.class), any(TypeReference.class))).thenReturn(List.of("PRFL-1"));

		RemoveOwnerTenantProfileResponse response = profileServices.removeProfileFromOwnerTenant(request);

		assertEquals("Atleast One Owner Required.", response.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_38, response.getMessageCode());
		verify(ownerRepository, never()).save(any(Owner.class));
	}

	@Test
	void removeProfileFromOwnerTenant_shouldRemoveOwnerProfileAndCreateNewOwner_whenMultipleOwnerProfilesExist() {
		RemoveOwnerTenantProfileRequest request = buildRequest(SecuraConstants.PROFILE_TYPE_OWNER, "FLAT-2", "PRFL-1");
		Owner owner = new Owner();
		owner.setOwnerId("OWNER-2");
		owner.setFlatNo("FLAT-2");
		owner.setPrflId("[\"PRFL-1\",\"PRFL-2\"]");

		when(ownerRepository.findByFlatNo("FLAT-2")).thenReturn(List.of(owner));
		when(genericService.fromJson(any(String.class), any(TypeReference.class))).thenReturn(List.of("PRFL-1", "PRFL-2"));
		when(genericService.toJson(any(List.class))).thenReturn("[\"PRFL-2\"]");

		RemoveOwnerTenantProfileResponse response = profileServices.removeProfileFromOwnerTenant(request);

		assertEquals(SuccessMessage.SUCC_MESSAGE_17, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_17, response.getMessageCode());
		verify(ownerRepository, times(2)).save(any(Owner.class));
	}

	@Test
	void removeProfileFromOwnerTenant_shouldRemoveTenantProfileAndCreateNewTenant_whenTenantProfileType() {
		RemoveOwnerTenantProfileRequest request = buildRequest(SecuraConstants.PROFILE_TYPE_TENANT, "FLAT-3", "PRFL-1");
		Tenant tenant = new Tenant();
		tenant.setTenantId("TENANT-1");
		tenant.setFlatNo("FLAT-3");
		tenant.setPrflId("[\"PRFL-1\",\"PRFL-2\"]");

		when(tenantRepository.findByFlatNo("FLAT-3")).thenReturn(List.of(tenant));
		when(genericService.fromJson(any(String.class), any(TypeReference.class))).thenReturn(List.of("PRFL-1", "PRFL-2"));
		when(genericService.toJson(any(List.class))).thenReturn("[\"PRFL-2\"]");

		RemoveOwnerTenantProfileResponse response = profileServices.removeProfileFromOwnerTenant(request);

		assertEquals(SuccessMessage.SUCC_MESSAGE_17, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_17, response.getMessageCode());
		verify(tenantRepository, never()).delete(any(Tenant.class));
		verify(tenantRepository, times(2)).save(any(Tenant.class));
	}

	@Test
	void getOwner_shouldReturnProfilesFromActiveOwner() {
		GetOwnerRequest request = new GetOwnerRequest();
		request.setFlatId("FLAT-10");

		Owner owner = new Owner();
		owner.setOwnerId("OWNER-10");
		owner.setFlatNo("FLAT-10");
		owner.setPrflId("[\"PRFL-10\"]");

		Profile profile = new Profile();
		profile.setPrflId("PRFL-10");

		when(profileValidation.getCurrentFlatOwner("FLAT-10")).thenReturn(owner);
		when(genericService.fromJson(eq("[\"PRFL-10\"]"), any(TypeReference.class))).thenReturn(List.of("PRFL-10"));
		when(profileRepository.findById("PRFL-10")).thenReturn(Optional.of(profile));

		GetOwnerResponse response = profileServices.getOwner(request);

		assertEquals(owner, response.getOwner());
		assertEquals(1, response.getProfile().size());
		assertEquals(SuccessMessage.SUCC_MESSAGE_15, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_15, response.getMessageCode());
		verify(flatRepository, never()).findById(any(String.class));
	}

	@Test
	void getOwner_shouldFallbackToFlatOwnerList_whenOwnerRecordMissing() {
		GetOwnerRequest request = new GetOwnerRequest();
		request.setFlatId("FLAT-20");

		Flat flat = new Flat();
		flat.setFlatNo("FLAT-20");
		flat.setFlatOwnerList("[\"PRFL-20\"]");

		Profile profile = new Profile();
		profile.setPrflId("PRFL-20");

		when(profileValidation.getCurrentFlatOwner("FLAT-20")).thenReturn(null);
		when(flatRepository.findById("FLAT-20")).thenReturn(Optional.of(flat));
		when(genericService.fromJson(eq("[\"PRFL-20\"]"), any(TypeReference.class))).thenReturn(List.of("PRFL-20"));
		when(profileRepository.findById("PRFL-20")).thenReturn(Optional.of(profile));

		GetOwnerResponse response = profileServices.getOwner(request);

		assertNull(response.getOwner());
		assertEquals(1, response.getProfile().size());
		assertEquals(SuccessMessage.SUCC_MESSAGE_15, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_15, response.getMessageCode());
	}

	private RemoveOwnerTenantProfileRequest buildRequest(String profileType, String flatId, String id) {
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APT-1");
		header.setUserId("PRFL-ADMIN");
		header.setRole("ADMIN");

		RemoveOwnerTenantProfileRequest request = new RemoveOwnerTenantProfileRequest();
		request.setHeader(header);
		request.setFlatId(flatId);
		request.setId(id);
		request.setProfileType(profileType);
		return request;
	}
}
