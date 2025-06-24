package com.fourthread.ozang.module.domain.user.service;

import com.fourthread.ozang.module.domain.user.dto.data.ProfileDto;
import com.fourthread.ozang.module.domain.user.dto.data.UserDto;
import com.fourthread.ozang.module.domain.user.dto.request.UserCreateRequest;
import com.fourthread.ozang.module.domain.user.dto.type.Role;
import java.util.UUID;

public interface UserService {

  UserDto createUser(UserCreateRequest request);

  UserDto updateUserRole(UUID userId, Role newRole);

  void updateUserPassword(UUID userId,  String newPassword);

  ProfileDto getUserProfile(UUID userId);
}
