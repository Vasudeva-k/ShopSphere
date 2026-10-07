package com.vasudev.shopsphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Product details response")
public class ProductResponse {

    @Schema(description = "Product unique identifier", example = "1")
    private Long id;

    @Schema(description = "Product name", example = "Wireless Bluetooth Headphones")
    private String name;

    @Schema(description = "Product description", example = "Noise-cancelling over-ear headphones with 30-hour battery life")
    private String description;

    @Schema(description = "Unit price", example = "99.99")
    private BigDecimal price;

    @Schema(description = "Available inventory stock quantity", example = "50")
    private Integer stockQuantity;

    @Schema(description = "Product availability/enabled status", example = "true")
    private Boolean enabled;

    @Schema(description = "Associated category identifier", example = "1")
    private Long categoryId;

    @Schema(description = "Product image URL", example = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e")
    private String imageUrl;

    @Schema(description = "Creation timestamp", example = "2026-10-04T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2026-10-04T10:00:00")
    private LocalDateTime updatedAt;

    public ProductResponse() {
    }

    public ProductResponse(Long id, String name, String description, BigDecimal price, Integer stockQuantity, Boolean enabled, Long categoryId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.enabled = enabled;
        this.categoryId = categoryId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public ProductResponse(Long id, String name, String description, BigDecimal price, Integer stockQuantity, Boolean enabled, Long categoryId, String imageUrl, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.enabled = enabled;
        this.categoryId = categoryId;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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
