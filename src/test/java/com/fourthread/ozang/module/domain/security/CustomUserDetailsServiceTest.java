package com.fourthread.ozang.module.domain.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.fourthread.ozang.module.domain.security.jwt.JwtPayloadDto;
import com.fourthread.ozang.module.domain.user.dto.data.UserDto;
import com.fourthread.ozang.module.domain.user.dto.type.Role;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.mapper.UserMapper;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

class CustomUserDetailsServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserMapper userMapper;

  @InjectMocks
  private CustomUserDetailsService userDetailsService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    ReflectionTestUtils.setField(userDetailsService, "expirationHours", 3L); // 설정값 수동 주입
  }

  @Test
  @DisplayName("정상 사용자일 경우 UserDetailsImpl 반환")
  void loadUserByUsername_success() {
    // given
    String email = "test@email.com";
    String encodedPassword = "encoded-password";
    LocalDateTime now = LocalDateTime.now();

    User user = new User("tester", email, encodedPassword, Role.USER);
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(user, "createdAt", now);
    user.setTempPasswordIssuedAt(null);

    UserDto userDto = new UserDto(
        (UUID) ReflectionTestUtils.getField(user, "id"),
        now,
        email,
        "tester",
        Role.USER,
        null,
        false
    );

    given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
    given(userMapper.toDto(user)).willReturn(userDto);

    // when
    var userDetails = userDetailsService.loadUserByUsername(email);

    // then
    assertNotNull(userDetails);
    assertEquals(email, userDetails.getUsername());
    assertEquals(encodedPassword, userDetails.getPassword());
  }

  @Test
  @DisplayName("임시 비밀번호가 만료되었을 경우 CredentialsExpiredException 발생")
  void loadUserByUsername_tempPasswordExpired() {
    // given
    String email = "expired@email.com";
    User user = new User("tester", email, "pass", Role.USER);
    user.setTempPasswordIssuedAt(LocalDateTime.now().minusHours(5));
    given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

    // when & then
    assertThrows(CredentialsExpiredException.class, () -> userDetailsService.loadUserByUsername(email));
  }

  @Test
  @DisplayName("이메일로 사용자를 찾지 못한 경우 UsernameNotFoundException 발생")
  void loadUserByUsername_userNotFound() {
    // given
    String email = "unknown@email.com";
    given(userRepository.findByEmail(email)).willReturn(Optional.empty());

    // when & then
    assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(email));
  }
}
