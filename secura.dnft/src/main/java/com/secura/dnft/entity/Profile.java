package com.secura.dnft.entity;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.request.response.CreateProfileRequest;
import com.secura.dnft.request.response.UpdateProfileRequest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "secura_profl")
public class Profile {

	@Id
    @Column(name = "prfl_id")
    private String prflId;

    @Column(name = "prfl_name")
    private String prflName;

    @Column(name = "prfl_flat_no")
    private String prflFlatNo;

    @Column(name = "prfl_phone_no")
    private String prflPhoneNo;

    @Column(name = "prfl_email_adrss")
    private String prflEmailAdrss;

    @Column(name = "prfl_othr_adrss", columnDefinition = "TEXT")
    private String prflOthrAdrss;

    @Column(name = "prfl_type")
    private String prflType;

    @Column(name = "prfl_stus")
    private String prflStus;

    @Column(name = "prfl_access")
    private String prflAccess;

    @Column(name = "prfl_position")
    private String prflPosition;

    @Column(name = "aprmnt_id")
    private String aprmntId;

    @CreationTimestamp
    @Column(name = "creat_ts")
    private LocalDateTime creatTs;

    @Column(name = "creat_usr_id")
    private String creatUsrId;

    @UpdateTimestamp
    @Column(name = "lst_updt_ts")
    private LocalDateTime lstUpdtTs;

    @Column(name = "lst_updt_usrid")
    private String lstUpdtUsrId;
    
    @Column(name = "gender")
    private String gender;

    @Column(name = "prfl_dob")
    private LocalDateTime prflDob;

    @Column(name = "profile_pic", columnDefinition = "TEXT")
    private String profilePic;   

    @Column(name = "password")
    private String password;   


    public Profile() {
		super();
	}
	public String getPrflId() { return prflId; }
    public String getGender() {
		return gender;
	}
	public void setGender(String gender) {
		this.gender = gender;
	}
	public void setPrflId(String prflId) { this.prflId = prflId; }

    public String getPrflName() { return prflName; }
    public void setPrflName(String prflName) { this.prflName = prflName; }

    public String getPrflFlatNo() { return prflFlatNo; }
    public void setPrflFlatNo(String prflFlatNo) { this.prflFlatNo = prflFlatNo; }

    public String getPrflPhoneNo() { return prflPhoneNo; }
    public void setPrflPhoneNo(String prflPhoneNo) { this.prflPhoneNo = prflPhoneNo; }

    public String getPrflEmailAdrss() { return prflEmailAdrss; }
    public void setPrflEmailAdrss(String prflEmailAdrss) { this.prflEmailAdrss = prflEmailAdrss; }

    public String getPrflOthrAdrss() { return prflOthrAdrss; }
    public void setPrflOthrAdrss(String prflOthrAdrss) { this.prflOthrAdrss = prflOthrAdrss; }

    public String getPrflType() { return prflType; }
    public void setPrflType(String prflType) { this.prflType = prflType; }

    public String getPrflStus() { return prflStus; }
    public void setPrflStus(String prflStus) { this.prflStus = prflStus; }

    public String getPrflAccess() { return prflAccess; }
    public void setPrflAccess(String prflAccess) { this.prflAccess = prflAccess; }

    public String getPrflPosition() { return prflPosition; }
    public void setPrflPosition(String prflPosition) { this.prflPosition = prflPosition; }

    public String getAprmntId() { return aprmntId; }
    public void setAprmntId(String aprmntId) { this.aprmntId = aprmntId; }

    public LocalDateTime getCreatTs() { return creatTs; }
    public void setCreatTs(LocalDateTime creatTs) { this.creatTs = creatTs; }

    public String getCreatUsrId() { return creatUsrId; }
    public void setCreatUsrId(String creatUsrId) { this.creatUsrId = creatUsrId; }

    public LocalDateTime getLstUpdtTs() { return lstUpdtTs; }
    public void setLstUpdtTs(LocalDateTime lstUpdtTs) { this.lstUpdtTs = lstUpdtTs; }

    public String getLstUpdtUsrId() { return lstUpdtUsrId; }
    public void setLstUpdtUsrId(String lstUpdtUsrId) { this.lstUpdtUsrId = lstUpdtUsrId; }

    public LocalDateTime getPrflDob() { return prflDob; }
    public void setPrflDob(LocalDateTime prflDob) { this.prflDob = prflDob; }

    public String getProfilePic() { return profilePic; }
    public void setProfilePic(String profilePic) { this.profilePic = profilePic; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    
    public Profile(CreateProfileRequest request, String profileId) {
        this.prflId =profileId;
        this.prflFlatNo = request.getProfileFlatNo();
        this.prflPhoneNo = request.getContact().getMobileNumber();
        this.prflEmailAdrss = request.getContact().getEmailId();
        this.prflType = request.getProfileType();
        this.prflPosition = request.getProfilePosition();
        this.gender = request.getGender();
        this.prflStus = SecuraConstants.PROFILE_STATUS_ACTIVE;
        this.creatUsrId =request.getHeader().getUserId();
        this.aprmntId=request.getHeader().getApartmentId();
    }
    
    public Profile(UpdateProfileRequest request, String profileId) {
    	if (request == null) {
            return;
        }
        this.prflId = profileId;

        if (request.getProfileFlatNo() != null) {
            this.prflFlatNo = request.getProfileFlatNo();
        }
        if (request.getContact() != null) {
            if (request.getContact().getMobileNumber() != null) {
                this.prflPhoneNo = request.getContact().getMobileNumber();
            }
            if (request.getContact().getEmailId() != null) {
                this.prflEmailAdrss = request.getContact().getEmailId();
            }
        }
        if (request.getProfileType() != null) {
            this.prflType = request.getProfileType();
        }
        if (request.getProfilePosition() != null) {
            this.prflPosition = request.getProfilePosition();
        }
        if (request.getProfileStatus() != null) {
            this.prflStus = request.getProfileStatus();
        }
        if (request.getHeader() != null) {
            if (request.getHeader().getUserId() != null) {
                this.creatUsrId = request.getHeader().getUserId();
            }
            if (request.getHeader().getApartmentId() != null) {
                this.aprmntId = request.getHeader().getApartmentId();
            }
        }
    }
}
