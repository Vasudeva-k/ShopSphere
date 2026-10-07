package com.vasudev.shopsphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Invoice email delivery request payload")
public class InvoiceEmailRequest {

    @NotBlank(message = "Email address is required")
    @Email(message = "Email address must be valid")
    @Schema(description = "Recipient email address for invoice PDF delivery", example = "customer@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    public InvoiceEmailRequest() {
    }

    public InvoiceEmailRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
