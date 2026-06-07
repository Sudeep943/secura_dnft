package com.secura.dnft.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.dao.DiscFinRepository;
import com.secura.dnft.dao.PaymentRepository;
import com.secura.dnft.entity.DiscFin;
import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.interfaceservice.DiscFinInterface;
import com.secura.dnft.request.response.AddDiscfinRequest;
import com.secura.dnft.request.response.AddDiscfinResponse;
import com.secura.dnft.request.response.DeTagDiscFinFromPaymentResponse;
import com.secura.dnft.request.response.DeleteDiscfinRequest;
import com.secura.dnft.request.response.DeleteDiscfinResponse;
import com.secura.dnft.request.response.DetagDiscFinFromPaymentRequest;
import com.secura.dnft.request.response.DiscFinCycleDiscount;
import com.secura.dnft.request.response.DiscfinRequestData;
import com.secura.dnft.request.response.GetDiscfinRequest;
import com.secura.dnft.request.response.GetDiscfinResponse;
import com.secura.dnft.request.response.TagDiscFinFromPaymentRequest;
import com.secura.dnft.request.response.TagDiscFinFromPaymentResponse;
import com.secura.dnft.request.response.UpdateDiscfinRequest;
import com.secura.dnft.request.response.UpdateDiscfinResponse;

