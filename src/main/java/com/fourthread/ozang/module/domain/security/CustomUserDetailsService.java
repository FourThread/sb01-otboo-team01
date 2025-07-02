package com.fourthread.ozang.module.domain.security;

import com.fourthread.ozang.module.domain.security.jwt.JwtPayloadDto;
import com.fourthread.ozang.module.domain.user.dto.data.UserDto;
import com.fourthread.ozang.module.domain.user.mapper.UserMapper;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {

  @Value("${security.temp-password.expiration-hours}")
  private long expirationHours;
  private final UserRepository userRepository;
  private final UserMapper userMapper;

  @Transactional(readOnly = true)
  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    log.info("[DaoAuthenticationProvider] -> [UserDetailsService]를 내부적으로 호출합니다");
    log.info("[UserDetailsService] 이메일을 통해서 사용자를 조회합니다");
    return userRepository.findByEmail(email)
        .map(user -> {
          log.info("[UserDetailsService] {} 사용자를 발견했습니다", email);
          if (user.getTempPasswordIssuedAt() != null && user.getTempPasswordIssuedAt().plusHours(expirationHours)
              .isBefore(LocalDateTime.now())) {
            log.warn("[UserDetailsService] {} 사용자의 임시 비밀번호 유효시간이 만료되었습니다", email);
            throw new CredentialsExpiredException("임시 비밀번호 유효시간이 만료되었습니다.");
          }
          UserDto userDto = userMapper.toDto(user);
          JwtPayloadDto payloadDto = UserDto.toJwtPayloadDto(userDto);
          String password = user.getPassword();
          log.info("[UserDetailsService] UserDetails 객체를 반환합니다");
          return new UserDetailsImpl(payloadDto, password);
        })
        .orElseThrow(() -> {
          log.warn("[UserDetailsService] {} 사용자를 찾을 수 없습니다", email);
          return new UsernameNotFoundException("User not found by email: " + email);
        });
  }
}