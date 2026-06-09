package com.secura.dnft.entity;

import java.io.Serializable;
import java.util.Objects;

public class RoleEntityId implements Serializable {

	private static final long serialVersionUID = 1L;

	private String aprtrmntId;
	private String roleId;

	public RoleEntityId() {
	}

	public RoleEntityId(String aprtrmntId, String roleId) {
		this.aprtrmntId = aprtrmntId;
		this.roleId = roleId;
	}

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

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof RoleEntityId other)) {
			return false;
		}
		return Objects.equals(aprtrmntId, other.aprtrmntId) && Objects.equals(roleId, other.roleId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(aprtrmntId, roleId);
	}
}
