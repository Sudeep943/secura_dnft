package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.secura.dnft.bean.ProfileAccountDetails;
import com.secura.dnft.dao.BankEntityRepository;
import com.secura.dnft.dao.DueAmountDetailsRepository;
import com.secura.dnft.dao.FlatRepository;
import com.secura.dnft.dao.OwnerRepository;
import com.secura.dnft.dao.PaymentRepository;
import com.secura.dnft.dao.ProfileRepository;
import com.secura.dnft.dao.TransactionRepository;
import com.secura.dnft.entity.BankEntity;
import com.secura.dnft.entity.DueAmountDetailsEntity;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.Owner;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.entity.Profile;
import com.secura.dnft.entity.Transaction;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.request.response.DueAmountDetails;
import com.secura.dnft.request.response.GetSampleExcellToUploadDataResponse;
import com.secura.dnft.request.response.GetAllFlatsRequest;
import com.secura.dnft.request.response.GetAllFlatsResponse;
import com.secura.dnft.request.response.GetDueAmountForFlatRequest;
import com.secura.dnft.request.response.GetDueAmountForFlatResponse;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.PaymentDetail;
import com.secura.dnft.request.response.UploadFlatDetailsRequest;
import com.secura.dnft.request.response.UploadFlatDetailsResponse;

@ExtendWith(MockitoExtension.class)
class FlatServicesTest {

	private static final String SUCCESS_TRANSACTION_STATUS = "SUCCESS";

	@Mock
	private FlatRepository flatRepository;

	@Mock
	private ProfileRepository profileRepository;

	@Mock
	private OwnerRepository ownerRepository;

	@Mock
	private DueAmountDetailsRepository dueAmountDetailsRepository;

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private BankEntityRepository bankEntityRepository;

	@Mock
	private GenericService genericService;

	@Mock
	private TransactionRepository transactionRepository;

