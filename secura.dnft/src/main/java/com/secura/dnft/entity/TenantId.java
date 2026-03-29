package com.secura.dnft.entity;

import java.io.Serializable;
import java.util.Objects;

public class TenantId implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String prflId;
    private String flatNo;
    private String status;
    private String aprmt_id;

    public TenantId() {}

    public TenantId(String prflId, String flatNo, String status,String apartmentId) {
        this.prflId = prflId;
        this.flatNo = flatNo;
        this.status = status;
        this.aprmt_id = apartmentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TenantId that = (TenantId) o;
        return Objects.equals(prflId, that.prflId) &&
               Objects.equals(flatNo, that.flatNo) &&
               Objects.equals(status, that.status) && 
               Objects.equals(aprmt_id, that.aprmt_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prflId, flatNo, status,aprmt_id);
    }
}