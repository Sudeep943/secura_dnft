package com.secura.dnft.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.Tenant;
import com.secura.dnft.entity.TenantId;

public interface TenantRepository extends JpaRepository<Tenant, TenantId> {

    List<Tenant> findByFlatNo(String flatNo);

    List<Tenant> findByAprmt_idAndFlatNo(String aprmt_id, String flatNo);
    
    Optional<Tenant> findByTenantIdAndFlatNoAndAprmt_id(String tenantId, String flatNo, String aprmt_id);

}
