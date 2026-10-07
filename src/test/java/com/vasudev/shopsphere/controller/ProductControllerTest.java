package com.vasudev.shopsphere.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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
import com.vasudev.shopsphere.dto.ProductCreateRequest;
import com.vasudev.shopsphere.dto.ProductResponse;
import com.vasudev.shopsphere.dto.ProductUpdateRequest;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.exception.ResourceNotFoundException;
import com.vasudev.shopsphere.security.JwtAuthenticationFilter;
import com.vasudev.shopsphere.security.JwtService;
import com.vasudev.shopsphere.service.ProductService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProductController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "jwt.secret=ShopSphere-test-secret-key-2026-very-long-and-secure",
        "jwt.expiration=3600000"
})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    private String validToken;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        User testUser = new User("Jane", "Doe", "jane.doe@shopsphere.com", "Password123", "9876543210");
        testUser.setId(1L);
        testUser.setEnabled(true);
        validToken = jwtService.generateToken(testUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createProduct_withValidRequest_returns201() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "Monitor",
                "4K gaming monitor",
                new BigDecimal("399.99"),
                15,
                true,
                null
        );

        ProductResponse response = new ProductResponse(
                1L,
                "Monitor",
                "4K gaming monitor",
                new BigDecimal("399.99"),
                15,
                true,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(productService.createProduct(any(ProductCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Monitor"))
                .andExpect(jsonPath("$.price").value(399.99));
    }

    @Test
    void createProduct_withImageUrl_returns201WithImageUrl() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "Monitor",
                "4K gaming monitor",
                new BigDecimal("399.99"),
                15,
                true,
                null,
                "https://images.unsplash.com/photo-monitor"
        );

        ProductResponse response = new ProductResponse(
                1L,
                "Monitor",
                "4K gaming monitor",
                new BigDecimal("399.99"),
                15,
                true,
                null,
                "https://images.unsplash.com/photo-monitor",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(productService.createProduct(any(ProductCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Monitor"))
                .andExpect(jsonPath("$.imageUrl").value("https://images.unsplash.com/photo-monitor"));
    }

    @Test
    void createProduct_withInvalidRequest_returns400() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "", // Blank name
                "Desc",
                new BigDecimal("-10.00"), // Invalid negative price
                -1, // Invalid stock
                true,
                null
        );

        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProductById_whenExists_returns200() throws Exception {
        ProductResponse response = new ProductResponse(
                5L,
                "Keyboard",
                "Mechanical keyboard",
                new BigDecimal("89.99"),
                30,
                true,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(productService.getProductById(5L)).thenReturn(response);

        mockMvc.perform(get("/api/products/5")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.name").value("Keyboard"));
    }

    @Test
    void getProductById_whenNotFound_returns404() throws Exception {
        when(productService.getProductById(999L)).thenThrow(new ResourceNotFoundException("Product not found with id: 999"));

        mockMvc.perform(get("/api/products/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id: 999"));
    }

    @Test
    void getProducts_withDefaults_returns200AndPage() throws Exception {
        ProductResponse p1 = new ProductResponse(1L, "P1", "D1", new BigDecimal("10.00"), 5, true, null, LocalDateTime.now(), LocalDateTime.now());
        PageImpl<ProductResponse> page = new PageImpl<>(List.of(p1), PageRequest.of(0, 20), 1);

        when(productService.getProducts(0, 20, "createdAt", "desc", null, null, null)).thenReturn(page);

        mockMvc.perform(get("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("P1"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0));

        verify(productService).getProducts(0, 20, "createdAt", "desc", null, null, null);
    }

    @Test
    void getProducts_withCustomPaginationParameters_callsServiceWithParams() throws Exception {
        PageImpl<ProductResponse> emptyPage = new PageImpl<>(List.of(), PageRequest.of(2, 10), 0);

        when(productService.getProducts(2, 10, "createdAt", "desc", null, null, null)).thenReturn(emptyPage);

        mockMvc.perform(get("/api/products?page=2&size=10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(2))
                .andExpect(jsonPath("$.size").value(10));

        verify(productService).getProducts(2, 10, "createdAt", "desc", null, null, null);
    }

    @Test
    void getProducts_withSortParameters_callsServiceWithSort() throws Exception {
        PageImpl<ProductResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(productService.getProducts(0, 20, "price", "asc", null, null, null)).thenReturn(page);

        mockMvc.perform(get("/api/products?sortBy=price&direction=asc")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        verify(productService).getProducts(0, 20, "price", "asc", null, null, null);
    }

    @Test
    void getProducts_withEnabledFilter_callsServiceWithFilter() throws Exception {
        PageImpl<ProductResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(productService.getProducts(0, 20, "createdAt", "desc", null, true, null)).thenReturn(page);

        mockMvc.perform(get("/api/products?enabled=true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        verify(productService).getProducts(0, 20, "createdAt", "desc", null, true, null);
    }

    @Test
    void getProducts_withCategoryFilter_callsServiceWithCategory() throws Exception {
        PageImpl<ProductResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(productService.getProducts(0, 20, "createdAt", "desc", null, null, 2L)).thenReturn(page);

        mockMvc.perform(get("/api/products?categoryId=2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        verify(productService).getProducts(0, 20, "createdAt", "desc", null, null, 2L);
    }

    @Test
    void getProducts_withCombinedFilters_callsServiceWithBoth() throws Exception {
        PageImpl<ProductResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(productService.getProducts(0, 20, "createdAt", "desc", null, true, 2L)).thenReturn(page);

        mockMvc.perform(get("/api/products?enabled=true&categoryId=2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        verify(productService).getProducts(0, 20, "createdAt", "desc", null, true, 2L);
    }

    @Test
    void getProducts_withSearch_callsServiceWithSearch() throws Exception {
        PageImpl<ProductResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(productService.getProducts(0, 20, "createdAt", "desc", "phone", null, null)).thenReturn(page);

        mockMvc.perform(get("/api/products?search=phone")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        verify(productService).getProducts(0, 20, "createdAt", "desc", "phone", null, null);
    }

    @Test
    void getProducts_withSearchAndEnabled_callsServiceWithBoth() throws Exception {
        PageImpl<ProductResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(productService.getProducts(0, 20, "createdAt", "desc", "phone", true, null)).thenReturn(page);

        mockMvc.perform(get("/api/products?search=phone&enabled=true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        verify(productService).getProducts(0, 20, "createdAt", "desc", "phone", true, null);
    }

    @Test
    void getProducts_withSearchAndCategoryId_callsServiceWithBoth() throws Exception {
        PageImpl<ProductResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(productService.getProducts(0, 20, "createdAt", "desc", "phone", null, 2L)).thenReturn(page);

        mockMvc.perform(get("/api/products?search=phone&categoryId=2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        verify(productService).getProducts(0, 20, "createdAt", "desc", "phone", null, 2L);
    }

    @Test
    void getProducts_withSearchAndEnabledAndCategoryId_callsServiceWithAll() throws Exception {
        PageImpl<ProductResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(productService.getProducts(0, 20, "createdAt", "desc", "phone", true, 2L)).thenReturn(page);

        mockMvc.perform(get("/api/products?search=phone&enabled=true&categoryId=2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        verify(productService).getProducts(0, 20, "createdAt", "desc", "phone", true, 2L);
    }

    @Test
    void getProducts_withSearchAndPaginationAndSorting_callsServiceWithAll() throws Exception {
        PageImpl<ProductResponse> page = new PageImpl<>(List.of(), PageRequest.of(1, 10), 0);

        when(productService.getProducts(1, 10, "price", "asc", "laptop", true, 5L)).thenReturn(page);

        mockMvc.perform(get("/api/products?search=laptop&enabled=true&categoryId=5&page=1&size=10&sortBy=price&direction=asc")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        verify(productService).getProducts(1, 10, "price", "asc", "laptop", true, 5L);
    }

    @Test
    void getProducts_withBlankSearch_callsServiceWithBlankSearch() throws Exception {
        PageImpl<ProductResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(productService.getProducts(0, 20, "createdAt", "desc", "   ", null, null)).thenReturn(page);

        mockMvc.perform(get("/api/products?search=   ")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        verify(productService).getProducts(0, 20, "createdAt", "desc", "   ", null, null);
    }

    @Test
    void getProducts_withInvalidParameter_returns400BadRequest() throws Exception {
        when(productService.getProducts(any(Integer.class), any(Integer.class), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Invalid sort field: invalidField"));

        mockMvc.perform(get("/api/products?sortBy=invalidField")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid sort field: invalidField"));
    }

    @Test
    void updateProduct_whenExists_returns200() throws Exception {
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName("Updated Keyboard");

        ProductResponse response = new ProductResponse(
                5L,
                "Updated Keyboard",
                "Mechanical keyboard",
                new BigDecimal("89.99"),
                30,
                true,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(productService.updateProduct(eq(5L), any(ProductUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/products/5")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Keyboard"));
    }

    @Test
    void updateProduct_whenNotFound_returns404() throws Exception {
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName("Updated Keyboard");

        when(productService.updateProduct(eq(999L), any(ProductUpdateRequest.class)))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 999"));

        mockMvc.perform(put("/api/products/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id: 999"));
    }

    @Test
    void deleteProduct_whenExists_returns204() throws Exception {
        doNothing().when(productService).deleteProduct(5L);

        mockMvc.perform(delete("/api/products/5")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteProduct_whenNotFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Product not found with id: 999"))
                .when(productService).deleteProduct(999L);

        mockMvc.perform(delete("/api/products/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id: 999"));
    }

    @Test
    void unauthenticatedGetProducts_returns200() throws Exception {
        ProductResponse response = new ProductResponse(
                1L,
                "Sample Product",
                "Description",
                new BigDecimal("49.99"),
                20,
                true,
                1L,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(productService.getProducts(any(Integer.class), any(Integer.class), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedMutation_returns403() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "Sample Product",
                "Description",
                new BigDecimal("49.99"),
                20,
                true,
                1L
        );
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
