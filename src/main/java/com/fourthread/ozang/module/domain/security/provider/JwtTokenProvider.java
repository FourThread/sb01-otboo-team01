package com.fourthread.ozang.module.domain.security.provider;

import com.fourthread.ozang.module.domain.security.dto.JwtToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/*
JWT 토큰 생성, 복호화, 검증 기능 구현
 */
@Slf4j
@Component
public class JwtTokenProvider {

  private static final long ACCESS_TOKEN_EXPIRATION_MS = 1000 * 60 * 60 * 24; // 24시간
  private static final long REFRESH_TOKEN_EXPIRATION_MS = 1000 * 60 * 60 * 24; // 24시간
  private static final String AUTHORITIES_KEY = "auth";
  private static final String GRANT_TYPE = "Bearer";

  private final Key key;

  // yml에서 secret 값을 가져와서 key에 저장
  public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    this.key = Keys.hmacShaKeyFor(keyBytes);
  }

  // User 정보를 가지고 Access, Refresh 토큰을 생성
  public JwtToken generateToken(Authentication authentication) {
    String authorities = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.joining(","));

    long now = System.currentTimeMillis();

    String accessToken = createAccessToken(authentication.getName(), authorities, now);
    String refreshToken = createRefreshToken(now);

    return JwtToken.builder()
        .grantType(GRANT_TYPE)
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .build();
  }

  // Access Token 생성
  private String createAccessToken(String subject, String authorities, long now) {
    return Jwts.builder()
        .setSubject(subject)
        .claim(AUTHORITIES_KEY, authorities)
        .setExpiration(new Date(now + ACCESS_TOKEN_EXPIRATION_MS))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  // Refresh Token 생성
  private String createRefreshToken(long now) {
    return Jwts.builder()
        .setExpiration(new Date(now + REFRESH_TOKEN_EXPIRATION_MS))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  // Jwt 토큰을 복호화 해서 토큰에 들어있는 정보를 추출
  public Authentication getAuthentication(String accessToken) {
    Claims claims = parseClaims(accessToken);

    String authorities = claims.get(AUTHORITIES_KEY, String.class);
    if (authorities == null || authorities.isBlank()) {
      throw new JwtAuthenticationException("권한 정보가 없는 토큰입니다.");
    }

    Collection<? extends GrantedAuthority> grantedAuthorities = Arrays.stream(authorities.split(","))
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());

    UserDetails principal = new User(claims.getSubject(), "", grantedAuthorities);
    return new UsernamePasswordAuthenticationToken(principal, accessToken, grantedAuthorities);
  }

  // 토큰 정보를 검증
  public boolean validateToken(String token) {
    try {
      Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
      return true;
    } catch (SecurityException | MalformedJwtException e) {
      log.warn("잘못된 JWT 서명입니다: {}", e.getMessage());
    } catch (ExpiredJwtException e) {
      log.warn("만료된 JWT 토큰입니다: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      log.warn("지원하지 않는 JWT 토큰입니다: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      log.warn("비어있는 JWT claims입니다: {}", e.getMessage());
    }
    return false;
  }

  private Claims parseClaims(String token) {
    try {
      return Jwts.parserBuilder()
          .setSigningKey(key)
          .build()
          .parseClaimsJws(token)
          .getBody();
    } catch (ExpiredJwtException e) {
      return e.getClaims(); // 만료된 경우에도 Claims는 꺼냄
    }
  }

  public static class JwtAuthenticationException extends RuntimeException {
    public JwtAuthenticationException(String message) {
      super(message);
    }
  }
}
