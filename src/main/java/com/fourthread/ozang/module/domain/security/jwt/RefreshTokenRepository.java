package com.fourthread.ozang.module.domain.security.jwt;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByEmail(String email);

  void deleteByEmail(String email);

}
