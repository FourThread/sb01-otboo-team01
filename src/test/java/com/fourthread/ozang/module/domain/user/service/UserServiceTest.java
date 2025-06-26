package com.fourthread.ozang.module.domain.user.service;

import com.fourthread.ozang.module.domain.security.provider.JwtTokenProvider;
import com.fourthread.ozang.module.domain.storage.ProfileStorage;
import com.fourthread.ozang.module.domain.user.dto.data.ProfileDto;
import com.fourthread.ozang.module.domain.user.dto.request.ProfileUpdateRequest;
import com.fourthread.ozang.module.domain.user.dto.request.UserCreateRequest;
import com.fourthread.ozang.module.domain.user.entity.Profile;
import com.fourthread.ozang.module.domain.user.exception.UserException;
import com.fourthread.ozang.module.domain.user.mapper.ProfileMapper;
import com.fourthread.ozang.module.domain.user.mapper.UserMapper;
import com.fourthread.ozang.module.domain.user.repository.ProfileRepository;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import com.fourthread.ozang.module.domain.user.service.impl.UserServiceImpl;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import static org.assertj.core.api.Assertions.*;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
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

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private AuthenticationManagerBuilder authenticationManagerBuilder;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private Authentication authentication;

  @Mock
  private AuthenticationManager authenticationManager;

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

  @Test
  @DisplayName("로그인 테스트 -> 유효한 로그인 시 JWT 토큰을 반환한다.")
  void signIn_success_returnJwtToken() {

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
