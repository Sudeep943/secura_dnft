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

	public boolean validateOwnerTenantExits(String flatId, String apartmentId, String profileType) {
		boolean ownerTenantExists = false;
		if (null != flatId && null != apartmentId && null != profileType) {
			if (profileType.equals(SecuraConstants.PROFILE_TYPE_OWNER)) {
				Owner owner = getCurrentFlatOwner(apartmentId, flatId);
				if (owner != null) {
					return true;
				}
			}
			if (profileType.equals(SecuraConstants.PROFILE_TYPE_TENANT)) {
				Tenant tenant = getCurrentFlatTenant(apartmentId, flatId);
				if (tenant != null) {
					return true;
				}
			}
		}
		return ownerTenantExists;
	}
	
	public Owner getCurrentFlatOwner(String apartmentId, String flatId) {
		List<Owner> ownerList = ownerRepository.findByAprmt_idAndFlatNo(apartmentId, flatId);
		Optional<Owner> owner = ownerList.stream().filter(tnt -> null == tnt.getEndDate()).findFirst();
		if (owner.isPresent()) {
			return owner.get();
		} else {
			return null;
		}
	}
	
	public Tenant getCurrentFlatTenant(String apartmentId, String flatId) {
		List<Tenant> tenantList = tenantRepository.findByAprmt_idAndFlatNo(apartmentId, flatId);
		Optional<Tenant> tenant = tenantList.stream().filter(tnt -> null == tnt.getEndDate()).findFirst();
		if (tenant.isPresent()) {
			return tenant.get();
		} else {
			return null;
		}

	}
}
