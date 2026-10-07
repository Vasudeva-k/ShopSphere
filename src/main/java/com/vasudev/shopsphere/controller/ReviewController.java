package com.vasudev.shopsphere.controller;

import com.vasudev.shopsphere.dto.ReviewCreateRequest;
import com.vasudev.shopsphere.dto.ReviewResponse;
import com.vasudev.shopsphere.dto.ReviewUpdateRequest;
import com.vasudev.shopsphere.entity.Review;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.exception.ResourceNotFoundException;
import com.vasudev.shopsphere.service.ReviewService;
import com.vasudev.shopsphere.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Reviews", description = "Product review and rating operations")
public class ReviewController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "rating", "createdAt", "updatedAt");

    private final ReviewService reviewService;
    private final UserService userService;

    public ReviewController(ReviewService reviewService, UserService userService) {
        this.reviewService = reviewService;
        this.userService = userService;
    }

    @PostMapping("/products/{productId}")
    @Operation(summary = "Create review", description = "Creates a product review and rating from the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Review created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid review request data"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ReviewResponse> createReview(
            @Parameter(description = "Product ID", required = true) @PathVariable Long productId,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        User user = getAuthenticatedUser();
        Review review = reviewService.createReview(user.getId(), productId, request.getRating(), request.getComment());
        ReviewResponse response = toResponse(review);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{reviewId}")
    @Operation(summary = "Get review by ID", description = "Retrieves a specific review by review ID for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Review not found")
    })
    public ResponseEntity<ReviewResponse> getReviewById(@Parameter(description = "Review ID", required = true) @PathVariable Long reviewId) {
        User user = getAuthenticatedUser();
        Review review = reviewService.getReviewById(user.getId(), reviewId);
        ReviewResponse response = toResponse(review);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/products/{productId}")
    @Operation(summary = "Get reviews by product", description = "Retrieves a paginated list of reviews for a specific product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reviews page retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or sorting parameters"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<Page<ReviewResponse>> getReviewsByProduct(
            @Parameter(description = "Product ID", required = true) @PathVariable Long productId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field (id, rating, createdAt, updatedAt)") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = createPageable(page, size, sortBy, direction);
        Page<Review> reviewPage = reviewService.getReviewsByProduct(productId, pageable);
        Page<ReviewResponse> responsePage = reviewPage.map(this::toResponse);
        return ResponseEntity.ok(responsePage);
    }

    @GetMapping("/my")
    @Operation(summary = "Get my reviews", description = "Retrieves a paginated list of reviews created by the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reviews page retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or sorting parameters")
    })
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field (id, rating, createdAt, updatedAt)") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "desc") String direction
    ) {
        User user = getAuthenticatedUser();
        Pageable pageable = createPageable(page, size, sortBy, direction);
        Page<Review> reviewPage = reviewService.getReviewsByUser(user.getId(), pageable);
        Page<ReviewResponse> responsePage = reviewPage.map(this::toResponse);
        return ResponseEntity.ok(responsePage);
    }

    @PutMapping("/{reviewId}")
    @Operation(summary = "Update review", description = "Updates an existing review's rating and comment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid review update data"),
            @ApiResponse(responseCode = "404", description = "Review not found")
    })
    public ResponseEntity<ReviewResponse> updateReview(
            @Parameter(description = "Review ID", required = true) @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request
    ) {
        User user = getAuthenticatedUser();
        Review review = reviewService.updateReview(user.getId(), reviewId, request.getRating(), request.getComment());
        ReviewResponse response = toResponse(review);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "Delete review", description = "Deletes a review created by the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Review deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Review not found")
    })
    public ResponseEntity<Void> deleteReview(@Parameter(description = "Review ID", required = true) @PathVariable Long reviewId) {
        User user = getAuthenticatedUser();
        reviewService.deleteReview(user.getId(), reviewId);
        return ResponseEntity.noContent().build();
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("Authenticated user not found");
        }

        String email = authentication.getName();
        return userService.getUserByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private ReviewResponse toResponse(Review review) {
        Long userId = (review.getUser() != null) ? review.getUser().getId() : null;
        Long productId = (review.getProduct() != null) ? review.getProduct().getId() : null;
        return new ReviewResponse(
                review.getId(),
                userId,
                productId,
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }

    private Pageable createPageable(int page, int size, String sortBy, String direction) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be less than zero");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }

        String effectiveSortBy = (sortBy != null && !sortBy.isBlank()) ? sortBy : "createdAt";
        if (!ALLOWED_SORT_FIELDS.contains(effectiveSortBy)) {
            throw new IllegalArgumentException("Invalid sort field: " + sortBy);
        }

        String effectiveDirection = (direction != null && !direction.isBlank()) ? direction : "desc";
        if (!"asc".equalsIgnoreCase(effectiveDirection) && !"desc".equalsIgnoreCase(effectiveDirection)) {
            throw new IllegalArgumentException("Invalid sort direction: " + direction);
        }

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(effectiveDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(sortDirection, effectiveSortBy));
    }
}
