package com.secura.dnft.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.secura.dnft.entity.ProfileAccount;
import com.secura.dnft.entity.ProfileAccountId;

public interface ProfileAccountRepository extends JpaRepository<ProfileAccount, ProfileAccountId> {
}
