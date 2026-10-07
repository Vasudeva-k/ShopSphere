package com.vasudev.shopsphere.config;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vasudev.shopsphere.controller.AuthController;
import com.vasudev.shopsphere.controller.UserController;
import com.vasudev.shopsphere.dto.LoginRequest;
import com.vasudev.shopsphere.dto.UserCreateRequest;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.security.JwtAuthenticationFilter;
import com.vasudev.shopsphere.security.JwtService;
import com.vasudev.shopsphere.service.RefreshTokenService;
import com.vasudev.shopsphere.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {AuthController.class, UserController.class})
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
}
