package com.secura.dnft.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.bean.BankAccountDetails;
import com.secura.dnft.bean.ExecutiveMember;
import com.secura.dnft.dao.ApartmentRepository;
import com.secura.dnft.dao.BankEntityRepository;
import com.secura.dnft.entity.ApartmentMaster;
import com.secura.dnft.entity.BankEntity;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.AddBankDetailsRequest;
import com.secura.dnft.request.response.AddBankDetailsResponse;
import com.secura.dnft.request.response.GetBankDetailsRequest;
import com.secura.dnft.request.response.GetBankDetailsResponse;
import com.secura.dnft.request.response.GetApartmentDetailsRequest;
import com.secura.dnft.request.response.GetApartmentDetailsResponse;
import com.secura.dnft.request.response.UpdateApartmentDetailsRequest;
import com.secura.dnft.request.response.UpdateApartmentDetailsResponse;
import com.secura.dnft.request.response.UpdateBankDetailsRequest;
import com.secura.dnft.request.response.UpdateBankDetailsResponse;
import com.secura.dnft.security.BusinessException;
import com.secura.dnft.validation.CommonValidations;

@Service
public class ApartmentService {

	    private static final char BANK_NAME_PADDING_CHAR = 'X';
	    private static final char ACCOUNT_NUMBER_PADDING_CHAR = '0';

	
	
	 @Autowired
	    private ApartmentRepository repository;

	 @Autowired
	 private BankEntityRepository bankEntityRepository;

	 @Autowired
	 private GenericService genericService;

	 @Autowired
	 private CommonValidations commonValidations;

	    public List<ApartmentMaster> getAllApartments() {
	        return repository.findAll();
	    }

