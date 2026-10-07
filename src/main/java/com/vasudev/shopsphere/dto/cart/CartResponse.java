package com.vasudev.shopsphere.dto.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Shopping cart details response")
public class CartResponse {

    @Schema(description = "Shopping cart unique identifier", example = "1")
    private Long id;

    @Schema(description = "User ID owning the cart", example = "1")
    private Long userId;

    @Schema(description = "List of items in the cart")
    private List<CartItemResponse> items = new ArrayList<>();

    @Schema(description = "Total cart price amount", example = "199.98")
    private BigDecimal total;

    public CartResponse() {
    }

    public CartResponse(Long id, Long userId, List<CartItemResponse> items, BigDecimal total) {
        this.id = id;
        this.userId = userId;
        this.items = items != null ? items : new ArrayList<>();
        this.total = total;
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

    public List<CartItemResponse> getItems() {
        return items;
    }

    public void setItems(List<CartItemResponse> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}
