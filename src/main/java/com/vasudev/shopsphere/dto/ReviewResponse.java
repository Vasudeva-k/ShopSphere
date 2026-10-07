package com.vasudev.shopsphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Product review details response")
public class ReviewResponse {

    @Schema(description = "Review unique identifier", example = "1")
    private Long id;

    @Schema(description = "User unique identifier who created the review", example = "1")
    private Long userId;

    @Schema(description = "Product unique identifier being reviewed", example = "1")
    private Long productId;

    @Schema(description = "Rating score (1 to 5)", example = "5")
    private Integer rating;

    @Schema(description = "Customer review comment", example = "Exceptional sound clarity and extremely comfortable fit!")
    private String comment;

    @Schema(description = "Timestamp when review was submitted", example = "2026-10-04T11:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when review was last updated", example = "2026-10-04T11:00:00")
    private LocalDateTime updatedAt;

    public ReviewResponse() {
    }

    public ReviewResponse(
            Long id,
            Long userId,
            Long productId,
            Integer rating,
            String comment,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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
