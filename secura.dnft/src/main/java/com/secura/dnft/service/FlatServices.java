package com.secura.dnft.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.bean.ProfileAccountDetails;
import com.secura.dnft.dao.FlatRepository;
import com.secura.dnft.dao.OwnerRepository;
import com.secura.dnft.dao.ProfileRepository;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.Owner;
import com.secura.dnft.entity.Profile;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.Name;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.interfaceservice.FlatInterface;
import com.secura.dnft.request.response.AddFlatDetailsRequest;
import com.secura.dnft.request.response.AddFlatDetailsResponse;
import com.secura.dnft.request.response.GetSampleExcellToUploadDataResponse;
import com.secura.dnft.request.response.UpdateFlatDetailsRequest;
import com.secura.dnft.request.response.UpdateFlatDetailsResponse;
import com.secura.dnft.request.response.UploadFlatDetailsRequest;
import com.secura.dnft.request.response.UploadFlatDetailsResponse;

@Service
public class FlatServices implements FlatInterface {

	private static final String[] UPLOAD_HEADERS = { "Flat No", "Owner Name", "Owner Gender", "Tower", "Block",
			"Possesion Date", "Owner Type", "Flat Area", "Owner DOB", "Owner Phone Number", "Owner Email Number" };
	private static final DateTimeFormatter SAMPLE_DATE_FORMAT = new DateTimeFormatterBuilder().parseCaseInsensitive()
			.appendPattern("d-MMM-yyyy").toFormatter(Locale.ENGLISH);

	@Autowired
	private FlatRepository flatRepository;

	@Autowired
	private ProfileRepository profileRepository;

	@Autowired
	private OwnerRepository ownerRepository;

	@Autowired
	private GenericService genericService;

	private final DataFormatter dataFormatter = new DataFormatter();

