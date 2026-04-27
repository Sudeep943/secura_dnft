package com.secura.dnft.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.AttendanceLogEntity;

public interface AttendanceLogRepository extends JpaRepository<AttendanceLogEntity, Long> {

    Optional<AttendanceLogEntity> findFirstByEmployeeIdAndExitTimeIsNullOrderByEntryTimeDesc(Long employeeId);

    List<AttendanceLogEntity> findByEntryTimeBetween(LocalDateTime from, LocalDateTime to);

    List<AttendanceLogEntity> findByEmployeeIdAndEntryTimeBetween(Long employeeId, LocalDateTime from, LocalDateTime to);
}
