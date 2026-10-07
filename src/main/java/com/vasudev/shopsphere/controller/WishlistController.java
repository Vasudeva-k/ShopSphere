package com.vasudev.shopsphere.controller;

import com.vasudev.shopsphere.dto.WishlistItemResponse;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.entity.WishlistItem;
import com.vasudev.shopsphere.exception.ResourceNotFoundException;
import com.vasudev.shopsphere.service.UserService;
import com.vasudev.shopsphere.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishlist")
@Tag(name = "Wishlist", description = "User wishlist management operations")
public class WishlistController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "createdAt");

    private final WishlistService wishlistService;
    private final UserService userService;

    public WishlistController(WishlistService wishlistService, UserService userService) {
        this.wishlistService = wishlistService;
        this.userService = userService;
    }

    @PostMapping("/products/{productId}")
    @Operation(summary = "Add item to wishlist", description = "Adds a product to the authenticated user's wishlist")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Item added to wishlist successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "409", description = "Product is already in the wishlist")
    })
    public ResponseEntity<WishlistItemResponse> addItem(@Parameter(description = "Product ID", required = true) @PathVariable Long productId) {
        User user = getAuthenticatedUser();
        WishlistItem result = wishlistService.addItem(user.getId(), productId);
        return new ResponseEntity<>(WishlistItemResponse.fromEntity(result), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get wishlist", description = "Retrieves a paginated list of items in the authenticated user's wishlist")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wishlist page retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or sorting parameters")
    })
    public ResponseEntity<Page<WishlistItemResponse>> getWishlist(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field (id, createdAt)") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "desc") String direction
    ) {
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
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, effectiveSortBy));

        User user = getAuthenticatedUser();
        Page<WishlistItem> pageResult = wishlistService.getWishlist(user.getId(), pageable);
        Page<WishlistItemResponse> responsePage = pageResult.map(WishlistItemResponse::fromEntity);
        return ResponseEntity.ok(responsePage);
    }

    @GetMapping("/{wishlistItemId}")
    @Operation(summary = "Get wishlist item", description = "Retrieves a specific wishlist item by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wishlist item retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Wishlist item not found")
    })
    public ResponseEntity<WishlistItemResponse> getItem(@Parameter(description = "Wishlist item ID", required = true) @PathVariable Long wishlistItemId) {
        User user = getAuthenticatedUser();
        WishlistItem result = wishlistService.getItem(user.getId(), wishlistItemId);
        return ResponseEntity.ok(WishlistItemResponse.fromEntity(result));
    }

    @DeleteMapping("/{wishlistItemId}")
    @Operation(summary = "Remove wishlist item", description = "Removes a specific item from the user's wishlist")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Wishlist item removed successfully"),
            @ApiResponse(responseCode = "404", description = "Wishlist item not found")
    })
    public ResponseEntity<Void> removeItem(@Parameter(description = "Wishlist item ID", required = true) @PathVariable Long wishlistItemId) {
        User user = getAuthenticatedUser();
        wishlistService.removeItem(user.getId(), wishlistItemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/products/{productId}/exists")
    @Operation(summary = "Check product in wishlist", description = "Checks whether a specific product exists in the user's wishlist")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product wishlist check completed successfully")
    })
    public ResponseEntity<Map<String, Boolean>> isProductInWishlist(@Parameter(description = "Product ID", required = true) @PathVariable Long productId) {
        User user = getAuthenticatedUser();
        boolean exists = wishlistService.isProductInWishlist(user.getId(), productId);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @DeleteMapping
    @Operation(summary = "Clear wishlist", description = "Removes all items from the authenticated user's wishlist")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Wishlist cleared successfully")
    })
    public ResponseEntity<Void> clearWishlist() {
        User user = getAuthenticatedUser();
        wishlistService.clearWishlist(user.getId());
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
}
