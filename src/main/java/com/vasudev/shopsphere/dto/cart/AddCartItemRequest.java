package com.vasudev.shopsphere.dto.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload to add an item to shopping cart")
public class AddCartItemRequest {

    @NotNull(message = "Product ID is required")
    @Schema(description = "Product ID to add to cart", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Quantity of product (minimum 1)", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;

    public AddCartItemRequest() {
    }

    public AddCartItemRequest(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
