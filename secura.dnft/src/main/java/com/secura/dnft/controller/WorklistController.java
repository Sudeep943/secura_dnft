package com.secura.dnft.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.request.response.ActionTransactionReviewWorkListRequest;
import com.secura.dnft.request.response.GenericResponse;
import com.secura.dnft.request.response.GetTransactionRequest;
import com.secura.dnft.request.response.GetTransactionResponse;
import com.secura.dnft.request.response.GetWorkListsRequest;
import com.secura.dnft.request.response.GetWorkListsResponse;
import com.secura.dnft.request.response.RejectTransactionWorkListRequest;
import com.secura.dnft.request.response.TransactionResponseItem;
import com.secura.dnft.service.TransactionAndReportsService;
import com.secura.dnft.service.WorklistService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/worklist")
public class WorklistController {

	@Autowired
	private WorklistService worklistService;
	
	@Autowired
	TransactionAndReportsService transactionAndReportsService;

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
	public GenericResponse actionTransactionReviewWorkList(@RequestBody ActionTransactionReviewWorkListRequest request) {
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
	
	@PostMapping("/rejectTransactionWorkList")
	@CrossOrigin(origins = "*")
	public GenericResponse rejectTransctionWorkList(@RequestBody RejectTransactionWorkListRequest request) {
		GenericResponse response = new GenericResponse();
		try {
			GetTransactionRequest getTransactionRequest = new GetTransactionRequest();
			getTransactionRequest.setGenericHeader(request.getGenericHeader());
			getTransactionRequest.setTransactionId(request.getTransactionId());
			GetTransactionResponse getTransactionResponse=transactionAndReportsService.getTransaction(getTransactionRequest);
			Optional<TransactionResponseItem> transactionResponseItem=getTransactionResponse.getTransactionList().stream().filter(trn->trn.getTrnscId().equals(request.getTransactionId())).findFirst();
			if(transactionResponseItem.isPresent()) {
				if(null!=transactionResponseItem.get().getWorkListId() && !transactionResponseItem.get().getWorkListId().isEmpty()) {
					 ActionTransactionReviewWorkListRequest actionWorkListrequest= new ActionTransactionReviewWorkListRequest();
					 actionWorkListrequest.setGenericHeader(request.getGenericHeader());
					 actionWorkListrequest.setWorklistId(transactionResponseItem.get().getWorkListId());
					 actionWorkListrequest.setAction(SecuraConstants.ACTION_REJECT);
					 return worklistService.actionTransactionReviewWorkList(actionWorkListrequest);
				}
			}
			//return worklistService.actionTransactionReviewWorkList(request);
		} catch (Exception e) {
			e.printStackTrace();
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}
}
