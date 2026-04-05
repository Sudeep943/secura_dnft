package com.secura.dnft.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.SecuraPayment;

public interface SecuraPaymentRepository extends JpaRepository<SecuraPayment, String> {

}
