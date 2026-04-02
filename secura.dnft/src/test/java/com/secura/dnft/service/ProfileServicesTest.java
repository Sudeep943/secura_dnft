package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.dao.OwnerRepository;
import com.secura.dnft.dao.TenantRepository;
import com.secura.dnft.entity.Owner;
import com.secura.dnft.entity.Tenant;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.RemoveOwnerTenantProfileRequest;
import com.secura.dnft.request.response.RemoveOwnerTenantProfileResponse;

@ExtendWith(MockitoExtension.class)
class ProfileServicesTest {

	@Mock
	private OwnerRepository ownerRepository;

	@Mock
	private TenantRepository tenantRepository;

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
		verify(tenantRepository, times(1)).delete(any(Tenant.class));
		verify(tenantRepository, times(2)).save(any(Tenant.class));
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
