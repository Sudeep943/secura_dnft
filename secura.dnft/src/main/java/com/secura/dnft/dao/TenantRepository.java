package com.secura.dnft.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.secura.dnft.entity.Tenant;
import com.secura.dnft.entity.TenantId;

public interface TenantRepository extends JpaRepository<Tenant, TenantId> {

    List<Tenant> findByFlatNo(String flatNo);

    @Query("SELECT t FROM Tenant t WHERE t.aprmt_id = :aprmtId AND t.flatNo = :flatNo")
    List<Tenant> findByAprmt_idAndFlatNo(@Param("aprmtId") String aprmtId, @Param("flatNo") String flatNo);
    
    @Query("SELECT t FROM Tenant t WHERE t.tenantId = :tenantId AND t.flatNo = :flatNo AND t.aprmt_id = :aprmtId")
    Optional<Tenant> findByTenantIdAndFlatNoAndAprmt_id(@Param("tenantId") String tenantId, @Param("flatNo") String flatNo, @Param("aprmtId") String aprmtId);

}
