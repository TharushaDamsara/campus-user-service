package com.campusflow.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public class UpdateStatusRequest {

    @NotBlank(message = "Status is required")
    private String status;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
