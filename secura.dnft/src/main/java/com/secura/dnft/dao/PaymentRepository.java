package com.secura.dnft.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.PaymentEntity;

public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {

	List<PaymentEntity> findByAprmtId(String aprmtId);
}
