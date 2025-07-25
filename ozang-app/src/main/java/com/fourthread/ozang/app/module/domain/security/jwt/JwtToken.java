//package com.fourthread.ozang.module.domain.security.jwt;
//
//import com.fourthread.ozang.module.domain.BaseUpdatableEntity;
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import java.time.Instant;
//import java.time.LocalDateTime;
//import lombok.AccessLevel;
//import lombok.AllArgsConstructor;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//
//@AllArgsConstructor
//@NoArgsConstructor(access = AccessLevel.PROTECTED)
//@Entity
//@Getter
//public class JwtToken extends BaseUpdatableEntity {
//
//  @Column(nullable = false, updatable = false, unique = true)
//  private String email;
//
//  @Column(columnDefinition = "varchar(512)", nullable = false, unique = true)
//  private String refreshToken;
//
//  @Column(nullable = false)
//  private Instant expiryDate;
//
//}
