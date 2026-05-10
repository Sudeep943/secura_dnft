package com.secura.dnft.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.secura.dnft.dao.SocietyCollectionTypesRepository;
import com.secura.dnft.entity.SocietyCollectionTypes;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.GetSocietyCollectionTypesResponse;

@Service
public class SocirtyCollectionTypesServices {

	@Autowired
	private SocietyCollectionTypesRepository socirtyCollectionTypesRepository;

	public GetSocietyCollectionTypesResponse getAllSocietyCollectionTypes() {
		GetSocietyCollectionTypesResponse response = new GetSocietyCollectionTypesResponse();
		List<SocietyCollectionTypes> collectionTypes = socirtyCollectionTypesRepository.findAll();
		response.setSocietyCollectionTypes(collectionTypes);
		if (collectionTypes.isEmpty()) {
			response.setMessage(SuccessMessage.SUCC_MESSAGE_31);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_31);
		} else {
			response.setMessage(SuccessMessage.SUCC_MESSAGE_30);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_30);
		}
		return response;
	}
}
