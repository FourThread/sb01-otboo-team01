package com.fourthread.ozang.module.domain.security.service.impl;

import com.fourthread.ozang.module.domain.security.service.AuthService;
import com.fourthread.ozang.module.domain.user.dto.data.UserDto;
import com.fourthread.ozang.module.domain.user.dto.type.Role;
import com.fourthread.ozang.module.domain.user.entity.Profile;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.mapper.UserMapper;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  @Value("${admin.username}")
  private String username;
  @Value("${admin.password}")
  private String password;
  @Value("${admin.email}")
  private String email;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;

  @Override
  public UserDto initAdmin() {
    if (userRepository.existsByEmail(email) || userRepository.existsByName(username)) {
      log.debug("이미 어드민 유저가 존재합니다!");
      return null;
    }
    String encodedPassword = passwordEncoder.encode(password);
    User admin = new User(username, email, encodedPassword, Role.ADMIN);

    log.debug("새로운 어드민 유저를 생성합니다");

    Profile emptyProfile = new Profile(username, null, null,
        null, null, null);

    admin.setProfile(emptyProfile);
    userRepository.save(admin);

    log.debug("Create user : id={}, username = {}", admin.getId(), username);

    return userMapper.toDto(admin);
  }
}
