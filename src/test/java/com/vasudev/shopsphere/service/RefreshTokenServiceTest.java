package com.vasudev.shopsphere.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vasudev.shopsphere.entity.RefreshToken;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.repository.RefreshTokenRepository;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository);
    }

    @Test
    void generateRefreshTokenValue_returnsValidOpaqueToken() {
        String token1 = refreshTokenService.generateRefreshTokenValue();
        String token2 = refreshTokenService.generateRefreshTokenValue();

        assertNotNull(token1);
        assertFalse(token1.isBlank());
        assertNotNull(token2);
        assertFalse(token2.isBlank());
        assertNotEquals(token1, token2);

        byte[] decodedBytes1 = Base64.getUrlDecoder().decode(token1);
        byte[] decodedBytes2 = Base64.getUrlDecoder().decode(token2);
        assertEquals(32, decodedBytes1.length);
        assertEquals(32, decodedBytes2.length);
    }

    @Test
    void createRefreshToken_savesHashedToken() {
        User user = new User();
        user.setEmail("user@shopsphere.com");
        String rawToken = "sample-refresh-token-opaque";
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken created = refreshTokenService.createRefreshToken(user, rawToken, expiresAt);

        assertNotNull(created);
        assertEquals(user, created.getUser());
        assertEquals(expiresAt, created.getExpiresAt());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken captured = captor.getValue();

        assertNotNull(captured.getTokenHash());
        assertNotEquals(rawToken, captured.getTokenHash());
        assertEquals(64, captured.getTokenHash().length());
        assertTrue(captured.getTokenHash().matches("^[0-9a-f]{64}$"));
    }

    @Test
    void findByToken_hashesRawTokenBeforeRepositoryLookup() {
        String rawToken = "sample-raw-token";
        RefreshToken refreshToken = new RefreshToken(new User(), "hash-placeholder", LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.findByTokenHash(any(String.class))).thenReturn(Optional.of(refreshToken));

        Optional<RefreshToken> result = refreshTokenService.findByToken(rawToken);

        assertTrue(result.isPresent());
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(refreshTokenRepository).findByTokenHash(hashCaptor.capture());
        String capturedHash = hashCaptor.getValue();

        assertNotNull(capturedHash);
        assertNotEquals(rawToken, capturedHash);
        assertEquals(64, capturedHash.length());
        assertTrue(capturedHash.matches("^[0-9a-f]{64}$"));
    }

    @Test
    void findByToken_identicalRawTokensProduceSameHash() {
        String rawToken = "sample-identical-token";

        refreshTokenService.findByToken(rawToken);
        refreshTokenService.findByToken(rawToken);

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(refreshTokenRepository, org.mockito.Mockito.times(2)).findByTokenHash(hashCaptor.capture());

        assertEquals(hashCaptor.getAllValues().get(0), hashCaptor.getAllValues().get(1));
    }

    @Test
    void findByToken_whenNotFound_returnsEmptyOptional() {
        String rawToken = "missing-token";
        when(refreshTokenRepository.findByTokenHash(any(String.class))).thenReturn(Optional.empty());

        Optional<RefreshToken> result = refreshTokenService.findByToken(rawToken);

        assertTrue(result.isEmpty());
        verify(refreshTokenRepository).findByTokenHash(any(String.class));
    }

    @Test
    void isExpired_withFutureExpiresAt_returnsFalse() {
        RefreshToken refreshToken = new RefreshToken(new User(), "dummyhash", LocalDateTime.now().plusDays(7));

        boolean expired = refreshTokenService.isExpired(refreshToken);

        assertFalse(expired);
    }

    @Test
    void isExpired_withPastExpiresAt_returnsTrue() {
        RefreshToken refreshToken = new RefreshToken(new User(), "dummyhash", LocalDateTime.now().minusMinutes(5));

        boolean expired = refreshTokenService.isExpired(refreshToken);

        assertTrue(expired);
    }

    @Test
    void isRevoked_withNullRevokedAt_returnsFalse() {
        RefreshToken refreshToken = new RefreshToken(new User(), "dummyhash", LocalDateTime.now().plusDays(7));

        boolean revoked = refreshTokenService.isRevoked(refreshToken);

        assertFalse(revoked);
    }

    @Test
    void isRevoked_withNonNullRevokedAt_returnsTrue() {
        RefreshToken refreshToken = new RefreshToken(new User(), "dummyhash", LocalDateTime.now().plusDays(7));
        refreshToken.setRevokedAt(LocalDateTime.now().minusMinutes(1));

        boolean revoked = refreshTokenService.isRevoked(refreshToken);

        assertTrue(revoked);
    }

    @Test
    void revokeToken_setsRevokedAtAndSaves() {
        RefreshToken refreshToken = new RefreshToken(new User(), "dummyhash", LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.save(refreshToken)).thenReturn(refreshToken);

        RefreshToken result = refreshTokenService.revokeToken(refreshToken);

        assertNotNull(result.getRevokedAt());
        verify(refreshTokenRepository).save(refreshToken);
    }

    @Test
    void deleteToken_deletesThroughRepository() {
        RefreshToken refreshToken = new RefreshToken(new User(), "dummyhash", LocalDateTime.now().plusDays(7));

        refreshTokenService.deleteToken(refreshToken);

        verify(refreshTokenRepository).delete(refreshToken);
    }
}
