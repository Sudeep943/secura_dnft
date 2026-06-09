package com.secura.dnft.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.RoleEntity;
import com.secura.dnft.entity.RoleEntityId;

public interface RoleRepository extends JpaRepository<RoleEntity, RoleEntityId> {

	long countByAprtrmntId(String aprtrmntId);

}
