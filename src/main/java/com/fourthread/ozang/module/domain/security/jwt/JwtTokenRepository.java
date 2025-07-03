package com.fourthread.ozang.module.domain.security.jwt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JwtTokenRepository extends JpaRepository<JwtToken, Long> {

  Optional<JwtToken> findByRefreshToken(String refreshToken);

  Optional<JwtToken> findByEmail(String email);

  List<JwtToken> findAllByExpiryDateAfter(Instant after);
}