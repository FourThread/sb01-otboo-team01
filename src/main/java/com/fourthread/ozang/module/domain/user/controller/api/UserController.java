package com.fourthread.ozang.module.domain.user.controller.api;

import com.fourthread.ozang.module.domain.user.dto.data.ProfileDto;
import com.fourthread.ozang.module.domain.user.dto.data.UserDto;
import com.fourthread.ozang.module.domain.user.dto.request.ProfileUpdateRequest;
import com.fourthread.ozang.module.domain.user.dto.request.UserCreateRequest;
import com.fourthread.ozang.module.domain.user.dto.request.UserRoleUpdateRequest;
import com.fourthread.ozang.module.domain.user.service.UserService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;

  @PostMapping
  public ResponseEntity<UserDto> createUser(
      @RequestBody @Validated UserCreateRequest request
  ) {

    UserDto user = userService.createUser(request);

    return ResponseEntity.status(HttpStatus.CREATED).body(user);
  }

  @PatchMapping("/{userId}/role")
  public ResponseEntity<UserDto> changeUserRole(
      @PathVariable(name = "userId") UUID userId,
      @RequestBody UserRoleUpdateRequest request
  ) {
    UserDto userDto = userService.updateUserRole(userId, request);

    return ResponseEntity.status(HttpStatus.OK).body(userDto);
  }

  @PatchMapping(
      path = "{userId}/profiles",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}
  )
  public ResponseEntity<ProfileDto> changeProfile(
      @PathVariable(name = "userId") UUID userId,
      @RequestBody ProfileUpdateRequest request,
      @RequestPart(value = "image", required = false) MultipartFile image
  ) {
    ProfileDto updatedProfile = userService.updateUserProfile(userId, request, Optional.ofNullable(image));
    return ResponseEntity.ok(updatedProfile);
  }

  @GetMapping("/{userId}/profiles")
  public ResponseEntity<ProfileDto> getProfile(@PathVariable(name = "userId") UUID userId) {
    ProfileDto profileDto = userService.getUserProfile(userId);

    return ResponseEntity.ok(profileDto);
  }
}
