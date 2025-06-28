package com.fourthread.ozang.module.domain.security;

import com.fourthread.ozang.module.domain.user.dto.data.UserDto;
import com.fourthread.ozang.module.domain.user.mapper.UserMapper;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;

  @Transactional(readOnly = true)
  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    log.debug("[CustomUserDetailsService] 이메일을 통해서 사용자를 조회합니다");
    return userRepository.findByEmail(email)
        .map(user -> {
          log.info("[CustomUserDetailsService] {} 사용자를 발견했습니다", email);
          UserDto userDto = userMapper.toDto(user);
          String password = user.getPassword();
          return new UserDetailsImpl(userDto, password);
        })
        .orElseThrow(() -> {
          log.warn("[CustomUserDetailsService] {} 사용자를 찾을 수 없습니다", email);
          return new UsernameNotFoundException("User not found by email: " + email);
        });
  }
}
