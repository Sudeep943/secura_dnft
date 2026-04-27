package com.secura.dnft.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.EmployeeEntity;

public interface EmployeeRepository extends JpaRepository<EmployeeEntity, Long> {

    Optional<EmployeeEntity> findByEmployeeCode(String employeeCode);

    java.util.List<EmployeeEntity> findByStatus(String status);
}
