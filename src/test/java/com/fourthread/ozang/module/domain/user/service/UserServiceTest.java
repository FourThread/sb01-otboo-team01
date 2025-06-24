package com.fourthread.ozang.module.domain.user.service;

import com.fourthread.ozang.module.domain.storage.ProfileStorage;
import com.fourthread.ozang.module.domain.user.dto.data.ProfileDto;
import com.fourthread.ozang.module.domain.user.dto.request.ProfileUpdateRequest;
import com.fourthread.ozang.module.domain.user.dto.request.UserCreateRequest;
import com.fourthread.ozang.module.domain.user.entity.Profile;
import com.fourthread.ozang.module.domain.user.mapper.ProfileMapper;
import com.fourthread.ozang.module.domain.user.mapper.UserMapper;
import com.fourthread.ozang.module.domain.user.repository.ProfileRepository;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import com.fourthread.ozang.module.domain.user.service.impl.UserServiceImpl;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import static org.assertj.core.api.Assertions.*;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

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
      }).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("실패 - 중복된 이메일 주소로 가입 시 예외가 발생함")
    void createUser_fail_DuplicateEmail() {
      UserCreateRequest request = new UserCreateRequest("test", "test@test.com", "test");
      when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

      assertThatThrownBy(() -> {
        userService.createUser(request);
      }).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("프로필을 수정합니다.")
  class UpdateProfile {

    @Test
    @DisplayName("프로필 업데이트")
    void updateProfile_success() {
      UUID userId = UUID.randomUUID();
      Profile profile = mock(Profile.class);
      MultipartFile mockFile = mock(MultipartFile.class);
      ProfileUpdateRequest request = new ProfileUpdateRequest("test", null, null, null, null);

      when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
      when(mockFile.isEmpty()).thenReturn(false);
      when(mockFile.getContentType()).thenReturn("image/jpeg");

      UUID s3Key = UUID.randomUUID();
      String presignedUrl = "https://mock-s3.com/" + s3Key;

      when(profileStorage.put(mockFile)).thenReturn(s3Key);
      when(profileStorage.generatePresignedUrl(s3Key, "image/jpeg")).thenReturn(presignedUrl);

      ProfileDto result = userService.updateUserProfile(userId, request, Optional.of(mockFile));

      verify(profile).updateProfile(eq("test"), any(), any(), any(), any(), eq(presignedUrl));
    }
  }
}
