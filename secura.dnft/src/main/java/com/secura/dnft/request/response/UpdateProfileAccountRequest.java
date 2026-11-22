package com.secura.dnft.request.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UpdateProfileAccountRequest extends GetProfileAccountRequest {
    private String position;
    private String role;
    private String jobeposition;
    private String flatId;
    private String status;
}
