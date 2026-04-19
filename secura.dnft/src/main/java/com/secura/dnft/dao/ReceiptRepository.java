package com.secura.dnft.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.Receipt;

public interface ReceiptRepository extends JpaRepository<Receipt, String> {

}
