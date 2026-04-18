package com.secura.dnft.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

}
