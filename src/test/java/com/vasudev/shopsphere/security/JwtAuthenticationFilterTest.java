package com.vasudev.shopsphere.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.vasudev.shopsphere.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private FilterChain filterChain;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private final String testSecret = "ShopSphere-test-secret-key-2026-very-long-and-secure";
    private final long testExpiration = 3600000L;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", testSecret);
        ReflectionTestUtils.setField(jwtService, "expiration", testExpiration);

        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService);
        filterChain = mock(FilterChain.class);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validToken_setsAuthenticatedUser() throws ServletException, IOException {
        User user = new User("John", "Doe", "john.doe@shopsphere.com", "Password123", "1234567890");
        user.setId(100L);
        String token = jwtService.generateToken(user);

        request.addHeader("Authorization", "Bearer " + token);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        assertEquals("john.doe@shopsphere.com", authentication.getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void missingAuthorizationHeader_doesNotAuthenticate() throws ServletException, IOException {
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void nonBearerAuthorizationHeader_doesNotAuthenticate() throws ServletException, IOException {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNzd29yZA==");

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void invalidToken_doesNotAuthenticate() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer invalid.malformed.token");

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void expiredToken_doesNotAuthenticate() throws ServletException, IOException {
        SecretKey key = Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiredDate = new Date(now.getTime() - 60000L);

        String expiredToken = Jwts.builder()
                .subject("expired.user@shopsphere.com")
                .claim("userId", 200L)
                .issuedAt(new Date(now.getTime() - 120000L))
                .expiration(expiredDate)
                .signWith(key)
                .compact();

        request.addHeader("Authorization", "Bearer " + expiredToken);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void invalidSignatureToken_doesNotAuthenticate() throws ServletException, IOException {
        String differentSecret = "A-different-test-secret-key-2026-very-long-and-secure";
        SecretKey differentKey = Keys.hmacShaKeyFor(differentSecret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + testExpiration);

        String tamperedToken = Jwts.builder()
                .subject("tampered.user@shopsphere.com")
                .claim("userId", 300L)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(differentKey)
                .compact();

        request.addHeader("Authorization", "Bearer " + tamperedToken);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);
        verify(filterChain).doFilter(request, response);
    }
}

