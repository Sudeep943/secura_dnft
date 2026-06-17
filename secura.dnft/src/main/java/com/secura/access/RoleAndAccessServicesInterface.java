package com.secura.access;

import com.secura.dnft.request.response.CreateRoleRequest;
import com.secura.dnft.request.response.CreateRoleResponse;
import com.secura.dnft.request.response.GetAllRolesRequest;
import com.secura.dnft.request.response.GetAllRolesResponse;
import com.secura.dnft.request.response.UpdateAccessRequest;
import com.secura.dnft.request.response.UpdateAccessResponse;
import com.secura.dnft.request.response.UpdateRoleRequest;
import com.secura.dnft.request.response.UpdateRoleResponse;
import com.secura.dnft.request.response.UpdateRoleStatusRequest;
import com.secura.dnft.request.response.UpdateRoleStatusResponse;

public interface RoleAndAccessServicesInterface {
	
	
	Access getAllAccess(String userID, String aprtmentId, String position) throws Exception;
	Object getSpecificAccess(String userID, String accessConstant) throws Exception;
	boolean validateAccess (String userID, String accessConstant) throws Exception;
	UpdateAccessResponse updateAccess(UpdateAccessRequest updateAccessRequest) throws Exception;
	CreateRoleResponse createRole(CreateRoleRequest createRoleRequest) throws Exception;
	UpdateRoleResponse updateRole(UpdateRoleRequest updateRoleRequest) throws Exception;
	UpdateRoleStatusResponse updateRoleStatus(UpdateRoleStatusRequest disableRoleRequest) throws Exception;
	GetAllRolesResponse getAllRoles(GetAllRolesRequest getAllRolesRequest) throws Exception;

	

}
