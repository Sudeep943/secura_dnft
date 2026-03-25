package com.secura.dnft.request.response;

import com.secura.dnft.generic.bean.Address;
import com.secura.dnft.generic.bean.ContactDetails;
import com.secura.dnft.generic.bean.Name;

public class CreateProfileRequest {
	
	private GenericHeader header;
	private Name profileName;
    private String profileFlatNo;
    private ContactDetails contact;
    private Address profileOthrAdrss;
    private String profileType;
    private String profilePosition;
    private String gender;
    
	public GenericHeader getHeader() {
		return header;
	}
	public void setHeader(GenericHeader header) {
		this.header = header;
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
	public String getGender() {
		return gender;
	}
	public void setGender(String gender) {
		this.gender = gender;
	}

    
}
