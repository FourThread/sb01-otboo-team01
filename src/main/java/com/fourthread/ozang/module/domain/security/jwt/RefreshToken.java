package com.fourthread.ozang.module.domain.security.jwt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Getter
public class RefreshToken {

  @Id
  private String email;

  @Column(nullable = false)
  private String token;

  @Column(nullable = false)
  private LocalDateTime expiryDate;

  public void update(String token, LocalDateTime expiryDate) {
    this.token = token;
    this.expiryDate = expiryDate;
  }
}
