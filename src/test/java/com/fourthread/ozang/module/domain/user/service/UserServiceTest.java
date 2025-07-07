package com.fourthread.ozang.module.domain.user.service;

import com.fourthread.ozang.module.domain.security.jwt.JwtService;
import com.fourthread.ozang.module.domain.storage.ProfileStorage;
import com.fourthread.ozang.module.domain.user.dto.request.UserCreateRequest;
import com.fourthread.ozang.module.domain.user.dto.request.UserLockUpdateRequest;
import com.fourthread.ozang.module.domain.user.dto.type.Role;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.exception.UserException;
import com.fourthread.ozang.module.domain.user.mapper.ProfileMapper;
import com.fourthread.ozang.module.domain.user.repository.ProfileRepository;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import com.fourthread.ozang.module.domain.user.service.impl.UserServiceImpl;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

  @InjectMocks
  private UserServiceImpl userService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ProfileStorage profileStorage;

  @Mock
  private ProfileRepository profileRepository;

  @Mock
  private ProfileMapper profileMapper;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private JwtService jwtService;

  @Nested
  @DisplayName("사용자 생성")
  class CreateUser {

    @Test
    @DisplayName("실패 - 중복된 이름으로 가입 시 예외가 발생함")
    void createUser_fail_DuplicateName() {
      UserCreateRequest request = new UserCreateRequest("test", "test@test.com", "test");
      when(userRepository.existsByName("test")).thenReturn(true);

      assertThatThrownBy(() -> {
        userService.createUser(request);
      }).isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("실패 - 중복된 이메일 주소로 가입 시 예외가 발생함")
    void createUser_fail_DuplicateEmail() {
      UserCreateRequest request = new UserCreateRequest("test", "test@test.com", "test");
      when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

      assertThatThrownBy(() -> {
        userService.createUser(request);
      }).isInstanceOf(UserException.class);
    }
  }

  @Test
  @DisplayName("비밀번호 암호화 전과 후 비교하기")
  void password_encode_success() {
    String password = "test";

    String encode = passwordEncoder.encode(password);

    assertNotEquals(encode, password);
  }

  @Nested
  @DisplayName("계정 잠금 관리")
  class AccountLockManagement {

    @Test
    @DisplayName("성공 - 계정을 잠금 상태로 변경하면 JWT 토큰이 무효화됨")
    void changeLock_success_lockAccount() {
      // given
      UUID userId = UUID.randomUUID();
      String email = "test@email.com";
      UserLockUpdateRequest request = new UserLockUpdateRequest(true);

      User user = new User("tester", email, "password", Role.USER);
      user.changeLocked(false); // 현재 잠금 해제 상태
      ReflectionTestUtils.setField(user, "id", userId);

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      // when
      UUID result = userService.changeLock(userId, request);

      // then
      assertThat(user.getLocked()).isTrue();
      verify(jwtService).invalidateJwtTokenByEmail(email);
    }

    @Test
    @DisplayName("성공 - 계정을 잠금 해제 상태로 변경하면 JWT 토큰 무효화하지 않음")
    void changeLock_success_unlockAccount() {
      // given
      UUID userId = UUID.randomUUID();
      String email = "test@email.com";
      UserLockUpdateRequest request = new UserLockUpdateRequest(false);

      User user = new User("tester", email, "password", Role.USER);
      user.changeLocked(true); // 현재 잠금 상태
      ReflectionTestUtils.setField(user, "id", userId);

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      // when
      UUID result = userService.changeLock(userId, request);

      // then
      assertThat(user.getLocked()).isFalse();
      verify(jwtService, never()).invalidateJwtTokenByEmail(any());
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 사용자 ID로 잠금 상태 변경 시 예외 발생")
    void changeLock_fail_userNotFound() {
      // given
      UUID userId = UUID.randomUUID();
      UserLockUpdateRequest request = new UserLockUpdateRequest(true);

      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userService.changeLock(userId, request))
          .isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("성공 - 이미 잠금 상태인 계정을 다시 잠금 상태로 변경해도 JWT 토큰 무효화하지 않음")
    void changeLock_success_alreadyLocked() {
      // given
      UUID userId = UUID.randomUUID();
      String email = "test@email.com";
      UserLockUpdateRequest request = new UserLockUpdateRequest(true);

      User user = new User("tester", email, "password", Role.USER);
      user.changeLocked(true); // 이미 잠금 상태
      ReflectionTestUtils.setField(user, "id", userId);

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      // when
      UUID result = userService.changeLock(userId, request);

      // then
      assertThat(user.getLocked()).isTrue();
      verify(jwtService, never()).invalidateJwtTokenByEmail(any());
    }
  }

//  @Nested
//  @DisplayName("프로필을 수정합니다.")
//  class UpdateProfile {
//
//    @Test
//    @DisplayName("프로필 업데이트 성공")
//    void updateProfile_success() throws IOException {
//      // given
//      UUID userId = UUID.randomUUID();
//      Profile profile = spy(new Profile("oldName", null, null, null, null, null));
//      MultipartFile mockFile = mock(MultipartFile.class);
//      ProfileUpdateRequest request = new ProfileUpdateRequest("test", null, null, null, null);
//      String imageUrl = "https://mock-s3.com/profile.png";
//
//      when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
//      when(mockFile.isEmpty()).thenReturn(false);
//      when(profileStorage.saveFile(mockFile)).thenReturn(imageUrl);
//      when(profileMapper.toDto(profile)).thenReturn(
//          new ProfileDto(userId, "test", null, null, null, null, imageUrl)
//      );
//
//      // when
//      ProfileDto result = userService.updateUserProfile(userId, request, Optional.of(mockFile));
//
//      // then
//      verify(profile).updateProfile(
//          eq("test"),
//          isNull(), isNull(), isNull(), isNull(),
//          eq(imageUrl)
//      );
//
//      assertNotNull(result);
//      assertEquals("test", result.name());
//      assertEquals(imageUrl, result.profileImageUrl());
//    }
//  }
}
