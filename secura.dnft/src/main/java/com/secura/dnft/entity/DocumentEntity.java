package com.secura.dnft.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "secura_doc")
public class DocumentEntity {

	    @Column(name = "aprmt_id")
	    private String aprmtId;
	    
	    @Id
	    @Column(name = "document_id")
	    private String documentId;
	    
	    @Column(name = "document_type")
	    private String documentType;
	    
	    @Column(name = "document_data" , columnDefinition = "TEXT")
	    private String documentData;

	    @Column(name = "creat_ts")
	    @CreationTimestamp
	    private LocalDateTime creatTs;

	    @Column(name = "creat_usr_id")
	    private String creatUsrId;

	    @Column(name = "lst_updt_ts")
	    @UpdateTimestamp
	    private LocalDateTime lstUpdtTs;

	    @Column(name = "lst_updt_usr_id")
	    private String lstUpdtUsrId;

		public String getDocumentId() {
			return documentId;
		}

		public void setDocumentId(String documentId) {
			this.documentId = documentId;
		}

		public String getDocumentType() {
			return documentType;
		}

		public void setDocumentType(String documentType) {
			this.documentType = documentType;
		}

		public String getDocumentData() {
			return documentData;
		}

		public void setDocumentData(String documentData) {
			this.documentData = documentData;
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

		public LocalDateTime getLstUpdtTs() {
			return lstUpdtTs;
		}

		public void setLstUpdtTs(LocalDateTime lstUpdtTs) {
			this.lstUpdtTs = lstUpdtTs;
		}

		public String getLstUpdtUsrId() {
			return lstUpdtUsrId;
		}

		public void setLstUpdtUsrId(String lstUpdtUsrId) {
			this.lstUpdtUsrId = lstUpdtUsrId;
		}

		public String getAprmtId() {
			return aprmtId;
		}

		public void setAprmtId(String aprmtId) {
			this.aprmtId = aprmtId;
		}
	    
	    
}
