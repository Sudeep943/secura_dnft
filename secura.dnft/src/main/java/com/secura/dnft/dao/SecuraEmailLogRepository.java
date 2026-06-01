package com.secura.dnft.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.SecuraEmailLog;

public interface SecuraEmailLogRepository extends JpaRepository<SecuraEmailLog, String> {

    Optional<SecuraEmailLog> findByTypeAndReferenceUniqueId(String type, String referenceUniqueId);

    List<SecuraEmailLog> findByType(String type);

    List<SecuraEmailLog> findByCreateTsBefore(LocalDateTime cutoff);

    List<SecuraEmailLog> findByFailedApplicableListIsNotNull();
}
