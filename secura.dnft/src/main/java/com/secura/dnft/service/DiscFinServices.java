package com.secura.dnft.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.secura.dnft.dao.DiscFinRepository;
import com.secura.dnft.entity.DiscFin;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.interfaceservice.DiscFinInterface;
import com.secura.dnft.request.response.AddDiscfinRequest;
import com.secura.dnft.request.response.AddDiscfinResponse;
import com.secura.dnft.request.response.DeleteDiscfinRequest;
import com.secura.dnft.request.response.DeleteDiscfinResponse;
import com.secura.dnft.request.response.GetDiscfinRequest;
import com.secura.dnft.request.response.GetDiscfinResponse;
import com.secura.dnft.request.response.UpdateDiscfinRequest;
import com.secura.dnft.request.response.UpdateDiscfinResponse;

@Service
public class DiscFinServices implements DiscFinInterface {
	private static final int MAX_ID_GENERATION_ATTEMPTS = 1000;
	private static final int ID_RANDOM_MIN = 1000;
	private static final int ID_RANDOM_MAX_EXCLUSIVE = 10000;

	@Autowired
	private DiscFinRepository discFinRepository;

	@Autowired
	private GenericService genericService;

	@Override
	public AddDiscfinResponse addDiscfin(AddDiscfinRequest request) throws Exception {
		AddDiscfinResponse response = new AddDiscfinResponse();
		response.setGenericHeader(request.getGenericHeader());

		DiscFin entity = new DiscFin();
		String discFnId = createDiscFnId(request.getDiscFnType());
		entity.setDiscFnId(discFnId);
		entity.setAprmtId(request.getGenericHeader() != null ? request.getGenericHeader().getApartmentId() : null);
		entity.setDiscFnType(request.getDiscFnType());
		entity.setDueDateAsStartDateFlag(request.getDueDateAsStartDateFlag());
		if (request.getDiscFnStrtDt() != null) {
			entity.setDiscFnStrtDt(genericService.getCorrectLocalDateForInputDate(request.getDiscFnStrtDt()));
		}
		if (request.getDiscFnEndDt() != null) {
			entity.setDiscFnEndDt(genericService.getCorrectLocalDateForInputDate(request.getDiscFnEndDt()));
		}
		entity.setDiscFnMode(request.getDiscFnMode());
		entity.setDiscFnCumlatonCycle(request.getDiscFnCumlatonCycle());
		entity.setDiscFnCycleType(request.getDiscFnCycleType());
		entity.setCreatUsrId(request.getGenericHeader() != null ? request.getGenericHeader().getUserId() : null);
		entity.setDiscFinValue(request.getDiscFnValue());
		discFinRepository.save(entity);

		response.setDiscFnId(discFnId);
		response.setMessage(SuccessMessage.SUCC_MESSAGE_29);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_29);
		return response;
	}

	@Override
	public GetDiscfinResponse getDiscfin(GetDiscfinRequest request) throws Exception {
		GetDiscfinResponse response = new GetDiscfinResponse();
		response.setGenericHeader(request.getGenericHeader());
		List<DiscFin> discFinList = new ArrayList<>();
		String apartmentId = request.getGenericHeader() != null ? request.getGenericHeader().getApartmentId() : null;

		if (request.getDiscFnId() != null && !request.getDiscFnId().isBlank()) {
			Optional<DiscFin> discFin = discFinRepository.findById(request.getDiscFnId());
			if (discFin.isPresent()) {
				if (apartmentId == null || apartmentId.isBlank() || apartmentId.equals(discFin.get().getAprmtId())) {
					discFinList.add(discFin.get());
				}
			}
		} else if (apartmentId != null && !apartmentId.isBlank()) {
			discFinList = discFinRepository.findByAprmtId(apartmentId);
		} else {
			discFinList = discFinRepository.findAll();
		}

		response.setDiscFinList(discFinList);
		if (discFinList.isEmpty()) {
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

		Optional<DiscFin> discFin = discFinRepository.findById(discFnId);
		if (discFin.isEmpty()) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_46);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_46);
		} else if (!apartmentId.equals(discFin.get().getAprmtId())) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_45);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_45);
		} else {
			discFinRepository.deleteById(discFnId);
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

		Optional<DiscFin> discFinOptional = discFinRepository.findById(discFinId);
		if (discFinOptional.isEmpty()) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_46);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_46);
			return response;
		}

		DiscFin existingDiscFin = discFinOptional.get();
		if (!apartmentId.equals(existingDiscFin.getAprmtId())) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_48);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_48);
			return response;
		}

		DiscFin discfinEntity = request.getDiscfinEntity();
		existingDiscFin.setDiscFnType(discfinEntity.getDiscFnType());
		existingDiscFin.setDueDateAsStartDateFlag(discfinEntity.getDueDateAsStartDateFlag());
		existingDiscFin.setDiscFnStrtDt(discfinEntity.getDiscFnStrtDt());
		existingDiscFin.setDiscFnEndDt(discfinEntity.getDiscFnEndDt());
		existingDiscFin.setDiscFnMode(discfinEntity.getDiscFnMode());
		existingDiscFin.setDiscFnCumlatonCycle(discfinEntity.getDiscFnCumlatonCycle());
		existingDiscFin.setDiscFnCycleType(discfinEntity.getDiscFnCycleType());
		existingDiscFin.setDiscFinValue(discfinEntity.getDiscFinValue());
		existingDiscFin.setLstUpdtUsrId(
				request.getGenericHeader() != null ? request.getGenericHeader().getUserId() : null);
		discFinRepository.save(existingDiscFin);

		response.setMessage(SuccessMessage.SUCC_MESSAGE_39);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_39);
		return response;
	}

	private String createDiscFnId(String discFnType) {
		String type = (discFnType == null || discFnType.isBlank()) ? "GEN" : discFnType.trim().toUpperCase();
		for (int attempt = 0; attempt < MAX_ID_GENERATION_ATTEMPTS; attempt++) {
			String discFnId = "DFN" + type + ThreadLocalRandom.current().nextInt(ID_RANDOM_MIN, ID_RANDOM_MAX_EXCLUSIVE);
			if (!discFinRepository.existsById(discFnId)) {
				return discFnId;
			}
		}
		throw new IllegalStateException("Unable to generate unique discFnId");
	}
}
