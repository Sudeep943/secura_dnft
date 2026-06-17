package com.secura.dnft.request.response;

import java.util.List;

import com.secura.dnft.entity.RoleEntity;

public class GetAllRolesResponse {
	private List<RoleEntity> roles;
	private GenericHeader genericHeader;
	private String message;
	private String messageCode;
	public List<RoleEntity> getRoles() {
		return roles;
	}
	public void setRoles(List<RoleEntity> roles) {
		this.roles = roles;
	}
	public GenericHeader getGenericHeader() {
		return genericHeader;
	}
	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getMessageCode() {
		return messageCode;
	}
	public void setMessageCode(String messageCode) {
		this.messageCode = messageCode;
	}
	
	
}
