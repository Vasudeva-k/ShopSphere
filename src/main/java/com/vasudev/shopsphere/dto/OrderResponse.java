package com.vasudev.shopsphere.dto;

import com.vasudev.shopsphere.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Order details response")
public class OrderResponse {

    @Schema(description = "Order unique identifier", example = "1")
    private Long id;

    @Schema(description = "User ID who placed the order", example = "1")
    private Long userId;

    @Schema(description = "Current order lifecycle status", example = "PENDING")
    private OrderStatus status;

    @Schema(description = "Total monetary amount for the order", example = "199.98")
    private BigDecimal totalAmount;

    @Schema(description = "Timestamp when order was created", example = "2026-10-04T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when order was last updated", example = "2026-10-04T10:30:00")
    private LocalDateTime updatedAt;

    @Schema(description = "List of order line items")
    private List<OrderItemResponse> items = new ArrayList<>();

    public OrderResponse() {
    }

    public OrderResponse(
            Long id,
            Long userId,
            OrderStatus status,
            BigDecimal totalAmount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<OrderItemResponse> items
    ) {
        this.id = id;
        this.userId = userId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.items = items != null ? items : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public void setItems(List<OrderItemResponse> items) {
        this.items = items != null ? items : new ArrayList<>();
    }
}
