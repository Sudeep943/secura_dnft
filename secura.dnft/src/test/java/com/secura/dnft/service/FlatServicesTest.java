package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.bean.ProfileAccountDetails;
import com.secura.dnft.dao.DiscFinRepository;
import com.secura.dnft.dao.FlatRepository;
import com.secura.dnft.dao.OwnerRepository;
import com.secura.dnft.dao.ProfileRepository;
import com.secura.dnft.entity.DiscFin;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.Owner;
import com.secura.dnft.entity.Profile;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.request.response.AddedCharges;
import com.secura.dnft.request.response.DueAmountDetails;
import com.secura.dnft.request.response.GetSampleExcellToUploadDataResponse;
import com.secura.dnft.request.response.GetAllFlatsRequest;
import com.secura.dnft.request.response.GetAllFlatsResponse;
import com.secura.dnft.request.response.GetDueAmountForFlatRequest;
import com.secura.dnft.request.response.GetDueAmountForFlatResponse;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.UploadFlatDetailsRequest;
import com.secura.dnft.request.response.UploadFlatDetailsResponse;

@ExtendWith(MockitoExtension.class)
class FlatServicesTest {

	@Mock
	private FlatRepository flatRepository;

	@Mock
	private DiscFinRepository discFinRepository;

	@Mock
	private ProfileRepository profileRepository;

	@Mock
	private OwnerRepository ownerRepository;

	@Mock
	private GenericService genericService;

	@InjectMocks
	private FlatServices flatServices;

