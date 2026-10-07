package com.vasudev.shopsphere.repository;

import com.vasudev.shopsphere.entity.RefreshToken;
import com.vasudev.shopsphere.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUser(User user);
}
