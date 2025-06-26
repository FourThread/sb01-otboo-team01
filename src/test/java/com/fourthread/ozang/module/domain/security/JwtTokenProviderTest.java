package com.fourthread.ozang.module.domain.security;

import com.fourthread.ozang.module.domain.security.dto.JwtToken;
import com.fourthread.ozang.module.domain.security.provider.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Collections;
import java.util.Date;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

public class JwtTokenProviderTest {

  private JwtTokenProvider jwtTokenProvider;

  private final String SECRET_KEY = "6d0e6ea1e5aaa8e2e3142b8b75b474e947bfa2845dec10abd9359656f3de6ee8";

  @BeforeEach
  void setUp() {
    jwtTokenProvider = new JwtTokenProvider(SECRET_KEY);
  }

  @Test
  @DisplayName("AccessToken, RefreshToken 생성 테스트하기")
  void generateToken_success() {
    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");
    Authentication authentication = new UsernamePasswordAuthenticationToken("test@test.com",
        "TestPass123!!");

    JwtToken jwtToken = jwtTokenProvider.generateToken(authentication);

    assertThat(jwtToken).isNotNull();
    assertThat(jwtToken.accessToken()).isNotBlank();
    assertThat(jwtToken.refreshToken()).isNotBlank();
    assertThat(jwtToken.grantType()).isEqualTo("Bearer");
  }

  @Test
  @DisplayName("AccessToken에서 인증 정보를 추출합니다.")
  void getAuthentication_success() {
    Authentication auth = new UsernamePasswordAuthenticationToken("test@example.com", null,
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    String token = jwtTokenProvider.generateToken(auth).accessToken();

    Authentication authentication = jwtTokenProvider.getAuthentication(token);

    assertThat(authentication).isNotNull();
    assertThat(authentication.getName()).isEqualTo("test@example.com");
    assertThat(authentication.getAuthorities()).extracting("authority").contains("ROLE_USER");
  }

  @Test
  @DisplayName("유효한 토큰인지 검증 가능합니다.")
  void validateToken_success() {
    Authentication auth = new UsernamePasswordAuthenticationToken(
        "test@example.com",
        null,
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
    );
    ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpirationMs", 600_000L);
    ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpirationMs", 86_400_000L);

    String token = jwtTokenProvider.generateToken(auth).accessToken();

    assertThat(jwtTokenProvider.validateToken(token)).isTrue();
  }

  @Test
  @DisplayName("만료된 토큰은 false를 반환합니다.")
  void validateToken_expired() {
    Date now = new Date();
    String expiredToken = Jwts.builder()
        .setSubject("expired@expired.com")
        .setExpiration(new Date(now.getTime() - 1000))
        .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_KEY)))
        .compact();

    Boolean valid = jwtTokenProvider.validateToken(expiredToken);

    assertThat(valid).isFalse();
  }
}
