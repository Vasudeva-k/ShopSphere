package com.vasudev.shopsphere.dto;

import com.vasudev.shopsphere.entity.Invoice;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Invoice details response")
public class InvoiceResponse {

    @Schema(description = "Invoice unique identifier", example = "1")
    private Long id;

    @Schema(description = "Associated order identifier", example = "1")
    private Long orderId;

    @Schema(description = "Formatted unique invoice number", example = "INV-20261004-0001")
    private String invoiceNumber;

    @Schema(description = "Subtotal monetary amount", example = "199.98")
    private BigDecimal subtotal;

    @Schema(description = "Total invoiced monetary amount", example = "199.98")
    private BigDecimal totalAmount;

    @Schema(description = "Timestamp when invoice was issued", example = "2026-10-04T11:20:00")
    private LocalDateTime issuedAt;

    @Schema(description = "Timestamp when invoice record was created", example = "2026-10-04T11:20:00")
    private LocalDateTime createdAt;

    public InvoiceResponse() {
    }

    public InvoiceResponse(
            Long id,
            Long orderId,
            String invoiceNumber,
            BigDecimal subtotal,
            BigDecimal totalAmount,
            LocalDateTime issuedAt,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.orderId = orderId;
        this.invoiceNumber = invoiceNumber;
        this.subtotal = subtotal;
        this.totalAmount = totalAmount;
        this.issuedAt = issuedAt;
        this.createdAt = createdAt;
    }

    public static InvoiceResponse fromEntity(Invoice invoice) {
        if (invoice == null) {
            return null;
        }
        Long orderId = (invoice.getOrder() != null) ? invoice.getOrder().getId() : null;
        return new InvoiceResponse(
                invoice.getId(),
                orderId,
                invoice.getInvoiceNumber(),
                invoice.getSubtotal(),
                invoice.getTotalAmount(),
                invoice.getIssuedAt(),
                invoice.getCreatedAt()
        );
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

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
