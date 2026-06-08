package com.secura.dnft.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@IdClass(RoleEntityId.class)
@Table(name = "secura_role")
public class RoleEntity {

	@Id
	@Column(name = "aprtrmnt_id")
	private String aprtrmntId;

	@Id
	@Column(name = "role_id")
	private String roleId;

	@Column(name = "role_name")
	private String roleName;

	@Column(name = "access", columnDefinition = "TEXT")
	private String access;

	@Column(name = "creat_ts")
	@CreationTimestamp
	private LocalDateTime creatTs;

	@Column(name = "creat_usr_id")
	private String creatUsrId;

	@Column(name = "lst_updt_usr_id")
	private String lstUpdtUsrId;

	@Column(name = "lst_updt_ts")
	@UpdateTimestamp
	private LocalDateTime lstUpdtTs;

	public String getAprtrmntId() {
		return aprtrmntId;
	}

	public void setAprtrmntId(String aprtrmntId) {
		this.aprtrmntId = aprtrmntId;
	}

	public String getRoleId() {
		return roleId;
	}

	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	public String getAccess() {
		return access;
	}

	public void setAccess(String access) {
		this.access = access;
	}

	public LocalDateTime getCreatTs() {
		return creatTs;
	}

	public void setCreatTs(LocalDateTime creatTs) {
		this.creatTs = creatTs;
	}

	public String getCreatUsrId() {
		return creatUsrId;
	}

	public void setCreatUsrId(String creatUsrId) {
		this.creatUsrId = creatUsrId;
	}

	public String getLstUpdtUsrId() {
		return lstUpdtUsrId;
	}

	public void setLstUpdtUsrId(String lstUpdtUsrId) {
		this.lstUpdtUsrId = lstUpdtUsrId;
	}

	public LocalDateTime getLstUpdtTs() {
		return lstUpdtTs;
	}

	public void setLstUpdtTs(LocalDateTime lstUpdtTs) {
		this.lstUpdtTs = lstUpdtTs;
	}
}
