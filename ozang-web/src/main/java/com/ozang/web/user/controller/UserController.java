package com.ozang.web.user.controller;

import com.ozang.web.feed.entity.SortDirection;
import com.ozang.web.security.userdetails.UserDetailsImpl;
import com.ozang.web.user.dto.data.ProfileDto;
import com.ozang.web.user.dto.data.UserDto;
import com.ozang.web.user.dto.request.ChangePasswordRequest;
import com.ozang.web.user.dto.request.LoginRequest;
import com.ozang.web.user.dto.request.ProfileUpdateRequest;
import com.ozang.web.user.dto.request.UserCreateRequest;
import com.ozang.web.user.dto.request.UserLockUpdateRequest;
import com.ozang.web.user.dto.request.UserRoleUpdateRequest;
import com.ozang.web.user.dto.response.UserCursorPageResponse;
import com.ozang.web.user.dto.type.Role;
import com.ozang.web.user.dto.type.SortBy;
import com.ozang.web.user.service.UserService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;

  @GetMapping
  public ResponseEntity<UserCursorPageResponse> getUserList(
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) UUID idAfter,
      @RequestParam int limit,
      @RequestParam SortBy sortBy,
      @RequestParam SortDirection sortDirection,
      @RequestParam(required = false) String emailLike,
      @RequestParam(required = false) Role roleEqual,
      @RequestParam(required = false) Boolean locked
  ) {
    UserCursorPageResponse response = userService.getUserList(
        cursor,
        idAfter,
        limit,
        sortBy,
        sortDirection,
        emailLike,
        roleEqual,
        locked
    );

    return ResponseEntity.ok(response);
  }

  @PostMapping
  public ResponseEntity<UserDto> createUser(
      @RequestBody @Validated UserCreateRequest request
  ) {

    UserDto user = userService.createUser(request);

    return ResponseEntity.status(HttpStatus.CREATED).body(user);
  }

  @PatchMapping("/{userId}/role")
  public ResponseEntity<UserDto> changeUserRole(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @PathVariable(name = "userId") UUID userId,
      @RequestBody UserRoleUpdateRequest request
  ) {
    UUID requesterId = userDetails.getPayloadDto().userId();
    UserDto userDto = userService.updateUserRole(userId, request, requesterId);

    return ResponseEntity.status(HttpStatus.OK).body(userDto);
  }

  @PatchMapping(
      path = "{userId}/profiles",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}
  )
  public ResponseEntity<ProfileDto> changeProfile(
      @PathVariable(name = "userId") UUID userId,
      @RequestPart(value = "request") ProfileUpdateRequest request,
      @RequestPart(value = "image", required = false) MultipartFile image
  ) {

    ProfileDto updatedProfile = userService.updateUserProfile(userId, request, Optional.ofNullable(image));
    return ResponseEntity.ok(updatedProfile);
  }

  @PatchMapping("/{userId}/lock")
  public ResponseEntity<UUID> changeLock(
      @PathVariable(name = "userId") UUID userId,
      @RequestBody UserLockUpdateRequest request
  ) {
    UUID uuid = userService.changeLock(userId, request);

    return ResponseEntity.ok(uuid);
  }

  @PatchMapping("/{userId}/password")
  public ResponseEntity<Void> changePassword(
      @PathVariable(name = "userId") UUID userId,
      @RequestBody @Validated ChangePasswordRequest request
  ) {
    userService.updateUserPassword(userId, request);

    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{userId}/profiles")
  public ResponseEntity<ProfileDto> getProfile(@PathVariable(name = "userId") UUID userId) {
    ProfileDto profileDto = userService.getUserProfile(userId);

    return ResponseEntity.ok(profileDto);
  }
}
