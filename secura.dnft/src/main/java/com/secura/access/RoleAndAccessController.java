package com.secura.access;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.generic.bean.ErrorMessage;
import com.secura.dnft.generic.bean.ErrorMessageCode;
import com.secura.dnft.request.response.CreateRoleRequest;
import com.secura.dnft.request.response.CreateRoleResponse;
import com.secura.dnft.request.response.GetAccessRequest;
import com.secura.dnft.request.response.GetAccessResponse;
import com.secura.dnft.request.response.GetAllRolesRequest;
import com.secura.dnft.request.response.GetAllRolesResponse;
import com.secura.dnft.request.response.UpdateRoleRequest;
import com.secura.dnft.request.response.UpdateRoleResponse;
import com.secura.dnft.request.response.UpdateRoleStatusRequest;
import com.secura.dnft.request.response.UpdateRoleStatusResponse;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/roleAndAccess")
public class RoleAndAccessController {
	
	 @Autowired
	 private RoleAndAccessServices roleAndAccessServices;
	
	
    @PostMapping("/getAllAccess")
    @CrossOrigin(origins = "*")
	public GetAccessResponse getAllAccess(@RequestBody GetAccessRequest getAccessRequest) {
		GetAccessResponse response = new GetAccessResponse();
		try {
			Access access=roleAndAccessServices.getAllAccess(getAccessRequest.getGenericHeader().getUserId(), getAccessRequest.getGenericHeader().getApartmentId(), getAccessRequest.getGenericHeader().getRole());
			response.setAccess(access);
		} catch (Exception e) {
			e.printStackTrace();
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}
	
    @PostMapping("/createRole")
    @CrossOrigin(origins = "*")
	public CreateRoleResponse createRole(@RequestBody CreateRoleRequest createRoleRequest) {
		CreateRoleResponse response = new CreateRoleResponse();
		try {
			response=roleAndAccessServices.createRole(createRoleRequest);
		} catch (Exception e) {
			e.printStackTrace();
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
	}
	
    
    @PostMapping("/updateRoleAccess")
    @CrossOrigin(origins = "*")
	public UpdateRoleResponse updateRoleAccess(@RequestBody UpdateRoleRequest updateRoleRequest) {
		UpdateRoleResponse response = new UpdateRoleResponse();
		try {
			response=roleAndAccessServices.updateRole(updateRoleRequest);
		} catch (Exception e) {
			e.printStackTrace();
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
		return response;
		
	}
    
    @PostMapping("/updateRoleStatus")
    @CrossOrigin(origins = "*")
     public UpdateRoleStatusResponse updateRoleStatus(@RequestBody UpdateRoleStatusRequest DisableRoleRequest) {
    	 UpdateRoleStatusResponse response = new UpdateRoleStatusResponse();
    	 try {
			response= roleAndAccessServices.updateRoleStatus(DisableRoleRequest);
		} catch (Exception e) {
			e.printStackTrace();
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
    	 return response;
	}
     
    @PostMapping("/getAllRoles")
    @CrossOrigin(origins = "*")
     public GetAllRolesResponse getAllRoles(@RequestBody GetAllRolesRequest getAllRolesRequest) {
    	 GetAllRolesResponse response = new GetAllRolesResponse();
    	 try {
			response= roleAndAccessServices.getAllRoles(getAllRolesRequest);
		} catch (Exception e) {
			e.printStackTrace();
			response.setMessage(ErrorMessage.ERR_MESSAGE_33);
			response.setMessageCode(ErrorMessageCode.ERR_MESSAGE_33);
		}
    	 return response;
	}
	
}
