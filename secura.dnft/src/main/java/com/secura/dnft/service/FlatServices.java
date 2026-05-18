package com.secura.dnft.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
import com.secura.dnft.dao.DueAmountDetailsRepository;
import com.secura.dnft.dao.FlatRepository;
import com.secura.dnft.dao.OwnerRepository;
import com.secura.dnft.dao.PaymentRepository;
import com.secura.dnft.dao.ProfileRepository;
import com.secura.dnft.dao.TransactionRepository;
import com.secura.dnft.entity.DueAmountDetailsEntity;
import com.secura.dnft.entity.Flat;
import com.secura.dnft.entity.Owner;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.entity.Profile;
import com.secura.dnft.entity.Transaction;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.Name;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.interfaceservice.FlatInterface;
import com.secura.dnft.request.response.AddFlatDetailsRequest;
import com.secura.dnft.request.response.AddFlatDetailsResponse;
import com.secura.dnft.request.response.GetAllFlatsRequest;
import com.secura.dnft.request.response.GetAllFlatsResponse;
import com.secura.dnft.request.response.GetDueAmountForFlatRequest;
import com.secura.dnft.request.response.GetDueAmountForFlatResponse;
import com.secura.dnft.request.response.GetDueAmountForPerHeadCalculationRequest;
import com.secura.dnft.request.response.GetDueAmountForPerHeadCalculationResponse;
import com.secura.dnft.request.response.GetSampleExcellToUploadDataResponse;
import com.secura.dnft.request.response.PaymentDetail;
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
	private DueAmountDetailsRepository dueAmountDetailsRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private GenericService genericService;

	@Autowired
	private TransactionRepository transactionRepository;

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

	@Override
	public GetAllFlatsResponse getAllFlats(GetAllFlatsRequest request) {
		GetAllFlatsResponse response = new GetAllFlatsResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		try {
			String apartmentId = request != null && request.getGenericHeader() != null
					? request.getGenericHeader().getApartmentId()
					: null;
			List<Flat> apartmentFlats = (apartmentId == null || apartmentId.isBlank()) ? flatRepository.findAll()
					: flatRepository.findByAprmntId(apartmentId);

			boolean hasNamedBlock = apartmentFlats.stream().anyMatch(flat -> hasText(flat.getFlatBlock()));
			if (!hasNamedBlock) {
				response.setTowerList(buildTowerHierarchy(apartmentFlats));
			} else {
				response.setBlockList(buildBlockHierarchy(apartmentFlats));
			}
			response.setMessage(SuccessMessage.SUCC_MESSAGE_27);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_27);
		} catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_43);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_43);
		}
		return response;
	}

	@Override
	public GetDueAmountForFlatResponse getDueAmountForFlat(GetDueAmountForFlatRequest request) {
		GetDueAmountForFlatResponse response = new GetDueAmountForFlatResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		initializeDefaultDueResponse(response);
		try {
			String flatId = request != null ? request.getFlatId() : null;
			Optional<Flat> optionalFlat = flatRepository.findById(flatId);
			if (optionalFlat.isPresent()) {
				Flat flat = optionalFlat.get();
				List<String> pendingDueKeys = parseStringList(flat.getFlatPndngPaymntLst());
				List<String> filteredPendingDueKeys = filterPendingDueKeys(pendingDueKeys);
				List<String> dueIds = extractDueIdsFromFlatPendingList(filteredPendingDueKeys);
				if (!dueIds.isEmpty()) {
					List<DueAmountDetailsEntity> dueEntities = dueAmountDetailsRepository.findByDueIdIn(dueIds);
					List<DueAmountDetailsEntity> filteredDues = filterDueEntitiesByPendingKeys(dueEntities,
							filteredPendingDueKeys);
					Map<String, List<String>> paymentIdToDueIdsMap = groupDueIdsByPayment(filteredDues);
					Map<String, List<DueAmountDetailsEntity>> finalPaymentMap = buildFinalPaymentMap(paymentIdToDueIdsMap,
							filteredDues);
					Map<PaymentDetail, List<DueAmountDetailsEntity>> dueDetails = buildDueDetails(finalPaymentMap);
					response.setDueDetails(dueDetails);
					DueTotals dueTotals = calculateDueTotalsFromDueDetails(dueDetails, flatId, flat.getFlatArea());
					response.setTotalDue(formatAmount(dueTotals.totalDue()));
					response.setTotalMandatoryPayment(formatAmount(dueTotals.totalMandatoryPayment()));
					response.setTotalOptionalPayment(formatAmount(dueTotals.totalOptionalPayment()));
					response.setPenaltyAdded(hasPenalty(finalPaymentMap));
				}
			}
			response.setMessage(SuccessMessage.SUCC_MESSAGE_28);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_28);
		} catch (Exception e) {
			initializeDefaultDueResponse(response);
			response.setMessage(ErrorMessage.ERR_MESSAGE_43);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_43);
		}
		return response;
	}

	@Override
	public GetDueAmountForPerHeadCalculationResponse getDueAmountForPerHeadCalculation(
			GetDueAmountForPerHeadCalculationRequest request) {
		GetDueAmountForPerHeadCalculationResponse response = new GetDueAmountForPerHeadCalculationResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		response.setMessage(SuccessMessage.SUCC_MESSAGE_28);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_28);
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

	private List<GetAllFlatsResponse.BlockDetails> buildBlockHierarchy(List<Flat> apartmentFlats) {
		Map<String, List<Flat>> groupedByBlock = apartmentFlats.stream()
				.collect(Collectors.groupingBy(flat -> normalizeHierarchyKey(flat.getFlatBlock()), LinkedHashMap::new,
						Collectors.toList()));
		List<GetAllFlatsResponse.BlockDetails> blockList = new ArrayList<>();
		for (Map.Entry<String, List<Flat>> blockEntry : groupedByBlock.entrySet()) {
			GetAllFlatsResponse.BlockDetails blockDetails = new GetAllFlatsResponse.BlockDetails();
			blockDetails.setBlockName(blankToNull(blockEntry.getKey()));
			List<Flat> blockFlats = blockEntry.getValue();
			List<Flat> directFlats = blockFlats.stream().filter(flat -> !hasText(flat.getFlatTower()))
					.collect(Collectors.toList());
			if (!directFlats.isEmpty()) {
				blockDetails.setFlatList(
						directFlats.stream().map(Flat::getFlatNo).filter(this::hasText).collect(Collectors.toList()));
			}
			List<Flat> towerFlats = blockFlats.stream().filter(flat -> hasText(flat.getFlatTower())).collect(Collectors.toList());
			if (!towerFlats.isEmpty()) {
				blockDetails.setTowerList(buildTowerHierarchy(towerFlats));
			}
			blockList.add(blockDetails);
		}
		return blockList;
	}

	private List<GetAllFlatsResponse.TowerDetails> buildTowerHierarchy(List<Flat> flats) {
		Map<String, List<Flat>> groupedByTower = flats.stream()
				.collect(Collectors.groupingBy(flat -> normalizeHierarchyKey(flat.getFlatTower()), LinkedHashMap::new,
						Collectors.toList()));
		List<GetAllFlatsResponse.TowerDetails> towerList = new ArrayList<>();
		for (Map.Entry<String, List<Flat>> towerEntry : groupedByTower.entrySet()) {
			GetAllFlatsResponse.TowerDetails towerDetails = new GetAllFlatsResponse.TowerDetails();
			towerDetails.setTowerName(blankToNull(towerEntry.getKey()));
			towerDetails.setFlatList(
					towerEntry.getValue().stream().map(Flat::getFlatNo).filter(this::hasText).collect(Collectors.toList()));
			towerList.add(towerDetails);
		}
		return towerList;
	}

	private String normalizeHierarchyKey(String value) {
		return value == null ? "" : value.trim();
	}

	private Map<String, List<String>> groupDueIdsByPayment(List<DueAmountDetailsEntity> dueEntities) {
		Map<String, List<String>> paymentIdToDueIdsMap = new LinkedHashMap<>();
		if (dueEntities == null || dueEntities.isEmpty()) {
			return paymentIdToDueIdsMap;
		}
		for (DueAmountDetailsEntity dueEntity : dueEntities) {
			String paymentId = dueEntity != null ? dueEntity.getPaymentId() : null;
			String dueId = dueEntity != null ? dueEntity.getDueId() : null;
			if (!hasText(paymentId) || !hasText(dueId)) {
				continue;
			}
			List<String> paymentDueIds = paymentIdToDueIdsMap.computeIfAbsent(paymentId, key -> new ArrayList<>());
			if (!paymentDueIds.contains(dueId)) {
				paymentDueIds.add(dueId);
			}
		}
		return paymentIdToDueIdsMap;
	}

	private Map<String, List<DueAmountDetailsEntity>> buildFinalPaymentMap(Map<String, List<String>> paymentIdToDueIdsMap,
			List<DueAmountDetailsEntity> dueEntities) {
		Map<String, List<DueAmountDetailsEntity>> finalPaymentMap = new LinkedHashMap<>();
		if (paymentIdToDueIdsMap == null || paymentIdToDueIdsMap.isEmpty()) {
			return finalPaymentMap;
		}
		Map<String, List<DueAmountDetailsEntity>> dueEntitiesByPayment = dueEntities == null ? Collections.emptyMap()
				: dueEntities.stream().filter(Objects::nonNull).filter(dueEntity -> hasText(dueEntity.getPaymentId()))
						.collect(Collectors.groupingBy(dueEntity -> dueEntity.getPaymentId(), LinkedHashMap::new,
								Collectors.toList()));
		for (Map.Entry<String, List<String>> entry : paymentIdToDueIdsMap.entrySet()) {
			List<DueAmountDetailsEntity> paymentDues = dueEntitiesByPayment.getOrDefault(entry.getKey(), Collections.emptyList())
					.stream().filter(dueEntity -> entry.getValue().contains(dueEntity.getDueId())).collect(Collectors.toList());
			List<DueAmountDetailsEntity> selectedDues = selectEligibleDues(paymentDues);
			if (!selectedDues.isEmpty()) {
				finalPaymentMap.put(entry.getKey(), selectedDues);
			}
		}
		return finalPaymentMap;
	}

	private List<DueAmountDetailsEntity> selectEligibleDues(List<DueAmountDetailsEntity> dueEntities) {
		if (dueEntities == null || dueEntities.isEmpty()) {
			return Collections.emptyList();
		}
		Map<String, List<DueAmountDetailsEntity>> duesByGroup = dueEntities.stream().filter(Objects::nonNull)
				.collect(Collectors.groupingBy(this::buildDueSelectionGroupKey, LinkedHashMap::new, Collectors.toList()));
		List<DueAmountDetailsEntity> selectedDues = new ArrayList<>();
		LocalDate today = LocalDate.now();
		for (List<DueAmountDetailsEntity> groupDues : duesByGroup.values()) {
			List<DueAmountDetailsEntity> sortedGroupDues = groupDues.stream().filter(Objects::nonNull)
					.sorted(Comparator.comparing(DueAmountDetailsEntity::getDueDate,
							Comparator.nullsLast(Comparator.naturalOrder())))
					.collect(Collectors.toList());
			selectedDues.addAll(sortedGroupDues.stream().filter(dueEntity -> dueEntity.getDueDate() == null
					|| !dueEntity.getDueDate().isAfter(today)).collect(Collectors.toList()));
			sortedGroupDues.stream().filter(dueEntity -> dueEntity.getDueDate() != null && dueEntity.getDueDate().isAfter(today))
					.findFirst().ifPresent(selectedDues::add);
		}
		selectedDues.sort(Comparator.comparingInt(
				(DueAmountDetailsEntity dueEntity) -> getCyclePriority(dueEntity != null ? dueEntity.getCollectionCycle() : null))
				.thenComparing(dueEntity -> dueEntity != null ? dueEntity.getDueDate() : null,
						Comparator.nullsLast(Comparator.naturalOrder())));
		return selectedDues;
	}

	private String buildDueSelectionGroupKey(DueAmountDetailsEntity dueEntity) {
		if (dueEntity == null) {
			return "";
		}
		return normalizeHierarchyKey(dueEntity.getDueId()).toUpperCase(Locale.ENGLISH) + "|"
				+ normalizeHierarchyKey(dueEntity.getCollectionCycle()).toUpperCase(Locale.ENGLISH) + "|"
				+ normalizeHierarchyKey(dueEntity.getFlatArea()).toUpperCase(Locale.ENGLISH);
	}

	private int getCyclePriority(String cycle) {
		if (cycle == null) {
			return Integer.MAX_VALUE;
		}
		switch (cycle.trim().toUpperCase(Locale.ENGLISH)) {
		case "MONTHLY":
			return 1;
		case "QUARTERLY":
			return 2;
		case "HALF_YEARLY":
		case "HALF YEARLY":
			return 3;
		case "YEARLY":
			return 4;
		default:
			return Integer.MAX_VALUE;
		}
	}

	private Map<PaymentDetail, List<DueAmountDetailsEntity>> buildDueDetails(
			Map<String, List<DueAmountDetailsEntity>> finalPaymentMap) {
		Map<PaymentDetail, List<DueAmountDetailsEntity>> dueDetails = new LinkedHashMap<>();
		if (finalPaymentMap == null || finalPaymentMap.isEmpty()) {
			return dueDetails;
		}
		for (Map.Entry<String, List<DueAmountDetailsEntity>> entry : finalPaymentMap.entrySet()) {
			PaymentDetail paymentDetail = new PaymentDetail();
			paymentDetail.setPaymentId(entry.getKey());
			paymentDetail.setPaymentName(resolvePaymentName(entry.getKey(), entry.getValue()));
			dueDetails.put(paymentDetail, entry.getValue());
		}
		return dueDetails;
	}

	private String resolvePaymentName(String paymentId, List<DueAmountDetailsEntity> dueEntities) {
		Optional<PaymentEntity> paymentEntity = paymentRepository.findFirstByPaymentId(paymentId);
		if (paymentEntity.isPresent() && hasText(paymentEntity.get().getPaymentName())) {
			return paymentEntity.get().getPaymentName();
		}
		if (dueEntities == null || dueEntities.isEmpty()) {
			return null;
		}
		return dueEntities.stream().map(DueAmountDetailsEntity::getPaymentName).filter(this::hasText).findFirst().orElse(null);
	}

	private DueTotals calculateDueTotalsFromDueDetails(Map<PaymentDetail, List<DueAmountDetailsEntity>> dueDetails, String flatId,
			String flatArea) {
		if (dueDetails == null || dueDetails.isEmpty()) {
			return new DueTotals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
		}
		List<String> paymentIds = dueDetails.keySet().stream().filter(Objects::nonNull).map(PaymentDetail::getPaymentId)
				.filter(this::hasText).collect(Collectors.toList());
		List<DueAmountDetailsEntity> duesForPayments = paymentIds.isEmpty() ? Collections.emptyList()
				: dueAmountDetailsRepository.findByPaymentIdIn(paymentIds);
		Map<String, List<DueAmountDetailsEntity>> duesByPaymentId = (duesForPayments == null ? Collections.<DueAmountDetailsEntity>emptyList()
				: duesForPayments)
				.stream().filter(Objects::nonNull).filter(due -> hasText(due.getPaymentId()))
				.collect(Collectors.groupingBy(DueAmountDetailsEntity::getPaymentId, LinkedHashMap::new, Collectors.toList()));
		BigDecimal totalDue = BigDecimal.ZERO;
		BigDecimal totalMandatoryPayment = BigDecimal.ZERO;
		BigDecimal totalOptionalPayment = BigDecimal.ZERO;
		for (Map.Entry<PaymentDetail, List<DueAmountDetailsEntity>> entry : dueDetails.entrySet()) {
			PaymentDetail paymentDetail = entry.getKey();
			String paymentId = paymentDetail != null ? paymentDetail.getPaymentId() : null;
			if (!hasText(paymentId)) {
				continue;
			}
			List<DueAmountDetailsEntity> paymentDues = duesByPaymentId.get(paymentId);
			if (paymentDues == null || paymentDues.isEmpty()) {
				paymentDues = entry.getValue();
			}
			List<DueAmountDetailsEntity> selectedCycleDues = selectCycleDuesForTotals(paymentDues);
			List<DueAmountDetailsEntity> filteredCycleDues = filterDuesByFlatArea(selectedCycleDues, flatArea);
			BigDecimal totalDuePerPaymentId = filteredCycleDues.stream().filter(Objects::nonNull)
					.map(dueEntity -> parseAmount(dueEntity.getTotalAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
			List<Transaction> paymentTransactions = transactionRepository.findByPymntIdAndFlatIdAndTrnsStatus(paymentId, flatId,
					"SUCCESS");
			List<Transaction> normalizedTransactions = paymentTransactions == null ? Collections.emptyList()
					: paymentTransactions;
			boolean hasPerHeadCapita = filteredCycleDues.stream().filter(Objects::nonNull)
					.anyMatch(dueEntity -> isPerHeadCapita(dueEntity.getPaymentCapita()));
			BigDecimal totalAmountPaidPerPaymentId = hasPerHeadCapita ? BigDecimal.ZERO
					: normalizedTransactions.stream().map(Transaction::getTrnsAmt).map(this::parseAmount)
							.reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal netDueForPaymentId = totalDuePerPaymentId.subtract(totalAmountPaidPerPaymentId);
			if (netDueForPaymentId.compareTo(BigDecimal.ZERO) < 0) {
				netDueForPaymentId = BigDecimal.ZERO;
			}
			totalDue = totalDue.add(netDueForPaymentId);
			String paymentType = filteredCycleDues.stream().filter(Objects::nonNull).findFirst()
					.map(DueAmountDetailsEntity::getPaymentType).orElse(null);
			if ("OPTIONAL".equalsIgnoreCase(paymentType)) {
				totalOptionalPayment = totalOptionalPayment.add(netDueForPaymentId);
			} else if ("MANDATORY".equalsIgnoreCase(paymentType)) {
				totalMandatoryPayment = totalMandatoryPayment.add(netDueForPaymentId);
			}
		}
		return new DueTotals(totalDue, totalMandatoryPayment, totalOptionalPayment);
	}

	private List<DueAmountDetailsEntity> filterDuesByFlatArea(List<DueAmountDetailsEntity> selectedCycleDues, String flatArea) {
		if (selectedCycleDues == null || selectedCycleDues.isEmpty()) {
			return Collections.emptyList();
		}
		DueAmountDetailsEntity firstDue = selectedCycleDues.stream().filter(Objects::nonNull).findFirst().orElse(null);
		if (firstDue == null) {
			return Collections.emptyList();
		}
		if ("ALL".equalsIgnoreCase(firstDue.getFlatArea()) || !hasText(flatArea)) {
			return selectedCycleDues.stream().filter(Objects::nonNull).collect(Collectors.toList());
		}
		return selectedCycleDues.stream().filter(Objects::nonNull)
				.filter(dueEntity -> hasText(dueEntity.getFlatArea()) && dueEntity.getFlatArea().equalsIgnoreCase(flatArea))
				.collect(Collectors.toList());
	}

	private List<DueAmountDetailsEntity> selectCycleDuesForTotals(List<DueAmountDetailsEntity> paymentDues) {
		if (paymentDues == null || paymentDues.isEmpty()) {
			return Collections.emptyList();
		}
		List<DueAmountDetailsEntity> onceCycleDues = paymentDues.stream().filter(Objects::nonNull)
				.filter(dueEntity -> isOnceCycleForTotals(dueEntity.getCollectionCycle())).collect(Collectors.toList());
		if (!onceCycleDues.isEmpty()) {
			return onceCycleDues;
		}
		int highestCyclePriority = paymentDues.stream().filter(Objects::nonNull)
				.map(DueAmountDetailsEntity::getCollectionCycle).map(this::getCyclePriorityForTotals).max(Integer::compareTo)
				.orElse(Integer.MIN_VALUE);
		if (highestCyclePriority <= 0) {
			return Collections.emptyList();
		}
		return paymentDues.stream().filter(Objects::nonNull)
				.filter(dueEntity -> getCyclePriorityForTotals(dueEntity.getCollectionCycle()) == highestCyclePriority)
				.collect(Collectors.toList());
	}

	private boolean isOnceCycleForTotals(String cycle) {
		if (!hasText(cycle)) {
			return false;
		}
		return SecuraConstants.PAYMENT_CYCLE_ONCE.equalsIgnoreCase(cycle.trim());
	}

	private boolean isPerHeadCapita(String paymentCapita) {
		if (!hasText(paymentCapita)) {
			return false;
		}
		String normalized = paymentCapita.toUpperCase(Locale.ROOT).replaceAll("[\\s_-]", "");
		return "PERHEAD".equals(normalized);
	}

	private int getCyclePriorityForTotals(String cycle) {
		if (!hasText(cycle)) {
			return 0;
		}
		String normalizedCycle = normalizeCycleForTotals(cycle);
		switch (normalizedCycle) {
		case SecuraConstants.PAYMENT_CYCLE_YEARLY:
			return 4;
		case SecuraConstants.PAYMENT_CYCLE_HALF_YEARLY:
			return 3;
		case "QUARTERLY":
		case SecuraConstants.PAYMENT_CYCLE_QUATERLY:
			return 2;
		case SecuraConstants.PAYMENT_CYCLE_MONTHLY:
			return 1;
		default:
			return 0;
		}
	}

	private String normalizeCycleForTotals(String cycle) {
		String normalized = cycle == null ? null : cycle.trim().toUpperCase(Locale.ENGLISH).replace("_", " ");
		if ("HALFYEARLY".equals(normalized)) {
			return SecuraConstants.PAYMENT_CYCLE_HALF_YEARLY;
		}
		if ("QUATERLY".equals(normalized)) {
			return SecuraConstants.PAYMENT_CYCLE_QUATERLY;
		}
		return normalized;
	}

	private record DueTotals(BigDecimal totalDue, BigDecimal totalMandatoryPayment, BigDecimal totalOptionalPayment) {
	}

	private boolean hasPenalty(Map<String, List<DueAmountDetailsEntity>> finalPaymentMap) {
		if (finalPaymentMap == null || finalPaymentMap.isEmpty()) {
			return false;
		}
		return finalPaymentMap.values().stream().flatMap(List::stream)
				.anyMatch(dueEntity -> parseAmount(dueEntity != null ? dueEntity.getFineAmount() : null)
						.compareTo(BigDecimal.ZERO) > 0);
	}

	private BigDecimal parseAmount(String amount) {
		if (!hasText(amount)) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(amount.trim());
		} catch (NumberFormatException ex) {
			return BigDecimal.ZERO;
		}
	}

	private String formatAmount(BigDecimal amount) {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO.toPlainString();
		}
		return amount.stripTrailingZeros().toPlainString();
	}

	private List<String> parseStringList(String json) {
		if (!hasText(json)) {
			return new ArrayList<>();
		}
		try {
			List<String> values = genericService.fromJson(json, new TypeReference<List<String>>() {
			});
			return values == null ? new ArrayList<>() : new ArrayList<>(values);
		} catch (Exception ex) {
			return new ArrayList<>();
		}
	}

	private List<String> extractDueIdsFromFlatPendingList(List<String> pendingDueKeys) {
		if (pendingDueKeys == null || pendingDueKeys.isEmpty()) {
			return new ArrayList<>();
		}
		LinkedHashSet<String> dueIds = new LinkedHashSet<>();
		for (String pendingDueKey : pendingDueKeys) {
			if (!hasText(pendingDueKey)) {
				continue;
			}
			int separatorIndex = pendingDueKey.indexOf('_');
			String dueId = separatorIndex > 0 ? pendingDueKey.substring(0, separatorIndex) : pendingDueKey;
			if (hasText(dueId)) {
				dueIds.add(dueId);
			}
		}
		return new ArrayList<>(dueIds);
	}

	private List<String> filterPendingDueKeys(List<String> pendingDueKeys) {
		if (pendingDueKeys == null || pendingDueKeys.isEmpty()) {
			return new ArrayList<>();
		}
		Map<String, List<PendingDueKey>> groupedKeys = pendingDueKeys.stream().map(this::parsePendingDueKey)
				.filter(Objects::nonNull).collect(Collectors.groupingBy(this::buildPendingDueGroupKey, LinkedHashMap::new,
						Collectors.toList()));
		LocalDate today = LocalDate.now();
		LinkedHashSet<String> selectedKeys = new LinkedHashSet<>();
		for (List<PendingDueKey> groupedDueKeys : groupedKeys.values()) {
			boolean allFuture = groupedDueKeys.stream()
					.allMatch(pendingDueKey -> pendingDueKey.dueDate() != null && pendingDueKey.dueDate().isAfter(today));
			if (allFuture) {
				groupedDueKeys.stream().sorted(Comparator.comparing(PendingDueKey::dueDate))
						.findFirst().ifPresent(dueKey -> selectedKeys.add(dueKey.originalKey()));
				continue;
			}
			groupedDueKeys.stream().map(PendingDueKey::originalKey).forEach(selectedKeys::add);
		}
		return new ArrayList<>(selectedKeys);
	}

	private List<DueAmountDetailsEntity> filterDueEntitiesByPendingKeys(List<DueAmountDetailsEntity> dueEntities,
			List<String> filteredPendingDueKeys) {
		if (dueEntities == null || dueEntities.isEmpty() || filteredPendingDueKeys == null || filteredPendingDueKeys.isEmpty()) {
			return Collections.emptyList();
		}
		LinkedHashSet<String> validDueEntityKeys = filteredPendingDueKeys.stream().map(this::parsePendingDueKey)
				.filter(Objects::nonNull).map(this::buildPendingDueEntityKey).collect(Collectors.toCollection(LinkedHashSet::new));
		return dueEntities.stream().filter(Objects::nonNull)
				.filter(dueEntity -> validDueEntityKeys.contains(buildPendingDueEntityKey(dueEntity)))
				.collect(Collectors.toList());
	}

	private PendingDueKey parsePendingDueKey(String pendingDueKey) {
		if (!hasText(pendingDueKey)) {
			return null;
		}
		String normalizedPendingKey = pendingDueKey.trim();
		int firstSeparatorIndex = normalizedPendingKey.indexOf('_');
		int lastSeparatorIndex = normalizedPendingKey.lastIndexOf('_');
		if (firstSeparatorIndex <= 0 || lastSeparatorIndex <= firstSeparatorIndex) {
			return null;
		}
		int secondLastSeparatorIndex = normalizedPendingKey.lastIndexOf('_', lastSeparatorIndex - 1);
		if (secondLastSeparatorIndex <= firstSeparatorIndex) {
			return null;
		}
		String dueId = normalizedPendingKey.substring(0, firstSeparatorIndex);
		String collectionCycle = normalizedPendingKey.substring(firstSeparatorIndex + 1, secondLastSeparatorIndex);
		String flatArea = normalizedPendingKey.substring(secondLastSeparatorIndex + 1, lastSeparatorIndex);
		String dueDateValue = normalizedPendingKey.substring(lastSeparatorIndex + 1);
		if (!hasText(dueId) || !hasText(collectionCycle) || !hasText(flatArea) || !hasText(dueDateValue)) {
			return null;
		}
		try {
			return new PendingDueKey(normalizedPendingKey, dueId.trim(), collectionCycle.trim(), flatArea.trim(),
					LocalDate.parse(dueDateValue.trim()));
		} catch (DateTimeParseException ex) {
			return null;
		}
	}

	private String buildPendingDueGroupKey(PendingDueKey pendingDueKey) {
		if (pendingDueKey == null) {
			return "";
		}
		return normalizeHierarchyKey(pendingDueKey.dueId()).toUpperCase(Locale.ENGLISH) + "|"
				+ normalizeHierarchyKey(pendingDueKey.collectionCycle()).toUpperCase(Locale.ENGLISH) + "|"
				+ normalizeHierarchyKey(pendingDueKey.flatArea()).toUpperCase(Locale.ENGLISH);
	}

	private String buildPendingDueEntityKey(PendingDueKey pendingDueKey) {
		if (pendingDueKey == null || pendingDueKey.dueDate() == null) {
			return "";
		}
		return normalizeHierarchyKey(pendingDueKey.dueId()).toUpperCase(Locale.ENGLISH) + "|"
				+ normalizeHierarchyKey(pendingDueKey.collectionCycle()).toUpperCase(Locale.ENGLISH) + "|"
				+ normalizeHierarchyKey(pendingDueKey.flatArea()).toUpperCase(Locale.ENGLISH) + "|"
				+ pendingDueKey.dueDate();
	}

	private String buildPendingDueEntityKey(DueAmountDetailsEntity dueEntity) {
		if (dueEntity == null || dueEntity.getDueDate() == null) {
			return "";
		}
		return normalizeHierarchyKey(dueEntity.getDueId()).toUpperCase(Locale.ENGLISH) + "|"
				+ normalizeHierarchyKey(dueEntity.getCollectionCycle()).toUpperCase(Locale.ENGLISH) + "|"
				+ normalizeHierarchyKey(dueEntity.getFlatArea()).toUpperCase(Locale.ENGLISH) + "|"
				+ dueEntity.getDueDate();
	}

	private void initializeDefaultDueResponse(GetDueAmountForFlatResponse response) {
		response.setDueDetails(new LinkedHashMap<>());
		response.setTotalDue(BigDecimal.ZERO.toPlainString());
		response.setTotalMandatoryPayment(BigDecimal.ZERO.toPlainString());
		response.setTotalOptionalPayment(BigDecimal.ZERO.toPlainString());
		response.setPenaltyAdded(Boolean.FALSE);
	}

	private record PendingDueKey(String originalKey, String dueId, String collectionCycle, String flatArea,
			LocalDate dueDate) {
	}

	private boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	private String blankToNull(String value) {
		return hasText(value) ? value : null;
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
				throw new IllegalArgumentException("Missing required value for " + getUploadHeaderName(cellIndex));
			}
			return "";
		}
		String value = dataFormatter.formatCellValue(cell);
		if (required && (value == null || value.isBlank())) {
			throw new IllegalArgumentException("Missing required value for " + getUploadHeaderName(cellIndex));
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
		throw new IllegalArgumentException("Invalid date format for " + getUploadHeaderName(cellIndex));
	}

	private String getUploadHeaderName(int cellIndex) {
		if (cellIndex >= 0 && cellIndex < UPLOAD_HEADERS.length) {
			return UPLOAD_HEADERS[cellIndex];
		}
		return "column " + (cellIndex + 1);
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
