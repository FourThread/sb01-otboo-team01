package com.fourthread.ozang.module.domain.security.jwt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourthread.ozang.module.common.exception.ErrorCode;
import com.fourthread.ozang.module.domain.user.dto.data.UserDto;
import com.fourthread.ozang.module.domain.user.dto.type.Role;
import com.fourthread.ozang.module.domain.user.exception.UserException;
import com.fourthread.ozang.module.domain.user.mapper.UserMapper;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.jsonwebtoken.Jwt;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

  @Value("${jwt.secret}")
  private String secret;
  @Value("${jwt.access-token-expiration-seconds}")
  private long accessTokenValiditySeconds;
  @Value("${jwt.refresh-token-expiration-seconds}")
  private long refreshTokenValiditySeconds;

  private final JwtTokenRepository jwtTokenRepository;
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final ObjectMapper objectMapper;
  private final JwtBlacklist jwtBlacklist;

  @Transactional
  public JwtToken registerJwtToken(JwtPayloadDto payloadDto) {
    log.info("[JwtService] 토큰 발급 요청 사용자 : {}", payloadDto.email());
    JwtDto accessJwtDto = generateJwtDto(payloadDto, accessTokenValiditySeconds);
    JwtDto refreshJwtDto = generateJwtDto(payloadDto, refreshTokenValiditySeconds);

    JwtToken JwtToken = new JwtToken(payloadDto.email(), accessJwtDto.token(),
        refreshJwtDto.token(), accessJwtDto.exp());
    jwtTokenRepository.save(JwtToken);
    log.info("[JwtService] 토큰 발급 완료 -> AccessToken 만료 시간 : {}, RefreshToken 만료 시간: {}", accessJwtDto.exp(), refreshJwtDto.exp());

    return JwtToken;
  }

  public boolean validate(String token) {
    try {
      JWSObject jwsObject = JWSObject.parse(token);
      JWSVerifier verifier = new MACVerifier(secret);

      if (!jwsObject.verify(verifier)) {
        log.warn("[JwtService] JWT 서명 검증 실패");
        return false;
      }

      JwtDto jwtDto = parse(token);
      if (jwtDto.isExpired()) {
        log.warn("[JwtService]  JWT 만료 - 만료 시간: {}", jwtDto.exp());
        return false;
      }

      if (jwtBlacklist.contains(token)) {
        log.warn("[JwtService] 블랙리스트 토큰 접근 차단");
        return false;
      }

      return true;

    } catch (JOSEException | ParseException e) {
      log.error("[JwtService] JWT 파싱 또는 검증 예외: {}", e.getMessage(), e);
      return false;
    }
  }

  public JwtDto parse(String token) {
    try {
      JWSObject jwsObject = JWSObject.parse(token);
      Payload payload = jwsObject.getPayload();
      Map<String, Object> jsonObject = payload.toJSONObject();

      UUID userId = UUID.fromString((String) jsonObject.get("userId"));
      String email = (String) jsonObject.get("email");
      String name = (String) jsonObject.get("name");
      Role role = Role.valueOf((String) jsonObject.get("role"));

      Instant issueTime = objectMapper.convertValue(jsonObject.get("iat"), Instant.class);
      Instant expirationTime = objectMapper.convertValue(jsonObject.get("exp"), Instant.class);

      JwtPayloadDto payloadDto = new JwtPayloadDto(userId, email, name, role);
      return new JwtDto(
          issueTime,
          expirationTime,
          payloadDto,
          token
      );
    } catch (ParseException e) {
      log.error(e.getMessage());
      throw new SecurityException("invalid token");
    }

  }

  @Transactional
  public JwtToken refreshJwtToken(String refreshToken) {
    log.info("[JwtService] RefreshToken 갱신 요청");
    if (!validate(refreshToken)) {
      log.info("[JwtService] 유효하지 않는 RefreshToken");
      throw new SecurityException("Refresh token invalid");
    }
    JwtToken session = jwtTokenRepository.findByRefreshToken(refreshToken)
        .orElseThrow(() -> new SecurityException("Token not found"));

    UUID userId = parse(refreshToken).payloadDto().userId();
    log.info("[JwtService] 사용자 ID : {} - Access Token 재발급", userId);
    UserDto userDto = userRepository.findById(userId)
        .map(userMapper::toDto)
        .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND, null, null));
    JwtPayloadDto payloadDto = UserDto.toJwtPayloadDto(userDto);
    log.info("[JwtService] AccessToken 생성 요청");
    JwtDto accessJwtDto = generateJwtDto(payloadDto, accessTokenValiditySeconds);
    log.info("[JwtService] RefreshToken 생성 요청");
    JwtDto refreshJwtDto = generateJwtDto(payloadDto, refreshTokenValiditySeconds);

    log.info("[JwtService] 토큰 발급 완료 -> AccessToken 만료 시간 : {}, RefreshToken 만료 시간: {}",
        accessJwtDto.exp(), refreshJwtDto.exp());

    session.update(
        accessJwtDto.token(),
        refreshJwtDto.token(),
        accessJwtDto.exp()
    );

    log.info("[JwtService] 토큰 갱신 완료 - 만료 : {}", accessJwtDto.exp());
    return session;
  }

  @Transactional
  public void invalidateJwtToken(String refreshToken) {
    jwtTokenRepository.findByRefreshToken(refreshToken)
        .ifPresent(this::invalidate);
  }

  @Transactional
  public void invalidateJwtTokenByEmail(String email) {
    jwtTokenRepository.findByEmail(email)
        .ifPresent(this::invalidate);
  }

  public JwtToken getJwtToken(String refreshToken) {
    log.info("[JwtService] refresh token을 이용해서 access token을 조회합니다");
    return jwtTokenRepository.findByRefreshToken(refreshToken)
        .orElseThrow(() -> new SecurityException("Token not Found"));
  }

  public List<JwtToken> getActiveJwtTokens() {
    return jwtTokenRepository.findAllByExpiryDateAfter(LocalDateTime.now());
  }

  private JwtDto generateJwtDto(JwtPayloadDto payloadDto, long tokenValiditySeconds) {
    Instant issueTime = Instant.now();
    Instant expirationTime = issueTime.plus(Duration.ofSeconds(tokenValiditySeconds));

    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .subject(payloadDto.email())
        .claim("userId", payloadDto.userId().toString())
        .claim("email", payloadDto.email())
        .claim("name", payloadDto.name())
        .claim("role", payloadDto.role().name())
        .issueTime(new Date(issueTime.toEpochMilli()))
        .expirationTime(new Date(expirationTime.toEpochMilli()))
        .build();

    JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
    SignedJWT signedJWT = new SignedJWT(header, claimsSet);

    try {
      signedJWT.sign(new MACSigner(secret));
    } catch (JOSEException e) {
      log.error("[JwtService] Jwt 서명 실패 : {}", e.getMessage());
      throw new SecurityException("Invalid Token");
    }

    return new JwtDto(issueTime, expirationTime, payloadDto, signedJWT.serialize());
  }

  private void invalidate(JwtToken session) {
    jwtTokenRepository.delete(session);
    log.info("[JwtService] 토큰 삭제 -> 이메일 : {}", session.getEmail());
    if (!session.isExpired()) {
      jwtBlacklist.put(session.getAccessToken(), session.getExpiryDate());
      log.info("[JwtService] 블랙 리스트 등록 - 만료 시간 : {}", session.getExpiryDate());
    }
  }
}
