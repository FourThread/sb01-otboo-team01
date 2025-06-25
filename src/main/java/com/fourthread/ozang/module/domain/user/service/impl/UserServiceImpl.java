package com.fourthread.ozang.module.domain.user.service.impl;

import com.fourthread.ozang.module.domain.user.entity.Profile;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.mapper.UserMapper;
import com.fourthread.ozang.module.domain.user.repository.ProfileRepository;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import com.fourthread.ozang.module.domain.user.dto.data.UserDto;
import com.fourthread.ozang.module.domain.user.dto.request.UserCreateRequest;
import com.fourthread.ozang.module.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;
  private final UserMapper userMapper;


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
}
