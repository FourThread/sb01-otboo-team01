package com.ozang.web.user.service;

import com.ozang.web.feed.entity.SortDirection;
import com.ozang.web.user.dto.data.ProfileDto;
import com.ozang.web.user.dto.data.UserDto;
import com.ozang.web.user.dto.request.ChangePasswordRequest;
import com.ozang.web.user.dto.request.ProfileUpdateRequest;
import com.ozang.web.user.dto.request.UserCreateRequest;
import com.ozang.web.user.dto.request.UserLockUpdateRequest;
import com.ozang.web.user.dto.request.UserRoleUpdateRequest;
import com.ozang.web.user.dto.response.UserCursorPageResponse;
import com.ozang.web.user.dto.type.Role;
import com.ozang.web.user.dto.type.SortBy;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

  UserDto createUser(UserCreateRequest request);

  UserDto updateUserRole(UUID userId, UserRoleUpdateRequest request, UUID requesterId);

  void updateUserPassword(UUID userId,  ChangePasswordRequest request);

  ProfileDto getUserProfile(UUID userId);

  ProfileDto updateUserProfile(UUID userId, ProfileUpdateRequest request,
      Optional<MultipartFile> nullableProfile);

  UUID changeLock(UUID userId, UserLockUpdateRequest request);

  void resetPassword(String email);

  UserCursorPageResponse getUserList(String cursor, UUID idAfter, int limit, SortBy sortBy,
      SortDirection sortDirection, String emailLike, Role roleEqual, Boolean locked);
}
