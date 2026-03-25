package com.secura.dnft.request.response;

import com.secura.dnft.generic.bean.Address;
import com.secura.dnft.generic.bean.ContactDetails;
import com.secura.dnft.generic.bean.Name;

public class UpdateProfileResponse {
	private GenericHeader header;
	private String profileId;
	private Name profileName;
    private String profileFlatNo;
    private ContactDetails contact;
    private Address profileOthrAdrss;
    private String profileType;
    private String profilePosition;
    private String profilePic;
	private String message;
	private String messageCode;
	public GenericHeader getHeader() {
		return header;
	}
	public void setHeader(GenericHeader header) {
		this.header = header;
	}
	public String getProfileId() {
		return profileId;
	}
	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}
	public Name getProfileName() {
		return profileName;
	}
	public void setProfileName(Name profileName) {
		this.profileName = profileName;
	}
	public String getProfileFlatNo() {
		return profileFlatNo;
	}
	public void setProfileFlatNo(String profileFlatNo) {
		this.profileFlatNo = profileFlatNo;
	}
	public ContactDetails getContact() {
		return contact;
	}
	public void setContact(ContactDetails contact) {
		this.contact = contact;
	}
	public Address getProfileOthrAdrss() {
		return profileOthrAdrss;
	}
	public void setProfileOthrAdrss(Address profileOthrAdrss) {
		this.profileOthrAdrss = profileOthrAdrss;
	}
	public String getProfileType() {
		return profileType;
	}
	public void setProfileType(String profileType) {
		this.profileType = profileType;
	}
	public String getProfilePosition() {
		return profilePosition;
	}
	public void setProfilePosition(String profilePosition) {
		this.profilePosition = profilePosition;
	}
	public String getProfilePic() {
		return profilePic;
	}
	public void setProfilePic(String profilePic) {
		this.profilePic = profilePic;
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
	
	public UpdateProfileResponse(UpdateProfileRequest request) {
	    if (request != null) {
	        this.profileId = request.getProfileId();
	        this.profileName = request.getProfileName();
	        this.profileFlatNo = request.getProfileFlatNo();
	        this.contact = request.getContact();
	        this.profileOthrAdrss = request.getProfileOthrAdrss();
	        this.profileType = request.getProfileType();
	        this.profilePosition = request.getProfilePosition();
	        this.profilePic = request.getProfilePic();
	    }
	    }
	public UpdateProfileResponse() {
		// TODO Auto-generated constructor stub
	}
}
