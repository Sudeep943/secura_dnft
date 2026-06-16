package com.secura.access;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.dao.ProfileRepository;
import com.secura.dnft.dao.RoleRepository;
import com.secura.dnft.entity.Profile;
import com.secura.dnft.entity.RoleEntity;
import com.secura.dnft.entity.RoleEntityId;
import com.secura.dnft.request.response.CreateRoleRequest;
import com.secura.dnft.request.response.CreateRoleResponse;
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
			else {
				List<RoleEntity> allRoles=roleRepository.findAll();
				Optional<RoleEntity> role= allRoles.stream().filter(rl->rl.getRoleName().equals(userRole)).findFirst();
				if(role.isPresent()) {
					access=genericService.fromJson(role.get().getAccess(),Access.class);
					return access;
			}
				
			}
		}
		
		return buildDefaultAccess();
		
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

		long roleCount = roleRepository.countByAprtrmntId(aprtrmntId);
		String roleId = "RL" + aprtrmntId + (roleCount + 1);
		roleEntity.setRoleId(roleId);

		Access defaultAccess = buildDefaultAccess();
		roleEntity.setAccess(genericService.toJson(defaultAccess));

		roleRepository.save(roleEntity);

		CreateRoleResponse response = new CreateRoleResponse();
		response.setGenericHeader(createRoleRequest.getGenericHeader());
		response.setMessage("Role Created Successfully");
		response.setMessageCode("SUCC");
		return response;
	}

	@Override
	public UpdateRoleResponse updateRole(UpdateRoleRequest updateRoleRequest) throws Exception {
		// TODO Auto-generated method stub
		return null;
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

	
}

