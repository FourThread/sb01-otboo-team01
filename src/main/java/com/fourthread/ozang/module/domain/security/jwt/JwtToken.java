package com.fourthread.ozang.module.domain.security.jwt;

import com.fourthread.ozang.module.domain.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Getter
public class JwtToken extends BaseUpdatableEntity {

  @Column(nullable = false, updatable = false)
  private String email;

  @Column(nullable = false, unique = true)
  private String accessToken;

  @Column(nullable = false, unique = true)
  private String refreshToken;

  @Column(nullable = false)
  private LocalDateTime expiryDate;

  public boolean isExpired() {
    return this.expiryDate.isBefore(LocalDateTime.now());
  }

  public void update(String accessToken, String refreshToken, LocalDateTime expiryDate) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.expiryDate = expiryDate;
  }
}
