package com.fourthread.ozang.module.domain.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourthread.ozang.module.domain.security.jwt.JwtBlacklist;
import com.fourthread.ozang.module.domain.security.jwt.JwtDto;
import com.fourthread.ozang.module.domain.security.jwt.JwtPayloadDto;
import com.fourthread.ozang.module.domain.security.jwt.JwtService;
import com.fourthread.ozang.module.domain.security.jwt.JwtToken;
import com.fourthread.ozang.module.domain.security.jwt.JwtTokenRepository;
import com.fourthread.ozang.module.domain.user.dto.type.Role;
import com.fourthread.ozang.module.domain.user.mapper.UserMapper;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

  @InjectMocks
  private JwtService jwtService;

  @Mock
  private JwtTokenRepository jwtTokenRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserMapper userMapper;

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private JwtBlacklist jwtBlacklist;

  private String secret;

  @BeforeEach
  void setUp() {
    secret= "abcdefghijklmnopqrstuvwxyz123456";
    ReflectionTestUtils.setField(jwtService, "secret", secret);
    ReflectionTestUtils.setField(jwtService, "accessTokenValiditySeconds", 60L);  // 1분
    ReflectionTestUtils.setField(jwtService, "refreshTokenValiditySeconds", 120L); // 2분
  }

  @Test
  void testRegisterAndParseJwtToken() {
    // given
    UUID userId = UUID.randomUUID();
    JwtPayloadDto payload = new JwtPayloadDto(userId, "test@email.com", "tester", Role.USER);

    // when
    JwtToken token = jwtService.registerJwtToken(payload);
    JwtDto parsed = jwtService.parse(token.getAccessToken());

    // then
    assertThat(parsed.payloadDto().userId()).isEqualTo(payload.userId());
    assertThat(parsed.payloadDto().email()).isEqualTo(payload.email());
    assertThat(parsed.payloadDto().name()).isEqualTo(payload.name());
    assertThat(parsed.payloadDto().role()).isEqualTo(payload.role());
  }

  @Test
  void testValidate_shouldReturnFalseForTamperedToken() {
    String tamperedToken = "invalid.token.value";

    boolean valid = jwtService.validate(tamperedToken);

    assertThat(valid).isFalse();
  }
}
