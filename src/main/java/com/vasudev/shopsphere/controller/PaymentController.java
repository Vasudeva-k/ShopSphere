package com.vasudev.shopsphere.controller;

import com.vasudev.shopsphere.dto.PaymentProcessRequest;
import com.vasudev.shopsphere.dto.PaymentResponse;
import com.vasudev.shopsphere.entity.Payment;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.exception.ResourceNotFoundException;
import com.vasudev.shopsphere.service.PaymentService;
import com.vasudev.shopsphere.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Payment creation, processing, and refund operations")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    public PaymentController(PaymentService paymentService, UserService userService) {
        this.paymentService = paymentService;
        this.userService = userService;
    }

    @PostMapping("/orders/{orderId}")
    @Operation(summary = "Create payment", description = "Initiates a new payment for a specified order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Payment initiated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid order state for payment"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<PaymentResponse> createPayment(@Parameter(description = "Order ID", required = true) @PathVariable Long orderId) {
        User user = getAuthenticatedUser();
        Payment payment = paymentService.createPayment(user.getId(), orderId);
        PaymentResponse response = toResponse(payment);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/{paymentId}/process")
    @Operation(summary = "Process payment", description = "Processes an existing pending payment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment processed successfully"),
            @ApiResponse(responseCode = "400", description = "Payment cannot be processed in current state"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<PaymentResponse> processPayment(
            @Parameter(description = "Payment ID", required = true) @PathVariable Long paymentId,
            @RequestBody PaymentProcessRequest request
    ) {
        User user = getAuthenticatedUser();
        boolean simulateSuccess = request != null && request.isSimulateSuccess();
        Payment payment = paymentService.processPayment(user.getId(), paymentId, simulateSuccess);
        PaymentResponse response = toResponse(payment);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "Refund payment", description = "Issues a refund for a completed payment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment refunded successfully"),
            @ApiResponse(responseCode = "400", description = "Payment cannot be refunded in current state"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<PaymentResponse> refundPayment(@Parameter(description = "Payment ID", required = true) @PathVariable Long paymentId) {
        User user = getAuthenticatedUser();
        Payment payment = paymentService.refundPayment(user.getId(), paymentId);
        PaymentResponse response = toResponse(payment);
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

    private PaymentResponse toResponse(Payment payment) {
        Long orderId = (payment.getOrder() != null) ? payment.getOrder().getId() : null;
        return new PaymentResponse(
                payment.getId(),
                orderId,
                payment.getAmount(),
                payment.getStatus(),
                payment.getTransactionId(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
