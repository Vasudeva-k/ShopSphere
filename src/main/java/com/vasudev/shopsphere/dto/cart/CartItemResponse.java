package com.vasudev.shopsphere.dto.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Shopping cart line item details")
public class CartItemResponse {

    @Schema(description = "Cart item unique identifier", example = "1")
    private Long id;

    @Schema(description = "Product unique identifier", example = "1")
    private Long productId;

    @Schema(description = "Product name", example = "Wireless Bluetooth Headphones")
    private String productName;

    @Schema(description = "Unit price of the product", example = "99.99")
    private BigDecimal unitPrice;

    @Schema(description = "Quantity of the product in cart", example = "2")
    private Integer quantity;

    @Schema(description = "Line item subtotal amount", example = "199.98")
    private BigDecimal subtotal;

    public CartItemResponse() {
    }

    public CartItemResponse(Long id, Long productId, String productName, BigDecimal unitPrice, Integer quantity, BigDecimal subtotal) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.subtotal = subtotal;
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

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
