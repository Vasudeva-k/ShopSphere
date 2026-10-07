package com.vasudev.shopsphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payment processing request payload")
public class PaymentProcessRequest {

    @Schema(description = "Flag to simulate successful (true) or failed (false) payment gateway transaction", example = "true")
    private boolean simulateSuccess;

    public PaymentProcessRequest() {
    }

    public PaymentProcessRequest(boolean simulateSuccess) {
        this.simulateSuccess = simulateSuccess;
    }

    public boolean isSimulateSuccess() {
        return simulateSuccess;
    }

    public void setSimulateSuccess(boolean simulateSuccess) {
        this.simulateSuccess = simulateSuccess;
    }
}
