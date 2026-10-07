package com.vasudev.shopsphere.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vasudev.shopsphere.config.SecurityConfig;
import com.vasudev.shopsphere.entity.Order;
import com.vasudev.shopsphere.entity.OrderItem;
import com.vasudev.shopsphere.entity.OrderStatus;
import com.vasudev.shopsphere.entity.Product;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.exception.ResourceNotFoundException;
import com.vasudev.shopsphere.security.JwtAuthenticationFilter;
import com.vasudev.shopsphere.security.JwtService;
import com.vasudev.shopsphere.service.OrderService;
import com.vasudev.shopsphere.service.UserService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = OrderController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "jwt.secret=ShopSphere-test-secret-key-2026-very-long-and-secure",
        "jwt.expiration=3600000"
})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private UserService userService;

    private User testUser;
    private String validToken;
    private Product testProduct;
    private Order testOrder;
    private OrderItem testOrderItem;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        testUser = new User("Alice", "Smith", "alice@example.com", "Password123", "1234567890");
        testUser.setId(1L);
        testUser.setEnabled(true);

        when(userService.getUserByEmail("alice@example.com")).thenReturn(Optional.of(testUser));
        validToken = jwtService.generateToken(testUser);

        testProduct = new Product("Laptop", "Tech laptop", new BigDecimal("1299.99"), 10, null);
        testProduct.setId(101L);

        testOrder = new Order(testUser, new BigDecimal("1299.99"), OrderStatus.PENDING);
        testOrder.setId(50L);
        testOrder.setCreatedAt(LocalDateTime.now());
        testOrder.setUpdatedAt(LocalDateTime.now());

        testOrderItem = new OrderItem(
                testOrder,
                testProduct,
                "Laptop",
                new BigDecimal("1299.99"),
                1,
                new BigDecimal("1299.99")
        );
        testOrderItem.setId(201L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // 1. POST /api/orders: Authenticated user successfully creates order -> 201
    @Test
    void createOrder_whenAuthenticated_returns201() throws Exception {
        when(orderService.createOrderFromCart(1L)).thenReturn(testOrder);
        when(orderService.getOrderItems(testOrder)).thenReturn(List.of(testOrderItem));

        mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50L))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    // 2. Correct authenticated user ID is passed to OrderService
    @Test
    void createOrder_passesAuthenticatedUserIdToService() throws Exception {
        when(orderService.createOrderFromCart(1L)).thenReturn(testOrder);
        when(orderService.getOrderItems(testOrder)).thenReturn(List.of(testOrderItem));

        mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isCreated());

        verify(orderService).createOrderFromCart(1L);
    }

    // 3. Response contains order details and item snapshots
    @Test
    void createOrder_responseContainsOrderDetailsAndItemSnapshots() throws Exception {
        when(orderService.createOrderFromCart(1L)).thenReturn(testOrder);
        when(orderService.getOrderItems(testOrder)).thenReturn(List.of(testOrderItem));

        mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50L))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.totalAmount").value(1299.99))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(201L))
                .andExpect(jsonPath("$.items[0].productId").value(101L))
                .andExpect(jsonPath("$.items[0].productName").value("Laptop"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(1299.99))
                .andExpect(jsonPath("$.items[0].quantity").value(1))
                .andExpect(jsonPath("$.items[0].subtotal").value(1299.99));
    }

    // 4. POST /api/orders: ResourceNotFoundException -> 404
    @Test
    void createOrder_whenResourceNotFound_returns404() throws Exception {
        when(orderService.createOrderFromCart(1L))
                .thenThrow(new ResourceNotFoundException("User not found with id: 1"));

        mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: 1"));
    }

    // 5. POST /api/orders: IllegalArgumentException -> 400
    @Test
    void createOrder_whenCartEmpty_returns400() throws Exception {
        when(orderService.createOrderFromCart(1L))
                .thenThrow(new IllegalArgumentException("Cannot create order from an empty cart"));

        mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot create order from an empty cart"));
    }

    // 6. POST /api/orders: Unauthenticated request -> 403
    @Test
    void createOrder_whenUnauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/orders"))
                .andExpect(status().isForbidden());
    }

    // 7. GET /api/orders/{orderId}: Authenticated owner retrieves order -> 200
    @Test
    void getOrder_whenAuthenticatedOwner_returns200() throws Exception {
        when(orderService.getOrderByIdAndUser(50L, 1L)).thenReturn(testOrder);
        when(orderService.getOrderItems(testOrder)).thenReturn(List.of(testOrderItem));

        mockMvc.perform(get("/api/orders/50")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50L))
                .andExpect(jsonPath("$.userId").value(1L));
    }

    // 8. Correct user ID and order ID are passed to service
    @Test
    void getOrder_passesCorrectUserIdAndOrderIdToService() throws Exception {
        when(orderService.getOrderByIdAndUser(50L, 1L)).thenReturn(testOrder);
        when(orderService.getOrderItems(testOrder)).thenReturn(List.of(testOrderItem));

        mockMvc.perform(get("/api/orders/50")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        verify(orderService).getOrderByIdAndUser(50L, 1L);
    }

    // 9. Response contains order items
    @Test
    void getOrder_responseContainsOrderItems() throws Exception {
        when(orderService.getOrderByIdAndUser(50L, 1L)).thenReturn(testOrder);
        when(orderService.getOrderItems(testOrder)).thenReturn(List.of(testOrderItem));

        mockMvc.perform(get("/api/orders/50")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productName").value("Laptop"));
    }

    // 10. GET /api/orders/{orderId}: ResourceNotFoundException -> 404
    @Test
    void getOrder_whenNotFound_returns404() throws Exception {
        when(orderService.getOrderByIdAndUser(999L, 1L))
                .thenThrow(new ResourceNotFoundException("Order not found with id: 999"));

        mockMvc.perform(get("/api/orders/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found with id: 999"));
    }

    // 11. GET /api/orders/{orderId}: Unauthenticated request -> 403
    @Test
    void getOrder_whenUnauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/orders/50"))
                .andExpect(status().isForbidden());
    }

    // 12. GET /api/orders: Successfully returns paginated orders -> 200
    @Test
    void getMyOrders_whenAuthenticated_returns200AndPage() throws Exception {
        Page<Order> orderPage = new PageImpl<>(List.of(testOrder), PageRequest.of(0, 20), 1);
        when(orderService.getOrdersByUser(eq(1L), any(Pageable.class))).thenReturn(orderPage);
        when(orderService.getOrderItems(testOrder)).thenReturn(List.of(testOrderItem));

        mockMvc.perform(get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(50L))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // 13. Correct authenticated user ID is passed
    @Test
    void getMyOrders_passesCorrectAuthenticatedUserId() throws Exception {
        Page<Order> orderPage = new PageImpl<>(List.of(testOrder), PageRequest.of(0, 20), 1);
        when(orderService.getOrdersByUser(eq(1L), any(Pageable.class))).thenReturn(orderPage);
        when(orderService.getOrderItems(testOrder)).thenReturn(List.of(testOrderItem));

        mockMvc.perform(get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        verify(orderService).getOrdersByUser(eq(1L), any(Pageable.class));
    }

    // 14. Page and size are forwarded correctly
    @Test
    void getMyOrders_forwardsPageAndSizeCorrectly() throws Exception {
        Page<Order> emptyPage = new PageImpl<>(List.of(), PageRequest.of(2, 10), 0);
        when(orderService.getOrdersByUser(eq(1L), any(Pageable.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/api/orders?page=2&size=10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderService).getOrdersByUser(eq(1L), captor.capture());

        Pageable captured = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(2, captured.getPageNumber());
        org.junit.jupiter.api.Assertions.assertEquals(10, captured.getPageSize());
    }

    // 15. Invalid page/size -> 400
    @Test
    void getMyOrders_whenInvalidPageOrSize_returns400() throws Exception {
        mockMvc.perform(get("/api/orders?page=-1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Page index must not be less than zero"));

        mockMvc.perform(get("/api/orders?size=0")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Page size must be between 1 and 100"));

        mockMvc.perform(get("/api/orders?size=101")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Page size must be between 1 and 100"));
    }

    // 16. GET /api/orders: ResourceNotFoundException -> 404
    @Test
    void getMyOrders_whenUserNotFound_returns404() throws Exception {
        when(orderService.getOrdersByUser(eq(1L), any(Pageable.class)))
                .thenThrow(new ResourceNotFoundException("User not found with id: 1"));

        mockMvc.perform(get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: 1"));
    }

    // 17. GET /api/orders: Unauthenticated request -> 403
    @Test
    void getMyOrders_whenUnauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isForbidden());
    }

    // 18. POST /api/orders/{orderId}/cancel: Successful cancellation -> 200
    @Test
    void cancelOrder_whenValid_returns200() throws Exception {
        Order cancelledOrder = new Order(testUser, new BigDecimal("1299.99"), OrderStatus.CANCELLED);
        cancelledOrder.setId(50L);

        when(orderService.cancelOrder(1L, 50L)).thenReturn(cancelledOrder);
        when(orderService.getOrderItems(cancelledOrder)).thenReturn(List.of(testOrderItem));

        mockMvc.perform(post("/api/orders/50/cancel")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50L))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // 19. Correct authenticated user ID and order ID are passed
    @Test
    void cancelOrder_passesCorrectUserIdAndOrderId() throws Exception {
        Order cancelledOrder = new Order(testUser, new BigDecimal("1299.99"), OrderStatus.CANCELLED);
        cancelledOrder.setId(50L);

        when(orderService.cancelOrder(1L, 50L)).thenReturn(cancelledOrder);
        when(orderService.getOrderItems(cancelledOrder)).thenReturn(List.of(testOrderItem));

        mockMvc.perform(post("/api/orders/50/cancel")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        verify(orderService).cancelOrder(1L, 50L);
    }

    // 20. POST /api/orders/{orderId}/cancel: ResourceNotFoundException -> 404
    @Test
    void cancelOrder_whenOrderNotFound_returns404() throws Exception {
        when(orderService.cancelOrder(1L, 999L))
                .thenThrow(new ResourceNotFoundException("Order not found with id: 999"));

        mockMvc.perform(post("/api/orders/999/cancel")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found with id: 999"));
    }

    // 21. POST /api/orders/{orderId}/cancel: IllegalArgumentException -> 400
    @Test
    void cancelOrder_whenStatusNotPendingOrConfirmed_returns400() throws Exception {
        when(orderService.cancelOrder(1L, 50L))
                .thenThrow(new IllegalArgumentException("Cannot cancel order with status: DELIVERED"));

        mockMvc.perform(post("/api/orders/50/cancel")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot cancel order with status: DELIVERED"));
    }

    // 22. POST /api/orders/{orderId}/cancel: Unauthenticated request -> 403
    @Test
    void cancelOrder_whenUnauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/orders/50/cancel"))
                .andExpect(status().isForbidden());
    }
}
