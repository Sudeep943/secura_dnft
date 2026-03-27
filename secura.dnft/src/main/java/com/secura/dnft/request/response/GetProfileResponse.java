package com.secura.dnft.request.response;

import java.time.LocalDateTime;

import com.secura.dnft.entity.Profile;
import com.secura.dnft.generic.bean.Acccess;
import com.secura.dnft.generic.bean.Address;
import com.secura.dnft.generic.bean.ContactDetails;
import com.secura.dnft.generic.bean.Name;

public class GetProfileResponse {

	private GenericHeader genericHeader;
	private String prflId;
    private Name prflName;
    private String prflFlatNo;
    private ContactDetails contactDetails;
    private Address prflOthrAdrss;
    private String prflType;
    private String prflStus;
    private Acccess prflAccess;
    private String prflPosition;
    private String aprmntId;
    private LocalDateTime creatTs;
    private String creatUsrId;
    private String lstUpdtUsrId;
    private String gender;
    private LocalDateTime prflDob;
    private String profilePic;
    private String message;
    private String messageCode;
    private String apartmentName;
    private String creatUsrName;
    private String lstUpdtUsrName;
    
	public String getApartmentName() {
		return apartmentName;
	}

	public void setApartmentName(String apartentName) {
		this.apartmentName = apartentName;
	}

	public String getCreatUsrName() {
		return creatUsrName;
	}

	public void setCreatUsrName(String creatUsrName) {
		this.creatUsrName = creatUsrName;
	}

	public String getLstUpdtUsrName() {
		return lstUpdtUsrName;
	}

	public void setLstUpdtUsrName(String lstUpdtUsrName) {
		this.lstUpdtUsrName = lstUpdtUsrName;
	}

	public GetProfileResponse() {
		super();
	}

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}
	
	public GetProfileResponse(Profile entity) {
	    this.prflId = entity.getPrflId();

	    // Name mapping (assuming Name is a custom object)
	  //  this.prflName = new Name(entity.getPrflName());

	    this.prflFlatNo = entity.getPrflFlatNo();

	    // ContactDetails mapping
	    this.contactDetails = new ContactDetails(entity.getPrflPhoneNo(),entity.getPrflEmailAdrss(),null);

	    // Address mapping
	   // this.prflOthrAdrss = new Address(entity.getPrflOthrAdrss());

	    this.prflType = entity.getPrflType();
	    this.prflStus = entity.getPrflStus();

	    // Access mapping
	    //this.prflAccess = new Acccess(entity.getPrflAccess());

	    this.prflPosition = entity.getPrflPosition();
	    this.aprmntId = entity.getAprmntId();
	    this.creatTs = entity.getCreatTs();
	    this.creatUsrId = entity.getCreatUsrId();
	    this.lstUpdtUsrId = entity.getLstUpdtUsrId();
	    this.gender = entity.getGender();
	    this.prflDob = entity.getPrflDob();
	    this.profilePic = entity.getProfilePic();
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}
	public String getPrflId() {
		return prflId;
	}
	public void setPrflId(String prflId) {
		this.prflId = prflId;
	}
	public Name getPrflName() {
		return prflName;
	}
	public void setPrflName(Name prflName) {
		this.prflName = prflName;
	}
	public String getPrflFlatNo() {
		return prflFlatNo;
	}
	public void setPrflFlatNo(String prflFlatNo) {
		this.prflFlatNo = prflFlatNo;
	}
	public ContactDetails getContactDetails() {
		return contactDetails;
	}
	public void setContactDetails(ContactDetails contactDetails) {
		this.contactDetails = contactDetails;
	}
	public Address getPrflOthrAdrss() {
		return prflOthrAdrss;
	}
	public void setPrflOthrAdrss(Address prflOthrAdrss) {
		this.prflOthrAdrss = prflOthrAdrss;
	}
	public String getPrflType() {
		return prflType;
	}
	public void setPrflType(String prflType) {
		this.prflType = prflType;
	}
	public String getPrflStus() {
		return prflStus;
	}
	public void setPrflStus(String prflStus) {
		this.prflStus = prflStus;
	}
	public Acccess getPrflAccess() {
		return prflAccess;
	}
	public void setPrflAccess(Acccess prflAccess) {
		this.prflAccess = prflAccess;
	}
	public String getPrflPosition() {
		return prflPosition;
	}
	public void setPrflPosition(String prflPosition) {
		this.prflPosition = prflPosition;
	}
	public String getAprmntId() {
		return aprmntId;
	}
	public void setAprmntId(String aprmntId) {
		this.aprmntId = aprmntId;
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
	public String getGender() {
		return gender;
	}
	public void setGender(String gender) {
		this.gender = gender;
	}
	public LocalDateTime getPrflDob() {
		return prflDob;
	}
	public void setPrflDob(LocalDateTime prflDob) {
		this.prflDob = prflDob;
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
    
    
}