	@Test
	void uploadFlatDetails_shouldFailRow_whenOwnerMobileMissing() throws Exception {
		UploadFlatDetailsRequest request = buildRequest(buildWorkbookBase64("", "A-101"));

		UploadFlatDetailsResponse response = flatServices.uploadFlatDetails(request);

		assertEquals(1, response.getTotalRows());
		assertEquals(0, response.getSuccessRows());
		assertEquals(1, response.getFailedRows());
		assertNotNull(response.getFailedRowsReportDocument());
		byte[] decodedWorkbook = Base64.getDecoder().decode(response.getFailedRowsReportDocument());
		try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(decodedWorkbook))) {
			Sheet sheet = workbook.getSheetAt(0);
			Row headerRow = sheet.getRow(0);
			CellStyle flatHeaderStyle = headerRow.getCell(0).getCellStyle();
			assertEquals(IndexedColors.GREEN.getIndex(), flatHeaderStyle.getFillForegroundColor());
			Font flatHeaderFont = workbook.getFontAt(flatHeaderStyle.getFontIndex());
			assertEquals(IndexedColors.WHITE.getIndex(), flatHeaderFont.getColor());
			assertTrue(flatHeaderFont.getBold());

			CellStyle reasonHeaderStyle = headerRow.getCell(11).getCellStyle();
			assertEquals(IndexedColors.GREEN.getIndex(), reasonHeaderStyle.getFillForegroundColor());
			Font reasonHeaderFont = workbook.getFontAt(reasonHeaderStyle.getFontIndex());
			assertEquals(IndexedColors.WHITE.getIndex(), reasonHeaderFont.getColor());
			assertTrue(reasonHeaderFont.getBold());
			Row failedRow = sheet.getRow(1);
			CellStyle reasonValueStyle = failedRow.getCell(11).getCellStyle();
			assertEquals(IndexedColors.RED.getIndex(), reasonValueStyle.getFillForegroundColor());
			assertEquals(expectedColumnWidth(sheet, 11), sheet.getColumnWidth(11));
		}
		verify(profileRepository, never()).save(any(Profile.class));
		verify(flatRepository, never()).save(any(Flat.class));
	}

	@Test
	void uploadFlatDetails_shouldReuseExistingProfileAndRotateOwner_whenPhoneExists() throws Exception {
		UploadFlatDetailsRequest request = buildRequest(buildWorkbookBase64("9999999999", "A-101"));

		Profile existingProfile = new Profile();
		existingProfile.setPrflId("PRFL0001");
		existingProfile.setPrflAcountDetails("ACCOUNT_JSON");
		existingProfile.setPrflPhoneNo("9999999999");

		Owner activeOwner = new Owner();
		activeOwner.setOwnerId("OWN-1");
		activeOwner.setFlatNo("A-101");
		activeOwner.setPrflId("[\"OLDPRFL\"]");
		activeOwner.setStatus(SecuraConstants.PROFILE_STATUS_ACTIVE);
		activeOwner.setStartDate(LocalDateTime.now().minusDays(2));

		when(profileRepository.findByPrflPhoneNo("9999999999")).thenReturn(List.of(existingProfile));
		when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(flatRepository.save(any(Flat.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(ownerRepository.findByFlatNo("A-101")).thenReturn(List.of(activeOwner));
		when(ownerRepository.save(any(Owner.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(genericService.toJson(any())).thenReturn("JSON_VALUE");
		when(genericService.fromJson(eq("ACCOUNT_JSON"), any(TypeReference.class)))
				.thenReturn(new ArrayList<ProfileAccountDetails>());
		when(genericService.fromJson(eq("[\"OLDPRFL\"]"), any(TypeReference.class))).thenReturn(List.of("OLDPRFL"));

		UploadFlatDetailsResponse response = flatServices.uploadFlatDetails(request);

		assertEquals(1, response.getTotalRows());
		assertEquals(1, response.getSuccessRows());
		assertEquals(0, response.getFailedRows());
		verify(profileRepository, times(1)).findByPrflPhoneNo("9999999999");
		verify(profileRepository, times(1)).save(any(Profile.class));
		verify(flatRepository, times(1)).save(any(Flat.class));
		verify(ownerRepository, times(2)).save(any(Owner.class));

		ArgumentCaptor<Owner> ownerCaptor = ArgumentCaptor.forClass(Owner.class);
		verify(ownerRepository, times(2)).save(ownerCaptor.capture());
		List<Owner> savedOwners = ownerCaptor.getAllValues();
		assertTrue(savedOwners.stream().anyMatch(owner -> SecuraConstants.PROFILE_STATUS_INACTIVE.equals(owner.getStatus())
				&& owner.getEndDate() != null));
		assertTrue(savedOwners.stream().anyMatch(owner -> SecuraConstants.PROFILE_STATUS_ACTIVE.equals(owner.getStatus())
				&& owner.getStartDate() != null && "A-101".equals(owner.getFlatNo())));
	}

	@Test
	void getSampleExcellToUploadData_shouldReturnBase64WorkbookWithHeadersAndSampleRow() throws Exception {
		GetSampleExcellToUploadDataResponse response = flatServices.getSampleExcellToUploadData();
		assertNotNull(response);
		assertNotNull(response.getSampleDocumentData());
		assertEquals("flat_upload_sample.xlsx", response.getSampleDocumentName());

		byte[] decodedWorkbook = Base64.getDecoder().decode(response.getSampleDocumentData());
		try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(decodedWorkbook))) {
			Sheet sheet = workbook.getSheetAt(0);
			Row headerRow = sheet.getRow(0);
			assertEquals("Flat No", headerRow.getCell(0).getStringCellValue());
			assertEquals("Owner Name", headerRow.getCell(1).getStringCellValue());
			assertEquals("Owner Gender", headerRow.getCell(2).getStringCellValue());
			assertEquals("Tower", headerRow.getCell(3).getStringCellValue());
			assertEquals("Block", headerRow.getCell(4).getStringCellValue());
			assertEquals("Possesion Date", headerRow.getCell(5).getStringCellValue());
			assertEquals("Owner Type", headerRow.getCell(6).getStringCellValue());
			assertEquals("Flat Area", headerRow.getCell(7).getStringCellValue());
			assertEquals("Owner DOB", headerRow.getCell(8).getStringCellValue());
			assertEquals("Owner Phone Number", headerRow.getCell(9).getStringCellValue());
			assertEquals("Owner Email Number", headerRow.getCell(10).getStringCellValue());
			CellStyle headerStyle = headerRow.getCell(0).getCellStyle();
			assertEquals(IndexedColors.GREEN.getIndex(), headerStyle.getFillForegroundColor());
			Font headerFont = workbook.getFontAt(headerStyle.getFontIndex());
			assertEquals(IndexedColors.WHITE.getIndex(), headerFont.getColor());
			assertTrue(headerFont.getBold());

			Row sampleRow = sheet.getRow(1);
			assertEquals("A-101", sampleRow.getCell(0).getStringCellValue());
			assertEquals("John Doe", sampleRow.getCell(1).getStringCellValue());
			assertEquals("1-Mar-2029", sampleRow.getCell(5).getStringCellValue());
			assertEquals("1-Jan-1990", sampleRow.getCell(8).getStringCellValue());
			assertEquals("9876543210", sampleRow.getCell(9).getStringCellValue());
			CellStyle sampleStyle = sampleRow.getCell(0).getCellStyle();
			assertEquals(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex(), sampleStyle.getFillForegroundColor());
			assertEquals(expectedColumnWidth(sheet, 10), sheet.getColumnWidth(10));
		}
	}

	@Test
	void uploadFlatDetails_shouldParseDateFieldsInDMMMYYYYFormat() throws Exception {
		UploadFlatDetailsRequest request = buildRequest(
				buildWorkbookBase64WithDates("9999999999", "A-102", "1-Mar-2029", "1-Jan-1990"));

		when(profileRepository.findByPrflPhoneNo("9999999999")).thenReturn(new ArrayList<>());
		when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(flatRepository.save(any(Flat.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(ownerRepository.findByFlatNo("A-102")).thenReturn(new ArrayList<>());
		when(ownerRepository.save(any(Owner.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(genericService.toJson(any())).thenReturn("JSON_VALUE");

		UploadFlatDetailsResponse response = flatServices.uploadFlatDetails(request);

		assertEquals(1, response.getTotalRows());
		assertEquals(1, response.getSuccessRows());
		assertEquals(0, response.getFailedRows());

		ArgumentCaptor<Profile> profileCaptor = ArgumentCaptor.forClass(Profile.class);
		verify(profileRepository, times(1)).save(profileCaptor.capture());
		assertEquals(LocalDateTime.of(1990, 1, 1, 0, 0), profileCaptor.getValue().getPrflDob());

		ArgumentCaptor<Flat> flatCaptor = ArgumentCaptor.forClass(Flat.class);
		verify(flatRepository, times(1)).save(flatCaptor.capture());
		assertEquals(LocalDateTime.of(2029, 3, 1, 0, 0), flatCaptor.getValue().getFlatPossnDate());
	}

	@Test
	void uploadFlatDetails_shouldShowHeaderName_whenDateFormatIsInvalid() throws Exception {
		UploadFlatDetailsRequest request = buildRequest(
				buildWorkbookBase64WithDates("9999999999", "A-103", "1-Mar-2029", "invalid-date"));

		UploadFlatDetailsResponse response = flatServices.uploadFlatDetails(request);

		assertEquals(1, response.getTotalRows());
		assertEquals(0, response.getSuccessRows());
		assertEquals(1, response.getFailedRows());
		assertNotNull(response.getFailedRowsReportDocument());

		byte[] decodedWorkbook = Base64.getDecoder().decode(response.getFailedRowsReportDocument());
		try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(decodedWorkbook))) {
			Sheet sheet = workbook.getSheetAt(0);
			assertEquals("Invalid date format for Owner DOB", sheet.getRow(1).getCell(11).getStringCellValue());
		}
	}

	@Test
	void getAllFlats_shouldReturnTowerHierarchyOnly_whenNoBlockNamesAvailable() {
		GetAllFlatsRequest request = new GetAllFlatsRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APRT001");
		request.setGenericHeader(header);

		Flat flat1 = new Flat();
		flat1.setAprmntId("APRT001");
		flat1.setFlatNo("A-101");
		flat1.setFlatTower("T1");
		flat1.setFlatBlock("");

		Flat flat2 = new Flat();
		flat2.setAprmntId("APRT001");
		flat2.setFlatNo("A-102");
		flat2.setFlatTower("T2");
		flat2.setFlatBlock(null);

		when(flatRepository.findByAprmntId("APRT001")).thenReturn(List.of(flat1, flat2));

		GetAllFlatsResponse response = flatServices.getAllFlats(request);

		assertNotNull(response);
		assertNotNull(response.getTowerList());
		assertEquals(2, response.getTowerList().size());
		assertNull(response.getBlockList());
		assertEquals("T1", response.getTowerList().get(0).getTowerName());
		assertEquals(List.of("A-101"), response.getTowerList().get(0).getFlatList());
		assertEquals("T2", response.getTowerList().get(1).getTowerName());
		assertEquals(List.of("A-102"), response.getTowerList().get(1).getFlatList());
	}

	@Test
	void getAllFlats_shouldReturnBlockHierarchy_withTowersAndDirectBlockFlats() {
		GetAllFlatsRequest request = new GetAllFlatsRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APRT001");
		request.setGenericHeader(header);

		Flat towerFlat = new Flat();
		towerFlat.setAprmntId("APRT001");
		towerFlat.setFlatNo("B-201");
		towerFlat.setFlatTower("T1");
		towerFlat.setFlatBlock("B1");

		Flat directBlockFlat = new Flat();
		directBlockFlat.setAprmntId("APRT001");
		directBlockFlat.setFlatNo("B-001");
		directBlockFlat.setFlatTower("");
		directBlockFlat.setFlatBlock("B1");

		when(flatRepository.findByAprmntId("APRT001")).thenReturn(List.of(towerFlat, directBlockFlat));

		GetAllFlatsResponse response = flatServices.getAllFlats(request);

		assertNotNull(response);
		assertNotNull(response.getBlockList());
		assertEquals(1, response.getBlockList().size());
		assertNull(response.getTowerList());
		GetAllFlatsResponse.BlockDetails blockDetails = response.getBlockList().get(0);
		assertEquals("B1", blockDetails.getBlockName());
		assertEquals(List.of("B-001"), blockDetails.getFlatList());
		assertNotNull(blockDetails.getTowerList());
		assertEquals(1, blockDetails.getTowerList().size());
		assertEquals("T1", blockDetails.getTowerList().get(0).getTowerName());
		assertEquals(List.of("B-201"), blockDetails.getTowerList().get(0).getFlatList());
	}

	@Test
	void getDueAmountForFlat_shouldApplyPercentageDiscountAndCalculateTotalDueAmount() {
		GetDueAmountForFlatRequest request = new GetDueAmountForFlatRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APRT001");
		request.setGenericHeader(header);
		request.setFlatId("A-101");

		DueAmountDetails details = new DueAmountDetails();
		details.setDueDate(LocalDate.now().minusDays(1));
		details.setDiscountCode("DFNDISCOUNT1234");
		details.setTotalAmount("18626");
		AddedCharges amountCharge = new AddedCharges();
		amountCharge.setChargeType("amount");
		amountCharge.setFinalChargeValue("100");
		details.setAddedCharges(List.of(amountCharge));

		Flat flat = new Flat();
		flat.setFlatNo("A-101");
		flat.setFlatPndngPaymntLst("DUE_JSON");

		DiscFin discount = new DiscFin();
		discount.setDiscFnId("DFNDISCOUNT1234");
		discount.setDiscFnType("DISCOUNT");
		discount.setDueDateAsStartDateFlag(true);
		discount.setDiscFnMode("PERCENTAGE");
		discount.setDiscFinValue("12");

		when(flatRepository.findById("A-101")).thenReturn(java.util.Optional.of(flat));
		when(genericService.fromJson(eq("DUE_JSON"), any(TypeReference.class))).thenReturn(List.of(details));
		when(discFinRepository.findById("DFNDISCOUNT1234")).thenReturn(java.util.Optional.of(discount));

		GetDueAmountForFlatResponse response = flatServices.getDueAmountForFlat(request);

		assertNotNull(response);
		assertEquals("16403", response.getDuePaymentList().get(0).getTotalAmount());
		assertEquals("12", response.getDuePaymentList().get(0).getDiscFnValue());
		assertEquals("2223.12", response.getDuePaymentList().get(0).getDiscountedAmount());
		assertEquals("16403", response.getTotalDueAmount());
	}

	@Test
	void getDueAmountForFlat_shouldSetMandatoryAndOptionalTotalsAtResponseLevel() {
		GetDueAmountForFlatRequest request = new GetDueAmountForFlatRequest();
		request.setFlatId("A-101");

		DueAmountDetails mandatoryDetails = new DueAmountDetails();
		mandatoryDetails.setDueDate(LocalDate.now().minusDays(1));
		mandatoryDetails.setPaymentType("mandatory");
		mandatoryDetails.setTotalAmount("1000");

		DueAmountDetails optionalDetails = new DueAmountDetails();
		optionalDetails.setDueDate(LocalDate.now().minusDays(1));
		optionalDetails.setPaymentType("optional");
		optionalDetails.setTotalAmount("200");

		Flat flat = new Flat();
		flat.setFlatNo("A-101");
		flat.setFlatPndngPaymntLst("DUE_JSON");

		when(flatRepository.findById("A-101")).thenReturn(java.util.Optional.of(flat));
		when(genericService.fromJson(eq("DUE_JSON"), any(TypeReference.class)))
				.thenReturn(List.of(mandatoryDetails, optionalDetails));

		GetDueAmountForFlatResponse response = flatServices.getDueAmountForFlat(request);

		assertNotNull(response);
		assertEquals("1200", response.getTotalDueAmount());
		assertEquals("1000", response.getTotalMandatoryPaymentAmount());
		assertEquals("200", response.getTotalOptionalPaymentAmount());
	}

	@Test
	void getDueAmountForFlat_shouldKeepOriginalTotal_whenNoDiscountCode() {
		GetDueAmountForFlatRequest request = new GetDueAmountForFlatRequest();
		request.setFlatId("A-101");

		DueAmountDetails details = new DueAmountDetails();
		details.setDueDate(LocalDate.now().minusDays(1));
		details.setTotalAmount("18626");

		Flat flat = new Flat();
		flat.setFlatNo("A-101");
		flat.setFlatPndngPaymntLst("DUE_JSON");

		when(flatRepository.findById("A-101")).thenReturn(java.util.Optional.of(flat));
		when(genericService.fromJson(eq("DUE_JSON"), any(TypeReference.class))).thenReturn(List.of(details));

		GetDueAmountForFlatResponse response = flatServices.getDueAmountForFlat(request);

		assertNotNull(response);
		assertEquals("18626", response.getDuePaymentList().get(0).getTotalAmount());
		assertNull(response.getDuePaymentList().get(0).getDiscFnValue());
		assertNull(response.getDuePaymentList().get(0).getDiscountedAmount());
		assertEquals("18626", response.getTotalDueAmount());
		verify(discFinRepository, never()).findById(any());
	}

	@Test
	void getDueAmountForFlat_shouldApplySimpleAmountFine_whenFineCodePresentAndInDateWindow() {
		GetDueAmountForFlatRequest request = new GetDueAmountForFlatRequest();
		request.setFlatId("A-101");

		DueAmountDetails details = new DueAmountDetails();
		details.setDueDate(LocalDate.now().minusDays(2));
		details.setFineCode("DFNFINE1234");
		details.setTotalAmount("1000");

		Flat flat = new Flat();
		flat.setFlatNo("A-101");
		flat.setFlatPndngPaymntLst("DUE_JSON");

		DiscFin fine = new DiscFin();
		fine.setDiscFnId("DFNFINE1234");
		fine.setDiscFnType("FINE");
		fine.setDiscFnCycleType("SIMPLE");
		fine.setDiscFnMode("AMOUNT");
		fine.setDiscFinValue("50");
		fine.setDiscFnStrtDt(LocalDateTime.now().minusDays(3));
		fine.setDiscFnEndDt(LocalDateTime.now().plusDays(5));

		when(flatRepository.findById("A-101")).thenReturn(java.util.Optional.of(flat));
		when(genericService.fromJson(eq("DUE_JSON"), any(TypeReference.class))).thenReturn(List.of(details));
		when(discFinRepository.findById("DFNFINE1234")).thenReturn(java.util.Optional.of(fine));

		GetDueAmountForFlatResponse response = flatServices.getDueAmountForFlat(request);

		assertNotNull(response);
		assertEquals("1050", response.getDuePaymentList().get(0).getTotalAmount());
		assertEquals("50", response.getDuePaymentList().get(0).getDiscFnValue());
		assertEquals("50", response.getDuePaymentList().get(0).getFineAmount());
		assertEquals("FINE", response.getDuePaymentList().get(0).getFineType());
		assertEquals("1050", response.getTotalDueAmount());
	}

	@Test
	void getDueAmountForFlat_shouldApplyDiscountThenFine_whenBothCodesPresent() {
		GetDueAmountForFlatRequest request = new GetDueAmountForFlatRequest();
		request.setFlatId("A-101");

		DueAmountDetails details = new DueAmountDetails();
		details.setDueDate(LocalDate.now().minusDays(2));
		details.setDiscountCode("DFNDISCOUNT1234");
		details.setFineCode("DFNFINE1234");
		details.setTotalAmount("1000");
		AddedCharges amountCharge = new AddedCharges();
		amountCharge.setChargeType("amount");
		amountCharge.setFinalChargeValue("100");
		details.setAddedCharges(List.of(amountCharge));

		Flat flat = new Flat();
		flat.setFlatNo("A-101");
		flat.setFlatPndngPaymntLst("DUE_JSON");

		DiscFin discount = new DiscFin();
		discount.setDiscFnId("DFNDISCOUNT1234");
		discount.setDiscFnType("DISCOUNT");
		discount.setDueDateAsStartDateFlag(true);
		discount.setDiscFnMode("PERCENTAGE");
		discount.setDiscFinValue("10");

		DiscFin fine = new DiscFin();
		fine.setDiscFnId("DFNFINE1234");
		fine.setDiscFnType("FINE");
		fine.setDiscFnCycleType("SIMPLE");
		fine.setDiscFnMode("AMOUNT");
		fine.setDiscFinValue("50");
		fine.setDiscFnStrtDt(LocalDateTime.now().minusDays(3));
		fine.setDiscFnEndDt(LocalDateTime.now().plusDays(5));

		when(flatRepository.findById("A-101")).thenReturn(java.util.Optional.of(flat));
		when(genericService.fromJson(eq("DUE_JSON"), any(TypeReference.class))).thenReturn(List.of(details));
		when(discFinRepository.findById("DFNDISCOUNT1234")).thenReturn(java.util.Optional.of(discount));
		when(discFinRepository.findById("DFNFINE1234")).thenReturn(java.util.Optional.of(fine));

		GetDueAmountForFlatResponse response = flatServices.getDueAmountForFlat(request);

		assertNotNull(response);
		assertEquals("960", response.getDuePaymentList().get(0).getTotalAmount());
		assertEquals("90", response.getDuePaymentList().get(0).getDiscountedAmount());
		assertEquals("50", response.getDuePaymentList().get(0).getFineAmount());
		assertEquals("FINE", response.getDuePaymentList().get(0).getFineType());
		assertEquals("50", response.getDuePaymentList().get(0).getDiscFnValue());
		assertEquals("960", response.getTotalDueAmount());
	}

	@Test
	void getDueAmountForFlat_shouldIgnoreDueDateFlagForDiscountStartDate() {
		GetDueAmountForFlatRequest request = new GetDueAmountForFlatRequest();
		request.setFlatId("A-101");

		DueAmountDetails details = new DueAmountDetails();
		details.setDueDate(LocalDate.now().minusDays(10));
		details.setDiscountCode("DFNDISCOUNT1234");
		details.setTotalAmount("1000");

		Flat flat = new Flat();
		flat.setFlatNo("A-101");
		flat.setFlatPndngPaymntLst("DUE_JSON");

		DiscFin discount = new DiscFin();
		discount.setDiscFnId("DFNDISCOUNT1234");
		discount.setDiscFnType("DISCOUNT");
		discount.setDueDateAsStartDateFlag(true);
		discount.setDiscFnMode("AMOUNT");
		discount.setDiscFinValue("50");
		discount.setDiscFnStrtDt(LocalDateTime.now().plusDays(1));

		when(flatRepository.findById("A-101")).thenReturn(java.util.Optional.of(flat));
		when(genericService.fromJson(eq("DUE_JSON"), any(TypeReference.class))).thenReturn(List.of(details));
		when(discFinRepository.findById("DFNDISCOUNT1234")).thenReturn(java.util.Optional.of(discount));

		GetDueAmountForFlatResponse response = flatServices.getDueAmountForFlat(request);

		assertNotNull(response);
		assertEquals("1000", response.getDuePaymentList().get(0).getTotalAmount());
		assertNull(response.getDuePaymentList().get(0).getDiscountedAmount());
	}

	@Test
	void getDueAmountForFlat_shouldUseDueDateAsFineStartDate_whenFlagTrue() {
		GetDueAmountForFlatRequest request = new GetDueAmountForFlatRequest();
		request.setFlatId("A-101");

		DueAmountDetails details = new DueAmountDetails();
		details.setDueDate(LocalDate.now().minusDays(10));
		details.setFineCode("DFNFINE1234");
		details.setTotalAmount("1000");

		Flat flat = new Flat();
		flat.setFlatNo("A-101");
		flat.setFlatPndngPaymntLst("DUE_JSON");

		DiscFin fine = new DiscFin();
		fine.setDiscFnId("DFNFINE1234");
		fine.setDiscFnType("FINE");
		fine.setDiscFnCycleType("SIMPLE");
		fine.setDueDateAsStartDateFlag(true);
		fine.setDiscFnMode("AMOUNT");
		fine.setDiscFinValue("50");
		fine.setDiscFnStrtDt(LocalDateTime.now().plusDays(1));
		fine.setDiscFnEndDt(LocalDateTime.now().plusDays(5));

		when(flatRepository.findById("A-101")).thenReturn(java.util.Optional.of(flat));
		when(genericService.fromJson(eq("DUE_JSON"), any(TypeReference.class))).thenReturn(List.of(details));
		when(discFinRepository.findById("DFNFINE1234")).thenReturn(java.util.Optional.of(fine));

		GetDueAmountForFlatResponse response = flatServices.getDueAmountForFlat(request);

		assertNotNull(response);
		assertEquals("1050", response.getDuePaymentList().get(0).getTotalAmount());
		assertEquals("50", response.getDuePaymentList().get(0).getFineAmount());
	}

	private UploadFlatDetailsRequest buildRequest(String documentData) {
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APRT001");
		header.setUserId("PRFLADMIN1");

		UploadFlatDetailsRequest request = new UploadFlatDetailsRequest();
		request.setHeader(header);
		request.setDocumentData(documentData);
		request.setSheetName("Sheet1");
		return request;
	}

	private String buildWorkbookBase64(String ownerPhone, String flatNo) throws Exception {
		return buildWorkbookBase64WithDates(ownerPhone, flatNo, "", "");
	}

	private String buildWorkbookBase64WithDates(String ownerPhone, String flatNo, String possessionDate, String ownerDob)
			throws Exception {
		try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("Sheet1");
			Row headerRow = sheet.createRow(0);
			headerRow.createCell(0).setCellValue("Flat No");
			headerRow.createCell(1).setCellValue("Owner Name");
			headerRow.createCell(2).setCellValue("Owner Gender");
			headerRow.createCell(3).setCellValue("Tower");
			headerRow.createCell(4).setCellValue("Block");
			headerRow.createCell(5).setCellValue("Possesion Date");
			headerRow.createCell(6).setCellValue("Owner Type");
			headerRow.createCell(7).setCellValue("Flat Area");
			headerRow.createCell(8).setCellValue("Owner DOB");
			headerRow.createCell(9).setCellValue("Owner Phone Number");
			headerRow.createCell(10).setCellValue("Owner Email Number");

			Row row = sheet.createRow(1);
			row.createCell(0).setCellValue(flatNo);
			row.createCell(1).setCellValue("John");
			row.createCell(2).setCellValue("M");
			row.createCell(3).setCellValue("T1");
			row.createCell(4).setCellValue("B1");
			row.createCell(5).setCellValue(possessionDate);
			row.createCell(6).setCellValue("OWNER");
			row.createCell(7).setCellValue("1200");
			row.createCell(8).setCellValue(ownerDob);
			row.createCell(9).setCellValue(ownerPhone);
			row.createCell(10).setCellValue("john@example.com");

			workbook.write(outputStream);
			return Base64.getEncoder().encodeToString(outputStream.toByteArray());
		}
	}

	private int expectedColumnWidth(Sheet sheet, int columnIndex) {
		int maxTextLength = 1;
		DataFormatter formatter = new DataFormatter();
		for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
			Row row = sheet.getRow(rowIndex);
			if (row == null || row.getCell(columnIndex) == null) {
				continue;
			}
			String value = formatter.formatCellValue(row.getCell(columnIndex));
			maxTextLength = Math.max(maxTextLength, value.length());
		}
		return (int) Math.ceil(maxTextLength * 1.5 * 256);
	}
}
