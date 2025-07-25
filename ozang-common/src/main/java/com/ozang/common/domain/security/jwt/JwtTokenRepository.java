//package com.fourthread.ozang.module.domain.security.jwt;
//
//import java.time.Instant;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Modifying;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//
//public interface JwtTokenRepository extends JpaRepository<JwtToken, Long> {
//
//  Optional<JwtToken> findByRefreshToken(String refreshToken);
//
//  Optional<JwtToken> findByEmail(String email);
//
//  /**
//   * 만료된 토큰 조회 (배치용)
//   */
//  @Query("SELECT j FROM JwtToken j WHERE j.expiryDate < :currentTime")
//  List<JwtToken> findExpiredTokens(@Param("currentTime") Instant currentTime);
//
//  /**
//   * 만료된 토큰 삭제 (배치용)
//   */
//  @Modifying
//  @Query("DELETE FROM JwtToken j WHERE j.expiryDate < :currentTime")
//  void deleteExpiredTokens(@Param("currentTime") Instant currentTime);
//
//  /**
//   * 만료된 토큰 개수 조회
//   */
//  @Query("SELECT COUNT(j) FROM JwtToken j WHERE j.expiryDate < :currentTime")
//  long countExpiredTokens(@Param("currentTime") Instant currentTime);
//}