package com.secura.dnft.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.NoticeEntity;

public interface NoticeRepository extends JpaRepository<NoticeEntity, String> {

	List<NoticeEntity> findByEmailSentflag(String emailSentflag);
}
