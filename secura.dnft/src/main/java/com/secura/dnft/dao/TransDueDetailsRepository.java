package com.secura.dnft.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.TransDueDetailsEntity;
import com.secura.dnft.entity.TransDueDetailsEntityId;

public interface TransDueDetailsRepository extends JpaRepository<TransDueDetailsEntity, TransDueDetailsEntityId> {
}