	@Mock
	private DueDetailsService dueDetailsService;

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
	void getDueAmountForFlat_shouldGroupSelectedDuesAndCalculateTotals() {
		GetDueAmountForFlatRequest request = new GetDueAmountForFlatRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APRT001");
		request.setGenericHeader(header);
		request.setFlatId("A-101");

		Flat flat = new Flat();
		flat.setFlatNo("A-101");
		flat.setFlatArea("1200");
		flat.setFlatPndngPaymntLst(
				"[\"D1_MONTHLY_1200_" + LocalDate.now().minusDays(30) + "\",\"D1_MONTHLY_1200_"
						+ LocalDate.now().plusDays(12) + "\",\"D1_MONTHLY_1200_" + LocalDate.now().plusDays(20)
						+ "\",\"D2_QUARTERLY_1200_" + LocalDate.now().plusDays(20) + "\",\"D2_QUARTERLY_1200_"
						+ LocalDate.now().plusDays(8) + "\",\"D3_YEARLY_ALL_"
						+ LocalDate.now().plusDays(60) + "\",\"D4_HALF YEARLY_1200_" + LocalDate.now().minusDays(5) + "\"]");

		List<String> dueIds = Arrays.asList("D1", "D2", "D3", "D4");
		List<String> pendingDueKeys = Arrays.asList("D1_MONTHLY_1200_" + LocalDate.now().minusDays(30),
				"D1_MONTHLY_1200_" + LocalDate.now().plusDays(12), "D1_MONTHLY_1200_" + LocalDate.now().plusDays(20),
				"D2_QUARTERLY_1200_" + LocalDate.now().plusDays(20), "D2_QUARTERLY_1200_" + LocalDate.now().plusDays(8),
				"D3_YEARLY_ALL_" + LocalDate.now().plusDays(60), "D4_HALF YEARLY_1200_" + LocalDate.now().minusDays(5));
		List<DueAmountDetailsEntity> dueEntities = Arrays.asList(
				buildDueEntity("D1", "PAY1", "MONTHLY", "1200", LocalDate.now().minusDays(30), "100", "0", "Maintenance",
						"MANDATORY"),
				buildDueEntity("D1", "PAY1", "MONTHLY", "1200", LocalDate.now().plusDays(12), "130", "10", "Maintenance",
						"MANDATORY"),
				buildDueEntity("D1", "PAY1", "MONTHLY", "1200", LocalDate.now().plusDays(20), "150", "0", "Maintenance",
						"MANDATORY"),
				buildDueEntity("D2", "PAY1", "QUARTERLY", "1200", LocalDate.now().plusDays(20), "300", "0", "Maintenance",
						"MANDATORY"),
				buildDueEntity("D2", "PAY1", "QUARTERLY", "1200", LocalDate.now().plusDays(8), "250", "0", "Maintenance",
						"MANDATORY"),
				buildDueEntity("D2", "PAY1", "QUARTERLY", "900", LocalDate.now().plusDays(8), "999", "0", "Maintenance",
						"MANDATORY"),
				buildDueEntity("D3", "PAY2", "YEARLY", "ALL", LocalDate.now().plusDays(60), "1000", "0", "Club Fund",
						"OPTIONAL"),
				buildDueEntity("D4", "PAY2", "HALF YEARLY", "1200", LocalDate.now().minusDays(5), "600", "0", "Club Fund",
						"OPTIONAL"));

		when(flatRepository.findById("A-101")).thenReturn(Optional.of(flat));
		when(genericService.fromJson(eq(flat.getFlatPndngPaymntLst()), any(TypeReference.class))).thenReturn(pendingDueKeys);
		when(dueAmountDetailsRepository.findByDueIdIn(dueIds)).thenReturn(dueEntities);
		when(dueAmountDetailsRepository.findByPaymentIdIn(anyList())).thenReturn(dueEntities);
		when(paymentRepository.findFirstByPaymentId("PAY1")).thenReturn(Optional.of(buildPaymentEntity("PAY1", "Maintenance")));
		when(paymentRepository.findFirstByPaymentId("PAY2")).thenReturn(Optional.of(buildPaymentEntity("PAY2", "Club Fund")));
		when(transactionRepository.findByPymntIdAndFlatIdAndTrnsStatus("PAY1", "A-101", SUCCESS_TRANSACTION_STATUS))
				.thenReturn(List.of(buildTransactionEntity("PAY1", "A-101", "200")));
		when(transactionRepository.findByPymntIdAndFlatIdAndTrnsStatus("PAY2", "A-101", SUCCESS_TRANSACTION_STATUS))
				.thenReturn(List.of(buildTransactionEntity("PAY2", "A-101", "1200")));

		GetDueAmountForFlatResponse response = flatServices.getDueAmountForFlat(request);

		assertEquals("350", response.getTotalDue());
		assertEquals("350", response.getTotalMandatoryPayment());
		assertEquals("0", response.getTotalOptionalPayment());
		assertEquals(Boolean.TRUE, response.getPenaltyAdded());
		assertNotNull(response.getDueDetails());
		assertEquals(2, response.getDueDetails().size());

		Map<PaymentDetail, List<DueAmountDetailsEntity>> dueDetails = response.getDueDetails();
		List<DueAmountDetailsEntity> pay1Dues = dueDetails.entrySet().stream()
				.filter(entry -> "PAY1".equals(entry.getKey().getPaymentId())).findFirst().map(Map.Entry::getValue).orElse(null);
		assertNotNull(pay1Dues);
		assertEquals(3, pay1Dues.size());
		assertTrue(pay1Dues.stream().anyMatch(due -> "D1".equals(due.getDueId())));
		assertEquals(2, pay1Dues.stream().filter(due -> "D1".equals(due.getDueId())).count());
		assertTrue(pay1Dues.stream()
				.noneMatch(due -> "D1".equals(due.getDueId()) && LocalDate.now().plusDays(20).equals(due.getDueDate())));
		assertEquals(1, pay1Dues.stream().filter(due -> "D2".equals(due.getDueId())).count());
		assertTrue(pay1Dues.stream().noneMatch(due -> "900".equals(due.getFlatArea())));

		List<DueAmountDetailsEntity> pay2Dues = dueDetails.entrySet().stream()
				.filter(entry -> "PAY2".equals(entry.getKey().getPaymentId())).findFirst().map(Map.Entry::getValue).orElse(null);
		assertNotNull(pay2Dues);
		assertEquals(2, pay2Dues.size());
	}

