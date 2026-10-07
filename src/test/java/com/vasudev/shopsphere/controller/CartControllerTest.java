package com.vasudev.shopsphere.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vasudev.shopsphere.config.SecurityConfig;
import com.vasudev.shopsphere.dto.cart.AddCartItemRequest;
import com.vasudev.shopsphere.dto.cart.CartItemResponse;
import com.vasudev.shopsphere.dto.cart.CartResponse;
import com.vasudev.shopsphere.dto.cart.UpdateCartItemRequest;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.exception.ResourceNotFoundException;
import com.vasudev.shopsphere.security.JwtAuthenticationFilter;
import com.vasudev.shopsphere.security.JwtService;
import com.vasudev.shopsphere.service.CartService;
import com.vasudev.shopsphere.service.UserService;
import java.math.BigDecimal;
import java.util.List;
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

@WebMvcTest(controllers = CartController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "jwt.secret=ShopSphere-test-secret-key-2026-very-long-and-secure",
        "jwt.expiration=3600000"
})
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private UserService userService;

    private User testUser;
    private String validToken;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        testUser = new User("Alice", "Smith", "alice@example.com", "Password123", "1234567890");
        testUser.setId(1L);
        testUser.setEnabled(true);

        when(userService.getUserByEmail("alice@example.com")).thenReturn(Optional.of(testUser));
        validToken = jwtService.generateToken(testUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCart_whenAuthenticated_returns200AndCartResponse() throws Exception {
        CartItemResponse item = new CartItemResponse(10L, 100L, "Wireless Mouse", new BigDecimal("25.00"), 2, new BigDecimal("50.00"));
        CartResponse cartResponse = new CartResponse(5L, 1L, List.of(item), new BigDecimal("50.00"));

        when(cartService.getOrCreateCart(1L)).thenReturn(cartResponse);

        mockMvc.perform(get("/api/cart")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productName").value("Wireless Mouse"))
                .andExpect(jsonPath("$.total").value(50.00));

        verify(cartService).getOrCreateCart(1L);
    }

    @Test
    void addItem_whenAuthenticatedAndValid_returns200AndUpdatedCart() throws Exception {
        AddCartItemRequest request = new AddCartItemRequest(100L, 2);
        CartItemResponse item = new CartItemResponse(10L, 100L, "Wireless Mouse", new BigDecimal("25.00"), 2, new BigDecimal("50.00"));
        CartResponse cartResponse = new CartResponse(5L, 1L, List.of(item), new BigDecimal("50.00"));

        when(cartService.addItem(eq(1L), any(AddCartItemRequest.class))).thenReturn(cartResponse);

        mockMvc.perform(post("/api/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        verify(cartService).addItem(eq(1L), any(AddCartItemRequest.class));
    }

    @Test
    void updateItem_whenAuthenticatedAndValid_returns200AndUpdatedCart() throws Exception {
        UpdateCartItemRequest request = new UpdateCartItemRequest(4);
        CartItemResponse item = new CartItemResponse(10L, 100L, "Wireless Mouse", new BigDecimal("25.00"), 4, new BigDecimal("100.00"));
        CartResponse cartResponse = new CartResponse(5L, 1L, List.of(item), new BigDecimal("100.00"));

        when(cartService.updateItem(eq(1L), eq(10L), any(UpdateCartItemRequest.class))).thenReturn(cartResponse);

        mockMvc.perform(put("/api/cart/items/10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(4))
                .andExpect(jsonPath("$.total").value(100.00));

        verify(cartService).updateItem(eq(1L), eq(10L), any(UpdateCartItemRequest.class));
    }

    @Test
    void removeItem_whenAuthenticated_returns200AndUpdatedCart() throws Exception {
        CartResponse cartResponse = new CartResponse(5L, 1L, List.of(), BigDecimal.ZERO);

        when(cartService.removeItem(1L, 10L)).thenReturn(cartResponse);

        mockMvc.perform(delete("/api/cart/items/10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.total").value(0));

        verify(cartService).removeItem(1L, 10L);
    }

    @Test
    void clearCart_whenAuthenticated_returns200AndEmptyCart() throws Exception {
        CartResponse cartResponse = new CartResponse(5L, 1L, List.of(), BigDecimal.ZERO);

        when(cartService.clearCart(1L)).thenReturn(cartResponse);

        mockMvc.perform(delete("/api/cart")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.total").value(0));

        verify(cartService).clearCart(1L);
    }

    @Test
    void addItem_whenInvalidRequest_returns400BadRequest() throws Exception {
        AddCartItemRequest invalidRequest = new AddCartItemRequest(null, 0);

        mockMvc.perform(post("/api/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateItem_whenInvalidRequest_returns400BadRequest() throws Exception {
        UpdateCartItemRequest invalidRequest = new UpdateCartItemRequest(-1);

        mockMvc.perform(put("/api/cart/items/10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCart_whenUnauthenticated_returns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isForbidden());
    }

    @Test
    void addItem_whenUnauthenticated_returns403Forbidden() throws Exception {
        AddCartItemRequest request = new AddCartItemRequest(100L, 1);

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateItem_whenUnauthenticated_returns403Forbidden() throws Exception {
        UpdateCartItemRequest request = new UpdateCartItemRequest(1);

        mockMvc.perform(put("/api/cart/items/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void removeItem_whenUnauthenticated_returns403Forbidden() throws Exception {
        mockMvc.perform(delete("/api/cart/items/10"))
                .andExpect(status().isForbidden());
    }

    @Test
    void clearCart_whenUnauthenticated_returns403Forbidden() throws Exception {
        mockMvc.perform(delete("/api/cart"))
                .andExpect(status().isForbidden());
    }

    @Test
    void addItem_whenServiceThrowsResourceNotFoundException_returns404() throws Exception {
        AddCartItemRequest request = new AddCartItemRequest(999L, 1);

        when(cartService.addItem(eq(1L), any(AddCartItemRequest.class)))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 999"));

        mockMvc.perform(post("/api/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id: 999"));
    }

    @Test
    void addItem_whenServiceThrowsIllegalArgumentException_returns400() throws Exception {
        AddCartItemRequest request = new AddCartItemRequest(100L, 500);

        when(cartService.addItem(eq(1L), any(AddCartItemRequest.class)))
                .thenThrow(new IllegalArgumentException("Requested quantity (500) exceeds available stock (50)"));

        mockMvc.perform(post("/api/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Requested quantity (500) exceeds available stock (50)"));
    }
}
