package com.secura.dnft.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.request.response.ActionTransactionReviewWorkListRequest;
import com.secura.dnft.request.response.GenericResponse;
import com.secura.dnft.request.response.GetWorkListsRequest;
import com.secura.dnft.request.response.GetWorkListsResponse;
import com.secura.dnft.service.WorklistService;

@ExtendWith(MockitoExtension.class)
class WorklistControllerTest {

	@Mock
	private WorklistService worklistService;

	@InjectMocks
	private WorklistController worklistController;

	@Test
	void getWorkLists_shouldReturnServiceResponse() {
		GetWorkListsRequest request = new GetWorkListsRequest();
		GetWorkListsResponse expected = new GetWorkListsResponse();
		expected.setMessage("ok");
		expected.setMessageCode("CODE");
		expected.setWorklists(List.of());
		when(worklistService.getWorkLists(request)).thenReturn(expected);

		GetWorkListsResponse actual = worklistController.getWorkLists(request);

		assertEquals(expected, actual);
	}

	@Test
	void getWorkLists_shouldReturnGenericErrorWhenServiceThrows() {
		GetWorkListsRequest request = new GetWorkListsRequest();
		when(worklistService.getWorkLists(request)).thenThrow(new RuntimeException("boom"));

		GetWorkListsResponse actual = worklistController.getWorkLists(request);

		assertEquals(ErrorMessage.ERR_MESSAGE_33, actual.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_33, actual.getMessageCode());
	}

	@Test
	void actionTransactionReviewWorkList_shouldReturnServiceResponse() {
		ActionTransactionReviewWorkListRequest request = new ActionTransactionReviewWorkListRequest();
		GenericResponse expected = new GenericResponse();
		expected.setMessage("ok");
		expected.setMessageCode("CODE");
		when(worklistService.actionTransactionReviewWorkList(request)).thenReturn(expected);

		GenericResponse actual = worklistController.actionTransactionReviewWorkList(request);

		assertEquals(expected, actual);
	}

	@Test
	void actionTransactionReviewWorkList_shouldReturnGenericErrorWhenServiceThrows() {
		ActionTransactionReviewWorkListRequest request = new ActionTransactionReviewWorkListRequest();
		when(worklistService.actionTransactionReviewWorkList(request)).thenThrow(new RuntimeException("boom"));

		GenericResponse actual = worklistController.actionTransactionReviewWorkList(request);

		assertEquals(ErrorMessage.ERR_MESSAGE_33, actual.getMessage());
		assertEquals(ErrorMessageCode.ERR_MESSAGE_33, actual.getMessageCode());
	}
}