	@Test
	void getDueAmountForFlat_shouldIgnorePaidAmountWhenAnyDueIsPerHead() {
		GetDueAmountForFlatRequest request = new GetDueAmountForFlatRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APRT001");
		request.setGenericHeader(header);
		request.setFlatId("A-101");

		Flat flat = new Flat();
		flat.setFlatNo("A-101");
		flat.setFlatArea("1200");
		flat.setFlatPndngPaymntLst("[\"D1_MONTHLY_1200_" + LocalDate.now() + "\"]");

		List<String> dueIds = List.of("D1");
		List<String> pendingDueKeys = List.of("D1_MONTHLY_1200_" + LocalDate.now());
		DueAmountDetailsEntity dueEntity = buildDueEntity("D1", "PAY1", "MONTHLY", "1200", LocalDate.now(), "100", "0",
				"Maintenance", "MANDATORY");
		dueEntity.setPaymentCapita("per_head");
		List<DueAmountDetailsEntity> dueEntities = List.of(dueEntity);

		when(flatRepository.findById("A-101")).thenReturn(Optional.of(flat));
		when(genericService.fromJson(eq(flat.getFlatPndngPaymntLst()), any(TypeReference.class))).thenReturn(pendingDueKeys);
		when(dueAmountDetailsRepository.findByDueIdIn(dueIds)).thenReturn(dueEntities);
		when(dueAmountDetailsRepository.findByPaymentIdIn(anyList())).thenReturn(dueEntities);
		when(paymentRepository.findFirstByPaymentId("PAY1")).thenReturn(Optional.of(buildPaymentEntity("PAY1", "Maintenance")));
		when(transactionRepository.findByPymntIdAndFlatIdAndTrnsStatus("PAY1", "A-101", SUCCESS_TRANSACTION_STATUS))
				.thenReturn(List.of(buildTransactionEntity("PAY1", "A-101", "100")));

		GetDueAmountForFlatResponse response = flatServices.getDueAmountForFlat(request);

		assertEquals("100", response.getTotalDue());
		assertEquals("100", response.getTotalMandatoryPayment());
		assertEquals("0", response.getTotalOptionalPayment());
	}

	@Test
	void getDueAmountForFlat_shouldSerializeDueDetailsWithPaymentDetailJsonKeys() throws Exception {
		GetDueAmountForFlatRequest request = new GetDueAmountForFlatRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APRT001");
		request.setGenericHeader(header);
		request.setFlatId("A-101");

		Flat flat = new Flat();
		flat.setFlatNo("A-101");
		flat.setFlatArea("1200");
		flat.setFlatPndngPaymntLst(
				"[\"D1_MONTHLY_1200_" + LocalDate.now() + "\",\"D2_YEARLY_ALL_" + LocalDate.now().plusDays(10) + "\"]");

		List<String> dueIds = Arrays.asList("D1", "D2");
		List<String> pendingDueKeys = Arrays.asList("D1_MONTHLY_1200_" + LocalDate.now(),
				"D2_YEARLY_ALL_" + LocalDate.now().plusDays(10));
		List<DueAmountDetailsEntity> dueEntities = Arrays.asList(
				buildDueEntity("D1", "PAY1", "MONTHLY", "1200", LocalDate.now(), "120", "10", "Maintenance", "MANDATORY"),
				buildDueEntity("D2", "PAY2", "YEARLY", "ALL", LocalDate.now().plusDays(10), "500", "0", "Club Fund",
						"OPTIONAL"));

		when(flatRepository.findById("A-101")).thenReturn(Optional.of(flat));
		when(genericService.fromJson(eq(flat.getFlatPndngPaymntLst()), any(TypeReference.class))).thenReturn(pendingDueKeys);
		when(dueAmountDetailsRepository.findByDueIdIn(dueIds)).thenReturn(dueEntities);
		when(dueAmountDetailsRepository.findByPaymentIdIn(anyList())).thenReturn(dueEntities);
		when(paymentRepository.findFirstByPaymentId("PAY1"))
				.thenReturn(Optional.of(buildPaymentEntity("PAY1", "Maintenance", "BANK1")));
		when(paymentRepository.findFirstByPaymentId("PAY2"))
				.thenReturn(Optional.of(buildPaymentEntity("PAY2", "Club Fund", "BANK2")));
		when(bankEntityRepository.findByAprmntIdAndBankDetailsID("APRT001", "BANK1"))
				.thenReturn(Optional.of(buildBankEntity("APRT001", "BANK1", "ENC_RAZORPAY")));
		when(bankEntityRepository.findByAprmntIdAndBankDetailsID("APRT001", "BANK2"))
				.thenReturn(Optional.of(buildBankEntity("APRT001", "BANK2", "ENC_ATOMS")));
		when(genericService.decrypt("ENC_RAZORPAY")).thenReturn("RAZORPAY");
		when(genericService.decrypt("ENC_ATOMS")).thenReturn("ATOMS");
		when(transactionRepository.findByPymntIdAndFlatIdAndTrnsStatus("PAY1", "A-101", SUCCESS_TRANSACTION_STATUS))
				.thenReturn(List.of());
		when(transactionRepository.findByPymntIdAndFlatIdAndTrnsStatus("PAY2", "A-101", SUCCESS_TRANSACTION_STATUS))
				.thenReturn(List.of());

		GetDueAmountForFlatResponse response = flatServices.getDueAmountForFlat(request);

		ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
		JsonNode dueDetailsNode = objectMapper.readTree(objectMapper.writeValueAsString(response)).get("dueDetails");

		assertNotNull(dueDetailsNode);
		assertTrue(
				dueDetailsNode.has("{\"paymentId\":\"PAY1\",\"paymentName\":\"Maintenance\",\"bankId\":\"BANK1\",\"paymentGateway\":\"RAZORPAY\"}"));
		assertTrue(
				dueDetailsNode.has("{\"paymentId\":\"PAY2\",\"paymentName\":\"Club Fund\",\"bankId\":\"BANK2\",\"paymentGateway\":\"ATOMS\"}"));
		assertEquals("D1",
				dueDetailsNode
						.get("{\"paymentId\":\"PAY1\",\"paymentName\":\"Maintenance\",\"bankId\":\"BANK1\",\"paymentGateway\":\"RAZORPAY\"}")
						.get(0).get("dueId").asText());
		assertEquals("D2",
				dueDetailsNode
						.get("{\"paymentId\":\"PAY2\",\"paymentName\":\"Club Fund\",\"bankId\":\"BANK2\",\"paymentGateway\":\"ATOMS\"}")
						.get(0).get("dueId").asText());
		assertEquals("120", response.getTotalMandatoryPayment());
		assertEquals("500", response.getTotalOptionalPayment());
	}