	@Override
	public AddFlatDetailsResponse addFlatDetails(AddFlatDetailsRequest request) {
		AddFlatDetailsResponse response = new AddFlatDetailsResponse();
		response.setHeader(request.getHeader());
		response.setFlatNo(request.getFlatNo());
		try {
			Flat flat = buildFlatEntity(request, null);
			flat.setFlatPndngPaymntLst("[]");
			flat.setCreatUsrId(request.getHeader() != null ? request.getHeader().getUserId() : null);
			flatRepository.save(flat);
			response.setMessage(SuccessMessage.SUCC_MESSAGE_24);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_24);
		} catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_40);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_40);
		}
		return response;
	}

	@Override
	public UpdateFlatDetailsResponse updateFlatDetails(UpdateFlatDetailsRequest request) {
		UpdateFlatDetailsResponse response = new UpdateFlatDetailsResponse();
		response.setHeader(request.getHeader());
		response.setFlatNo(request.getFlatNo());
		try {
			Optional<Flat> existingFlat = flatRepository.findById(request.getFlatNo());
			if (existingFlat.isEmpty()) {
				response.setMessage(ErrorMessage.ERR_MESSAGE_41);
				response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_41);
				return response;
			}
			Flat flat = buildFlatEntity(null, request);
			flat.setCreatTs(existingFlat.get().getCreatTs());
			flat.setCreatUsrId(existingFlat.get().getCreatUsrId());
			flat.setFlatPndngPaymntLst(existingFlat.get().getFlatPndngPaymntLst());
			flat.setLstUpdtUsrId(request.getHeader() != null ? request.getHeader().getUserId() : null);
			flatRepository.save(flat);
			response.setMessage(SuccessMessage.SUCC_MESSAGE_25);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_25);
		} catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_41);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_41);
		}
		return response;
	}

	@Override
	public UploadFlatDetailsResponse uploadFlatDetails(UploadFlatDetailsRequest request) {
		UploadFlatDetailsResponse response = new UploadFlatDetailsResponse();
		response.setHeader(request.getHeader());
		List<List<String>> failedRows = new ArrayList<>();
		int totalRows = 0;
		int successRows = 0;

		try (Workbook workbook = new XSSFWorkbook(
				new ByteArrayInputStream(Base64.getDecoder().decode(stripDataUrlPrefix(request.getDocumentData()))))) {
			Sheet sheet = resolveSheet(workbook, request.getSheetName());
			for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
				Row row = sheet.getRow(rowIndex);
				if (row == null || isRowBlank(row)) {
					continue;
				}
				totalRows++;
				try {
					UploadedFlatRow uploadedRow = extractRow(row);
					ProfileContext profileContext = createOrUpdateProfileFromUploadedRow(uploadedRow, request);
					try {
						createFlatFromUploadedRow(uploadedRow, profileContext.profileId, request);
						ensureOwnerEntry(uploadedRow, profileContext.profileId, request);
						successRows++;
					} catch (Exception flatEx) {
						if (profileContext.created) {
							profileRepository.deleteById(profileContext.profileId);
						}
						failedRows.add(buildFailedRow(uploadedRow, flatEx.getMessage()));
					}
				} catch (Exception e) {
					failedRows.add(buildFailedRow(row, e.getMessage()));
				}
			}

			response.setTotalRows(totalRows);
			response.setSuccessRows(successRows);
			response.setFailedRows(failedRows.size());

			if (!failedRows.isEmpty()) {
				String failedBase64 = generateFailedRowsWorkbook(failedRows);
				response.setFailedRowsReportDocument(failedBase64);
				response.setFailedRowsReportDocumentName("flat_upload_failed_rows.xlsx");
				response.setMessage(ErrorMessage.ERR_MESSAGE_42);
				response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_42);
			} else {
				response.setMessage(SuccessMessage.SUCC_MESSAGE_26);
				response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_26);
			}
		} catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_42);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_42);
		}
		return response;
	}

	@Override
	public GetSampleExcellToUploadDataResponse getSampleExcellToUploadData() {
		GetSampleExcellToUploadDataResponse response = new GetSampleExcellToUploadDataResponse();
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Sheet sampleSheet = workbook.createSheet("flat_upload_sample");
			CellStyle headerStyle = createHeaderStyle(workbook);
			CellStyle sampleDataStyle = createSampleDataStyle(workbook);
			Row headerRow = sampleSheet.createRow(0);
			for (int i = 0; i < UPLOAD_HEADERS.length; i++) {
				Cell headerCell = headerRow.createCell(i);
				headerCell.setCellValue(UPLOAD_HEADERS[i]);
				headerCell.setCellStyle(headerStyle);
			}

			Row sampleRow = sampleSheet.createRow(1);
			createStyledCell(sampleRow, 0, "A-101", sampleDataStyle);
			createStyledCell(sampleRow, 1, "John Doe", sampleDataStyle);
			createStyledCell(sampleRow, 2, "MALE", sampleDataStyle);
			createStyledCell(sampleRow, 3, "T1", sampleDataStyle);
			createStyledCell(sampleRow, 4, "B1", sampleDataStyle);
			createStyledCell(sampleRow, 5, SAMPLE_DATE_FORMAT.format(LocalDate.of(2029, 3, 1)), sampleDataStyle);
			createStyledCell(sampleRow, 6, "OWNER", sampleDataStyle);
			createStyledCell(sampleRow, 7, "1200", sampleDataStyle);
			createStyledCell(sampleRow, 8, SAMPLE_DATE_FORMAT.format(LocalDate.of(1990, 1, 1)), sampleDataStyle);
			createStyledCell(sampleRow, 9, "9876543210", sampleDataStyle);
			createStyledCell(sampleRow, 10, "john.doe@example.com", sampleDataStyle);
			setColumnWidthsBasedOnTextLength(sampleSheet, UPLOAD_HEADERS.length);

			workbook.write(outputStream);
			response.setSampleDocumentData(Base64.getEncoder().encodeToString(outputStream.toByteArray()));
			response.setSampleDocumentName("flat_upload_sample.xlsx");
			response.setMessage(SuccessMessage.SUCC_MESSAGE_26);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_26);
		} catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_42);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_42);
		}
		return response;
	}

	private Flat buildFlatEntity(AddFlatDetailsRequest addRequest, UpdateFlatDetailsRequest updateRequest) {
		Flat flat = new Flat();
		boolean isAddRequest = addRequest != null;
		flat.setFlatNo(isAddRequest ? addRequest.getFlatNo() : updateRequest.getFlatNo());
		flat.setAprmntId(resolveApartmentId(isAddRequest ? addRequest.getAprmntId() : updateRequest.getAprmntId(),
				isAddRequest ? addRequest.getHeader().getApartmentId() : updateRequest.getHeader().getApartmentId()));
		flat.setFlatOwnerList(isAddRequest ? addRequest.getFlatOwnerList() : updateRequest.getFlatOwnerList());
		flat.setFlatTower(isAddRequest ? addRequest.getFlatTower() : updateRequest.getFlatTower());
		flat.setFlatBlock(isAddRequest ? addRequest.getFlatBlock() : updateRequest.getFlatBlock());
		flat.setFlatOwnerType(isAddRequest ? addRequest.getFlatOwnerType() : updateRequest.getFlatOwnerType());
		flat.setFlatArea(isAddRequest ? addRequest.getFlatArea() : updateRequest.getFlatArea());
		Date possnDate = isAddRequest ? addRequest.getFlatPossnDate() : updateRequest.getFlatPossnDate();
		if (possnDate != null) {
			flat.setFlatPossnDate(genericService.getCorrectLocalDateForInputDate(possnDate));
		}
		return flat;
	}

	private String resolveApartmentId(String requestApartmentId, String headerApartmentId) {
		if (requestApartmentId != null && !requestApartmentId.isBlank()) {
			return requestApartmentId;
		}
		return headerApartmentId;
	}

	private Sheet resolveSheet(Workbook workbook, String sheetName) {
		if (sheetName != null && !sheetName.isBlank()) {
			Sheet namedSheet = workbook.getSheet(sheetName);
			if (namedSheet != null) {
				return namedSheet;
			}
		}
		return workbook.getSheetAt(0);
	}

	private UploadedFlatRow extractRow(Row row) {
		UploadedFlatRow uploadedRow = new UploadedFlatRow();
		uploadedRow.flatNo = readStringCell(row, 0, true);
		uploadedRow.ownerName = readStringCell(row, 1, true);
		uploadedRow.ownerGender = readStringCell(row, 2, false);
		uploadedRow.tower = readStringCell(row, 3, false);
		uploadedRow.block = readStringCell(row, 4, false);
		uploadedRow.possessionDate = readDateCell(row, 5);
		uploadedRow.ownerType = readStringCell(row, 6, false);
		uploadedRow.flatArea = readStringCell(row, 7, false);
		uploadedRow.ownerDob = readDateCell(row, 8);
		uploadedRow.ownerPhone = readStringCell(row, 9, true);
		uploadedRow.ownerEmail = readStringCell(row, 10, false);
		return uploadedRow;
	}

	private ProfileContext createOrUpdateProfileFromUploadedRow(UploadedFlatRow row, UploadFlatDetailsRequest request) {
		if (row.ownerPhone == null || row.ownerPhone.isBlank()) {
			throw new IllegalArgumentException("Owner mobile number is required");
		}

		List<Profile> existingProfiles = profileRepository.findByPrflPhoneNo(row.ownerPhone);
		if (existingProfiles != null && !existingProfiles.isEmpty()) {
			Profile existingProfile = existingProfiles.get(0);
			List<ProfileAccountDetails> mergedDetails = mergeProfileAccountDetails(existingProfile.getPrflAcountDetails(),
					existingProfile.getPrflId(), row.flatNo, request);
			existingProfile.setPrflAcountDetails(genericService.toJson(mergedDetails));
			existingProfile.setLst_updt_usrId(request.getHeader() != null ? request.getHeader().getUserId() : null);
			profileRepository.save(existingProfile);
			return new ProfileContext(existingProfile.getPrflId(), false);
		}

		Profile profile = new Profile();
		String profileId = createProfileId();
		profile.setPrflId(profileId);
		Name name = new Name();
		name.setFirstName(row.ownerName);
		profile.setPrflName(genericService.toJson(name));
		profile.setGender(row.ownerGender);
		profile.setPrflPhoneNo(row.ownerPhone);
		profile.setPrflEmailAdrss(row.ownerEmail);
		profile.setPrflDob(row.ownerDob != null ? row.ownerDob.atStartOfDay() : null);
		profile.setProfileKind(row.ownerType);
		profile.setCreat_usr_id(request.getHeader() != null ? request.getHeader().getUserId() : null);
		profile.setPrflAcountDetails(genericService.toJson(buildProfileAccountDetails(row.flatNo, request, profileId)));
		profileRepository.save(profile);
		return new ProfileContext(profileId, true);
	}

	private List<ProfileAccountDetails> mergeProfileAccountDetails(String profileAccountDetailsJson, String profileId,
			String flatNo, UploadFlatDetailsRequest request) {
		List<ProfileAccountDetails> detailsList = new ArrayList<>();
		if (profileAccountDetailsJson != null && !profileAccountDetailsJson.isBlank()) {
			List<ProfileAccountDetails> existing = genericService.fromJson(profileAccountDetailsJson,
					new TypeReference<List<ProfileAccountDetails>>() {
					});
			if (existing != null) {
				detailsList.addAll(existing);
			}
		}

		String apartmentId = request.getHeader() != null ? request.getHeader().getApartmentId() : null;
		Optional<ProfileAccountDetails> apartmentDetails = detailsList.stream()
				.filter(details -> apartmentId != null && apartmentId.equals(details.getApartmentId())).findFirst();
		if (apartmentDetails.isPresent()) {
			ProfileAccountDetails details = apartmentDetails.get();
			List<String> flatIds = details.getFlatId() != null ? new ArrayList<>(details.getFlatId()) : new ArrayList<>();
			if (!flatIds.contains(flatNo)) {
				flatIds.add(flatNo);
			}
			details.setFlatId(flatIds);
			details.setProfileType(SecuraConstants.PROFILE_TYPE_OWNER);
			details.setPosition("MEMBER");
			details.setStatus(SecuraConstants.PROFILE_STATUS_ACTIVE);
		} else {
			detailsList.add(buildSingleProfileAccountDetail(flatNo, request, profileId));
		}
		return detailsList;
	}

	private List<ProfileAccountDetails> buildProfileAccountDetails(String flatNo, UploadFlatDetailsRequest request,
			String profileId) {
		return Collections.singletonList(buildSingleProfileAccountDetail(flatNo, request, profileId));
	}

	private ProfileAccountDetails buildSingleProfileAccountDetail(String flatNo, UploadFlatDetailsRequest request,
			String profileId) {
		ProfileAccountDetails details = new ProfileAccountDetails();
		details.setApartmentId(request.getHeader() != null ? request.getHeader().getApartmentId() : null);
		details.setApartmentName(profileId);
		details.setFlatId(Collections.singletonList(flatNo));
		details.setProfileType(SecuraConstants.PROFILE_TYPE_OWNER);
		details.setPosition("MEMBER");
		details.setStatus(SecuraConstants.PROFILE_STATUS_ACTIVE);
		return details;
	}

	private void createFlatFromUploadedRow(UploadedFlatRow row, String profileId, UploadFlatDetailsRequest request) {
		Flat flat = new Flat();
		flat.setFlatNo(row.flatNo);
		flat.setAprmntId(request.getHeader() != null ? request.getHeader().getApartmentId() : null);
		flat.setFlatOwnerList(genericService.toJson(Collections.singletonList(profileId)));
		flat.setFlatTower(row.tower);
		flat.setFlatBlock(row.block);
		flat.setFlatPossnDate(row.possessionDate != null ? row.possessionDate.atStartOfDay() : null);
		flat.setFlatOwnerType(row.ownerType);
		flat.setFlatArea(row.flatArea);
		flat.setFlatPndngPaymntLst("[]");
		flat.setCreatUsrId(request.getHeader() != null ? request.getHeader().getUserId() : null);
		flatRepository.save(flat);
	}

	private void ensureOwnerEntry(UploadedFlatRow row, String profileId, UploadFlatDetailsRequest request) {
		List<Owner> owners = ownerRepository.findByFlatNo(row.flatNo);
		if (owners != null) {
			for (Owner owner : owners) {
				List<String> ownerProfiles = parseProfileIds(owner.getPrflId());
				if (ownerProfiles.contains(profileId)) {
					return;
				}
			}

			Optional<Owner> activeOwner = owners.stream().filter(owner -> owner.getEndDate() == null).findFirst();
			if (activeOwner.isPresent()) {
				Owner currentOwner = activeOwner.get();
				currentOwner.setStatus(SecuraConstants.PROFILE_STATUS_INACTIVE);
				currentOwner.setEndDate(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
				currentOwner.setLstUpdtUsrId(request.getHeader() != null ? request.getHeader().getUserId() : null);
				ownerRepository.save(currentOwner);
			}
		}

		Owner owner = new Owner();
		owner.setOwnerId(createOwnerId(row.flatNo));
		owner.setAprmt_id(request.getHeader() != null ? request.getHeader().getApartmentId() : null);
		owner.setCreatUsrId(request.getHeader() != null ? request.getHeader().getUserId() : null);
		owner.setFlatNo(row.flatNo);
		owner.setPrflId(genericService.toJson(Collections.singletonList(profileId)));
		owner.setStatus(SecuraConstants.PROFILE_STATUS_ACTIVE);
		owner.setStartDate(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
		ownerRepository.save(owner);
	}

	private List<String> parseProfileIds(String profileIdsJson) {
		if (profileIdsJson == null || profileIdsJson.isBlank()) {
			return new ArrayList<>();
		}
		List<String> profileIds = genericService.fromJson(profileIdsJson, new TypeReference<List<String>>() {
		});
		return profileIds != null ? profileIds : new ArrayList<>();
	}

	private String createProfileId() {
		return SecuraConstants.PROFILE_ID_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
	}

	private String createOwnerId(String flatNo) {
		return SecuraConstants.PROFILE_TYPE_OWNER + flatNo
				+ UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
	}

	private String generateFailedRowsWorkbook(List<List<String>> failedRows) throws Exception {
		try (Workbook failedWorkbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Sheet failedSheet = failedWorkbook.createSheet("failed_rows");
			CellStyle headerStyle = createHeaderStyle(failedWorkbook);
			CellStyle reasonStyle = createReasonStyle(failedWorkbook);
			Row headerRow = failedSheet.createRow(0);
			for (int i = 0; i < UPLOAD_HEADERS.length; i++) {
				createStyledCell(headerRow, i, UPLOAD_HEADERS[i], headerStyle);
			}
			createStyledCell(headerRow, UPLOAD_HEADERS.length, "Reason", headerStyle);

			for (int i = 0; i < failedRows.size(); i++) {
				Row row = failedSheet.createRow(i + 1);
				List<String> values = failedRows.get(i);
				for (int col = 0; col < values.size(); col++) {
					Cell cell = row.createCell(col);
					cell.setCellValue(values.get(col));
					if (col == UPLOAD_HEADERS.length) {
						cell.setCellStyle(reasonStyle);
					}
				}
			}
			setColumnWidthsBasedOnTextLength(failedSheet, UPLOAD_HEADERS.length + 1);
			failedWorkbook.write(outputStream);
			return Base64.getEncoder().encodeToString(outputStream.toByteArray());
		}
	}

	private List<String> buildFailedRow(UploadedFlatRow row, String reason) {
		List<String> values = new ArrayList<>();
		values.add(safeValue(row.flatNo));
		values.add(safeValue(row.ownerName));
		values.add(safeValue(row.ownerGender));
		values.add(safeValue(row.tower));
		values.add(safeValue(row.block));
		values.add(row.possessionDate != null ? row.possessionDate.toString() : "");
		values.add(safeValue(row.ownerType));
		values.add(safeValue(row.flatArea));
		values.add(row.ownerDob != null ? row.ownerDob.toString() : "");
		values.add(safeValue(row.ownerPhone));
		values.add(safeValue(row.ownerEmail));
		values.add(safeValue(reason));
		return values;
	}

	private List<String> buildFailedRow(Row row, String reason) {
		List<String> values = new ArrayList<>();
		for (int i = 0; i < UPLOAD_HEADERS.length; i++) {
			values.add(readStringCell(row, i, false));
		}
		values.add(safeValue(reason));
		return values;
	}

	private String readStringCell(Row row, int cellIndex, boolean required) {
		Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
		if (cell == null) {
			if (required) {
				throw new IllegalArgumentException("Missing required value at column " + (cellIndex + 1));
			}
			return "";
		}
		String value = dataFormatter.formatCellValue(cell);
		if (required && (value == null || value.isBlank())) {
			throw new IllegalArgumentException("Missing required value at column " + (cellIndex + 1));
		}
		return value != null ? value.trim() : "";
	}

	private LocalDate readDateCell(Row row, int cellIndex) {
		Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
		if (cell == null) {
			return null;
		}
		if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
			return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		}
		String value = dataFormatter.formatCellValue(cell);
		if (value == null || value.isBlank()) {
			return null;
		}
		DateTimeFormatter[] formatters = { DateTimeFormatter.ofPattern("dd-MM-yyyy"),
				new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d-MMM-yyyy")
						.toFormatter(Locale.ENGLISH),
				DateTimeFormatter.ofPattern("dd/MM/yyyy"), DateTimeFormatter.ofPattern("yyyy-MM-dd"),
				DateTimeFormatter.ofPattern("MM/dd/yyyy") };
		for (DateTimeFormatter formatter : formatters) {
			try {
				return LocalDate.parse(value.trim(), formatter);
			} catch (DateTimeParseException e) {
			}
		}
		throw new IllegalArgumentException("Invalid date format at column " + (cellIndex + 1));
	}

	private boolean isRowBlank(Row row) {
		for (int i = 0; i < UPLOAD_HEADERS.length; i++) {
			Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
			if (cell != null && !dataFormatter.formatCellValue(cell).isBlank()) {
				return false;
			}
		}
		return true;
	}

	private String stripDataUrlPrefix(String base64Value) {
		if (base64Value == null) {
			return "";
		}
		int commaIndex = base64Value.indexOf(',');
		if (commaIndex >= 0) {
			return base64Value.substring(commaIndex + 1);
		}
		return base64Value;
	}

	private String safeValue(String value) {
		return value == null ? "" : value;
	}

	private void createStyledCell(Row row, int columnIndex, String value, CellStyle style) {
		Cell cell = row.createCell(columnIndex);
		cell.setCellValue(value);
		cell.setCellStyle(style);
	}

	private CellStyle createHeaderStyle(Workbook workbook) {
		CellStyle headerStyle = workbook.createCellStyle();
		headerStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		Font headerFont = workbook.createFont();
		headerFont.setColor(IndexedColors.WHITE.getIndex());
		headerFont.setBold(true);
		headerStyle.setFont(headerFont);
		return headerStyle;
	}

	private CellStyle createSampleDataStyle(Workbook workbook) {
		CellStyle sampleDataStyle = workbook.createCellStyle();
		sampleDataStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
		sampleDataStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		return sampleDataStyle;
	}

	private CellStyle createReasonStyle(Workbook workbook) {
		CellStyle reasonStyle = workbook.createCellStyle();
		reasonStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
		reasonStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		Font reasonFont = workbook.createFont();
		reasonFont.setColor(IndexedColors.BLACK.getIndex());
		reasonFont.setBold(true);
		reasonStyle.setFont(reasonFont);
		return reasonStyle;
	}

	private void setColumnWidthsBasedOnTextLength(Sheet sheet, int columnCount) {
		for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
			int maxTextLength = 1;
			for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
				Row row = sheet.getRow(rowIndex);
				if (row == null) {
					continue;
				}
				Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
				if (cell == null) {
					continue;
				}
				String value = dataFormatter.formatCellValue(cell);
				maxTextLength = Math.max(maxTextLength, value.length());
			}
			int width = (int) Math.ceil(maxTextLength * 1.5 * 256);
			sheet.setColumnWidth(columnIndex, Math.min(width, 255 * 256));
		}
	}

	private static class UploadedFlatRow {
		private String flatNo;
		private String ownerName;
		private String ownerGender;
		private String tower;
		private String block;
		private LocalDate possessionDate;
		private String ownerType;
		private String flatArea;
		private LocalDate ownerDob;
		private String ownerPhone;
		private String ownerEmail;
	}

	private static class ProfileContext {
		private final String profileId;
		private final boolean created;

		private ProfileContext(String profileId, boolean created) {
			this.profileId = profileId;
			this.created = created;
		}
	}
}
