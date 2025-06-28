package com.fourthread.ozang.module.domain.security.Service.impl;

import com.fourthread.ozang.module.common.exception.ErrorCode;
import com.fourthread.ozang.module.domain.security.Service.AuthService;
import com.fourthread.ozang.module.domain.user.dto.data.UserDto;
import com.fourthread.ozang.module.domain.user.dto.type.Role;
import com.fourthread.ozang.module.domain.user.entity.Profile;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.exception.UserException;
import com.fourthread.ozang.module.domain.user.mapper.UserMapper;
import com.fourthread.ozang.module.domain.user.repository.ProfileRepository;
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

  @Value("${admin.username")
  private String username = "admin";
  @Value("${admin.password")
  private String password = "Admin1234!!";
  @Value("${admin.email")
  private String email = "admin@admin.com";
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;

  @Override
  public UserDto initAdmin() {
    if (userRepository.existsByEmail(email) || userRepository.existsByName(username)) {
      log.warn("Already admin exists!");
      return null;
    }
    String encodedPassword = passwordEncoder.encode(password);
    User admin = new User(username, email, encodedPassword, Role.ADMIN);

    log.debug("Create admin start");

    // 빈 프로필 생성하기
    Profile emptyProfile = new Profile(username, null, null,
        null, null, null);

    admin.setProfile(emptyProfile);
    userRepository.save(admin);

    log.debug("Create user : id={}, username = {}", admin.getId(), username);

    return userMapper.toDto(admin);
  }
}
