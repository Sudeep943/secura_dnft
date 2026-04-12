package com.secura.dnft.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.secura.dnft.dao.DiscFinRepository;
import com.secura.dnft.entity.DiscFin;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.interfaceservice.DiscFinInterface;
import com.secura.dnft.request.response.AddDiscfinRequest;
import com.secura.dnft.request.response.AddDiscfinResponse;
import com.secura.dnft.request.response.GetDiscfinRequest;
import com.secura.dnft.request.response.GetDiscfinResponse;

@Service
public class DiscFinServices implements DiscFinInterface {

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
		entity.setCreatUsrId(request.getGenericHeader() != null ? request.getGenericHeader().getUserId() : null);

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

	private String createDiscFnId(String discFnType) {
		String type = (discFnType == null || discFnType.isBlank()) ? "GEN" : discFnType.trim().toUpperCase();
		return ("DFN" + type + ThreadLocalRandom.current().nextInt(1000, 10000));
	}
}
