package com.vasudev.shopsphere.config;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vasudev.shopsphere.controller.AuthController;
import com.vasudev.shopsphere.controller.CategoryController;
import com.vasudev.shopsphere.controller.ProductController;
import com.vasudev.shopsphere.controller.UserController;
import com.vasudev.shopsphere.dto.LoginRequest;
import com.vasudev.shopsphere.dto.ProductCreateRequest;
import com.vasudev.shopsphere.dto.ProductResponse;
import com.vasudev.shopsphere.dto.UserCreateRequest;
import com.vasudev.shopsphere.entity.Category;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.security.JwtAuthenticationFilter;
import com.vasudev.shopsphere.security.JwtService;
import com.vasudev.shopsphere.service.CategoryService;
import com.vasudev.shopsphere.service.ProductService;
import com.vasudev.shopsphere.service.RefreshTokenService;
import com.vasudev.shopsphere.service.UserService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {AuthController.class, UserController.class, ProductController.class, CategoryController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "jwt.secret=ShopSphere-test-secret-key-2026-very-long-and-secure",
        "jwt.expiration=3600000"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    @DisplayName("1. GET /actuator/health without authentication is permitted by SecurityConfig")
    void getActuatorHealth_withoutAuth_isPermitted() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().is(not(401)))
                .andExpect(status().is(not(403)));
    }

    @Test
    @DisplayName("2. GET /actuator/info without authentication is permitted by SecurityConfig")
    void getActuatorInfo_withoutAuth_isPermitted() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().is(not(401)))
                .andExpect(status().is(not(403)));
    }

    @Test
    @DisplayName("3. GET /actuator/env without authentication remains protected/not exposed")
    void getActuatorEnv_withoutAuth_isNotPubliclyExposed() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().is(not(200)));
    }

    @Test
    @DisplayName("4. GET /actuator/beans without authentication remains protected/not exposed")
    void getActuatorBeans_withoutAuth_isNotPubliclyExposed() throws Exception {
        mockMvc.perform(get("/actuator/beans"))
                .andExpect(status().is(not(200)));
    }

    @Test
    @DisplayName("5. GET /actuator/configprops without authentication remains protected/not exposed")
    void getActuatorConfigprops_withoutAuth_isNotPubliclyExposed() throws Exception {
        mockMvc.perform(get("/actuator/configprops"))
                .andExpect(status().is(not(200)));
    }

    @Test
    @DisplayName("6. Protected application endpoint GET /api/auth/me without authentication is rejected")
    void getMe_withoutAuth_isRejected() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("7. Public authentication endpoint POST /api/auth/login without authentication is accessible")
    void postLogin_withoutAuth_reachesEndpoint() throws Exception {
        LoginRequest loginRequest = new LoginRequest("nonexistent@example.com", "InvalidPass123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("8. Swagger/OpenAPI documentation endpoints remain publicly accessible")
    void getOpenApiAndSwagger_withoutAuth_areAccessible() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().is(not(401)))
                .andExpect(status().is(not(403)));

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().is(not(401)))
                .andExpect(status().is(not(403)));
    }

    @Test
    @DisplayName("9. Public registration endpoint POST /api/users without authentication is permitted")
    void postUsers_withoutAuth_isPermitted() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Jane",
                "Doe",
                "jane.doe@example.com",
                "Password123!",
                "+1234567890"
        );

        User mockSavedUser = new User("Jane", "Doe", "jane.doe@example.com", "encodedPassword", "+1234567890");
        mockSavedUser.setId(1L);
        Mockito.when(userService.saveUser(Mockito.any(User.class))).thenReturn(mockSavedUser);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("10. Protected user retrieval endpoint GET /api/users without authentication is rejected")
    void getUsers_withoutAuth_isForbidden() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("11. Public catalog endpoint GET /api/products without authentication is permitted")
    void getProducts_withoutAuth_isPermitted() throws Exception {
        Mockito.when(productService.getProducts(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("12. Public catalog endpoint GET /api/products/{id} without authentication is permitted")
    void getProductById_withoutAuth_isPermitted() throws Exception {
        ProductResponse response = new ProductResponse(1L, "Product", "Desc", new BigDecimal("19.99"), 10, true, 1L, null, null);
        Mockito.when(productService.getProductById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("13. Mutation endpoint POST /api/products without authentication is forbidden")
    void postProduct_withoutAuth_isForbidden() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest("New Product", "Desc", new BigDecimal("29.99"), 5, true, 1L);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("14. Mutation endpoint PUT /api/products/{id} without authentication is forbidden")
    void putProduct_withoutAuth_isForbidden() throws Exception {
        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("15. Mutation endpoint DELETE /api/products/{id} without authentication is forbidden")
    void deleteProduct_withoutAuth_isForbidden() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("16. Public catalog endpoint GET /api/categories without authentication is permitted")
    void getCategories_withoutAuth_isPermitted() throws Exception {
        Mockito.when(categoryService.getAllCategories()).thenReturn(List.of());

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("17. Public catalog endpoint GET /api/categories/active without authentication is permitted")
    void getActiveCategories_withoutAuth_isPermitted() throws Exception {
        Mockito.when(categoryService.getActiveCategories()).thenReturn(List.of());

        mockMvc.perform(get("/api/categories/active"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("18. Public catalog endpoint GET /api/categories/{id} without authentication is permitted")
    void getCategoryById_withoutAuth_isPermitted() throws Exception {
        Category category = new Category("Electronics", "Desc");
        category.setId(1L);
        Mockito.when(categoryService.getCategoryById(1L)).thenReturn(Optional.of(category));

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("19. Mutation endpoint POST /api/categories without authentication is forbidden")
    void postCategory_withoutAuth_isForbidden() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("20. Mutation endpoint DELETE /api/categories/{id} without authentication is forbidden")
    void deleteCategory_withoutAuth_isForbidden() throws Exception {
        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("21. Preflight OPTIONS request from allowed frontend origin returns 200 with CORS headers")
    void preflightCors_fromAllowedOrigin_returns200AndHeaders() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/products")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    @DisplayName("22. Simple GET request from allowed frontend origin includes Access-Control-Allow-Origin header")
    void getProducts_withAllowedOrigin_includesCorsHeader() throws Exception {
        Mockito.when(productService.getProducts(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/products")
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    @DisplayName("23. Preflight OPTIONS request from unauthorized origin does not return allow header")
    void preflightCors_fromUnauthorizedOrigin_isForbidden() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/products")
                        .header("Origin", "http://unauthorized-origin.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