	@Test
	void getDueAmountForFlat_shouldRecalculateFinalDueValuesBeforeBuildingResponse() {
		GetDueAmountForFlatRequest request = new GetDueAmountForFlatRequest();
		GenericHeader header = new GenericHeader();
		header.setApartmentId("APRT001");
		request.setGenericHeader(header);
		request.setFlatId("A-101");

		Flat flat = new Flat();
		flat.setFlatNo("A-101");
		flat.setFlatArea("1200");
		flat.setFlatPndngPaymntLst("[\"D1_MONTHLY_1200_" + LocalDate.now() + "\"]");

		List<String> pendingDueKeys = List.of("D1_MONTHLY_1200_" + LocalDate.now());
		DueAmountDetailsEntity dueEntity = buildDueEntity("D1", "PAY1", "MONTHLY", "1200", LocalDate.now(), "120", "10",
				"Maintenance", "MANDATORY");
		dueEntity.setAmount("100");
		dueEntity.setGstAmount("20");
		dueEntity.setDiscountedAmount("0");
		dueEntity.setTotalAddedCharges("0");
		List<DueAmountDetailsEntity> dueEntities = List.of(dueEntity);

		DueAmountDetails recalculatedDue = new DueAmountDetails();
		recalculatedDue.setPaymentId("PAY1");
		recalculatedDue.setCollectionCycle("MONTHLY");
		recalculatedDue.setDueDate(LocalDate.now());
		recalculatedDue.setDueEndDate(LocalDate.now().plusDays(29));
		recalculatedDue.setAmount("150");
		recalculatedDue.setGstAmount("27");
		recalculatedDue.setTotalAmount("190");
		recalculatedDue.setPaymentName("Maintenance Updated");
		recalculatedDue.setPaymentType("MANDATORY");
		recalculatedDue.setPaymentCapita("per_flat");
		recalculatedDue.setTotalAddedCharges("8");
		recalculatedDue.setEstimatedCollectionAmount("190");
		recalculatedDue.setGstPercentage("18");
		recalculatedDue.setDiscountedAmount("5");
		recalculatedDue.setFineAmount("10");
		recalculatedDue.setRoundUpAmount("0");
		recalculatedDue.setAlreadyPaidAmount("0");
		recalculatedDue.setAdminDiscount("0");
		recalculatedDue.setApplicableFlats(List.of("A-101"));
		recalculatedDue.setAllowedPaymentModes(List.of("UPI"));

		Map<String, List<Map<String, DueAmountDetails>>> recalculatedDueMap = Map.of("MONTHLY",
				List.of(Map.of("1200", recalculatedDue)));

		when(flatRepository.findById("A-101")).thenReturn(Optional.of(flat));
		when(genericService.fromJson(eq(flat.getFlatPndngPaymntLst()), any(TypeReference.class))).thenReturn(pendingDueKeys);
		when(genericService.toJson(any())).thenReturn("[]");
		when(dueAmountDetailsRepository.findByDueIdIn(List.of("D1"))).thenReturn(dueEntities);
		when(dueAmountDetailsRepository.findByPaymentIdIn(anyList())).thenReturn(dueEntities);
		when(paymentRepository.findFirstByPaymentId("PAY1")).thenReturn(Optional.of(buildPaymentEntity("PAY1", "Maintenance")));
		when(paymentRepository.findByPaymentId("PAY1")).thenReturn(List.of(buildPaymentEntity("PAY1", "Maintenance")));
		when(dueDetailsService.previewDuesForPayment(anyList(), eq(null))).thenReturn(recalculatedDueMap);
		when(transactionRepository.findByPymntIdAndFlatIdAndTrnsStatus("PAY1", "A-101", SUCCESS_TRANSACTION_STATUS))
				.thenReturn(List.of());

		GetDueAmountForFlatResponse response = flatServices.getDueAmountForFlat(request);

		List<DueAmountDetailsEntity> responseDues = response.getDueDetails().values().iterator().next();
		assertEquals("150", responseDues.get(0).getAmount());
		assertEquals("27", responseDues.get(0).getGstAmount());
		assertEquals("190", responseDues.get(0).getTotalAmount());
		assertEquals("Maintenance Updated", responseDues.get(0).getPaymentName());
		assertEquals("8", responseDues.get(0).getTotalAddedCharges());
		verify(dueDetailsService, times(1)).previewDuesForPayment(anyList(), eq(null));
	}

