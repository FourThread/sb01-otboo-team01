package com.fourthread.ozang.module.domain.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourthread.ozang.module.common.exception.ErrorCode;
import com.fourthread.ozang.module.domain.user.dto.data.UserDto;
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
  @Value("${jwt.access-token-expiration-ms}")
  private long accessTokenValiditySeconds;
  @Value("${jwt.refresh-token-expiration-ms}")
  private long refreshTokenValiditySeconds;

  private final JwtTokenRepository jwtTokenRepository;
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final ObjectMapper objectMapper;
  private final JwtBlacklist jwtBlacklist;

  @Transactional
  public JwtToken registerJwtToken(UserDto userDto) {
    JwtDto accessJwtDto = generateJwtDto(userDto, accessTokenValiditySeconds);
    JwtDto refreshJwtDto = generateJwtDto(userDto, refreshTokenValiditySeconds);

    JwtToken JwtToken = new JwtToken(userDto.email(), accessJwtDto.token(),
        refreshJwtDto.token(), accessJwtDto.exp());
    jwtTokenRepository.save(JwtToken);

    return JwtToken;
  }

  public boolean validate(String token) {
    boolean verified;

    try {
      JWSVerifier verifier = new MACVerifier(secret);
      JWSObject jwsObject = JWSObject.parse(token);
      verified = jwsObject.verify(verifier);

      if (verified) {
        JwtDto JwtDto = parse(token);
        verified = !JwtDto.isExpired();
      }

      if (verified) {
        verified = !jwtBlacklist.contains(token);
      }

    } catch (JOSEException | ParseException e) {
      log.error(e.getMessage());
      verified = false;
    }

    return verified;
  }

  public JwtDto parse(String token) {
    try {
      JWSObject jwsObject = JWSObject.parse(token);
      Payload payload = jwsObject.getPayload();
      Map<String, Object> jsonObject = payload.toJSONObject();
      return new JwtDto(
          objectMapper.convertValue(jsonObject.get("iat"), LocalDateTime.class),
          objectMapper.convertValue(jsonObject.get("exp"), LocalDateTime.class),
          objectMapper.convertValue(jsonObject.get("userDto"), UserDto.class),
          token
      );
    } catch (ParseException e) {
      log.error(e.getMessage());
      throw new SecurityException("invalid token");
    }

  }

  @Transactional
  public JwtToken refreshJwtToken(String refreshToken) {
    if (!validate(refreshToken)) {
      throw new SecurityException("Refresh token invalid");
    }
    JwtToken session = jwtTokenRepository.findByRefreshToken(refreshToken)
        .orElseThrow(() -> new SecurityException("Token not found"));

    UUID userId = parse(refreshToken).userDto().id();
    UserDto userDto = userRepository.findById(userId)
        .map(userMapper::toDto)
        .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND, null, null));
    JwtDto accessJwtDto = generateJwtDto(userDto, accessTokenValiditySeconds);
    JwtDto refreshJwtDto = generateJwtDto(userDto, refreshTokenValiditySeconds);

    session.update(
        accessJwtDto.token(),
        refreshJwtDto.token(),
        accessJwtDto.exp()
    );

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
    return jwtTokenRepository.findByRefreshToken(refreshToken)
        .orElseThrow(() -> new SecurityException("Token not Found"));
  }

  public List<JwtToken> getActiveJwtTokens() {
    return jwtTokenRepository.findAllByExpirationTimeAfter(Instant.now());
  }

  private JwtDto generateJwtDto(UserDto userDto, long tokenValiditySeconds) {
    LocalDateTime issueTime = LocalDateTime.now();
    LocalDateTime expirationTime = issueTime.plus(Duration.ofSeconds(tokenValiditySeconds));

    Instant issueInstant = issueTime.atZone(ZoneId.of("UTC")).toInstant();
    Instant expirationInstant = expirationTime.atZone(ZoneId.of("UTC")).toInstant();

    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .subject(userDto.email())
        .claim("userDto", userDto)
        .issueTime(Date.from(issueInstant))
        .expirationTime(Date.from(expirationInstant))
        .build();

    JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
    SignedJWT signedJWT = new SignedJWT(header, claimsSet);

    try {
      signedJWT.sign(new MACSigner(secret));
    } catch (JOSEException e) {
      log.error(e.getMessage());
      throw new SecurityException("Invalid Token");
    }

    return new JwtDto(issueTime, expirationTime, userDto, signedJWT.serialize());
  }

  private void invalidate(JwtToken session) {
    jwtTokenRepository.delete(session);
    if (!session.isExpired()) {
      jwtBlacklist.put(session.getAccessToken(), session.getExpiryDate());
    }
  }
}
