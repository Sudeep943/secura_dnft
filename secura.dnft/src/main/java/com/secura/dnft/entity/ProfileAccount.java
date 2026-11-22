package com.secura.dnft.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "secura_profile_account")
@IdClass(ProfileAccountId.class)
public class ProfileAccount {
    @Id
    @Column(name = "profile_id", nullable = false)
    private String profileId;

    @Id
    @Column(name = "apartment_id", nullable = false)
    private String apartmentId;

    @Column(name = "position")
    private String position;

    @Column(name = "role")
    private String role;

    @Column(name = "jobeposition")
    private String jobeposition;

    @Column(name = "flat_id")
    private String flatId;

    @Column(name = "status")
    private String status;

    @CreationTimestamp
    @Column(name = "created_on", updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "create_by", updatable = false)
    private String createBy;

    @Column(name = "last_updt_by")
    private String lastUpdtBy;

    @UpdateTimestamp
    @Column(name = "lst_updt_on")
    private LocalDateTime lstUpdtOn;
}
