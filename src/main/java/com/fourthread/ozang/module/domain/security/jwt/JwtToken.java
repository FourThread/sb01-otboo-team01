package com.fourthread.ozang.module.domain.security.jwt;

import com.fourthread.ozang.module.domain.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.Instant;
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

  @Column(columnDefinition = "varchar(512)", nullable = false, unique = true)
  private String accessToken;

  @Column(columnDefinition = "varchar(512)", nullable = false, unique = true)
  private String refreshToken;

  @Column(nullable = false)
  private Instant expiryDate;

  public boolean isExpired() {
    return this.expiryDate.isBefore(Instant.now());
  }

  public void update(String accessToken, String refreshToken, Instant expiryDate) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.expiryDate = expiryDate;
  }
}