@Service
public class DiscFinServices implements DiscFinInterface {
	private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DiscFinServices.class);
	private static final int MAX_ID_GENERATION_ATTEMPTS = 1000;
	private static final int ID_RANDOM_MIN = 1000;
	private static final int ID_RANDOM_MAX_EXCLUSIVE = 10000;

	@Autowired
	private DiscFinRepository discFinRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private GenericService genericService;

	@Override
	public AddDiscfinResponse addDiscfin(AddDiscfinRequest request) throws Exception {
		AddDiscfinResponse response = new AddDiscfinResponse();
		response.setGenericHeader(request.getGenericHeader());

		String discFnId = createDiscFnId(request.getDiscFnType());
		String apartmentId = request.getGenericHeader() != null ? request.getGenericHeader().getApartmentId() : null;
		String userId = request.getGenericHeader() != null ? request.getGenericHeader().getUserId() : null;

		List<DiscFinCycleDiscount> cycleDiscountList = request.getDiscFinCycleDiscountList();
		if (cycleDiscountList != null && !cycleDiscountList.isEmpty()) {
			for (DiscFinCycleDiscount cycleDiscount : cycleDiscountList) {
				DiscFin entity = buildBaseEntity(request, discFnId, apartmentId, userId);
				entity.setDiscFnCycleType(cycleDiscount.getCycle());
				entity.setDiscFnMode(cycleDiscount.getType());
				entity.setDiscFinValue(cycleDiscount.getValue());
				discFinRepository.save(entity);
			}
		} else {
			DiscFin entity = buildBaseEntity(request, discFnId, apartmentId, userId);
			entity.setDiscFnCycleType(SecuraConstants.DISC_FN_CYCLE_FIXED);
			discFinRepository.save(entity);
		}

		response.setDiscFnId(discFnId);
		response.setMessage(SuccessMessage.SUCC_MESSAGE_29);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_29);
		return response;
	}

	private DiscFin buildBaseEntity(AddDiscfinRequest request, String discFnId, String apartmentId, String userId)
			throws Exception {
		DiscFin entity = new DiscFin();
		entity.setDiscFnId(discFnId);
		entity.setAprmtId(apartmentId);
		entity.setDiscFnType(request.getDiscFnType());
		entity.setDueDateAsStartDateFlag(request.getDueDateAsStartDateFlag());
		if (request.getDiscFnStrtDt() != null) {
			entity.setDiscFnStrtDt(request.getDiscFnStrtDt().toLocalDate());
		}
		if (request.getDiscFnEndDt() != null) {
			entity.setDiscFnEndDt(request.getDiscFnEndDt().toLocalDate());
		}
		entity.setDiscFnMode(request.getDiscFnMode());
		entity.setDiscFnCumlatonCycle(request.getDiscFnCumlatonCycle());
		// discFnCycleType from the request carries the fine calculation type (CUMULATIVE/SIMPLE)
		entity.setFnCalculationType(request.getDiscFnCycleType());
		entity.setCreatUsrId(userId);
		entity.setDiscFinValue(request.getDiscFnValue());
		entity.setMinimumPaymentAmount(request.getMinimumPaymentAmount());
		return entity;
	}

	@Override
	public GetDiscfinResponse getDiscfin(GetDiscfinRequest request) throws Exception {
		GetDiscfinResponse response = new GetDiscfinResponse();
		response.setGenericHeader(request.getGenericHeader());
		List<DiscFin> discFinList = new ArrayList<>();
		String apartmentId = request.getGenericHeader() != null ? request.getGenericHeader().getApartmentId() : null;

		if (request.getDiscFnId() != null && !request.getDiscFnId().isBlank()) {
			List<DiscFin> found = discFinRepository.findByDiscFnId(request.getDiscFnId());
			for (DiscFin discFin : found) {
				if (apartmentId == null || apartmentId.isBlank() || apartmentId.equals(discFin.getAprmtId())) {
					discFinList.add(discFin);
				}
			}
		} else if (apartmentId != null && !apartmentId.isBlank()) {
			discFinList = discFinRepository.findByAprmtId(apartmentId);
		} else {
			discFinList = discFinRepository.findAll();
		}

		Map<String, List<DiscFin>> discFinMap = new LinkedHashMap<>();
		for (DiscFin discFin : discFinList) {
			String discFnId = discFin.getDiscFnId();
			if (discFnId == null || discFnId.isBlank()) {
				discFnId = "UNKNOWN";
			}
			discFinMap.computeIfAbsent(discFnId, key -> new ArrayList<>()).add(discFin);
		}
		response.setDiscFinList(discFinMap);
		if (discFinMap.isEmpty()) {
			response.setMessage(SuccessMessage.SUCC_MESSAGE_31);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_31);
		} else {
			response.setMessage(SuccessMessage.SUCC_MESSAGE_30);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_30);
		}
		return response;
	}

	@Override
	public DeleteDiscfinResponse deleteDiscfin(DeleteDiscfinRequest request) throws Exception {
		DeleteDiscfinResponse response = new DeleteDiscfinResponse();
		response.setGenericHeader(request.getGenericHeader());
		String apartmentId = request.getGenericHeader() != null ? request.getGenericHeader().getApartmentId() : null;
		String discFnId = request.getDiscFinId();

		if (discFnId == null || discFnId.isBlank()) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_44);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_44);
			return response;
		}
		if (apartmentId == null || apartmentId.isBlank()) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_05);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_05);
			return response;
		}

		List<DiscFin> discFinList = discFinRepository.findByDiscFnId(discFnId);
		if (discFinList.isEmpty()) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_46);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_46);
		} else if (discFinList.stream().anyMatch(d -> !apartmentId.equals(d.getAprmtId()))) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_45);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_45);
		} else {
			discFinRepository.deleteByDiscFnId(discFnId);
			response.setMessage(SuccessMessage.SUCC_MESSAGE_32);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_32);
		}
		return response;
	}

	@Override
	public UpdateDiscfinResponse updateDiscfin(UpdateDiscfinRequest request) throws Exception {
		UpdateDiscfinResponse response = new UpdateDiscfinResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		String apartmentId = request != null && request.getGenericHeader() != null
				? request.getGenericHeader().getApartmentId()
				: null;
		String discFinId = request != null ? request.getDiscFinId() : null;

		if (discFinId == null || discFinId.isBlank()) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_44);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_44);
			return response;
		}
		if (apartmentId == null || apartmentId.isBlank()) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_05);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_05);
			return response;
		}
		if (request.getDiscfinEntity() == null) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
			return response;
		}

		List<DiscFin> discFinList = discFinRepository.findByDiscFnId(discFinId);
		if (discFinList.isEmpty()) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_46);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_46);
			return response;
		}

		if (discFinList.stream().anyMatch(d -> !apartmentId.equals(d.getAprmtId()))) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_48);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_48);
			return response;
		}

		String lstUpdtUsrId = request.getGenericHeader() != null ? request.getGenericHeader().getUserId() : null;
		for (DiscFin discFin : discFinList) {
			applyDiscfinUpdates(discFin, request.getDiscfinEntity());
			discFin.setLstUpdtUsrId(lstUpdtUsrId);
			discFinRepository.save(discFin);
		}

		response.setMessage(SuccessMessage.SUCC_MESSAGE_39);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_39);
		return response;
	}

	private void applyDiscfinUpdates(DiscFin target, DiscFin source) {
		target.setDiscFnType(source.getDiscFnType());
		target.setDueDateAsStartDateFlag(source.getDueDateAsStartDateFlag());
		target.setDiscFnStrtDt(source.getDiscFnStrtDt());
		target.setDiscFnEndDt(source.getDiscFnEndDt());
		target.setDiscFnMode(source.getDiscFnMode());
		target.setDiscFnCumlatonCycle(source.getDiscFnCumlatonCycle());
		target.setDiscFinValue(source.getDiscFinValue());
		target.setMinimumPaymentAmount(source.getMinimumPaymentAmount());
	}

	@Override
	public TagDiscFinFromPaymentResponse tagDiscFinFromPayment(TagDiscFinFromPaymentRequest request) throws Exception {
		TagDiscFinFromPaymentResponse response = new TagDiscFinFromPaymentResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);

		AddDiscfinRequest addRequest = buildAddDiscfinRequest(
				request != null ? request.getGenericHeader() : null,
				request != null ? request.getDiscfinRequestData() : null);

		AddDiscfinResponse addResponse = addDiscfin(addRequest);
		response.setMessage(addResponse.getMessage());
		response.setMessageCode(addResponse.getMessageCode());
		response.setDiscFinId(addResponse.getDiscFnId());

		if (SuccessMessageCode.SUCC_MESSAGE_29.equals(addResponse.getMessageCode())) {
			String paymentId = request != null ? request.getPaymentId() : null;
			if (paymentId != null && !paymentId.isBlank()) {
				List<PaymentEntity> paymentEntityList = paymentRepository.findByPaymentIdAndAprmtId(paymentId,request.getGenericHeader().getApartmentId());
				if (paymentEntityList != null && !paymentEntityList.isEmpty()) {
					PaymentEntity paymentEntity=paymentEntityList.get(0);
					String discFinJson = paymentEntity.getDiscFin();
					List<Map<String, Object>> entries =getUpdatedDisFinjson(discFinJson,addRequest.getDiscFnType(),addResponse.getDiscFnId());
					if(null!=entries && !entries.isEmpty()) {
						String discFinUpdatedJson=genericService.toJson(entries);
						paymentEntity.setDiscFin(discFinUpdatedJson);
						paymentRepository.save(paymentEntity);
					}
				}
			}
		}

		return response;
	}

	private AddDiscfinRequest buildAddDiscfinRequest(
			com.secura.dnft.request.response.GenericHeader genericHeader, DiscfinRequestData data) {
		AddDiscfinRequest addRequest = new AddDiscfinRequest();
		addRequest.setGenericHeader(genericHeader);
		if (data != null) {
			addRequest.setDiscFnType(data.getDiscFnType());
			addRequest.setDueDateAsStartDateFlag(data.getDueDateAsStartDateFlag());
			addRequest.setDiscFnStrtDt(data.getDiscFnStrtDt());
			addRequest.setDiscFnEndDt(data.getDiscFnEndDt());
			addRequest.setDiscFnMode(data.getDiscFnMode());
			addRequest.setDiscFnCumlatonCycle(data.getDiscFnCumlatonCycle());
			addRequest.setDiscFnCycleType(data.getDiscFnCycleType());
			addRequest.setDiscFnValue(data.getDiscFnValue());
			addRequest.setDiscFinCycleDiscountList(data.getDiscFinCycleDiscountList());
			addRequest.setMinimumPaymentAmount(data.getMinimumPaymentAmount());
		}
		return addRequest;
	}

	private List<Map<String, Object>>  getUpdatedDisFinjson(String discFinJson, String discFinType, String discFinCode) {
		//List<String> codes = new ArrayList<>();
		if (discFinJson == null || discFinJson.isBlank()) {
			return null;
		}
		try {
			List<Map<String, Object>> entries = genericService.fromJson(discFinJson,
					new TypeReference<List<Map<String, Object>>>() {
					});
			if (entries != null) {
				for (Map<String, Object> entry : entries) {
					if (entry == null) {
						continue;
					}
					String type = stringValue(entry.get("DISTFIN_TYPE"));
					if(discFinType.equalsIgnoreCase(SecuraConstants.DISC_FN_TYPE_DISCOUNT)) {
						if(type.equalsIgnoreCase(SecuraConstants.DISC_FN_TYPE_DISCOUNT)) {
							entry.put("Status", SecuraConstants.DISC_FIN_STATUS_INACTIVE);
						}
					}
					if(discFinType.equalsIgnoreCase(SecuraConstants.DISC_FN_TYPE_FINE)) {
						if(type.equalsIgnoreCase(SecuraConstants.DISC_FN_TYPE_FINE)) {
							entry.put("Status", SecuraConstants.DISC_FIN_STATUS_INACTIVE);
						}
					}
					
				}
				
				Map<String, Object> newDiscFinEntry= new HashMap();
				if(discFinType.equalsIgnoreCase(SecuraConstants.DISC_FN_TYPE_DISCOUNT)) {
				newDiscFinEntry.put("DISTFIN_TYPE", SecuraConstants.DISC_FN_TYPE_DISCOUNT);
				}
				if(discFinType.equalsIgnoreCase(SecuraConstants.DISC_FN_TYPE_FINE)) {
					newDiscFinEntry.put("DISTFIN_TYPE", SecuraConstants.DISC_FN_TYPE_FINE);
				}
				newDiscFinEntry.put("code", discFinCode);
				newDiscFinEntry.put("Status", SecuraConstants.DISC_FIN_STATUS_ACTIVE);
				
				entries.add(newDiscFinEntry);
				return entries;
				
			}
		} catch (Exception ex) {
			LOGGER.warn("Failed to parse discFin JSON while extracting codes: {}", ex.getMessage());
		}
		return null;
	}
	
	private String stringValue(Object value) {
		return value == null ? null : value.toString();
	}

	private String createDiscFnId(String discFnType) {
		String type = (discFnType == null || discFnType.isBlank()) ? "GEN" : discFnType.trim().toUpperCase();
		for (int attempt = 0; attempt < MAX_ID_GENERATION_ATTEMPTS; attempt++) {
			String discFnId = "DFN" + type + ThreadLocalRandom.current().nextInt(ID_RANDOM_MIN, ID_RANDOM_MAX_EXCLUSIVE);
			if (!discFinRepository.existsByDiscFnId(discFnId)) {
				return discFnId;
			}
		}
		throw new IllegalStateException("Unable to generate unique discFnId");
	}

	@Override
	public DeTagDiscFinFromPaymentResponse deTagDiscFinFromPayment(DetagDiscFinFromPaymentRequest request)
			throws Exception {
		DeTagDiscFinFromPaymentResponse deTagDiscFinFromPaymentResponse = new DeTagDiscFinFromPaymentResponse();
		List<PaymentEntity> paymentEntityList = paymentRepository.findByPaymentIdAndAprmtId(request.getPaymentId(),request.getGenericHeader().getApartmentId());
		PaymentEntity paymentEntity= paymentEntityList.get(0);
		
	    String discFinJson= paymentEntity.getDiscFin();
	    List<Map<String, Object>> entries = genericService.fromJson(discFinJson,
				new TypeReference<List<Map<String, Object>>>() {
				});
	    if (entries != null) {
			for (Map<String, Object> entry : entries) {
				if(request.getDiscFinType().equals(SecuraConstants.DISC_FN_TYPE_DISCOUNT)) {
					if(stringValue(entry.get("DISTFIN_TYPE")).equalsIgnoreCase(SecuraConstants.DISC_FN_TYPE_DISCOUNT)) {
						entry.put("Status",  SecuraConstants.DISC_FIN_STATUS_INACTIVE);
					}
				}
				
				if(request.getDiscFinType().equals(SecuraConstants.DISC_FN_TYPE_FINE)) {
					if(stringValue(entry.get("DISTFIN_TYPE")).equalsIgnoreCase(SecuraConstants.DISC_FN_TYPE_FINE)) {
						entry.put("Status",  SecuraConstants.DISC_FIN_STATUS_INACTIVE);
					}
				}
				
				}
			}
	    
	    discFinJson=genericService.toJson(entries);
	    paymentEntity.setDiscFin(discFinJson);
	    paymentRepository.save(paymentEntity);
	    deTagDiscFinFromPaymentResponse.setGenericHeader(request.getGenericHeader());
	    deTagDiscFinFromPaymentResponse.setMessage(SuccessMessageCode.SUCC_MESSAGE_50);
	    deTagDiscFinFromPaymentResponse.setMessageCode(SuccessMessage.SUCC_MESSAGE_50);
		return deTagDiscFinFromPaymentResponse;
	}
}
