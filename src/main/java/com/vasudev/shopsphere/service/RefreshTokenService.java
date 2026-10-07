package com.vasudev.shopsphere.service;

import com.vasudev.shopsphere.entity.RefreshToken;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String generateRefreshTokenValue() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public RefreshToken createRefreshToken(User user, String token, LocalDateTime expiresAt) {
        String tokenHash = hashToken(token);
        RefreshToken refreshToken = new RefreshToken(user, tokenHash, expiresAt);
        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        String tokenHash = hashToken(token);
        return refreshTokenRepository.findByTokenHash(tokenHash);
    }

    public boolean isExpired(RefreshToken refreshToken) {
        return LocalDateTime.now().isAfter(refreshToken.getExpiresAt());
    }

    public boolean isRevoked(RefreshToken refreshToken) {
        return refreshToken.getRevokedAt() != null;
    }

    public RefreshToken revokeToken(RefreshToken refreshToken) {
        refreshToken.setRevokedAt(LocalDateTime.now());
        return refreshTokenRepository.save(refreshToken);
    }

    public void deleteToken(RefreshToken refreshToken) {
        refreshTokenRepository.delete(refreshToken);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
