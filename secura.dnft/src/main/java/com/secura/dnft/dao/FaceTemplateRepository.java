package com.secura.dnft.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.FaceTemplateEntity;

public interface FaceTemplateRepository extends JpaRepository<FaceTemplateEntity, Long> {

    List<FaceTemplateEntity> findByEmployeeId(Long employeeId);
}
