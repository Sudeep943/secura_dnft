package com.secura.dnft.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.PaymentEntity;
import com.secura.dnft.entity.PaymentEntityId;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, PaymentEntityId> {

	List<PaymentEntity> findByAprmtId(String aprmtId);

	List<PaymentEntity> findByPaymentId(String paymentId);

	List<PaymentEntity> findByPaymentIdAndAprmtId(String paymentId, String aprmtId);

	Optional<PaymentEntity> findFirstByPaymentId(String paymentId);

	Optional<PaymentEntity> findFirstByPaymentIdAndAprmtId(String paymentId, String aprmtId);

	long countByAprmtIdAndCauseIdIgnoreCase(String aprmtId, String causeId);

	List<PaymentEntity> findByEmailSentflag(String emailSentflag);
}
