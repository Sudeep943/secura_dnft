package com.secura.dnft.validation;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.secura.dnft.dao.OwnerRepository;
import com.secura.dnft.dao.TenantRepository;
import com.secura.dnft.entity.Owner;
import com.secura.dnft.entity.Tenant;
import com.secura.dnft.generic.bean.SecuraConstants;

@Service
public class ProfileServiceValidation {

	@Autowired
	OwnerRepository ownerRepository;
	
	@Autowired
	TenantRepository tenantRepository;

	public boolean validateOwnerTenantExits(String flatId, String profileType) {
		boolean ownerTenantExists = false;
		if (null != flatId && null != profileType) {
			if (profileType.equals(SecuraConstants.PROFILE_TYPE_OWNER)) {
				Owner owner = getCurrentFlatOwner(flatId);
				if (owner != null) {
					return true;
				}
			}
			if (profileType.equals(SecuraConstants.PROFILE_TYPE_TENANT)) {
				Tenant tenant =getCurrentFlatTenant(flatId);
				if (tenant != null) {
					return true;
				}
			}
		}
		return ownerTenantExists;
	}
	
	public Owner getCurrentFlatOwner(String flatId) {
		List<Owner> ownerList = ownerRepository.findByFlatNo(flatId);
		Optional<Owner> owner = ownerList.stream().filter(tnt -> null == tnt.getEndDate()).findFirst();
		if (owner.isPresent()) {
			return owner.get();
		} else {
			return null;
		}
	}
	
	public Tenant getCurrentFlatTenant(String flatId) {
		List<Tenant> tenantList = tenantRepository.findByFlatNo(flatId);
		Optional<Tenant> tenant = tenantList.stream().filter(tnt -> null == tnt.getEndDate()).findFirst();
		if (tenant.isPresent()) {
			return tenant.get();
		} else {
			return null;
		}

	}
}
