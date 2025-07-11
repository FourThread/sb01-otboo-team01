package com.fourthread.ozang.module.domain.user.service.impl;

import com.fourthread.ozang.module.common.exception.ErrorCode;
import com.fourthread.ozang.module.domain.feed.entity.SortDirection;
import com.fourthread.ozang.module.domain.security.jwt.JwtService;
import com.fourthread.ozang.module.domain.storage.ImageService;
import com.fourthread.ozang.module.domain.user.dto.data.ProfileDto;
import com.fourthread.ozang.module.domain.user.dto.data.UserDto;
import com.fourthread.ozang.module.domain.user.dto.request.ChangePasswordRequest;
import com.fourthread.ozang.module.domain.user.dto.request.ProfileUpdateRequest;
import com.fourthread.ozang.module.domain.user.dto.request.UserCreateRequest;
import com.fourthread.ozang.module.domain.user.dto.request.UserLockUpdateRequest;
import com.fourthread.ozang.module.domain.user.dto.request.UserRoleUpdateRequest;
import com.fourthread.ozang.module.domain.user.dto.response.UserCursorPageResponse;
import com.fourthread.ozang.module.domain.user.dto.type.Role;
import com.fourthread.ozang.module.domain.user.dto.type.SortBy;
import com.fourthread.ozang.module.domain.user.entity.Profile;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.exception.UserException;
import com.fourthread.ozang.module.domain.user.mapper.ProfileMapper;
import com.fourthread.ozang.module.domain.user.mapper.UserMapper;
import com.fourthread.ozang.module.domain.user.repository.ProfileRepository;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import com.fourthread.ozang.module.domain.user.service.MailService;
import com.fourthread.ozang.module.domain.user.service.UserService;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;
  private final UserMapper userMapper;
  private final ProfileMapper profileMapper;
  private final PasswordEncoder passwordEncoder;
  private final MailService mailService;
  private final JwtService jwtService;
  private final ImageService imageService;

  public UserServiceImpl(UserRepository userRepository,
      ProfileRepository profileRepository,
      UserMapper userMapper,
      ProfileMapper profileMapper,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      MailService mailService,
      @Qualifier("profileImageService") ImageService imageService) {
    this.userRepository = userRepository;
    this.profileRepository = profileRepository;
    this.userMapper = userMapper;
    this.profileMapper = profileMapper;
    this.passwordEncoder = passwordEncoder;
    this.mailService = mailService;
    this.jwtService = jwtService;
    this.imageService = imageService;
  }

  @Transactional
  @Override
  public UserDto createUser(UserCreateRequest request) {
    log.info("[UserService] 사용자를 생성합니다");

    String username = request.name();
    String email = request.email();

    if (userRepository.existsByName(username)) {
      log.info("[UserService] 이미 해당 사용자 이름이 존재합니다");
      throw new UserException(ErrorCode.USERNAME_ALREADY_EXISTS, username,
          this.getClass().getSimpleName());
    }

    if (userRepository.existsByEmail(email)) {
      log.info("[UserService] 이미 해당 이메일이 사용 중입니다");
      throw new UserException(ErrorCode.EMAIL_ALREADY_EXISTS, email,
          this.getClass().getSimpleName());
    }

    String password = request.password();
    String encodePassword = passwordEncoder.encode(password);

    // 빈 프로필 생성하기
    Profile emptyProfile = new Profile(username, null, null,
        null, null, null);

    User user = new User(username, email, encodePassword);
    user.setProfile(emptyProfile);
    userRepository.save(user);

    log.info("[UserService] 사용자 생성이 완료되었습니다");;
    return userMapper.toDto(user);
  }

  @Transactional
  @Override
  public UserDto updateUserRole(UUID userId, UserRoleUpdateRequest request) {
    Role newRole = request.role();
    log.info("사용자 Role를 업데이트 합니다 : {}", newRole);
    User findUser = userRepository.findById(userId)
        .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND, userId.toString(),
            this.getClass().getSimpleName()));

    findUser.updateRole(newRole);

    log.info("{} 사용자 Role 업데이트를 완료했습니다", findUser.getName());

    return userMapper.toDto(findUser);
  }

  @Transactional
  @Override
  public void updateUserPassword(UUID userId, ChangePasswordRequest request) {
    log.info("[UserService] 비밀번호를 업데이트 합니다");
    String newPassword = request.password();
    String encodePassword = passwordEncoder.encode(newPassword);
    User findUser = userRepository.findById(userId)
        .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND, userId.toString(),
            this.getClass().getSimpleName()));

    findUser.updatePassword(encodePassword);
    log.info("비밀번호 변경을 완료했습니다");

  }

  @Transactional(readOnly = true)
  @Override
  public ProfileDto getUserProfile(UUID userId) {
    log.info("[UserService] 프로필 단건 조회를 합니다");
    Profile profile = profileRepository.findByUserId(userId)
        .orElseThrow(() -> new UserException(ErrorCode.PROFILE_NOT_FOUND, userId.toString(),
            this.getClass().getSimpleName()));

//    if (profile.getLocation() != null && profile.getLocation().getLocationNames() != null) {
//      profile.getLocation().getLocationNames().size();
//    }

    return profileMapper.toDto(profile);
  }

  @Transactional
  @Override
  public ProfileDto updateUserProfile(UUID userId, ProfileUpdateRequest request,
      Optional<MultipartFile> nullableProfile) {
    log.info("[UserService] 사용자 프로필을 업데이트 합니다");
    Profile findProfile = profileRepository.findByUserId(userId)
        .orElseThrow(() -> new UserException(ErrorCode.PROFILE_NOT_FOUND, userId.toString(),
            this.getClass().getSimpleName()));

    String profileImageUrl = findProfile.getProfileImageUrl();

    if (nullableProfile.isPresent() && !nullableProfile.get().isEmpty()) {
      MultipartFile file = nullableProfile.get();

      // 기존 이미지가 있다면 S3에서 삭제
      if (profileImageUrl != null && !profileImageUrl.isBlank()) {
        imageService.deleteImage(profileImageUrl);
      }

      // 새 이미지 업로드
      profileImageUrl = imageService.uploadImage(file);
    }

    findProfile.updateProfile(
        request.name(),
        request.gender(),
        request.birthDate(),
        request.location(),
        request.temperatureSensitivity(),
        profileImageUrl
    );

    log.info("[UserService] 사용자 프로필 업데이트를 완료했습니다");

    return profileMapper.toDto(findProfile);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  @Override
  public UUID changeLock(UUID userId, UserLockUpdateRequest request) {
    boolean locked = request.locked();
    User findUser = userRepository.findById(userId)
        .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND, userId.toString(),
            this.getClass().getSimpleName()));

    log.info("[UserService] 사용자 계정 잠금 상태를 변경합니다 : before {} -> after {}", findUser.getLocked(),
        request.locked());
    boolean wasUnlocked = !findUser.getLocked();
    findUser.changeLocked(locked);

    // 계정이 잠금 상태로 변경된 경우, 해당 사용자의 모든 JWT 토큰을 무효화
    if (wasUnlocked && locked) {
      log.info("[UserService] 계정이 잠금되었으므로 사용자 {}의 모든 활성 세션을 로그아웃시킵니다", findUser.getEmail());
      jwtService.invalidateJwtTokenByEmail(findUser.getEmail());
    }

    return findUser.getId();
  }

  @Override
  public void resetPassword(String email) {
    User user = userRepository.findByEmail(email).orElseThrow(
        () -> new UserException(ErrorCode.USER_NOT_FOUND, email, this.getClass().getSimpleName()));

    String tempPassword = mailService.generateTempPassword();

    String encodePass = passwordEncoder.encode(tempPassword);
    user.setPassword(encodePass);
    user.setTempPasswordIssuedAt(LocalDateTime.now());

    userRepository.save(user);

    mailService.sendResetPasswordEmail(user.getEmail(), tempPassword);
  }

  @Transactional(readOnly = true)
  @Override
  public UserCursorPageResponse getUserList(String cursor, UUID idAfter, int limit, SortBy sortBy,
      SortDirection sortDirection, String emailLike, Role roleEqual, Boolean locked) {
    log.info("[UserService] 사용자 목록을 조회합니다");
    if (sortBy != SortBy.EMAIL) {
      throw new IllegalArgumentException("현재는 EMAIL 기준 정렬만 지원합니다");
    }

    return userRepository.searchUsers(
        cursor,
        idAfter,
        limit,
        sortDirection,
        emailLike,
        roleEqual,
        locked
    );
  }
}
