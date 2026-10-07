package com.vasudev.shopsphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Product update request payload")
public class ProductUpdateRequest {

    @Size(max = 200, message = "Product name must not exceed 200 characters")
    @Schema(description = "Updated product name", example = "Wireless Bluetooth Headphones Pro")
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    @Schema(description = "Updated product description", example = "Upgraded noise-cancelling headphones with 40-hour battery life")
    private String description;

    @DecimalMin(value = "0.00", inclusive = true, message = "Price must be greater than or equal to 0")
    @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 integer digits and 2 decimal places")
    @Schema(description = "Updated unit price (>= 0.00)", example = "119.99")
    private BigDecimal price;

    @Min(value = 0, message = "Stock quantity must be greater than or equal to 0")
    @Schema(description = "Updated available stock quantity", example = "45")
    private Integer stockQuantity;

    @Schema(description = "Updated enabled/availability status", example = "true")
    private Boolean enabled;

    @Schema(description = "Updated category identifier", example = "1")
    private Long categoryId;

    @Size(max = 1000, message = "Image URL must not exceed 1000 characters")
    @Schema(description = "Updated product image URL", example = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e")
    private String imageUrl;

    public ProductUpdateRequest() {
    }

    public ProductUpdateRequest(String name, String description, BigDecimal price, Integer stockQuantity, Boolean enabled, Long categoryId) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.enabled = enabled;
        this.categoryId = categoryId;
    }

    public ProductUpdateRequest(String name, String description, BigDecimal price, Integer stockQuantity, Boolean enabled, Long categoryId, String imageUrl) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.enabled = enabled;
        this.categoryId = categoryId;
        this.imageUrl = imageUrl;
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
}
