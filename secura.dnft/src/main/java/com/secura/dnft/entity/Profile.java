package com.secura.dnft.entity;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.secura.dnft.request.response.CreateProfileRequest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "secura_profl")
public class Profile {


    @Id
    @Column(name = "prfl_id", nullable = false)
    private String prflId;

    @Column(name = "prfl_name")
    private String prflName;

    @Column(name = "prfl_acount_details", columnDefinition = "TEXT")
    private String prflAcountDetails;

    @Column(name = "prfl_dob")
    private LocalDateTime prflDob;

    @Column(name = "prfl_phone_no")
    private String prflPhoneNo;

    @Column(name = "prfl_email_adrss")
    private String prflEmailAdrss;

    @Column(name = "prfl_primary_postal_adrss", columnDefinition = "TEXT")
    private String prflPrimaryPostalAdrss;

    @Column(name = "prfl_othr_adrss" , columnDefinition = "TEXT")
    private String prflOthrAdrss;
    
    @Column(name = "prfl_access")
    private String prfl_access;
    
    @Column(name = "profile_pic" , columnDefinition = "TEXT")
    private String profile_pic; 
    
    @Column(name = "gender")
    private String gender;
    
    @Column(name = "password" , columnDefinition = "TEXT")
    private String password; 

    @Column(name = " creat_ts")
    @CreationTimestamp
    private LocalDateTime creat_ts; 
    
    @Column(name = "creat_usr_id")
    private String creat_usr_id; 
    
    @Column(name = "lst_updt_ts")
    @UpdateTimestamp
    private LocalDateTime lst_updt_ts; 
    
    @Column(name = "lst_updt_usr_id")
    private String lst_updt_usrId; 
    
    @Column(name = "profile_kind")
    private String profileKind;
    

    public String getPrfl_access() {
		return prfl_access;
	}

	public void setPrfl_access(String prfl_access) {
		this.prfl_access = prfl_access;
	}

	public String getProfile_pic() {
		return profile_pic;
	}

	public void setProfile_pic(String profile_pic) {
		this.profile_pic = profile_pic;
	}

	public String getGender() {
		return gender;
	}

	public String getProfileKind() {
		return profileKind;
	}

	public void setProfileKind(String profileKind) {
		this.profileKind = profileKind;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public LocalDateTime getCreat_ts() {
		return creat_ts;
	}

	public void setCreat_ts(LocalDateTime creat_ts) {
		this.creat_ts = creat_ts;
	}

	public String getCreat_usr_id() {
		return creat_usr_id;
	}

	public void setCreat_usr_id(String creat_usr_id) {
		this.creat_usr_id = creat_usr_id;
	}

	public LocalDateTime getLst_updt_ts() {
		return lst_updt_ts;
	}

	public void setLst_updt_ts(LocalDateTime lst_updt_ts) {
		this.lst_updt_ts = lst_updt_ts;
	}

	public String getLst_updt_usrId() {
		return lst_updt_usrId;
	}

	public void setLst_updt_usrId(String lst_updt_usrId) {
		this.lst_updt_usrId = lst_updt_usrId;
	}

	// Constructors
    public Profile() {}

    // Getters and Setters

    public String getPrflId() {
        return prflId;
    }

    public void setPrflId(String prflId) {
        this.prflId = prflId;
    }

    public String getPrflName() {
        return prflName;
    }

    public void setPrflName(String prflName) {
        this.prflName = prflName;
    }

    public String getPrflAcountDetails() {
        return prflAcountDetails;
    }

    public void setPrflAcountDetails(String prflAcountDetails) {
        this.prflAcountDetails = prflAcountDetails;
    }

    public LocalDateTime getPrflDob() {
        return prflDob;
    }

    public void setPrflDob(LocalDateTime prflDob) {
        this.prflDob = prflDob;
    }

    public String getPrflPhoneNo() {
        return prflPhoneNo;
    }

    public void setPrflPhoneNo(String prflPhoneNo) {
        this.prflPhoneNo = prflPhoneNo;
    }

    public String getPrflEmailAdrss() {
        return prflEmailAdrss;
    }

    public void setPrflEmailAdrss(String prflEmailAdrss) {
        this.prflEmailAdrss = prflEmailAdrss;
    }

    public String getPrflPrimaryPostalAdrss() {
        return prflPrimaryPostalAdrss;
    }

    public void setPrflPrimaryPostalAdrss(String prflPrimaryPostalAdrss) {
        this.prflPrimaryPostalAdrss = prflPrimaryPostalAdrss;
    }

    public String getPrflOthrAdrss() {
        return prflOthrAdrss;
    }

    public void setPrflOthrAdrss(String prflOthrAdrss) {
        this.prflOthrAdrss = prflOthrAdrss;
    }

    public Profile(CreateProfileRequest request, String profileId,String acoountDetails,String primaryAddress,String otherAddress) {
    	this.prflId =profileId;
        this.prflAcountDetails=acoountDetails;
        this.gender=request.getGender();
        this.prflPhoneNo = request.getContact().getMobileNumber();
        this.prflPrimaryPostalAdrss= primaryAddress;
        this.prflOthrAdrss= otherAddress;
        //this.prfl_access
        this.profileKind=request.getProfileKind();
        this.prflEmailAdrss = request.getContact().getEmailId();
        this.creat_usr_id =request.getHeader().getUserId();
        if(request.getProfileDob() != null) {
            this.prflDob = request.getProfileDob()
            		.toLocalDate().atStartOfDay();
        }
    }
   
}
