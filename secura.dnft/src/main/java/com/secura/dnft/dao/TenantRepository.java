package com.secura.dnft.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.Tenant;

public interface TenantRepository extends JpaRepository<Tenant, String> {

}
