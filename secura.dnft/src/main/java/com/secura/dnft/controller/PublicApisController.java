package com.secura.dnft.controller;

import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.dao.DueAmountDetailsRepository;
import com.secura.dnft.dao.FlatRepository;
import com.secura.dnft.entity.DueAmountDetailsEntity;
import com.secura.dnft.entity.Profile;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.interfaceservice.ThirdPartyPaymentGayeway;
import com.secura.dnft.request.response.ActionTransactionReviewWorkListRequest;
import com.secura.dnft.request.response.GenericHeader;
import com.secura.dnft.request.response.GenericResponse;
import com.secura.dnft.request.response.GetAllFlatsRequest;
import com.secura.dnft.request.response.GetAllFlatsResponse;
import com.secura.dnft.request.response.GetDueAmountForFlatRequest;
import com.secura.dnft.request.response.GetDueAmountForFlatResponse;
import com.secura.dnft.request.response.GetOwnerRequest;
import com.secura.dnft.request.response.GetOwnerResponse;
import com.secura.dnft.request.response.GetTransactionRequest;
import com.secura.dnft.request.response.GetTransactionResponse;
import com.secura.dnft.request.response.PayDueRequest;
import com.secura.dnft.request.response.PayDueResponse;
import com.secura.dnft.request.response.PaymentGayewayOrderRequest;
import com.secura.dnft.request.response.PaymentGayewayOrderResponse;
import com.secura.dnft.request.response.PaymentGayewayOrderVerificationRequest;
import com.secura.dnft.request.response.PaymentGayewayOrderVerificationResponse;
import com.secura.dnft.request.response.PaymentGayewayPayOrderRequest;
import com.secura.dnft.request.response.PaymentGayewayPayOrderResponse;
import com.secura.dnft.request.response.PaymentGayewayPaymentDetailRequest;
import com.secura.dnft.request.response.PaymentGayewayPaymentDetailResponse;
import com.secura.dnft.request.response.PaymentGayewayProcessRefundRequest;
import com.secura.dnft.request.response.PaymentGayewayProcessRefundResponse;
import com.secura.dnft.request.response.RejectTransactionWorkListRequest;
import com.secura.dnft.request.response.TransactionResponseItem;
import com.secura.dnft.request.response.ValidateOtpRequest;
import com.secura.dnft.request.response.ValidateOtpResponse;
import com.secura.dnft.request.response.ValidatePriorDuePaymnentRequest;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.CreateOtpRequest;
import com.secura.dnft.request.response.CreateOtpResponse;
import com.secura.dnft.security.BusinessException;
import com.secura.dnft.service.AtomsPaymentServices;
import com.secura.dnft.service.DeepLinkServices;
import com.secura.dnft.service.FlatServices;
import com.secura.dnft.service.GenericService;
import com.secura.dnft.service.PaymentServices;
import com.secura.dnft.service.ProfileServices;
import com.secura.dnft.service.RazorPayPaymentServices;
import com.secura.dnft.service.TransactionAndReportsService;
import com.secura.dnft.service.WorklistService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/publicapis")
public class PublicApisController {

	private static final String PUBLIC_USER_ID = "ext";
	private static final String PUBLIC_APARTMENT_ID = "APRT001";

	@Autowired
	private FlatServices flatServices;

	@Autowired
	private FlatRepository flatRepository;

	@Autowired
	private DueAmountDetailsRepository dueAmountDetailsRepository;

	@Autowired
	private PaymentServices paymentServices;

	@Autowired
	private RazorPayPaymentServices razorPayPaymentServices;
	
	@Autowired
	private AtomsPaymentServices atomsPaymentServices;

	@Autowired
	private DeepLinkServices deepLinkServices;

	@Autowired
	private ProfileServices profileServices;
	
	@Autowired
	private GenericService genericService;
	
	@Autowired
	TransactionAndReportsService transactionAndReportsService;
	
	@Autowired
	private WorklistService worklistService;

	@PostMapping("/getFlatsPublic")
	@CrossOrigin(origins = "*")
	public GetAllFlatsResponse getFlatsPublic(@RequestBody GetAllFlatsRequest request) {
		return flatServices.getAllFlats(request);
	}

	@PostMapping("/getDueDetailsForFlatPublic")
	@CrossOrigin(origins = "*")
	public GetDueAmountForFlatResponse getDueDetailsForFlatPublic(@RequestBody GetDueAmountForFlatRequest request) {
		try {
			return flatServices.getDueAmountForFlat(request);
		} catch (Exception e) {
			GetDueAmountForFlatResponse response = new GetDueAmountForFlatResponse();
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
			return response;
		}
	}

