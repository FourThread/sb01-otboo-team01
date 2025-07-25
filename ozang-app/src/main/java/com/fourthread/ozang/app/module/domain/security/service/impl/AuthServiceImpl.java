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
import org.springframework.transaction.annotation.Transactional;

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
  @Transactional
  public UserDto initAdmin() {
    // 환경변수 확인 및 로깅
    log.info("[AuthService] Admin 초기화 시작");
    log.info("[AuthService] username: {}", username);
    log.info("[AuthService] email: {}", email);
    log.info("[AuthService] password 설정됨: {}", password != null && !password.trim().isEmpty());

    // 환경변수 유효성 검사
    if (username == null || username.trim().isEmpty()) {
      log.error("[AuthService] admin.username이 설정되지 않았습니다.");
      return null;
    }
    if (password == null || password.trim().isEmpty()) {
      log.error("[AuthService] admin.password가 설정되지 않았습니다.");
      return null;
    }
    if (email == null || email.trim().isEmpty()) {
      log.error("[AuthService] admin.email이 설정되지 않았습니다.");
      return null;
    }

    // 기존 사용자 확인
    boolean existsByEmail = userRepository.existsByEmail(email);
    boolean existsByName = userRepository.existsByName(username);

    log.info("[AuthService] 기존 사용자 확인:");
    log.info("  - 이메일({}) 존재: {}", email, existsByEmail);
    log.info("  - 사용자명({}) 존재: {}", username, existsByName);

    if (existsByEmail || existsByName) {
      log.info("[AuthService] 이미 어드민 유저가 존재합니다!");
      return null;
    }

    // 새 Admin 사용자 생성
    try {
      log.info("[AuthService] 새로운 어드민 유저를 생성합니다");

      String encodedPassword = passwordEncoder.encode(password);
      User admin = new User(username, email, encodedPassword, Role.ADMIN);

      Profile emptyProfile = new Profile(username, null, null,
          null, null, null);
      admin.setProfile(emptyProfile);

      User savedAdmin = userRepository.save(admin);
      log.info("[AuthService] Create user : id={}, username={}, email={}, role={}",
          savedAdmin.getId(), savedAdmin.getName(), savedAdmin.getEmail(), savedAdmin.getRole());

      return userMapper.toDto(savedAdmin);
    } catch (Exception e) {
      log.error("[AuthService] Admin 사용자 생성 중 오류 발생: {}", e.getMessage(), e);
      throw new RuntimeException("Admin 사용자 생성 실패", e);
    }
  }
//  @Override
//  public UserDto initAdmin() {
//    if (userRepository.existsByEmail(email) || userRepository.existsByName(username)) {
//      log.debug("이미 어드민 유저가 존재합니다!");
//      return null;
//    }
//    String encodedPassword = passwordEncoder.encode(password);
//    User admin = new User(username, email, encodedPassword, Role.ADMIN);
//
//    log.debug("새로운 어드민 유저를 생성합니다");
//
//    Profile emptyProfile = new Profile(username, null, null,
//        null, null, null);
//
//    admin.setProfile(emptyProfile);
//    userRepository.save(admin);
//
//    log.debug("Create user : id={}, username = {}", admin.getId(), username);
//
//    return userMapper.toDto(admin);
//  }
}
