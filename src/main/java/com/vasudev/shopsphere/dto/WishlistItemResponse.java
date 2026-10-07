package com.vasudev.shopsphere.dto;

import com.vasudev.shopsphere.entity.WishlistItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Wishlist item details response")
public class WishlistItemResponse {

    @Schema(description = "Wishlist item unique identifier", example = "1")
    private Long id;

    @Schema(description = "Product unique identifier", example = "1")
    private Long productId;

    @Schema(description = "Product name", example = "Wireless Bluetooth Headphones")
    private String productName;

    @Schema(description = "Current product price", example = "99.99")
    private BigDecimal productPrice;

    @Schema(description = "Product enabled/availability status", example = "true")
    private Boolean productEnabled;

    @Schema(description = "Timestamp when item was added to wishlist", example = "2026-10-04T11:15:00")
    private LocalDateTime createdAt;

    public WishlistItemResponse() {
    }

    public WishlistItemResponse(
            Long id,
            Long productId,
            String productName,
            BigDecimal productPrice,
            Boolean productEnabled,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.productEnabled = productEnabled;
        this.createdAt = createdAt;
    }

    public static WishlistItemResponse fromEntity(WishlistItem item) {
        if (item == null) {
            return null;
        }
        Long productId = (item.getProduct() != null) ? item.getProduct().getId() : null;
        String productName = (item.getProduct() != null) ? item.getProduct().getName() : null;
        BigDecimal productPrice = (item.getProduct() != null) ? item.getProduct().getPrice() : null;
        Boolean productEnabled = (item.getProduct() != null) ? item.getProduct().getEnabled() : null;

        return new WishlistItemResponse(
                item.getId(),
                productId,
                productName,
                productPrice,
                productEnabled,
                item.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(BigDecimal productPrice) {
        this.productPrice = productPrice;
    }

    public Boolean getProductEnabled() {
        return productEnabled;
    }

    public void setProductEnabled(Boolean productEnabled) {
        this.productEnabled = productEnabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
