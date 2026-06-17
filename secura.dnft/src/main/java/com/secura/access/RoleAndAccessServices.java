package com.secura.access;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.dao.ProfileRepository;
import com.secura.dnft.dao.RoleRepository;
import com.secura.dnft.entity.Profile;
import com.secura.dnft.entity.RoleEntity;
import com.secura.dnft.entity.RoleEntityId;
import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.generic.bean.SuccessMessage;
import com.secura.dnft.generic.bean.SuccessMessageCode;
import com.secura.dnft.request.response.CreateRoleRequest;
import com.secura.dnft.request.response.CreateRoleResponse;
import com.secura.dnft.request.response.GetAllRolesRequest;
import com.secura.dnft.request.response.GetAllRolesResponse;
import com.secura.dnft.request.response.UpdateRoleStatusRequest;
import com.secura.dnft.request.response.UpdateRoleStatusResponse;
import com.secura.dnft.request.response.UpdateAccessRequest;
import com.secura.dnft.request.response.UpdateAccessResponse;
import com.secura.dnft.request.response.UpdateRoleRequest;
import com.secura.dnft.request.response.UpdateRoleResponse;
import com.secura.dnft.service.GenericService;

@Service
public class RoleAndAccessServices implements RoleAndAccessServicesInterface{

	
	@Autowired
	ProfileRepository profileRepository;
	
	@Autowired
	GenericService genericService;

	@Autowired
	RoleRepository roleRepository;
	
	@Override
	public Access getAllAccess(String userID, String aprtmentId, String userRole) throws Exception {
		Optional<Profile> prfl = profileRepository.findById(userID);
		Access access = new Access();
		if(prfl.isPresent()) {
			Profile profile=prfl.get();
			Map<String, Access> accessList=genericService.fromJson(profile.getPrfl_access(),
					new TypeReference<Map<String, Access>>() {
					});
			access= accessList.get(aprtmentId);
			if(null!=access) {
				return access;
			}
			else{
				List<RoleEntity> allRoles=roleRepository.findAll();
				Optional<RoleEntity> role= allRoles.stream().filter(rl->rl.getRoleName().equals(userRole)).findFirst();
				if(role.isPresent()) {
					access=genericService.fromJson(role.get().getAccess(),Access.class);
					return access;
			}
				else {
					return buildDefaultAccess();
				}
				
			}
			
		}
		else {
			return buildDefaultAccess();
		}
		
		
		
	}

