package com.secura.dnft.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.secura.dnft.dao.SocirtyCollectionTypesRepository;
import com.secura.dnft.entity.SocirtyCollectionTypes;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.GetSocirtyCollectionTypesResponse;

@Service
public class SocirtyCollectionTypesServices {

	@Autowired
	private SocirtyCollectionTypesRepository socirtyCollectionTypesRepository;

	public GetSocirtyCollectionTypesResponse getAllSocirtyCollectionTypes() {
		GetSocirtyCollectionTypesResponse response = new GetSocirtyCollectionTypesResponse();
		List<SocirtyCollectionTypes> collectionTypes = socirtyCollectionTypesRepository.findAll();
		response.setSocirtyCollectionTypes(collectionTypes);
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