	@Test
	void paymentDetail_toStringShouldExposePaymentIdAndNameAsJson() {
		PaymentDetail paymentDetail = new PaymentDetail();
		paymentDetail.setPaymentId("PAY\"1");
		paymentDetail.setPaymentName("Main\\Fund");

		assertEquals("{\"paymentId\":\"PAY\\\"1\",\"paymentName\":\"Main\\\\Fund\"}", paymentDetail.toString());
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

	private DueAmountDetailsEntity buildDueEntity(String dueId, String paymentId, String collectionCycle, String flatArea,
			LocalDate dueDate, String totalAmount, String fineAmount, String paymentName, String paymentType) {
		DueAmountDetailsEntity entity = new DueAmountDetailsEntity();
		entity.setDueId(dueId);
		entity.setPaymentId(paymentId);
		entity.setCollectionCycle(collectionCycle);
		entity.setFlatArea(flatArea);
		entity.setDueDate(dueDate);
		entity.setTotalAmount(totalAmount);
		entity.setFineAmount(fineAmount);
		entity.setPaymentName(paymentName);
		entity.setPaymentType(paymentType);
		return entity;
	}

	private PaymentEntity buildPaymentEntity(String paymentId, String paymentName) {
		return buildPaymentEntity(paymentId, paymentName, null);
	}

	private PaymentEntity buildPaymentEntity(String paymentId, String paymentName, String bankId) {
		PaymentEntity paymentEntity = new PaymentEntity();
		paymentEntity.setPaymentId(paymentId);
		paymentEntity.setPaymentName(paymentName);
		paymentEntity.setBankAccountId(bankId);
		return paymentEntity;
	}

	private BankEntity buildBankEntity(String apartmentId, String bankId, String encryptedGatewayName) {
		BankEntity bankEntity = new BankEntity();
		bankEntity.setAprmntId(apartmentId);
		bankEntity.setBankDetailsID(bankId);
		bankEntity.setPgName(encryptedGatewayName);
		return bankEntity;
	}

	private Transaction buildTransactionEntity(String paymentId, String flatId, String amount) {
		Transaction transaction = new Transaction();
		transaction.setPymntId(paymentId);
		transaction.setFlatId(flatId);
		transaction.setTrnsAmt(amount);
		return transaction;
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
