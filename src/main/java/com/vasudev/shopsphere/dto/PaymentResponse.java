package com.vasudev.shopsphere.dto;

import com.vasudev.shopsphere.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Payment details response")
public class PaymentResponse {

    @Schema(description = "Payment unique identifier", example = "1")
    private Long id;

    @Schema(description = "Associated order unique identifier", example = "1")
    private Long orderId;

    @Schema(description = "Payment monetary amount", example = "199.98")
    private BigDecimal amount;

    @Schema(description = "Current payment status", example = "COMPLETED")
    private PaymentStatus status;

    @Schema(description = "Gateway transaction reference identifier", example = "txn_1234567890abcdef")
    private String transactionId;

    @Schema(description = "Timestamp when payment was created", example = "2026-10-04T10:35:00")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when payment was last updated", example = "2026-10-04T10:35:00")
    private LocalDateTime updatedAt;

    public PaymentResponse() {
    }

    public PaymentResponse(
            Long id,
            Long orderId,
            BigDecimal amount,
            PaymentStatus status,
            String transactionId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.transactionId = transactionId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
