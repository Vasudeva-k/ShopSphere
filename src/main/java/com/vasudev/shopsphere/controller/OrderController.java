package com.vasudev.shopsphere.controller;

import com.vasudev.shopsphere.dto.OrderItemResponse;
import com.vasudev.shopsphere.dto.OrderResponse;
import com.vasudev.shopsphere.entity.Order;
import com.vasudev.shopsphere.entity.OrderItem;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.exception.ResourceNotFoundException;
import com.vasudev.shopsphere.service.OrderService;
import com.vasudev.shopsphere.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order creation, retrieval, and cancellation")
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    public OrderController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "Create order", description = "Creates a new order from items currently in the user's cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Cart is empty or contains invalid items")
    })
    public ResponseEntity<OrderResponse> createOrder() {
        User user = getAuthenticatedUser();
        Order order = orderService.createOrderFromCart(user.getId());
        OrderResponse response = toResponse(order);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID", description = "Retrieves an order by its ID for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> getOrder(@Parameter(description = "Order ID", required = true) @PathVariable Long orderId) {
        User user = getAuthenticatedUser();
        Order order = orderService.getOrderByIdAndUser(orderId, user.getId());
        OrderResponse response = toResponse(order);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get user orders", description = "Retrieves a paginated list of orders placed by the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders page retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid page index or page size")
    })
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)") @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be less than zero");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }

        User user = getAuthenticatedUser();
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orderPage = orderService.getOrdersByUser(user.getId(), pageable);
        Page<OrderResponse> responsePage = orderPage.map(this::toResponse);
        return ResponseEntity.ok(responsePage);
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel order", description = "Cancels an order if it is in a cancellable status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Order cannot be cancelled in its current status"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> cancelOrder(@Parameter(description = "Order ID", required = true) @PathVariable Long orderId) {
        User user = getAuthenticatedUser();
        Order order = orderService.cancelOrder(user.getId(), orderId);
        OrderResponse response = toResponse(order);
        return ResponseEntity.ok(response);
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

    private OrderResponse toResponse(Order order) {
        List<OrderItem> items = orderService.getOrderItems(order);
        List<OrderItemResponse> itemResponses = (items != null)
                ? items.stream().map(this::toItemResponse).toList()
                : List.of();

        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemResponses
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        Long productId = (item.getProduct() != null) ? item.getProduct().getId() : null;
        return new OrderItemResponse(
                item.getId(),
                productId,
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal()
        );
    }
}
