package com.fourthread.ozang.module.domain.user.service.impl;

import com.fourthread.ozang.module.domain.user.dto.data.ProfileDto;
import com.fourthread.ozang.module.domain.user.dto.type.Role;
import com.fourthread.ozang.module.domain.user.entity.Profile;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.mapper.ProfileMapper;
import com.fourthread.ozang.module.domain.user.mapper.UserMapper;
import com.fourthread.ozang.module.domain.user.repository.ProfileRepository;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import com.fourthread.ozang.module.domain.user.dto.data.UserDto;
import com.fourthread.ozang.module.domain.user.dto.request.UserCreateRequest;
import com.fourthread.ozang.module.domain.user.service.UserService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;
  private final UserMapper userMapper;
  private final ProfileMapper profileMapper;


  @Transactional
  @Override
  public UserDto createUser(UserCreateRequest request) {
    log.debug("Create user start : {}", request);

    String username = request.name();
    String email = request.email();

    if (userRepository.existsByName(username)) {
      throw new IllegalArgumentException("Username is already in use");
    }

    if (userRepository.existsByEmail(email)) {
      throw new IllegalArgumentException("Email is already in use");
    }

    String password = request.password();

    // 빈 프로필 생성하기
    Profile emptyProfile = new Profile(username, null, null,
        null, null, null);

    User user = new User(username, email, password);
    user.setProfile(emptyProfile);
    userRepository.save(user);

    log.debug("Create user : id={}, username = {}", user.getId(), username);

    return userMapper.toDto(user);
  }

  @Transactional
  @Override
  public UserDto updateUserRole(UUID userId, Role newRole) {
    log.debug("Update user role start : {}", newRole);
    User findUser = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));

    findUser.updateRole(newRole);

    log.debug("Update user role end : username = {}", findUser.getName());

    return userMapper.toDto(findUser);
  }

  @Override
  public void updateUserPassword(UUID userId, String newPassword) {
    log.info("Update user password - start");
    User findUser = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));

    findUser.updatePassword(newPassword);
    log.info("Update user password - end");

  }

  @Override
  public ProfileDto getUserProfile(UUID userId) {
    Profile findProfile = profileRepository.findByUserId(userId)
        .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

    return profileMapper.toDto(findProfile);
  }
}
