package com.vasudev.shopsphere.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import com.vasudev.shopsphere.dto.ReviewCreateRequest;
import com.vasudev.shopsphere.dto.ReviewUpdateRequest;
import com.vasudev.shopsphere.entity.Product;
import com.vasudev.shopsphere.entity.Review;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.exception.ResourceNotFoundException;
import com.vasudev.shopsphere.security.JwtAuthenticationFilter;
import com.vasudev.shopsphere.security.JwtService;
import com.vasudev.shopsphere.service.ReviewService;
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
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ReviewController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "jwt.secret=ShopSphere-test-secret-key-2026-very-long-and-secure",
        "jwt.expiration=3600000"
})
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private UserService userService;

    private User testUser;
    private String validToken;
    private Product testProduct;
    private Review testReview;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        testUser = new User("Alice", "Smith", "alice@example.com", "Password123", "1234567890");
        testUser.setId(1L);
        testUser.setEnabled(true);

        when(userService.getUserByEmail("alice@example.com")).thenReturn(Optional.of(testUser));
        validToken = jwtService.generateToken(testUser);

        testProduct = new Product("Smartphone", "Latest model", new BigDecimal("799.99"), 50, null);
        testProduct.setId(10L);

        testReview = new Review(testUser, testProduct, 5, "Excellent phone!");
        testReview.setId(100L);
        testReview.setCreatedAt(LocalDateTime.now().minusDays(1));
        testReview.setUpdatedAt(LocalDateTime.now().minusDays(1));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==========================================
    // 1. POST /api/reviews/products/{productId}
    // ==========================================

    @Test
    void createReview_success_returns201() throws Exception {
        when(reviewService.createReview(1L, 10L, 5, "Excellent phone!")).thenReturn(testReview);

        ReviewCreateRequest request = new ReviewCreateRequest(5, "Excellent phone!");

        mockMvc.perform(post("/api/reviews/products/10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.productId").value(10L))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Excellent phone!"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(jsonPath("$.product").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(reviewService).createReview(1L, 10L, 5, "Excellent phone!");
    }

    @Test
    void createReview_whenMissingRating_returns400() throws Exception {
        String json = "{\"comment\":\"No rating provided\"}";

        mockMvc.perform(post("/api/reviews/products/10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(), any(), any(), any());
    }

    @Test
    void createReview_whenRatingBelowOne_returns400() throws Exception {
        ReviewCreateRequest request = new ReviewCreateRequest(0, "Rating 0");

        mockMvc.perform(post("/api/reviews/products/10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(), any(), any(), any());
    }

    @Test
    void createReview_whenRatingAboveFive_returns400() throws Exception {
        ReviewCreateRequest request = new ReviewCreateRequest(6, "Rating 6");

        mockMvc.perform(post("/api/reviews/products/10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(), any(), any(), any());
    }

    @Test
    void createReview_whenCommentTooLong_returns400() throws Exception {
        String longComment = "a".repeat(2001);
        ReviewCreateRequest request = new ReviewCreateRequest(5, longComment);

        mockMvc.perform(post("/api/reviews/products/10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(), any(), any(), any());
    }

    @Test
    void createReview_ignoresClientSuppliedUserId() throws Exception {
        when(reviewService.createReview(1L, 10L, 5, "Good")).thenReturn(testReview);

        String jsonWithSpoofedUserId = "{\"rating\":5,\"comment\":\"Good\",\"userId\":999}";

        mockMvc.perform(post("/api/reviews/products/10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithSpoofedUserId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1L));

        verify(reviewService).createReview(1L, 10L, 5, "Good");
    }

    @Test
    void createReview_whenUnauthenticated_returns403() throws Exception {
        ReviewCreateRequest request = new ReviewCreateRequest(5, "Great");

        mockMvc.perform(post("/api/reviews/products/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createReview_whenProductNotFound_returns404() throws Exception {
        when(reviewService.createReview(1L, 999L, 5, "Great"))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 999"));

        ReviewCreateRequest request = new ReviewCreateRequest(5, "Great");

        mockMvc.perform(post("/api/reviews/products/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id: 999"));
    }

    // ==========================================
    // 2. GET /api/reviews/{reviewId}
    // ==========================================

    @Test
    void getReviewById_success_returns200() throws Exception {
        when(reviewService.getReviewById(1L, 100L)).thenReturn(testReview);

        mockMvc.perform(get("/api/reviews/100")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.productId").value(10L))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Excellent phone!"));

        verify(reviewService).getReviewById(1L, 100L);
    }

    @Test
    void getReviewById_whenNotFound_returns404() throws Exception {
        when(reviewService.getReviewById(1L, 999L))
                .thenThrow(new ResourceNotFoundException("Review not found with id: 999"));

        mockMvc.perform(get("/api/reviews/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Review not found with id: 999"));
    }

    @Test
    void getReviewById_whenUnauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/reviews/100"))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 3. GET /api/reviews/products/{productId}
    // ==========================================

    @Test
    void getReviewsByProduct_defaultPagination_returns200() throws Exception {
        Page<Review> page = new PageImpl<>(List.of(testReview), PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")), 1);
        when(reviewService.getReviewsByProduct(eq(10L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/reviews/products/10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(100L))
                .andExpect(jsonPath("$.content[0].rating").value(5))
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewService).getReviewsByProduct(eq(10L), captor.capture());
        Pageable captured = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(0, captured.getPageNumber());
        org.junit.jupiter.api.Assertions.assertEquals(20, captured.getPageSize());
        org.junit.jupiter.api.Assertions.assertEquals(Sort.Direction.DESC, captured.getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    void getReviewsByProduct_explicitPagination_passesParametersCorrectly() throws Exception {
        Page<Review> page = new PageImpl<>(List.of(testReview), PageRequest.of(1, 10, Sort.by(Sort.Direction.ASC, "rating")), 1);
        when(reviewService.getReviewsByProduct(eq(10L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/reviews/products/10?page=1&size=10&sortBy=rating&direction=asc")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewService).getReviewsByProduct(eq(10L), captor.capture());
        Pageable captured = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(1, captured.getPageNumber());
        org.junit.jupiter.api.Assertions.assertEquals(10, captured.getPageSize());
        org.junit.jupiter.api.Assertions.assertEquals(Sort.Direction.ASC, captured.getSort().getOrderFor("rating").getDirection());
    }

    @Test
    void getReviewsByProduct_whenInvalidPagination_returns400() throws Exception {
        mockMvc.perform(get("/api/reviews/products/10?size=101")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Page size must be between 1 and 100"));

        mockMvc.perform(get("/api/reviews/products/10?sortBy=invalidField")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid sort field: invalidField"));

        mockMvc.perform(get("/api/reviews/products/10?direction=invalidDir")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid sort direction: invalidDir"));
    }

    // ==========================================
    // 4. GET /api/reviews/my
    // ==========================================

    @Test
    void getMyReviews_success_returns200() throws Exception {
        Page<Review> page = new PageImpl<>(List.of(testReview), PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")), 1);
        when(reviewService.getReviewsByUser(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/reviews/my")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(100L))
                .andExpect(jsonPath("$.content[0].userId").value(1L));

        verify(reviewService).getReviewsByUser(eq(1L), any(Pageable.class));
    }

    @Test
    void getMyReviews_explicitPagination_passesParametersCorrectly() throws Exception {
        Page<Review> page = new PageImpl<>(List.of(testReview), PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "createdAt")), 1);
        when(reviewService.getReviewsByUser(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/reviews/my?page=1&size=10&sortBy=createdAt&direction=desc")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewService).getReviewsByUser(eq(1L), captor.capture());
        Pageable captured = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(1, captured.getPageNumber());
        org.junit.jupiter.api.Assertions.assertEquals(10, captured.getPageSize());
        org.junit.jupiter.api.Assertions.assertEquals(Sort.Direction.DESC, captured.getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    void getMyReviews_ignoresClientProvidedUserId() throws Exception {
        Page<Review> page = new PageImpl<>(List.of(testReview), PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")), 1);
        when(reviewService.getReviewsByUser(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/reviews/my?userId=999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value(1L));

        verify(reviewService).getReviewsByUser(eq(1L), any(Pageable.class));
        verify(reviewService, never()).getReviewsByUser(eq(999L), any());
    }

    // ==========================================
    // 5. PUT /api/reviews/{reviewId}
    // ==========================================

    @Test
    void updateReview_success_returns200() throws Exception {
        Review updatedReview = new Review(testUser, testProduct, 4, "Updated review");
        updatedReview.setId(100L);
        updatedReview.setCreatedAt(testReview.getCreatedAt());
        updatedReview.setUpdatedAt(LocalDateTime.now());

        when(reviewService.updateReview(1L, 100L, 4, "Updated review")).thenReturn(updatedReview);

        ReviewUpdateRequest request = new ReviewUpdateRequest(4, "Updated review");

        mockMvc.perform(put("/api/reviews/100")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.comment").value("Updated review"));

        verify(reviewService).updateReview(1L, 100L, 4, "Updated review");
    }

    @Test
    void updateReview_whenMissingRating_returns400() throws Exception {
        String json = "{\"comment\":\"No rating\"}";

        mockMvc.perform(put("/api/reviews/100")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).updateReview(any(), any(), any(), any());
    }

    @Test
    void updateReview_whenRatingZero_returns400() throws Exception {
        ReviewUpdateRequest request = new ReviewUpdateRequest(0, "Rating 0");

        mockMvc.perform(put("/api/reviews/100")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).updateReview(any(), any(), any(), any());
    }

    @Test
    void updateReview_whenRatingAboveFive_returns400() throws Exception {
        ReviewUpdateRequest request = new ReviewUpdateRequest(6, "Rating 6");

        mockMvc.perform(put("/api/reviews/100")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).updateReview(any(), any(), any(), any());
    }

    @Test
    void updateReview_whenCommentTooLong_returns400() throws Exception {
        String longComment = "a".repeat(2001);
        ReviewUpdateRequest request = new ReviewUpdateRequest(4, longComment);

        mockMvc.perform(put("/api/reviews/100")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).updateReview(any(), any(), any(), any());
    }

    @Test
    void updateReview_whenNotFound_returns404() throws Exception {
        when(reviewService.updateReview(1L, 999L, 4, "Updated"))
                .thenThrow(new ResourceNotFoundException("Review not found with id: 999"));

        ReviewUpdateRequest request = new ReviewUpdateRequest(4, "Updated");

        mockMvc.perform(put("/api/reviews/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Review not found with id: 999"));
    }

    // ==========================================
    // 6. DELETE /api/reviews/{reviewId}
    // ==========================================

    @Test
    void deleteReview_success_returns204() throws Exception {
        mockMvc.perform(delete("/api/reviews/100")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isNoContent());

        verify(reviewService).deleteReview(1L, 100L);
    }

    @Test
    void deleteReview_whenNotFound_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Review not found with id: 999"))
                .when(reviewService).deleteReview(1L, 999L);

        mockMvc.perform(delete("/api/reviews/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Review not found with id: 999"));
    }

    @Test
    void deleteReview_whenUnauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/reviews/100"))
                .andExpect(status().isForbidden());
    }
}
