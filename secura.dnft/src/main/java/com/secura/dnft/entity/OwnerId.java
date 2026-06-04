package com.secura.dnft.entity;

import java.io.Serializable;
import java.util.Objects;

public class OwnerId implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String ownerId;
    private String flatNo;
    private String aprmt_id;

    public OwnerId() {}

    public OwnerId(String ownerId, String flatNo, String apartmentId) {
        this.ownerId = ownerId;
        this.flatNo = flatNo;
        this.aprmt_id = apartmentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OwnerId that = (OwnerId) o;
        return Objects.equals(ownerId, that.ownerId) &&
               Objects.equals(flatNo, that.flatNo) &&
               Objects.equals(aprmt_id, that.aprmt_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerId, flatNo, aprmt_id);
    }
}