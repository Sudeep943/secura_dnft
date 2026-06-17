package com.secura.dnft.request.response;

import com.secura.access.Access;

public class UpdateRoleRequest {

	private GenericHeader genericHeader;
	private String roleId;
	private Access access;
	public String getRoleId() {
		return roleId;
	}
	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}
	public Access getAccess() {
		return access;
	}
	public void setAccess(Access access) {
		this.access = access;
	}
	public GenericHeader getGenericHeader() {
		return genericHeader;
	}
	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}
	
	
	
}
