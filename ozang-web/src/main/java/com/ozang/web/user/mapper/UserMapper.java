package com.fourthread.ozang.module.domain.user.mapper;

import com.fourthread.ozang.module.domain.user.dto.data.UserDto;
import com.fourthread.ozang.module.domain.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

  UserDto toDto(User user);
}
