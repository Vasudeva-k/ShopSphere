package com.vasudev.shopsphere.controller;

import com.vasudev.shopsphere.dto.LoginRequest;
import com.vasudev.shopsphere.dto.LoginResponse;
import com.vasudev.shopsphere.dto.LogoutRequest;
import com.vasudev.shopsphere.dto.RefreshTokenRequest;
import com.vasudev.shopsphere.dto.RefreshTokenResponse;
import com.vasudev.shopsphere.dto.UserResponse;
import com.vasudev.shopsphere.entity.RefreshToken;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.security.JwtService;
import com.vasudev.shopsphere.service.RefreshTokenService;
import com.vasudev.shopsphere.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and JWT token operations")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(UserService userService, JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user credentials and returns JWT access and refresh tokens", security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully authenticated"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or account disabled")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return userService.authenticate(request.getEmail(), request.getPassword())
                .map(user -> {
                    String token = jwtService.generateToken(user);
                    String refreshTokenValue = refreshTokenService.generateRefreshTokenValue();
                    LocalDateTime refreshTokenExpiry = LocalDateTime.now().plusDays(7);
                    refreshTokenService.createRefreshToken(user, refreshTokenValue, refreshTokenExpiry);
                    return ResponseEntity.ok(toLoginResponse(user, token, refreshTokenValue));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Rotates refresh token and returns a new access token and refresh token", security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Expired, revoked, or invalid refresh token")
    })
    public ResponseEntity<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return refreshTokenService.findByToken(request.getRefreshToken())
                .filter(oldToken -> !refreshTokenService.isExpired(oldToken))
                .filter(oldToken -> !refreshTokenService.isRevoked(oldToken))
                .map(oldToken -> {
                    User user = oldToken.getUser();
                    String newAccessToken = jwtService.generateToken(user);
                    String newRefreshTokenValue = refreshTokenService.generateRefreshTokenValue();
                    LocalDateTime newExpiry = LocalDateTime.now().plusDays(7);

                    refreshTokenService.revokeToken(oldToken);
                    refreshTokenService.createRefreshToken(user, newRefreshTokenValue, newExpiry);

                    return ResponseEntity.ok(new RefreshTokenResponse(newAccessToken, newRefreshTokenValue));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Revokes the specified refresh token", security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logged out successfully / token revoked"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "404", description = "Refresh token not found")
    })
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        Optional<RefreshToken> tokenOptional = refreshTokenService.findByToken(request.getRefreshToken());
        if (tokenOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        RefreshToken refreshToken = tokenOptional.get();
        if (!refreshTokenService.isRevoked(refreshToken)) {
            refreshTokenService.revokeToken(refreshToken);
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user", description = "Returns details of the currently authenticated user from JWT token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user profile retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        return userService.getUserByEmail(email)
                .map(user -> ResponseEntity.ok(toUserResponse(user)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    private LoginResponse toLoginResponse(User user, String token, String refreshToken) {
        return new LoginResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getEnabled(),
                token,
                refreshToken
        );
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
