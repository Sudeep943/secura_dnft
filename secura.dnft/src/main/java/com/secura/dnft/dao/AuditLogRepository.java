package com.secura.dnft.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.AuditLogEntity;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
}
