package com.vasudev.shopsphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Product review update request payload")
public class ReviewUpdateRequest {

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    @Schema(description = "Updated product rating score (1 to 5)", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer rating;

    @Size(max = 2000, message = "Comment must not exceed 2000 characters")
    @Schema(description = "Updated customer review comments and feedback", example = "Great sound quality, though the battery life is slightly shorter than advertised.")
    private String comment;

    public ReviewUpdateRequest() {
    }

    public ReviewUpdateRequest(Integer rating, String comment) {
        this.rating = rating;
        this.comment = comment;
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
}
