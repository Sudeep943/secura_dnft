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

    public TenantId() {}

    public TenantId(String prflId, String flatNo, String status) {
        this.prflId = prflId;
        this.flatNo = flatNo;
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TenantId that = (TenantId) o;
        return Objects.equals(prflId, that.prflId) &&
               Objects.equals(flatNo, that.flatNo) &&
               Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prflId, flatNo, status);
    }
}