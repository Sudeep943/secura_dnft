package com.secura.dnft.request.response;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GetProfileAccountRequest {
    @NotBlank
    private String profileId;

    @NotBlank
    private String apartmentId;
}
