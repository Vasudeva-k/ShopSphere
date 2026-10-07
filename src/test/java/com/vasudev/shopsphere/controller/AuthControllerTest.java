package com.vasudev.shopsphere.controller;

import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vasudev.shopsphere.config.SecurityConfig;
import com.vasudev.shopsphere.dto.LoginRequest;
import com.vasudev.shopsphere.dto.LogoutRequest;
import com.vasudev.shopsphere.dto.RefreshTokenRequest;
import com.vasudev.shopsphere.entity.RefreshToken;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.security.JwtAuthenticationFilter;
import com.vasudev.shopsphere.security.JwtService;
import com.vasudev.shopsphere.service.RefreshTokenService;
import com.vasudev.shopsphere.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
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

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "jwt.secret=ShopSphere-test-secret-key-2026-very-long-and-secure",
        "jwt.expiration=3600000"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    private User testUser;
    private String validToken;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        testUser = new User("Jane", "Doe", "jane.doe@shopsphere.com", "Password123", "9876543210");
        testUser.setId(42L);
        testUser.setEnabled(true);

        validToken = jwtService.generateToken(testUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void login_withValidCredentials_returnsAccessAndRefreshTokensAndPersistsRefreshToken() throws Exception {
        LoginRequest request = new LoginRequest("jane.doe@shopsphere.com", "Password123");
        String mockRefreshTokenValue = "mock-refresh-token-opaque-string";
        RefreshToken mockRefreshToken = new RefreshToken(testUser, mockRefreshTokenValue, LocalDateTime.now().plusDays(7));

        when(userService.authenticate(request.getEmail(), request.getPassword())).thenReturn(Optional.of(testUser));
        when(refreshTokenService.generateRefreshTokenValue()).thenReturn(mockRefreshTokenValue);
        when(refreshTokenService.createRefreshToken(eq(testUser), eq(mockRefreshTokenValue), any(LocalDateTime.class)))
                .thenReturn(mockRefreshToken);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").value(mockRefreshTokenValue))
                .andExpect(jsonPath("$.token").value(not(mockRefreshTokenValue)))
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.firstName").value(testUser.getFirstName()))
                .andExpect(jsonPath("$.lastName").value(testUser.getLastName()))
                .andExpect(jsonPath("$.phone").value(testUser.getPhone()))
                .andExpect(jsonPath("$.enabled").value(testUser.getEnabled()))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(refreshTokenService).createRefreshToken(eq(testUser), eq(mockRefreshTokenValue), any(LocalDateTime.class));
    }

    @Test
    void login_withInvalidCredentials_doesNotCreateRefreshTokenAndReturns401() throws Exception {
        LoginRequest request = new LoginRequest("jane.doe@shopsphere.com", "WrongPassword");

        when(userService.authenticate(request.getEmail(), request.getPassword())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(refreshTokenService, never()).generateRefreshTokenValue();
        verify(refreshTokenService, never()).createRefreshToken(any(), any(), any());
    }

    @Test
    void refresh_withValidRefreshToken_rotatesRefreshTokenAndReturnsBothNewTokens() throws Exception {
        String oldRefreshTokenValue = "old-valid-refresh-token";
        String newRefreshTokenValue = "new-rotated-refresh-token";

        RefreshToken oldTokenEntity = new RefreshToken(testUser, oldRefreshTokenValue, LocalDateTime.now().plusDays(7));
        RefreshToken newTokenEntity = new RefreshToken(testUser, newRefreshTokenValue, LocalDateTime.now().plusDays(7));
        RefreshTokenRequest request = new RefreshTokenRequest(oldRefreshTokenValue);

        when(refreshTokenService.findByToken(oldRefreshTokenValue)).thenReturn(Optional.of(oldTokenEntity));
        when(refreshTokenService.isExpired(oldTokenEntity)).thenReturn(false);
        when(refreshTokenService.isRevoked(oldTokenEntity)).thenReturn(false);
        when(refreshTokenService.generateRefreshTokenValue()).thenReturn(newRefreshTokenValue);
        when(refreshTokenService.revokeToken(oldTokenEntity)).thenReturn(oldTokenEntity);
        when(refreshTokenService.createRefreshToken(eq(testUser), eq(newRefreshTokenValue), any(LocalDateTime.class)))
                .thenReturn(newTokenEntity);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").value(newRefreshTokenValue))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());

        verify(refreshTokenService).revokeToken(oldTokenEntity);
        verify(refreshTokenService).createRefreshToken(eq(testUser), eq(newRefreshTokenValue), any(LocalDateTime.class));
    }

    @Test
    void refresh_withNonExistentToken_returns401() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("nonexistent-token");

        when(refreshTokenService.findByToken("nonexistent-token")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(refreshTokenService, never()).revokeToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any(), any(), any());
    }

    @Test
    void refresh_withExpiredToken_returns401() throws Exception {
        String refreshTokenValue = "expired-refresh-token";
        RefreshToken expiredToken = new RefreshToken(testUser, refreshTokenValue, LocalDateTime.now().minusDays(1));
        RefreshTokenRequest request = new RefreshTokenRequest(refreshTokenValue);

        when(refreshTokenService.findByToken(refreshTokenValue)).thenReturn(Optional.of(expiredToken));
        when(refreshTokenService.isExpired(expiredToken)).thenReturn(true);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(refreshTokenService, never()).revokeToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any(), any(), any());
    }

    @Test
    void refresh_withRevokedToken_returns401() throws Exception {
        String refreshTokenValue = "revoked-refresh-token";
        RefreshToken revokedToken = new RefreshToken(testUser, refreshTokenValue, LocalDateTime.now().plusDays(7));
        revokedToken.setRevokedAt(LocalDateTime.now().minusMinutes(10));
        RefreshTokenRequest request = new RefreshTokenRequest(refreshTokenValue);

        when(refreshTokenService.findByToken(refreshTokenValue)).thenReturn(Optional.of(revokedToken));
        when(refreshTokenService.isExpired(revokedToken)).thenReturn(false);
        when(refreshTokenService.isRevoked(revokedToken)).thenReturn(true);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(refreshTokenService, never()).revokeToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any(), any(), any());
    }

    @Test
    void refresh_withBlankToken_returns400() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(refreshTokenService, never()).revokeToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any(), any(), any());
    }

    @Test
    void logout_withValidRefreshToken_revokesTokenAndReturns204() throws Exception {
        String refreshTokenValue = "valid-logout-token";
        RefreshToken token = new RefreshToken(testUser, refreshTokenValue, LocalDateTime.now().plusDays(7));
        LogoutRequest request = new LogoutRequest(refreshTokenValue);

        when(refreshTokenService.findByToken(refreshTokenValue)).thenReturn(Optional.of(token));
        when(refreshTokenService.isRevoked(token)).thenReturn(false);

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(refreshTokenService).revokeToken(token);
    }

    @Test
    void logout_withUnknownToken_returns404() throws Exception {
        String refreshTokenValue = "unknown-logout-token";
        LogoutRequest request = new LogoutRequest(refreshTokenValue);

        when(refreshTokenService.findByToken(refreshTokenValue)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(refreshTokenService, never()).revokeToken(any());
    }

    @Test
    void logout_withAlreadyRevokedToken_returns204() throws Exception {
        String refreshTokenValue = "already-revoked-logout-token";
        RefreshToken token = new RefreshToken(testUser, refreshTokenValue, LocalDateTime.now().plusDays(7));
        token.setRevokedAt(LocalDateTime.now().minusMinutes(5));
        LogoutRequest request = new LogoutRequest(refreshTokenValue);

        when(refreshTokenService.findByToken(refreshTokenValue)).thenReturn(Optional.of(token));
        when(refreshTokenService.isRevoked(token)).thenReturn(true);

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(refreshTokenService, never()).revokeToken(any());
    }

    @Test
    void logout_withExpiredToken_stillRevokesAndReturns204() throws Exception {
        String refreshTokenValue = "expired-logout-token";
        RefreshToken token = new RefreshToken(testUser, refreshTokenValue, LocalDateTime.now().minusDays(1));
        LogoutRequest request = new LogoutRequest(refreshTokenValue);

        when(refreshTokenService.findByToken(refreshTokenValue)).thenReturn(Optional.of(token));
        when(refreshTokenService.isRevoked(token)).thenReturn(false);

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(refreshTokenService).revokeToken(token);
    }

    @Test
    void logout_withBlankToken_returns400() throws Exception {
        LogoutRequest request = new LogoutRequest("");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(refreshTokenService, never()).findByToken(any());
    }

    @Test
    void authenticatedUser_canAccessMeEndpoint() throws Exception {
        when(userService.getUserByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.firstName").value(testUser.getFirstName()))
                .andExpect(jsonPath("$.lastName").value(testUser.getLastName()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.phone").value(testUser.getPhone()))
                .andExpect(jsonPath("$.enabled").value(testUser.getEnabled()));
    }

    @Test
    void meEndpoint_doesNotExposePassword() throws Exception {
        when(userService.getUserByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void unauthenticatedUser_cannotAccessMeEndpoint() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void meEndpoint_withExpiredToken_returns403Forbidden() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor("ShopSphere-test-secret-key-2026-very-long-and-secure".getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiredDate = new Date(now.getTime() - 60000L);

        String expiredToken = Jwts.builder()
                .subject(testUser.getEmail())
                .claim("userId", testUser.getId())
                .issuedAt(new Date(now.getTime() - 120000L))
                .expiration(expiredDate)
                .signWith(key)
                .compact();

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(userService, never()).getUserByEmail(any());
    }
}

