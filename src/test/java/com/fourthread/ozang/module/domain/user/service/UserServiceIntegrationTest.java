package com.fourthread.ozang.module.domain.user.service;

import com.fourthread.ozang.module.domain.user.dto.data.ProfileDto;
import com.fourthread.ozang.module.domain.user.dto.data.UserDto;
import com.fourthread.ozang.module.domain.user.dto.request.ChangePasswordRequest;
import com.fourthread.ozang.module.domain.user.dto.request.UserCreateRequest;
import com.fourthread.ozang.module.domain.user.dto.request.UserLockUpdateRequest;
import com.fourthread.ozang.module.domain.user.dto.request.UserRoleUpdateRequest;
import com.fourthread.ozang.module.domain.user.dto.type.Role;
import com.fourthread.ozang.module.domain.user.entity.Profile;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.exception.UserException;
import com.fourthread.ozang.module.domain.user.repository.ProfileRepository;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import java.util.UUID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SpringBootTest
@ActiveProfiles("test")
public class UserServiceIntegrationTest {

  @Autowired
  private UserService userService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ProfileRepository profileRepository;

  private UserDto savedUser;

  @BeforeEach
  void setUp() {
    UserCreateRequest request = new UserCreateRequest("tester1", "tester1@codeit.com", "password");
    savedUser = userService.createUser(request);
  }

  @Nested
  @DisplayName("사용자 생성")
  class CreateUser {

    @Test
    @DisplayName("사용자 생성 시 아무 정보 없는 프로필이 생성됩니다")
    void createUser_success_with_profile() {
      // given
      UserCreateRequest request = new UserCreateRequest("test", "test@email.com", "securePassword");

      UserDto createdUser = userService.createUser(request);

      User user = userRepository.findByName("test")
          .orElseThrow(() -> new RuntimeException("User not found"));

      assertThat(user.getProfile()).isNotNull();
      assertThat(user.getProfile().getName()).isEqualTo("test");

      long profileCount = profileRepository.count();
      assertThat(profileCount).isEqualTo(2);
    }
  }

  @Nested
  @DisplayName("사용자 정보 업데이트")
  class UpdateUser {

    @Test
    @DisplayName("사용자 역할을 업데이트 합니다.")
    void updateUser_success_role_changed() {
      UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ADMIN);
      UserDto userDto = userService.updateUserRole(savedUser.id(), request);
      User findUser = userRepository.findById(savedUser.id())
          .orElseThrow(() -> new IllegalArgumentException());
      assertThat(userDto.role()).isEqualTo(Role.ADMIN);
      assertThat(findUser.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("사용자 역할 변경 실패 - 존재하지 않는 사용자")
    void updateUserRole_fail_userNotFound() {
      UUID unknownId = UUID.randomUUID(); // 존재하지 않는 ID
      UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ADMIN);

      assertThatThrownBy(() -> userService.updateUserRole(unknownId, request))
          .isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("사용자 비밀번호를 변경합니다")
    void updateUser_success_password_changed() {
      ChangePasswordRequest request = new ChangePasswordRequest("newPassword!!");
      userService.updateUserPassword(savedUser.id(), request);
      User findUser = userRepository.findById(savedUser.id())
          .orElseThrow(() -> new IllegalArgumentException());

      assertThat(findUser.getPassword()).isEqualTo("newPassword!!");
    }

    @Test
    @DisplayName("사용자 비밀번호 변경 실패 - 존재하지 않는 사용자")
    void updateUserPassword_fail_userNotFound() {
      UUID unknownId = UUID.randomUUID();
      ChangePasswordRequest request = new ChangePasswordRequest("newPassword!!");

      assertThatThrownBy(() -> userService.updateUserPassword(unknownId, request))
          .isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("사용자 계정 잠금 상태를 변경합니다")
    void updateUser_success_locked_changed() {
      UserLockUpdateRequest request = new UserLockUpdateRequest(true);
      userService.changeLock(savedUser.id(), request);
      User findUser = userRepository.findById(savedUser.id())
          .orElseThrow(() -> new IllegalArgumentException());

      assertThat(findUser.getLocked()).isEqualTo(true);
    }

    @Test
    @DisplayName("사용자 역할 변경 실패 - 존재하지 않는 사용자")
    void updateUserLocked_fail_userNotFound() {
      UUID unknownId = UUID.randomUUID();
      UserLockUpdateRequest request = new UserLockUpdateRequest(true);

      assertThatThrownBy(() -> userService.changeLock(unknownId, request))
          .isInstanceOf(UserException.class);
    }
  }

  @Nested
  @DisplayName("프로필 조회하기")
  class GetProfile {

    @Test
    @DisplayName("생성된 사용자의 프로필 조회하기")
    void getProfile_success() {
      ProfileDto userProfile = userService.getUserProfile(savedUser.id());

      assertThat(userProfile).isNotNull();
    }
  }
}