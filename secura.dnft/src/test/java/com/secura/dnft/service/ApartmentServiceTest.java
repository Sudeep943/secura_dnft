package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.bean.BankAccountDetails;
import com.secura.dnft.bean.ExecutiveMember;
import com.secura.dnft.dao.ApartmentRepository;
import com.secura.dnft.entity.ApartmentMaster;
import com.secura.dnft.generic.bean.Address;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.GetApartmentDetailsRequest;
import com.secura.dnft.request.response.GetApartmentDetailsResponse;
import com.secura.dnft.request.response.UpdateApartmentDetailsRequest;
import com.secura.dnft.request.response.UpdateApartmentDetailsResponse;
import com.secura.dnft.validation.CommonValidations;

@ExtendWith(MockitoExtension.class)
class ApartmentServiceTest {

	@Mock
	private ApartmentRepository repository;

	@Mock
	private GenericService genericService;

	@Mock
	private CommonValidations commonValidations;

	@InjectMocks
	private ApartmentService apartmentService;

	@Test
	void updateApartmentDetails_shouldSerializeEncryptAndPersist() throws Exception {
		UpdateApartmentDetailsRequest request = buildUpdateRequest();
		ApartmentMaster apartment = new ApartmentMaster();
		apartment.setAprmntId("APR-1");

		doNothing().when(commonValidations).genericHeaderValidation(request.getGenericHeader());
		when(repository.findById("APR-1")).thenReturn(Optional.of(apartment));
		when(genericService.toJson(request.getAddress())).thenReturn("{\"city\":\"Springfield\"}");
		when(genericService.toJson(request.getBankAccountDetails())).thenReturn("[{\"bankName\":\"ABC Bank\"}]");
		when(genericService.encrypt("[{\"bankName\":\"ABC Bank\"}]")).thenReturn("encrypted-bank-json");
		when(genericService.toJson(request.getExecutiveMemberList())).thenReturn("[{\"memberId\":\"MEM-1\"}]");

		UpdateApartmentDetailsResponse response = apartmentService.updateApartmentDetails(request);

		assertEquals(SuccessMessage.SUCC_MESSAGE_35, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_35, response.getMessageCode());
		ArgumentCaptor<ApartmentMaster> captor = ArgumentCaptor.forClass(ApartmentMaster.class);
		verify(repository).save(captor.capture());
		assertEquals("{\"city\":\"Springfield\"}", captor.getValue().getAprmntAddress());
		assertEquals("encrypted-bank-json", captor.getValue().getAprmnt_bank_acccount_list());
		assertEquals("[{\"memberId\":\"MEM-1\"}]", captor.getValue().getAprmnt_executive_role_list());
		assertEquals("logo-data", captor.getValue().getAprmnt_logo());
		assertEquals("letter-head-data", captor.getValue().getAprmntLetterHead());
		assertEquals("ADMIN-1", captor.getValue().getLst_updt_usrid());
	}

	@Test
	void getApartmentDetails_shouldDecryptAndDeserializeStoredValues() throws Exception {
		GetApartmentDetailsRequest request = new GetApartmentDetailsRequest();
		request.setGenericHeader(buildHeader());
		ApartmentMaster apartment = new ApartmentMaster();
		apartment.setAprmntId("APR-1");
		apartment.setAprmnt_logo("logo-data");
		apartment.setAprmntAddress("{\"city\":\"Springfield\"}");
		apartment.setAprmnt_bank_acccount_list("encrypted-bank-json");
		apartment.setAprmnt_executive_role_list("[{\"memberId\":\"MEM-1\"}]");
		apartment.setAprmntLetterHead("letter-head-data");
		Address address = new Address();
		address.setAddressLine1("12 Main Street");
		address.setCity("Springfield");
		BankAccountDetails bankAccountDetails = new BankAccountDetails();
		bankAccountDetails.setBankName("ABC Bank");
		ExecutiveMember executiveMember = new ExecutiveMember();
		executiveMember.setMemberId("MEM-1");
		executiveMember.setPositionName("Secretary");

		doNothing().when(commonValidations).genericHeaderValidation(request.getGenericHeader());
		when(repository.findById("APR-1")).thenReturn(Optional.of(apartment));
		when(genericService.fromJson("{\"city\":\"Springfield\"}", Address.class)).thenReturn(address);
		when(genericService.decrypt("encrypted-bank-json")).thenReturn("[{\"bankName\":\"ABC Bank\"}]");
		when(genericService.fromJson(eq("[{\"bankName\":\"ABC Bank\"}]"), any(TypeReference.class)))
				.thenReturn(List.of(bankAccountDetails));
		when(genericService.fromJson(eq("[{\"memberId\":\"MEM-1\"}]"), any(TypeReference.class)))
				.thenReturn(List.of(executiveMember));

		GetApartmentDetailsResponse response = apartmentService.getApartmentDetails(request);

		assertEquals(SuccessMessage.SUCC_MESSAGE_36, response.getMessage());
		assertEquals(SuccessMessageCode.SUCC_MESSAGE_36, response.getMessageCode());
		assertEquals("logo-data", response.getApartmentLogo());
		assertSame(address, response.getAddress());
		assertEquals("ABC Bank", response.getBankAccountDetails().get(0).getBankName());
		assertEquals("MEM-1", response.getExecutiveMemberList().get(0).getMemberId());
		assertEquals("letter-head-data", response.getApartmentLetterHead());
	}

	@Test
	void getApartmentDetails_shouldReturnNotFoundWhenApartmentMissing() throws Exception {
		GetApartmentDetailsRequest request = new GetApartmentDetailsRequest();
		request.setGenericHeader(buildHeader());

		doNothing().when(commonValidations).genericHeaderValidation(request.getGenericHeader());
		when(repository.findById("APR-1")).thenReturn(Optional.empty());

		GetApartmentDetailsResponse response = apartmentService.getApartmentDetails(request);

		assertEquals(ErrorMessage.ERR_MESSAGE_47, response.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_47, response.getMessageCode());
	}

	private UpdateApartmentDetailsRequest buildUpdateRequest() {
		UpdateApartmentDetailsRequest request = new UpdateApartmentDetailsRequest();
		request.setGenericHeader(buildHeader());
		request.setApartmentLogo("logo-data");
		request.setApartmentLetterHead("letter-head-data");
		Address address = new Address();
		address.setAddressLine1("12 Main Street");
		address.setCity("Springfield");
		request.setAddress(address);
		BankAccountDetails bankAccountDetails = new BankAccountDetails();
		bankAccountDetails.setBankName("ABC Bank");
		bankAccountDetails.setAccountNumber("12345");
		request.setBankAccountDetails(List.of(bankAccountDetails));
		ExecutiveMember executiveMember = new ExecutiveMember();
		executiveMember.setMemberId("MEM-1");
		executiveMember.setPositionName("Secretary");
		executiveMember.setPositiontype("EXEC");
		executiveMember.setStatus("ACTIVE");
		executiveMember.setStartDate(LocalDate.of(2025, 1, 1));
		request.setExecutiveMemberList(List.of(executiveMember));
		return request;
	}

	private GenericHeader buildHeader() {
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APR-1");
		header.setApartmentName("Secura Heights");
		header.setUserId("ADMIN-1");
		header.setRole("ADMIN");
		return header;
	}
}
