package com.vasudev.shopsphere.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vasudev.shopsphere.dto.LoginRequest;
import com.vasudev.shopsphere.dto.LogoutRequest;
import com.vasudev.shopsphere.dto.RefreshTokenRequest;
import com.vasudev.shopsphere.entity.RefreshToken;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.repository.RefreshTokenRepository;
import com.vasudev.shopsphere.repository.UserRepository;
import com.vasudev.shopsphere.service.UserService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<User> createdUsers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        try {
            jdbcTemplate.execute("ALTER TABLE refresh_tokens DROP COLUMN token");
        } catch (Exception ignored) {
            // column already dropped or does not exist
        }
        createdUsers.clear();
    }

    @AfterEach
    void tearDown() {
        for (User user : createdUsers) {
            userRepository.findById(user.getId()).ifPresent(persistedUser -> {
                refreshTokenRepository.deleteAll(refreshTokenRepository.findByUser(persistedUser));
                userRepository.delete(persistedUser);
            });
        }
    }

    private User createTestUser() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String uniqueEmail = "integration_" + uniqueId + "@shopsphere.com";

        User user = new User("Integration", "Tester", uniqueEmail, "Password123", "9876543210");
        user.setEnabled(true);
        User savedUser = userService.saveUser(user);
        createdUsers.add(savedUser);
        return savedUser;
    }

    @Test
    void login_storesOnlyHashedRefreshTokenInDatabase() throws Exception {
        User user = createTestUser();

        LoginRequest loginRequest = new LoginRequest(user.getEmail(), "Password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String rawRefreshToken = loginJson.get("refreshToken").asText();

        List<RefreshToken> tokens = refreshTokenRepository.findByUser(user);
        assertEquals(1, tokens.size());

        RefreshToken storedToken = tokens.get(0);
        assertNotNull(storedToken.getTokenHash());
        assertNotEquals(rawRefreshToken, storedToken.getTokenHash());
        assertEquals(64, storedToken.getTokenHash().length());
        assertTrue(storedToken.getTokenHash().matches("^[0-9a-f]{64}$"));

        Optional<RefreshToken> foundByHash = refreshTokenRepository.findByTokenHash(storedToken.getTokenHash());
        assertTrue(foundByHash.isPresent());
    }

    @Test
    void loginThenRefresh_rotatesRefreshTokenAndReturnsNewTokens() throws Exception {
        User user = createTestUser();

        LoginRequest loginRequest = new LoginRequest(user.getEmail(), "Password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String originalAccessToken = loginJson.get("token").asText();
        String originalRefreshToken = loginJson.get("refreshToken").asText();

        assertNotNull(originalAccessToken);
        assertFalse(originalAccessToken.isBlank());
        assertNotNull(originalRefreshToken);
        assertFalse(originalRefreshToken.isBlank());

        Thread.sleep(1000);

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(originalRefreshToken);

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andReturn();

        JsonNode refreshJson = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newAccessToken = refreshJson.get("token").asText();
        String newRefreshToken = refreshJson.get("refreshToken").asText();

        assertNotNull(newAccessToken);
        assertFalse(newAccessToken.isBlank());
        assertNotNull(newRefreshToken);
        assertFalse(newRefreshToken.isBlank());

        assertNotEquals(originalAccessToken, newAccessToken);
        assertNotEquals(originalRefreshToken, newRefreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(originalRefreshToken))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(newRefreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void loginThenLogout_revokesRefreshToken() throws Exception {
        User user = createTestUser();

        LoginRequest loginRequest = new LoginRequest(user.getEmail(), "Password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String refreshToken = loginJson.get("refreshToken").asText();

        LogoutRequest logoutRequest = new LogoutRequest(refreshToken);

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isNoContent());

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshThenLogout_existingAccessTokenRemainsValidUntilExpiration() throws Exception {
        User user = createTestUser();

        LoginRequest loginRequest = new LoginRequest(user.getEmail(), "Password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String initialAccessToken = loginJson.get("token").asText();
        String initialRefreshToken = loginJson.get("refreshToken").asText();

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(initialRefreshToken))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode refreshJson = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String rotatedRefreshToken = refreshJson.get("refreshToken").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + initialAccessToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()));

        LogoutRequest logoutRequest = new LogoutRequest(rotatedRefreshToken);

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + initialAccessToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(rotatedRefreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidRefreshToken_returns401() throws Exception {
        String randomToken = "random-unpersisted-refresh-token-" + UUID.randomUUID();
        RefreshTokenRequest request = new RefreshTokenRequest(randomToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").doesNotExist());
    }
}
