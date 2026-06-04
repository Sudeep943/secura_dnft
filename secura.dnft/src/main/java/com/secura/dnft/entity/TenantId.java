package com.secura.dnft.entity;

import java.io.Serializable;
import java.util.Objects;

public class TenantId implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String tenantId;
    private String flatNo;
    private String aprmt_id;

    public TenantId() {}

    public TenantId(String tenantId, String flatNo, String apartmentId) {
        this.tenantId = tenantId;
        this.flatNo = flatNo;
        this.aprmt_id = apartmentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TenantId that = (TenantId) o;
        return Objects.equals(tenantId, that.tenantId) &&
               Objects.equals(flatNo, that.flatNo) &&
               Objects.equals(aprmt_id, that.aprmt_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, flatNo, aprmt_id);
    }
}