package com.vasudev.shopsphere.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vasudev.shopsphere.config.SecurityConfig;
import com.vasudev.shopsphere.entity.Product;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.entity.WishlistItem;
import com.vasudev.shopsphere.exception.ResourceNotFoundException;
import com.vasudev.shopsphere.security.JwtAuthenticationFilter;
import com.vasudev.shopsphere.security.JwtService;
import com.vasudev.shopsphere.service.UserService;
import com.vasudev.shopsphere.service.WishlistService;
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
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = WishlistController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "jwt.secret=ShopSphere-test-secret-key-2026-very-long-and-secure",
        "jwt.expiration=3600000"
})
class WishlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private WishlistService wishlistService;

    @MockitoBean
    private UserService userService;

    private User testUser;
    private String validToken;
    private Product testProduct;
    private WishlistItem testItem;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        testUser = new User("Alice", "Smith", "alice@example.com", "Password123", "1234567890");
        testUser.setId(1L);
        testUser.setEnabled(true);

        when(userService.getUserByEmail("alice@example.com")).thenReturn(Optional.of(testUser));
        validToken = jwtService.generateToken(testUser);

        testProduct = new Product("Wireless Headphones", "Noise cancelling", new BigDecimal("199.99"), 30, null);
        testProduct.setId(10L);
        testProduct.setEnabled(true);

        testItem = new WishlistItem(testUser, testProduct);
        testItem.setId(100L);
        testItem.setCreatedAt(LocalDateTime.now().minusDays(1));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==========================================
    // 1. POST /api/wishlist/products/{productId}
    // ==========================================

    @Test
    void addItem_success_returns201() throws Exception {
        when(wishlistService.addItem(1L, 10L)).thenReturn(testItem);

        mockMvc.perform(post("/api/wishlist/products/10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.productId").value(10L))
                .andExpect(jsonPath("$.productName").value("Wireless Headphones"))
                .andExpect(jsonPath("$.productPrice").value(199.99))
                .andExpect(jsonPath("$.productEnabled").value(true))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(jsonPath("$.product").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(wishlistService).addItem(1L, 10L);
    }

    @Test
    void addItem_whenUnauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/wishlist/products/10"))
                .andExpect(status().isForbidden());

        verify(wishlistService, never()).addItem(any(), any());
    }

    @Test
    void addItem_whenProductNotFound_returns404() throws Exception {
        when(wishlistService.addItem(1L, 999L))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 999"));

        mockMvc.perform(post("/api/wishlist/products/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id: 999"));
    }

    @Test
    void addItem_whenDuplicateItem_propagatesIllegalStateException() {
        when(wishlistService.addItem(1L, 10L))
                .thenThrow(new IllegalStateException("Product is already in the wishlist"));

        Exception exception = org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                mockMvc.perform(post("/api/wishlist/products/10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
        );

        org.junit.jupiter.api.Assertions.assertInstanceOf(IllegalStateException.class, exception.getCause());
        org.junit.jupiter.api.Assertions.assertEquals("Product is already in the wishlist", exception.getCause().getMessage());
    }

    @Test
    void addItem_ignoresClientSuppliedUserId() throws Exception {
        when(wishlistService.addItem(1L, 10L)).thenReturn(testItem);

        mockMvc.perform(post("/api/wishlist/products/10?userId=999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L));

        verify(wishlistService).addItem(1L, 10L);
        verify(wishlistService, never()).addItem(eq(999L), any());
    }

    // ==========================================
    // 2. GET /api/wishlist
    // ==========================================

    @Test
    void getWishlist_defaultPagination_returns200() throws Exception {
        Page<WishlistItem> page = new PageImpl<>(List.of(testItem), PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")), 1);
        when(wishlistService.getWishlist(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/wishlist")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(100L))
                .andExpect(jsonPath("$.content[0].productId").value(10L))
                .andExpect(jsonPath("$.content[0].productName").value("Wireless Headphones"))
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(wishlistService).getWishlist(eq(1L), captor.capture());
        Pageable captured = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(0, captured.getPageNumber());
        org.junit.jupiter.api.Assertions.assertEquals(20, captured.getPageSize());
        org.junit.jupiter.api.Assertions.assertEquals(Sort.Direction.DESC, captured.getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    void getWishlist_explicitPageAndSize_passesParametersCorrectly() throws Exception {
        Page<WishlistItem> page = new PageImpl<>(List.of(testItem), PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "createdAt")), 1);
        when(wishlistService.getWishlist(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/wishlist?page=1&size=10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(wishlistService).getWishlist(eq(1L), captor.capture());
        Pageable captured = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(1, captured.getPageNumber());
        org.junit.jupiter.api.Assertions.assertEquals(10, captured.getPageSize());
    }

    @Test
    void getWishlist_sortByCreatedAt_passesCorrectSort() throws Exception {
        Page<WishlistItem> page = new PageImpl<>(List.of(testItem), PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt")), 1);
        when(wishlistService.getWishlist(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/wishlist?sortBy=createdAt&direction=asc")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(wishlistService).getWishlist(eq(1L), captor.capture());
        Pageable captured = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(Sort.Direction.ASC, captured.getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    void getWishlist_sortById_passesCorrectSort() throws Exception {
        Page<WishlistItem> page = new PageImpl<>(List.of(testItem), PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id")), 1);
        when(wishlistService.getWishlist(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/wishlist?sortBy=id&direction=asc")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(wishlistService).getWishlist(eq(1L), captor.capture());
        Pageable captured = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(Sort.Direction.ASC, captured.getSort().getOrderFor("id").getDirection());
    }

    @Test
    void getWishlist_whenInvalidPage_returns400() throws Exception {
        mockMvc.perform(get("/api/wishlist?page=-1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Page index must not be less than zero"));
    }

    @Test
    void getWishlist_whenSizeZero_returns400() throws Exception {
        mockMvc.perform(get("/api/wishlist?size=0")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Page size must be between 1 and 100"));
    }

    @Test
    void getWishlist_whenSizeGreaterThan100_returns400() throws Exception {
        mockMvc.perform(get("/api/wishlist?size=101")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Page size must be between 1 and 100"));
    }

    @Test
    void getWishlist_whenInvalidSortField_returns400() throws Exception {
        mockMvc.perform(get("/api/wishlist?sortBy=price")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid sort field: price"));
    }

    @Test
    void getWishlist_whenInvalidDirection_returns400() throws Exception {
        mockMvc.perform(get("/api/wishlist?direction=sideways")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid sort direction: sideways"));
    }

    @Test
    void getWishlist_whenUnauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/wishlist"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getWishlist_ignoresClientSuppliedUserId() throws Exception {
        Page<WishlistItem> page = new PageImpl<>(List.of(testItem), PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")), 1);
        when(wishlistService.getWishlist(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/wishlist?userId=999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        verify(wishlistService).getWishlist(eq(1L), any(Pageable.class));
        verify(wishlistService, never()).getWishlist(eq(999L), any());
    }

    // ==========================================
    // 3. GET /api/wishlist/{wishlistItemId}
    // ==========================================

    @Test
    void getItem_success_returns200() throws Exception {
        when(wishlistService.getItem(1L, 100L)).thenReturn(testItem);

        mockMvc.perform(get("/api/wishlist/100")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.productId").value(10L))
                .andExpect(jsonPath("$.productName").value("Wireless Headphones"))
                .andExpect(jsonPath("$.productPrice").value(199.99))
                .andExpect(jsonPath("$.productEnabled").value(true))
                .andExpect(jsonPath("$.createdAt").exists());

        verify(wishlistService).getItem(1L, 100L);
    }

    @Test
    void getItem_whenNotFoundOrWrongOwner_returns404() throws Exception {
        when(wishlistService.getItem(1L, 999L))
                .thenThrow(new ResourceNotFoundException("Wishlist item not found with id: 999"));

        mockMvc.perform(get("/api/wishlist/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Wishlist item not found with id: 999"));
    }

    @Test
    void getItem_whenUnauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/wishlist/100"))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 4. DELETE /api/wishlist/{wishlistItemId}
    // ==========================================

    @Test
    void removeItem_success_returns204() throws Exception {
        mockMvc.perform(delete("/api/wishlist/100")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isNoContent());

        verify(wishlistService).removeItem(1L, 100L);
    }

    @Test
    void removeItem_whenNotFoundOrWrongOwner_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Wishlist item not found with id: 999"))
                .when(wishlistService).removeItem(1L, 999L);

        mockMvc.perform(delete("/api/wishlist/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Wishlist item not found with id: 999"));
    }

    @Test
    void removeItem_whenUnauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/wishlist/100"))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 5. GET /api/wishlist/products/{productId}/exists
    // ==========================================

    @Test
    void isProductInWishlist_whenTrue_returnsExistsTrue() throws Exception {
        when(wishlistService.isProductInWishlist(1L, 10L)).thenReturn(true);

        mockMvc.perform(get("/api/wishlist/products/10/exists")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));

        verify(wishlistService).isProductInWishlist(1L, 10L);
    }

    @Test
    void isProductInWishlist_whenFalse_returnsExistsFalse() throws Exception {
        when(wishlistService.isProductInWishlist(1L, 10L)).thenReturn(false);

        mockMvc.perform(get("/api/wishlist/products/10/exists")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));

        verify(wishlistService).isProductInWishlist(1L, 10L);
    }

    @Test
    void isProductInWishlist_whenProductNotFound_returns404() throws Exception {
        when(wishlistService.isProductInWishlist(1L, 999L))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 999"));

        mockMvc.perform(get("/api/wishlist/products/999/exists")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id: 999"));
    }

    @Test
    void isProductInWishlist_whenUnauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/wishlist/products/10/exists"))
                .andExpect(status().isForbidden());
    }

    @Test
    void isProductInWishlist_ignoresClientSuppliedUserId() throws Exception {
        when(wishlistService.isProductInWishlist(1L, 10L)).thenReturn(true);

        mockMvc.perform(get("/api/wishlist/products/10/exists?userId=999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));

        verify(wishlistService).isProductInWishlist(1L, 10L);
        verify(wishlistService, never()).isProductInWishlist(eq(999L), any());
    }

    // ==========================================
    // 6. DELETE /api/wishlist
    // ==========================================

    @Test
    void clearWishlist_success_returns204() throws Exception {
        mockMvc.perform(delete("/api/wishlist")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isNoContent());

        verify(wishlistService).clearWishlist(1L);
    }

    @Test
    void clearWishlist_whenUnauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/wishlist"))
                .andExpect(status().isForbidden());

        verify(wishlistService, never()).clearWishlist(any());
    }
}
