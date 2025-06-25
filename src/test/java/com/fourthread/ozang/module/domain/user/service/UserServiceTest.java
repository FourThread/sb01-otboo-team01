package com.fourthread.ozang.module.domain.user.service;

import com.fourthread.ozang.module.domain.user.dto.request.UserCreateRequest;
import com.fourthread.ozang.module.domain.user.mapper.UserMapper;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import com.fourthread.ozang.module.domain.user.service.impl.UserServiceImpl;
import org.assertj.core.api.Assertions;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

  @InjectMocks
  private UserServiceImpl userService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserMapper userMapper;

  @Nested
  @DisplayName("사용자 생성")
  class CreateUser {

    @Test
    @DisplayName("실패 - 중복된 이름으로 가입 시 예외가 발생함")
    void createUser_fail_DuplicateName() {
      UserCreateRequest request = new UserCreateRequest("test", "test@test.com", "test");
      Mockito.when(userRepository.existsByName("test")).thenReturn(true);

      assertThatThrownBy(() -> {
        userService.createUser(request);
      }).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("실패 - 중복된 이메일 주소로 가입 시 예외가 발생함")
    void createUser_fail_DuplicateEmail() {
      UserCreateRequest request = new UserCreateRequest("test", "test@test.com", "test");
      Mockito.when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

      assertThatThrownBy(() -> {
        userService.createUser(request);
      }).isInstanceOf(IllegalArgumentException.class);
    }
  }

}
