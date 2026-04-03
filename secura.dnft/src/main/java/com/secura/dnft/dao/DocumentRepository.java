package com.secura.dnft.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.DocumentEntity;

public interface DocumentRepository extends JpaRepository<DocumentEntity, String> {


}