	@PostMapping("/payduesPublic")
	@CrossOrigin(origins = "*")
	public PayDueResponse payDuesPublic(@RequestBody PayDueRequest request) {
		PayDueResponse response = new PayDueResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		if (!isPayDuesPublicRequestValid(request)) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_60);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_60);
			return response;
		}
		try {
			return paymentServices.payDues(request);
		} catch (Exception e) {
			e.printStackTrace();
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}

	@PostMapping("/getOwnerPublic")
	@CrossOrigin(origins = "*")
	public GetOwnerResponse getOwnerPublic(@RequestBody GetOwnerRequest request) {
		GetOwnerResponse response = new GetOwnerResponse();
		response.setGenericHeader(request != null ? request.getGenericHeader() : null);
		try {
			GetOwnerResponse getOwnerResponse =profileServices.getOwner(request);
            for(Profile profile: getOwnerResponse.getProfile()) {
            	profile.setPassword(null);
            	profile.setPrflEmailAdrss(genericService.maskEmail(profile.getPrflEmailAdrss()));
            	profile.setPrflPhoneNo(genericService.maskPhoneNumber(profile.getPrflPhoneNo()));
            }
			return getOwnerResponse;
		} catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
			return response;
		}
	}

	@PostMapping("/payGatewayCreateOrder")
	@CrossOrigin(origins = "*")
	public PaymentGayewayOrderResponse createOrderPublic(@RequestBody PaymentGayewayOrderRequest request) {
		try {
			return resolvePaymentGatewayService(request != null ? request.getPaymentGateway() : null).createOrder(request);
		} catch (Exception e) {
			PaymentGayewayOrderResponse response = new PaymentGayewayOrderResponse();
			response.setGenericHeader(request != null ? request.getGenericHeader() : null);
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
			return response;
		}
	}

	@PostMapping("/verifyPaymentPublic")
	@CrossOrigin(origins = "*")
	public PaymentGayewayOrderVerificationResponse verifyPaymentPublic(@RequestBody PaymentGayewayOrderVerificationRequest request) {
		try {
			return resolvePaymentGatewayService(request != null ? request.getPaymentGateway() : null).verifypayment(request);
		} catch (Exception e) {
			PaymentGayewayOrderVerificationResponse response = new PaymentGayewayOrderVerificationResponse();
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
			return response;
		}
	}
	
	@PostMapping("/getPaymentDetailsPublic")
	@CrossOrigin(origins = "*")
	public PaymentGayewayPaymentDetailResponse getPaymentDetailsPublic(@RequestBody PaymentGayewayPaymentDetailRequest request) {
		try {
			return resolvePaymentGatewayService(request != null ? request.getPaymentGateway() : null).getPaymentDetails(request);
		} catch (Exception e) {
			return new PaymentGayewayPaymentDetailResponse();
		}
	}
	
	@PostMapping("/payOrderPublic")
	@CrossOrigin(origins = "*")
	public PaymentGayewayPayOrderResponse payOrderPublic(@RequestBody PaymentGayewayPayOrderRequest request) {
		try {
			return resolvePaymentGatewayService(request != null ? request.getPaymentGateway() : null).payorder(request);
		} catch (Exception e) {
			PaymentGayewayPayOrderResponse response = new PaymentGayewayPayOrderResponse();
			response.setGenericHeader(request != null ? request.getGenericHeader() : null);
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
			return response;
		}
	}
	
	@PostMapping("/processRefundPublic")
	@CrossOrigin(origins = "*")
	public PaymentGayewayProcessRefundResponse processRefundPublic(@RequestBody PaymentGayewayProcessRefundRequest request) {
		try {
			return resolvePaymentGatewayService(request != null ? request.getPaymentGateway() : null).processRefund(request);
		} catch (Exception e) {
			PaymentGayewayProcessRefundResponse response = new PaymentGayewayProcessRefundResponse();
			response.setGenericHeader(request != null ? request.getGenericHeader() : null);
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
			return response;
		}
	}

	@PostMapping("/validatePriorDuePaymnentPublic")
	@CrossOrigin(origins = "*")
	public GenericResponse validatePriorDuePaymnentPublic(@RequestBody ValidatePriorDuePaymnentRequest request) {
		GenericResponse response = new GenericResponse();
		try {
			return paymentServices.validatePriorDuePaymnent(request);
		}
		catch (Exception e) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}
	
	private ThirdPartyPaymentGayeway resolvePaymentGatewayService(String paymentGateway) {
		if ("RAZORPAY".equalsIgnoreCase(paymentGateway)) {
			return razorPayPaymentServices;
		}
		if ("ATOMS".equalsIgnoreCase(paymentGateway)) {
			return atomsPaymentServices;
		}
		if ("DEEPLINK".equalsIgnoreCase(paymentGateway)) {
			return deepLinkServices;
		}
		throw new IllegalArgumentException("Unsupported payment gateway");
	}

	private boolean isPayDuesPublicRequestValid(PayDueRequest request) {
		if (request == null) {
			return false;
		}
		if(request.getFiles().isEmpty()) {
			return false;
		}
		if (!trimValue(request.getDueId()).equals(trimValue(request.getPaidDueDetails().getDueId()))) {
			return false;
		}
		GenericHeader genericHeader = request.getGenericHeader();
		if (genericHeader == null) {
			return false;
		}
		if (!PUBLIC_USER_ID.equalsIgnoreCase(trimValue(genericHeader.getUserId()))) {
			return false;
		}
		if (!PUBLIC_APARTMENT_ID.equalsIgnoreCase(trimValue(genericHeader.getApartmentId()))) {
			return false;
		}
		String flatNo = trimValue(genericHeader.getFlatNo());
		if (!StringUtils.hasText(flatNo)) {
			return false;
		}
		if (flatRepository.findByAprmntIdAndFlatNo(PUBLIC_APARTMENT_ID, flatNo).isEmpty()) {
			return false;
		}
		if (!StringUtils.hasText(trimValue(request.getPaymentId())) || !StringUtils.hasText(trimValue(request.getDueId()))) {
			return false;
		}
		DueAmountDetailsEntity paidDueDetails = request.getPaidDueDetails();
		if (paidDueDetails == null) {
			return false;
		}
		List<DueAmountDetailsEntity> dueDetails = dueAmountDetailsRepository
				.findByPaymentIdAndDueId(trimValue(request.getPaymentId()), trimValue(request.getDueId()));
		if (dueDetails == null || dueDetails.isEmpty()) {
			return false;
		}
		String normalizedFlatNo = normalizeFlatNo(flatNo);
		for (DueAmountDetailsEntity dueDetail : dueDetails) {
			if (dueDetail != null && isFlatApplicableForDue(dueDetail.getApplicableFlats(), normalizedFlatNo)) {
				return true;
			}
		}
		return false;
	}

	private boolean isFlatApplicableForDue(String applicableFlats, String flatNo) {
		if (!StringUtils.hasText(applicableFlats) || !StringUtils.hasText(flatNo)) {
			return false;
		}
		String normalizedApplicableFlats = applicableFlats.trim();
		if ("ALL".equalsIgnoreCase(normalizedApplicableFlats)) {
			return true;
		}
		try {
			List<String> flats = genericService.fromJson(normalizedApplicableFlats, new TypeReference<List<String>>() {});
			if (flats == null) {
				return false;
			}
			for (String applicableFlat : flats) {
				if (flatNo.equals(normalizeFlatNo(applicableFlat))) {
					return true;
				}
			}
			return false;
		} catch (RuntimeException exception) {
			for (String applicableFlat : normalizedApplicableFlats.split(",")) {
				if (flatNo.equals(normalizeFlatNo(applicableFlat))) {
					return true;
				}
			}
			return false;
		}
	}

	private String normalizeFlatNo(String flatNo) {
		String value = trimValue(flatNo);
		return StringUtils.hasText(value) ? value.toUpperCase() : null;
	}

	private String trimValue(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
	
	@PostMapping("/createOTP")
	@CrossOrigin(origins = "*")
	public CreateOtpResponse createOTP(@RequestBody CreateOtpRequest request, HttpServletRequest httpRequest) {
		CreateOtpResponse response = new CreateOtpResponse();
		if (request == null || !StringUtils.hasText(request.getUserId())) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_60);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_60);
			return response;
		}
		try {
			String sessionId = httpRequest.getSession(true).getId();
			String message = genericService.createOTP(sessionId, request.getUserId().trim());
			response.setMessage(message);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_61);
		} catch (BusinessException e) {
			response.setMessage(e.getErrorMessage());
			response.setMessageCode(e.getErrorMessageCode());
		} catch (Exception e) {
			e.printStackTrace();
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}

	@PostMapping("/validateOTP")
	@CrossOrigin(origins = "*")
	public ValidateOtpResponse validateOTP(@RequestBody ValidateOtpRequest request, HttpServletRequest httpRequest) {
		ValidateOtpResponse response = new ValidateOtpResponse();
		if (request == null || !StringUtils.hasText(request.getOtp())) {
			response.setMessage(ErrorMessage.ERR_MESSAGE_60);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_60);
			return response;
		}
		try {
			String sessionId = httpRequest.getSession(true).getId();
			genericService.validateOTP(sessionId, request.getOtp().trim());
			response.setMessage(SuccessMessage.SUCC_MESSAGE_62);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_62);
		} catch (BusinessException e) {
			response.setMessage(e.getErrorMessage());
			response.setMessageCode(e.getErrorMessageCode());
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
		} catch (Exception e) {
			e.printStackTrace();
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}
}
