package com.fourthread.ozang.module.domain.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourthread.ozang.module.common.exception.ErrorCode;
import com.fourthread.ozang.module.domain.security.jwt.dto.data.JwtDto;
import com.fourthread.ozang.module.domain.security.jwt.dto.data.JwtPayloadDto;
import com.fourthread.ozang.module.domain.security.jwt.dto.response.JwtTokenResponse;
import com.fourthread.ozang.module.domain.security.jwt.dto.type.TokenType;
import com.fourthread.ozang.module.domain.user.dto.type.Role;
import com.fourthread.ozang.module.domain.user.exception.UserException;
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
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
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

  //  private final JwtTokenRepository jwtTokenRepository;
  private final RedisDao redisDao;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;
  private final JwtBlacklist jwtBlacklist;

  @Transactional
  public JwtTokenResponse registerJwtToken(JwtPayloadDto payloadDto) {
    log.info("[JwtService] 토큰 발급 요청 사용자 : {}", payloadDto.email());
    JwtDto accessJwtDto = generateJwtDto(payloadDto, accessTokenValiditySeconds);
    JwtDto refreshJwtDto = generateJwtDto(payloadDto, refreshTokenValiditySeconds);

    redisDao.setValues("refresh:" + payloadDto.email(), refreshJwtDto.token(), Duration.ofSeconds(refreshTokenValiditySeconds));
    log.info("[JwtService] 토큰 발급 완료 -> AccessToken 만료 시간 : {}, RefreshToken 만료 시간: {}", accessJwtDto.exp(), refreshJwtDto.exp());

    return new JwtTokenResponse(accessJwtDto.token(),
        refreshJwtDto.token());
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
  public JwtTokenResponse refreshJwtToken(String refreshToken) {
    log.info("[JwtService] RefreshToken 갱신 요청");
    if (!validate(refreshToken)) {
      log.info("[JwtService] 유효하지 않는 RefreshToken");
      throw new SecurityException("Refresh token invalid");
    }

    UUID userId = parse(refreshToken).payloadDto().userId();
    log.info("[JwtService] 사용자 ID : {} - Access Token 재발급", userId);
    JwtPayloadDto payloadDto = userRepository.findById(userId)
        .map(JwtPayloadDto::toJwtPayloadDto)
        .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND, null, null));
    log.info("[JwtService] AccessToken 생성 요청");
    JwtDto accessJwtDto = generateJwtDto(payloadDto, accessTokenValiditySeconds);

    log.info("[JwtService] 토큰 발급 완료 -> AccessToken 만료 시간 : {}",
        accessJwtDto.exp());

    log.info("[JwtService] 토큰 갱신 완료 - 만료 : {}", accessJwtDto.exp());
    return new JwtTokenResponse(accessJwtDto.token(), refreshToken);
  }

  @Transactional
  public void invalidateRefreshToken(String refreshToken) {
    JwtDto jwtDto = parse(refreshToken);
    String key = "refresh:" + jwtDto.payloadDto().email();

    redisDao.delete(key);

    if (!jwtDto.isExpired()) {
      jwtBlacklist.put(refreshToken, jwtDto.exp(), TokenType.REFRESH);
    }
  }

  @Transactional
  public void invalidateJwtTokenByEmail(String email) {
    String key = "refresh:" + email;
    String token = (String) redisDao.getValue(key);

    if (token != null) {
      invalidateRefreshToken(token);
    }
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

  public void invalidateAccessToken(String accessToken) {
    JwtDto jwtDto = parse(accessToken);
    jwtBlacklist.put(accessToken, jwtDto.exp(), TokenType.ACCESS);
    log.info("[JwtService - invalidateAccessToken] AccessToken 블랙리스트 등록 - 만료 시간 : {}", jwtDto.exp());
  }
}