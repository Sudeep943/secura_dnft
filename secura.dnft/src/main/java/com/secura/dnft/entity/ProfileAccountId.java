package com.secura.dnft.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileAccountId implements Serializable {
    private static final long serialVersionUID = 1L;

    private String profileId;
    private String apartmentId;
}
