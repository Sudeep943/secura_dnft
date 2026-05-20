package com.secura.dnft.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.request.response.ActionTransactionReviewWorkListRequest;
import com.secura.dnft.request.response.GenericResponse;
import com.secura.dnft.request.response.GetWorkListsRequest;
import com.secura.dnft.request.response.GetWorkListsResponse;
import com.secura.dnft.service.WorklistService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/worklist")
public class WorklistController {

	@Autowired
	private WorklistService worklistService;

	@PostMapping("/getWorkLists")
	@CrossOrigin(origins = "*")
	public GetWorkListsResponse getWorkLists(@RequestBody GetWorkListsRequest request) {
		GetWorkListsResponse response = new GetWorkListsResponse();
		try {
			return worklistService.getWorkLists(request);
		} catch (Exception e) {
			e.printStackTrace();
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}

	@PostMapping("/actionTransctionReviewWorkList")
	@CrossOrigin(origins = "*")
	public GenericResponse actionTransctionReviewWorkList(@RequestBody ActionTransactionReviewWorkListRequest request) {
		GenericResponse response = new GenericResponse();
		try {
			return worklistService.actionTransactionReviewWorkList(request);
		} catch (Exception e) {
			e.printStackTrace();
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}
}