	    public UpdateApartmentDetailsResponse updateApartmentDetails(UpdateApartmentDetailsRequest request) {
	    	UpdateApartmentDetailsResponse response = new UpdateApartmentDetailsResponse();
	    	response.setGenericHeader(request != null ? request.getGenericHeader() : null);
	    	try {
	    		commonValidations.genericHeaderValidation(request.getGenericHeader());
	    		ApartmentMaster apartment = getOrCreateApartment(request.getGenericHeader().getApartmentId(),
	    				request.getGenericHeader().getUserId());
	    		if (request.getGenericHeader().getApartmentName() != null) {
	    			apartment.setAprmntName(request.getGenericHeader().getApartmentName());
	    		}
	    		apartment.setAprmnt_logo(request.getApartmentLogo());
	    		apartment.setAprmntAddress(genericService.toJson(request.getAddress()));
	    		List<String> bankDetailsIds = saveBankAccounts(apartment.getAprmntId(),
	    				defaultIfNull(request.getBankAccountDetails()));
	    		apartment.setAprmnt_bank_acccount_list(genericService.encrypt(genericService.toJson(bankDetailsIds)));
	    		apartment.setAprmnt_executive_role_list(genericService.toJson(defaultIfNull(request.getExecutiveMemberList())));
	    		apartment.setAprmntLetterHead(request.getApartmentLetterHead());
	    		apartment.setLst_updt_ts(LocalDateTime.now());
	    		apartment.setLst_updt_usrid(request.getGenericHeader().getUserId());
	    		repository.save(apartment);
	    		response.setMessage(SuccessMessage.SUCC_MESSAGE_35);
	    		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_35);
	    	} catch (BusinessException e) {
	    		response.setMessage(e.getErrorMessage());
	    		response.setMessageCode(e.getErrorMessageCode());
	    	}
	    	return response;
	    }

	    public AddBankDetailsResponse addBankDetails(AddBankDetailsRequest request) {
	    	AddBankDetailsResponse response = new AddBankDetailsResponse();
	    	response.setGenericHeader(request != null ? request.getGenericHeader() : null);
	    	try {
	    		commonValidations.genericHeaderValidation(request.getGenericHeader());
	    		ApartmentMaster apartment = getOrCreateApartment(request.getGenericHeader().getApartmentId(),
	    				request.getGenericHeader().getUserId());
	    		String bankDetailsID = upsertBankDetailsForApartment(apartment, request.getGenericHeader().getUserId(),
	    				request.getBankAccountDetails());
	    		response.setBankDetailsID(bankDetailsID);
	    		response.setMessage(SuccessMessage.SUCC_MESSAGE_35);
	    		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_35);
	    	} catch (BusinessException e) {
	    		response.setMessage(e.getErrorMessage());
	    		response.setMessageCode(e.getErrorMessageCode());
	    	}
	    	return response;
	    }

	    public UpdateBankDetailsResponse updateBankDetails(UpdateBankDetailsRequest request) {
	    	UpdateBankDetailsResponse response = new UpdateBankDetailsResponse();
	    	response.setGenericHeader(request != null ? request.getGenericHeader() : null);
	    	try {
	    		commonValidations.genericHeaderValidation(request.getGenericHeader());
	    		Optional<ApartmentMaster> apartmentOptional = repository.findById(request.getGenericHeader().getApartmentId());
	    		if (apartmentOptional.isEmpty()) {
	    			response.setMessage(ErrorMessage.ERR_MESSAGE_47);
	    			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_47);
	    			return response;
	    		}
	    		ApartmentMaster apartment = apartmentOptional.get();
	    		String bankDetailsID = upsertBankDetailsForApartment(apartment, request.getGenericHeader().getUserId(),
	    				request.getBankAccountDetails());
	    		response.setBankDetailsID(bankDetailsID);
	    		response.setMessage(SuccessMessage.SUCC_MESSAGE_35);
	    		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_35);
	    	} catch (BusinessException e) {
	    		response.setMessage(e.getErrorMessage());
	    		response.setMessageCode(e.getErrorMessageCode());
	    	}
	    	return response;
	    }

	    public GetBankDetailsResponse getBankDetails(GetBankDetailsRequest request) {
	    	GetBankDetailsResponse response = new GetBankDetailsResponse();
	    	response.setGenericHeader(request != null ? request.getGenericHeader() : null);
	    	response.setBankAccountDetails(new ArrayList<>());
	    	try {
	    		commonValidations.genericHeaderValidation(request.getGenericHeader());
	    		Optional<ApartmentMaster> apartmentOptional = repository.findById(request.getGenericHeader().getApartmentId());
	    		if (apartmentOptional.isEmpty()) {
	    			response.setMessage(ErrorMessage.ERR_MESSAGE_47);
	    			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_47);
	    			return response;
	    		}
	    		ApartmentMaster apartment = apartmentOptional.get();
	    		if (request.getBankDetailsID() != null && !request.getBankDetailsID().isBlank()) {
	    			Optional<BankEntity> bankEntityOptional = bankEntityRepository.findByAprmntIdAndBankDetailsID(
	    					apartment.getAprmntId(), request.getBankDetailsID());
	    			bankEntityOptional.ifPresent(bankEntity -> response.getBankAccountDetails().add(mapToBankAccountDetails(bankEntity)));
	    		} else {
	    			response.setBankAccountDetails(readBankAccounts(apartment.getAprmntId(), apartment.getAprmnt_bank_acccount_list()));
	    		}
	    		response.setMessage(SuccessMessage.SUCC_MESSAGE_36);
	    		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_36);
	    	} catch (BusinessException e) {
	    		response.setMessage(e.getErrorMessage());
	    		response.setMessageCode(e.getErrorMessageCode());
	    	}
	    	return response;
	    }

	    public GetApartmentDetailsResponse getApartmentDetails(GetApartmentDetailsRequest request) {
	    	GetApartmentDetailsResponse response = new GetApartmentDetailsResponse();
	    	response.setGenericHeader(request != null ? request.getGenericHeader() : null);
	    	try {
	    		commonValidations.genericHeaderValidation(request.getGenericHeader());
	    		Optional<ApartmentMaster> apartmentOptional = repository.findById(request.getGenericHeader().getApartmentId());
	    		if (apartmentOptional.isEmpty()) {
	    			response.setMessage(ErrorMessage.ERR_MESSAGE_47);
	    			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_47);
	    			return response;
	    		}
	    		ApartmentMaster apartment = apartmentOptional.get();
	    		response.setApartmentName(apartment.getAprmntName());
	    		response.setApartmentLogo(apartment.getAprmnt_logo());
	    		response.setAddress(genericService.fromJson(apartment.getAprmntAddress(),
	    				com.secura.dnft.generic.bean.Address.class));
	    		response.setExecutiveMemberList(readExecutiveMembers(apartment.getAprmnt_executive_role_list()));
	    		response.setBankAccountDetails(readBankAccounts(apartment.getAprmntId(), apartment.getAprmnt_bank_acccount_list()));
	    		response.setApartmentLetterHead(apartment.getAprmntLetterHead());
	    		response.setMessage(SuccessMessage.SUCC_MESSAGE_36);
	    		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_36);
	    	} catch (BusinessException e) {
	    		response.setMessage(e.getErrorMessage());
	    		response.setMessageCode(e.getErrorMessageCode());
	    	}
	    	return response;
	    }

	    private ApartmentMaster getOrCreateApartment(String apartmentId, String userId) {
	    	ApartmentMaster apartment = repository.findById(apartmentId).orElseGet(ApartmentMaster::new);
	    	if (apartment.getAprmntId() == null) {
	    		apartment.setAprmntId(apartmentId);
	    		apartment.setCreat_ts(LocalDateTime.now());
	    		apartment.setCreat_usr_id(userId);
	    	}
	    	return apartment;
	    }

	    private List<String> saveBankAccounts(String apartmentId, List<BankAccountDetails> bankAccountDetailsList) {
	    	List<String> bankDetailIds = new ArrayList<>();
	    	for (BankAccountDetails bankAccountDetails : bankAccountDetailsList) {
	    		if (bankAccountDetails == null) {
	    			continue;
	    		}
	    		String bankDetailsID = ensureBankDetailsId(bankAccountDetails.getBankDetailsID(), bankAccountDetails);
	    		bankAccountDetails.setBankDetailsID(bankDetailsID);
	    		saveBankEntity(apartmentId, bankAccountDetails);
	    		bankDetailIds.add(bankDetailsID);
	    	}
	    	return bankDetailIds;
	    }

	    private void saveBankEntity(String apartmentId, BankAccountDetails bankAccountDetails) {
	    	BankEntity bankEntity = bankEntityRepository.findByAprmntIdAndBankDetailsID(apartmentId,
	    			bankAccountDetails.getBankDetailsID()).orElseGet(BankEntity::new);
	    	bankEntity.setAprmntId(apartmentId);
	    	bankEntity.setBankDetailsID(bankAccountDetails.getBankDetailsID());
	    	bankEntity.setBankName(encryptNullable(bankAccountDetails.getBankName()));
	    	bankEntity.setAccountNumber(encryptNullable(bankAccountDetails.getAccountNumber()));
	    	bankEntity.setIfscCode(encryptNullable(bankAccountDetails.getIfscCode()));
	    	bankEntity.setBranch(encryptNullable(bankAccountDetails.getBranch()));
	    	bankEntity.setAccountName(encryptNullable(bankAccountDetails.getAccountName()));
	    	bankEntity.setPgKey(encryptNullable(bankAccountDetails.getPgKey()));
	    	bankEntity.setPgSecret(encryptNullable(bankAccountDetails.getPgSecret()));
	    	bankEntity.setPgName(encryptNullable(bankAccountDetails.getPgName()));
	    	bankEntity.setUpiId(encryptNullable(bankAccountDetails.getUpiId()));
	    	bankEntityRepository.save(bankEntity);
	    }

	    private String upsertBankDetailsForApartment(ApartmentMaster apartment, String userId, BankAccountDetails bankAccountDetails) {
	    	BankAccountDetails details = bankAccountDetails == null ? new BankAccountDetails() : bankAccountDetails;
	    	String bankDetailsID = ensureBankDetailsId(details.getBankDetailsID(), details);
	    	details.setBankDetailsID(bankDetailsID);
	    	saveBankEntity(apartment.getAprmntId(), details);
	    	List<String> existingBankIds = readBankDetailIds(apartment.getAprmnt_bank_acccount_list(), apartment.getAprmntId());
	    	if (!existingBankIds.contains(bankDetailsID)) {
	    		existingBankIds.add(bankDetailsID);
	    	}
	    	apartment.setAprmnt_bank_acccount_list(genericService.encrypt(genericService.toJson(existingBankIds)));
	    	apartment.setLst_updt_ts(LocalDateTime.now());
	    	apartment.setLst_updt_usrid(userId);
	    	repository.save(apartment);
	    	return bankDetailsID;
	    }

	    private List<BankAccountDetails> readBankAccounts(String apartmentId, String value) {
	    	if (value == null || value.isBlank()) {
	    		return readAllBankAccounts(apartmentId);
	    	}
	    	String decryptedValue = genericService.decrypt(value);
	    	try {
	    		List<String> bankDetailIds = genericService.fromJson(decryptedValue, new TypeReference<List<String>>() {
	    		});
	    		return readBankAccountsByIds(apartmentId, defaultIfNull(bankDetailIds));
	    	} catch (RuntimeException exception) {
	    		return genericService.fromJson(decryptedValue, new TypeReference<List<BankAccountDetails>>() {
	    		});
	    	}
	    }

	    private List<String> readBankDetailIds(String value, String apartmentId) {
	    	if (value == null || value.isBlank()) {
	    		return new ArrayList<>();
	    	}
	    	String decryptedValue = genericService.decrypt(value);
	    	try {
	    		List<String> bankDetailIds = genericService.fromJson(decryptedValue, new TypeReference<List<String>>() {
	    		});
	    		return defaultIfNull(bankDetailIds);
	    	} catch (RuntimeException exception) {
	    		List<BankAccountDetails> legacyBankAccounts = genericService.fromJson(decryptedValue,
	    				new TypeReference<List<BankAccountDetails>>() {
	    				});
	    		return saveBankAccounts(apartmentId, defaultIfNull(legacyBankAccounts));
	    	}
	    }

	    private List<BankAccountDetails> readBankAccountsByIds(String apartmentId, List<String> bankDetailIds) {
	    	List<BankAccountDetails> bankAccountDetailsList = new ArrayList<>();
	    	if (bankDetailIds.isEmpty()) {
	    		return readAllBankAccounts(apartmentId);
	    	}
	    	for (String bankDetailsID : bankDetailIds) {
	    		if (bankDetailsID == null || bankDetailsID.isBlank()) {
	    			continue;
	    		}
	    		Optional<BankEntity> bankEntityOptional = bankEntityRepository.findByAprmntIdAndBankDetailsID(apartmentId, bankDetailsID);
	    		bankEntityOptional.ifPresent(bankEntity -> bankAccountDetailsList.add(mapToBankAccountDetails(bankEntity)));
	    	}
	    	return bankAccountDetailsList;
	    }

	    private List<BankAccountDetails> readAllBankAccounts(String apartmentId) {
	    	List<BankAccountDetails> bankAccountDetailsList = new ArrayList<>();
	    	for (BankEntity bankEntity : bankEntityRepository.findByAprmntId(apartmentId)) {
	    		bankAccountDetailsList.add(mapToBankAccountDetails(bankEntity));
	    	}
	    	return bankAccountDetailsList;
	    }

	    private BankAccountDetails mapToBankAccountDetails(BankEntity bankEntity) {
	    	BankAccountDetails bankAccountDetails = new BankAccountDetails();
	    	bankAccountDetails.setBankDetailsID(bankEntity.getBankDetailsID());
	    	bankAccountDetails.setBankName(decryptNullable(bankEntity.getBankName()));
	    	bankAccountDetails.setAccountNumber(decryptNullable(bankEntity.getAccountNumber()));
	    	bankAccountDetails.setIfscCode(decryptNullable(bankEntity.getIfscCode()));
	    	bankAccountDetails.setBranch(decryptNullable(bankEntity.getBranch()));
	    	bankAccountDetails.setAccountName(decryptNullable(bankEntity.getAccountName()));
	    	bankAccountDetails.setPgKey(decryptNullable(bankEntity.getPgKey()));
	    	bankAccountDetails.setPgSecret(decryptNullable(bankEntity.getPgSecret()));
	    	bankAccountDetails.setPgName(decryptNullable(bankEntity.getPgName()));
	    	bankAccountDetails.setUpiId(decryptNullable(bankEntity.getUpiId()));
	    	return bankAccountDetails;
	    }

	    private String encryptNullable(String value) {
	    	if (value == null || value.isBlank()) {
	    		return null;
	    	}
	    	return genericService.encrypt(value);
	    }

	    private String decryptNullable(String value) {
	    	if (value == null || value.isBlank()) {
	    		return null;
	    	}
	    	return genericService.decrypt(value);
	    }

	    private String ensureBankDetailsId(String bankDetailsID, BankAccountDetails bankAccountDetails) {
	    	if (bankDetailsID != null && !bankDetailsID.isBlank()) {
	    		return bankDetailsID;
	    	}
	    	String bankPrefix = firstFourBankName(bankAccountDetails != null ? bankAccountDetails.getBankName() : null);
	    	String randomDigits = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
	    	String accountSuffix = lastFourAccountNumber(bankAccountDetails != null ? bankAccountDetails.getAccountNumber() : null);
	    	return (bankPrefix + randomDigits + accountSuffix).toUpperCase(Locale.ROOT);
	    }

	    private String firstFourBankName(String bankName) {
	    	String cleaned = alphanumeric(bankName);
	    	if (cleaned.length() >= 4) {
	    		return cleaned.substring(0, 4);
	    	}
	    	return cleaned + String.valueOf(BANK_NAME_PADDING_CHAR).repeat(4 - cleaned.length());
	    }

	    private String lastFourAccountNumber(String accountNumber) {
	    	String cleaned = alphanumeric(accountNumber);
	    	if (cleaned.length() >= 4) {
	    		return cleaned.substring(cleaned.length() - 4);
	    	}
	    	return String.valueOf(ACCOUNT_NUMBER_PADDING_CHAR).repeat(4 - cleaned.length()) + cleaned;
	    }

	    private String alphanumeric(String value) {
	    	if (value == null || value.isBlank()) {
	    		return "";
	    	}
	    	return value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
	    }

	    private List<ExecutiveMember> readExecutiveMembers(String value) {
	    	if (value == null || value.isBlank()) {
	    		return new ArrayList<>();
	    	}
	    	return genericService.fromJson(value, new TypeReference<List<ExecutiveMember>>() {
	    	});
	    }

	    private <T> List<T> defaultIfNull(List<T> value) {
	    	return value == null ? new ArrayList<>() : value;
	    }

}
