package com.secura.access;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.type.TypeReference;
import com.secura.dnft.dao.ProfileRepository;
import com.secura.dnft.entity.Profile;
import com.secura.dnft.request.response.CreateRoleRequest;
import com.secura.dnft.request.response.CreateRoleResponse;
import com.secura.dnft.request.response.UpdateAccessRequest;
import com.secura.dnft.request.response.UpdateAccessResponse;
import com.secura.dnft.request.response.UpdateRoleRequest;
import com.secura.dnft.request.response.UpdateRoleResponse;
import com.secura.dnft.service.GenericService;

public class RoleAndAccessServices implements RoleAndAccessServicesInterface{

	
	@Autowired
	ProfileRepository profileRepository;
	
	@Autowired
	GenericService genericService;
	
	@Override
	public Access getAllAccess(String userID, String aprtmentId, String position) throws Exception {
		// TODO Auto-generated method stub
		Optional<Profile> prfl = profileRepository.findById(userID);
		if(prfl.isPresent()) {
			Profile profile=prfl.get();
			Map<String, Access> accessList=genericService.fromJson(profile.getPrfl_access(),
					new TypeReference<Map<String, Access>>() {
					});
			Access access= accessList.get(aprtmentId);
			if(null!=access) {
				return access;
			}
			
			
		}
		
		return null;
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UpdateRoleResponse updateRole(UpdateRoleRequest updateRoleRequest) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	
}
