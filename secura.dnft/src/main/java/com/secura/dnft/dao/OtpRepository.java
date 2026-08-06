package com.secura.dnft.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.SecuraOtp;

public interface OtpRepository extends JpaRepository<SecuraOtp, String> {

    List<SecuraOtp> findByUserId(String userId);

    List<SecuraOtp> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    List<SecuraOtp> findByOtpIdAndSessionIdOrderByCreatedAtDesc(String otpId, String sessionId);

    List<SecuraOtp> findByOtpIdOrderByCreatedAtDesc(String otpId);

    List<SecuraOtp> findByExpiryAtBefore(LocalDateTime cutoff);
}
