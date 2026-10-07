package com.vasudev.shopsphere.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vasudev.shopsphere.config.SecurityConfig;
import com.vasudev.shopsphere.dto.PaymentProcessRequest;
import com.vasudev.shopsphere.entity.Order;
import com.vasudev.shopsphere.entity.OrderStatus;
import com.vasudev.shopsphere.entity.Payment;
import com.vasudev.shopsphere.entity.PaymentStatus;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.exception.ResourceNotFoundException;
import com.vasudev.shopsphere.security.JwtAuthenticationFilter;
import com.vasudev.shopsphere.security.JwtService;
import com.vasudev.shopsphere.service.PaymentService;
import com.vasudev.shopsphere.service.UserService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PaymentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "jwt.secret=ShopSphere-test-secret-key-2026-very-long-and-secure",
        "jwt.expiration=3600000"
})
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private UserService userService;

    private User testUser;
    private String validToken;
    private Order testOrder;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        testUser = new User("Alice", "Smith", "alice@example.com", "Password123", "1234567890");
        testUser.setId(1L);
        testUser.setEnabled(true);

        when(userService.getUserByEmail("alice@example.com")).thenReturn(Optional.of(testUser));
        validToken = jwtService.generateToken(testUser);

        testOrder = new Order(testUser, new BigDecimal("1299.99"), OrderStatus.PENDING);
        testOrder.setId(50L);

        testPayment = new Payment(testOrder, new BigDecimal("1299.99"));
        testPayment.setId(100L);
        testPayment.setCreatedAt(LocalDateTime.now());
        testPayment.setUpdatedAt(LocalDateTime.now());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // 1. Authenticated create payment -> 201
    @Test
    void createPayment_whenAuthenticated_returns201() throws Exception {
        when(paymentService.createPayment(1L, 50L)).thenReturn(testPayment);

        mockMvc.perform(post("/api/payments/orders/50")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.orderId").value(50L))
                .andExpect(jsonPath("$.amount").value(1299.99))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    // 2. Correct authenticated user ID and order ID passed to service
    @Test
    void createPayment_passesCorrectUserIdAndOrderIdToService() throws Exception {
        when(paymentService.createPayment(1L, 50L)).thenReturn(testPayment);

        mockMvc.perform(post("/api/payments/orders/50")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isCreated());

        verify(paymentService).createPayment(1L, 50L);
    }

    // 3. Response contains payment information
    @Test
    void createPayment_responseContainsPaymentDetails() throws Exception {
        when(paymentService.createPayment(1L, 50L)).thenReturn(testPayment);

        mockMvc.perform(post("/api/payments/orders/50")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.orderId").value(50L))
                .andExpect(jsonPath("$.amount").value(1299.99))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    // 4. ResourceNotFoundException -> 404
    @Test
    void createPayment_whenOrderNotFound_returns404() throws Exception {
        when(paymentService.createPayment(1L, 999L))
                .thenThrow(new ResourceNotFoundException("Order not found with id: 999"));

        mockMvc.perform(post("/api/payments/orders/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found with id: 999"));
    }

    // 5. IllegalArgumentException -> 400
    @Test
    void createPayment_whenPaymentAlreadyExists_returns400() throws Exception {
        when(paymentService.createPayment(1L, 50L))
                .thenThrow(new IllegalArgumentException("Payment already exists for order id: 50"));

        mockMvc.perform(post("/api/payments/orders/50")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Payment already exists for order id: 50"));
    }

    // 6. Unauthenticated request -> 403
    @Test
    void createPayment_whenUnauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/payments/orders/50"))
                .andExpect(status().isForbidden());
    }

    // 7. Successful simulated payment -> 200
    @Test
    void processPayment_whenSuccessful_returns200() throws Exception {
        Payment successfulPayment = new Payment(testOrder, new BigDecimal("1299.99"));
        successfulPayment.setId(100L);
        successfulPayment.setStatus(PaymentStatus.SUCCESS);
        successfulPayment.setTransactionId("TXN-abc-123");
        successfulPayment.setCreatedAt(LocalDateTime.now());
        successfulPayment.setUpdatedAt(LocalDateTime.now());

        when(paymentService.processPayment(1L, 100L, true)).thenReturn(successfulPayment);

        PaymentProcessRequest request = new PaymentProcessRequest(true);

        mockMvc.perform(post("/api/payments/100/process")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.transactionId").value("TXN-abc-123"));
    }

    // 8. simulateSuccess=true is passed correctly
    @Test
    void processPayment_passesSimulateSuccessTrueCorrectly() throws Exception {
        when(paymentService.processPayment(1L, 100L, true)).thenReturn(testPayment);

        PaymentProcessRequest request = new PaymentProcessRequest(true);

        mockMvc.perform(post("/api/payments/100/process")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(paymentService).processPayment(1L, 100L, true);
    }

    // 9. simulateSuccess=false is passed correctly
    @Test
    void processPayment_passesSimulateSuccessFalseCorrectly() throws Exception {
        Payment failedPayment = new Payment(testOrder, new BigDecimal("1299.99"));
        failedPayment.setId(100L);
        failedPayment.setStatus(PaymentStatus.FAILED);
        failedPayment.setCreatedAt(LocalDateTime.now());
        failedPayment.setUpdatedAt(LocalDateTime.now());

        when(paymentService.processPayment(1L, 100L, false)).thenReturn(failedPayment);

        PaymentProcessRequest request = new PaymentProcessRequest(false);

        mockMvc.perform(post("/api/payments/100/process")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));

        verify(paymentService).processPayment(1L, 100L, false);
    }

    // 10. Response contains updated payment status/transaction ID
    @Test
    void processPayment_responseContainsUpdatedStatusAndTransactionId() throws Exception {
        Payment processedPayment = new Payment(testOrder, new BigDecimal("1299.99"));
        processedPayment.setId(100L);
        processedPayment.setStatus(PaymentStatus.SUCCESS);
        processedPayment.setTransactionId("TXN-unique-id-999");
        processedPayment.setCreatedAt(LocalDateTime.now());
        processedPayment.setUpdatedAt(LocalDateTime.now());

        when(paymentService.processPayment(1L, 100L, true)).thenReturn(processedPayment);

        PaymentProcessRequest request = new PaymentProcessRequest(true);

        mockMvc.perform(post("/api/payments/100/process")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.transactionId").value("TXN-unique-id-999"));
    }

    // 11. ResourceNotFoundException -> 404
    @Test
    void processPayment_whenNotFound_returns404() throws Exception {
        when(paymentService.processPayment(1L, 999L, true))
                .thenThrow(new ResourceNotFoundException("Payment not found with id: 999"));

        PaymentProcessRequest request = new PaymentProcessRequest(true);

        mockMvc.perform(post("/api/payments/999/process")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Payment not found with id: 999"));
    }

    // 12. IllegalArgumentException -> 400
    @Test
    void processPayment_whenInvalidStatus_returns400() throws Exception {
        when(paymentService.processPayment(1L, 100L, true))
                .thenThrow(new IllegalArgumentException("Cannot process payment with status: SUCCESS"));

        PaymentProcessRequest request = new PaymentProcessRequest(true);

        mockMvc.perform(post("/api/payments/100/process")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot process payment with status: SUCCESS"));
    }

    // 13. Unauthenticated request -> 403
    @Test
    void processPayment_whenUnauthenticated_returns403() throws Exception {
        PaymentProcessRequest request = new PaymentProcessRequest(true);

        mockMvc.perform(post("/api/payments/100/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // 14. Successful refund -> 200
    @Test
    void refundPayment_whenSuccessful_returns200() throws Exception {
        Payment refundedPayment = new Payment(testOrder, new BigDecimal("1299.99"));
        refundedPayment.setId(100L);
        refundedPayment.setStatus(PaymentStatus.REFUNDED);
        refundedPayment.setTransactionId("TXN-abc-123");
        refundedPayment.setCreatedAt(LocalDateTime.now());
        refundedPayment.setUpdatedAt(LocalDateTime.now());

        when(paymentService.refundPayment(1L, 100L)).thenReturn(refundedPayment);

        mockMvc.perform(post("/api/payments/100/refund")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.transactionId").value("TXN-abc-123"));
    }

    // 15. Correct authenticated user ID and payment ID passed
    @Test
    void refundPayment_passesCorrectUserIdAndPaymentId() throws Exception {
        Payment refundedPayment = new Payment(testOrder, new BigDecimal("1299.99"));
        refundedPayment.setId(100L);
        refundedPayment.setStatus(PaymentStatus.REFUNDED);

        when(paymentService.refundPayment(1L, 100L)).thenReturn(refundedPayment);

        mockMvc.perform(post("/api/payments/100/refund")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        verify(paymentService).refundPayment(1L, 100L);
    }

    // 16. Response contains REFUNDED status
    @Test
    void refundPayment_responseContainsRefundedStatus() throws Exception {
        Payment refundedPayment = new Payment(testOrder, new BigDecimal("1299.99"));
        refundedPayment.setId(100L);
        refundedPayment.setStatus(PaymentStatus.REFUNDED);

        when(paymentService.refundPayment(1L, 100L)).thenReturn(refundedPayment);

        mockMvc.perform(post("/api/payments/100/refund")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    // 17. ResourceNotFoundException -> 404
    @Test
    void refundPayment_whenNotFound_returns404() throws Exception {
        when(paymentService.refundPayment(1L, 999L))
                .thenThrow(new ResourceNotFoundException("Payment not found with id: 999"));

        mockMvc.perform(post("/api/payments/999/refund")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Payment not found with id: 999"));
    }

    // 18. IllegalArgumentException -> 400
    @Test
    void refundPayment_whenInvalidStatus_returns400() throws Exception {
        when(paymentService.refundPayment(1L, 100L))
                .thenThrow(new IllegalArgumentException("Cannot refund payment with status: PENDING"));

        mockMvc.perform(post("/api/payments/100/refund")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot refund payment with status: PENDING"));
    }

    // 19. Unauthenticated request -> 403
    @Test
    void refundPayment_whenUnauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/payments/100/refund"))
                .andExpect(status().isForbidden());
    }
}