	@Override
	public Object getSpecificAccess(String userID, String accessConstant) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean validateAccess(String userID, String accessConstant) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public UpdateAccessResponse updateAccess(UpdateAccessRequest updateAccessRequest) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CreateRoleResponse createRole(CreateRoleRequest createRoleRequest) throws Exception {
		String aprtrmntId = createRoleRequest.getGenericHeader().getApartmentId();

		RoleEntity roleEntity = new RoleEntity();
		roleEntity.setAprtrmntId(aprtrmntId);
		roleEntity.setRoleName(createRoleRequest.getRoleName());
		roleEntity.setCreatUsrId(createRoleRequest.getGenericHeader().getUserId());
		roleEntity.setLstUpdtUsrId(createRoleRequest.getGenericHeader().getUserId());
		roleEntity.setRoleStatus(SecuraConstants.STATUS_ACTIVE);
		long roleCount = roleRepository.countByAprtrmntId(aprtrmntId);
		String roleId = "RL" + aprtrmntId + (roleCount + 1);
		roleEntity.setRoleId(roleId);

		Access defaultAccess = buildDefaultAccess();
		roleEntity.setAccess(genericService.toJson(defaultAccess));

		roleRepository.save(roleEntity);

		CreateRoleResponse response = new CreateRoleResponse();
		response.setGenericHeader(createRoleRequest.getGenericHeader());
		response.setMessage(SuccessMessage.SUCC_MESSAGE_51);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_51);
		return response;
	}

	@Override
	public UpdateRoleStatusResponse updateRoleStatus(UpdateRoleStatusRequest updateRoleStatusRequest) throws Exception {
		UpdateRoleStatusResponse response = new UpdateRoleStatusResponse();
		RoleEntityId roleEntityId = new RoleEntityId();
		roleEntityId.setAprtrmntId(updateRoleStatusRequest.getGenericHeader().getApartmentId());
		roleEntityId.setRoleId(updateRoleStatusRequest.getRoleId());
		Optional<RoleEntity> roleEntity=roleRepository.findById(roleEntityId);

		if(roleEntity.isPresent()) {
			RoleEntity role = roleEntity.get();
			if(updateRoleStatusRequest.getStatus().equalsIgnoreCase(SecuraConstants.STATUS_ACTIVE)) {
				role.setRoleStatus(SecuraConstants.STATUS_ACTIVE);
			}
			else {
				role.setRoleStatus(SecuraConstants.STATUS_INACTIVE);
			}
			roleRepository.save(role);
			response.setMessage(SuccessMessage.SUCC_MESSAGE_53);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_53);
		}
		else {
			response.setMessage(ErrorMessage.ERR_MESSAGE_56);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_56);
		}
		return response;
	}

	private Access buildDefaultAccess() {
		AccountManagmentAccess accountManagmentAccess = new AccountManagmentAccess();
		accountManagmentAccess.setParentAccess(true);
		accountManagmentAccess.setCreateUpdateProfileAccess(false);
		accountManagmentAccess.setViewAllProfile(false);
		accountManagmentAccess.setAllTenantManagement(false);
		accountManagmentAccess.setAllOwnerManagement(false);

		AdminAccess adminAccess = new AdminAccess();
		adminAccess.setParentAccess(false);
		adminAccess.setRoleManagmentparentAccess(false);
		adminAccess.setCreateRoleAccess(false);
		adminAccess.setAssigneRoleAccess(false);
		adminAccess.setManageRoleAccess(false);
		adminAccess.setStaffeManagmentparentAccess(false);
		adminAccess.setOnboardStaffAccess(false);
		adminAccess.setStaffAttendanceAccess(false);
		adminAccess.setFlateManagmentparentAccess(false);
		adminAccess.setAddUpdateFlatAccess(false);
		adminAccess.setSocietyDetailsManagmentparentAccess(false);

		BookingAccess bookingAccess = new BookingAccess();
		bookingAccess.setParentAccess(true);
		bookingAccess.setViewAllBookingtAccess(false);
		bookingAccess.setCreateSoceityBookingtAccess(false);
		bookingAccess.setManageBookingtAccess(false);

		FinanceAccess financeAccess = new FinanceAccess();
		financeAccess.setParentAccess(true);
		financeAccess.setLedgerEntryAccess(false);
		financeAccess.setCreateNewPaymentAccess(false);
		financeAccess.setUpdatePaymentAccess(false);
		financeAccess.setCreateReceiptAccess(false);
		financeAccess.setViewAllTransactionAccess(false);
		financeAccess.setUploadPastPaymentAccess(false);
		financeAccess.setReconcilePaymentAccess(false);
		financeAccess.setBudgetManagment(false);

		MeetingAndNoticeAccess meetingAndNoticeAccess = new MeetingAndNoticeAccess();
		meetingAndNoticeAccess.setParentAccess(true);
		meetingAndNoticeAccess.setScheduleMeetingAccess(false);
		meetingAndNoticeAccess.setUpdateMeetingAccess(false);
		meetingAndNoticeAccess.setUpdateMOMgAccess(false);
		meetingAndNoticeAccess.setCreateNoticeAccess(false);
		meetingAndNoticeAccess.setCreateEventAccess(false);
		meetingAndNoticeAccess.setUpdateEventAccess(false);
		meetingAndNoticeAccess.setCreatePollAccess(false);

		ReportAccess reportAccess = new ReportAccess();
		reportAccess.setParentAccess(true);
		reportAccess.setViewTotalsAccess(true);
		reportAccess.setDashboardAccess(true);
		reportAccess.setBalanceSheetAccess(false);
		reportAccess.setTaxSheetAccess(false);
		reportAccess.setPaymentWiseCollectionAccess(false);
		reportAccess.setDefaulterReportAccess(false);
		reportAccess.setPenaltyReportAccess(false);

		SecurityAccess securityAccess = new SecurityAccess();
		securityAccess.setParentAccess(true);
		securityAccess.setCreateAllFlatEntryAccess(false);
		securityAccess.setCreateDailyVisitorEntryAccess(false);
		securityAccess.setCreateVehiclePass(false);

		TicketManagementAccess ticketManagementAccess = new TicketManagementAccess();
		ticketManagementAccess.setParentAccess(true);
		ticketManagementAccess.setViewAllTicketAccess(false);
		ticketManagementAccess.setReAsignTicketAccess(false);
		ticketManagementAccess.setTicketHeadAccess(false);

		VendorManagementAccess vendorManagementAccess = new VendorManagementAccess();
		vendorManagementAccess.setParentAccess(false);
		vendorManagementAccess.setAddNewVendorAccess(false);
		vendorManagementAccess.setUpdateVendorAccess(false);
		vendorManagementAccess.setViewVendorsAccess(false);

		GroupManagmentAccess groupManagmentAccess = new GroupManagmentAccess();
		groupManagmentAccess.setParentAccess(false);

		SkillClassAccess skillClassAccess = new SkillClassAccess();
		skillClassAccess.setParentAccess(false);

		Access access = new Access();
		access.setAccountManagmentAccess(accountManagmentAccess);
		access.setAdminAccess(adminAccess);
		access.setBookingAccess(bookingAccess);
		access.setFinanceAccess(financeAccess);
		access.setMeetingAndNoticeAccess(meetingAndNoticeAccess);
		access.setReportAccess(reportAccess);
		access.setSecurityAccess(securityAccess);
		access.setTicketManagementAccess(ticketManagementAccess);
		access.setVendorManagementAccess(vendorManagementAccess);
		access.setGroupManagmentAccess(groupManagmentAccess);
		access.setSkillClassAccess(skillClassAccess);
		return access;
	}

	@Override
	public UpdateRoleResponse updateRole(UpdateRoleRequest updateRoleRequest) throws Exception {
		UpdateRoleResponse response = new UpdateRoleResponse();
		RoleEntityId roleEntityId = new RoleEntityId();
		roleEntityId.setAprtrmntId(updateRoleRequest.getGenericHeader().getApartmentId());
		roleEntityId.setRoleId(updateRoleRequest.getRoleId());
		Optional<RoleEntity> roleEntity=roleRepository.findById(roleEntityId);

		if(roleEntity.isPresent()) {
			RoleEntity role = roleEntity.get();
			role.setAccess(genericService.toJson(updateRoleRequest.getAccess()));
			roleRepository.save(role);
			response.setMessage(SuccessMessage.SUCC_MESSAGE_52);
			response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_52);
		}
		else {
			response.setMessage(ErrorMessage.ERR_MESSAGE_56);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_56);
		}
		return response;
	}

	@Override
	public GetAllRolesResponse getAllRoles(GetAllRolesRequest getAllRolesRequest) throws Exception {
		GetAllRolesResponse response = new GetAllRolesResponse();
		List<RoleEntity> roles=roleRepository.findAll();
		roles=roles.stream().filter(rl->rl.getAprtrmntId().equals(getAllRolesRequest.getGenericHeader().getApartmentId())).collect(Collectors.toList());
		response.setRoles(roles);
		response.setMessage(SuccessMessage.SUCC_MESSAGE_54);
		response.setMessageCode(SuccessMessageCode.SUCC_MESSAGE_54);
		return response;
	}

}

