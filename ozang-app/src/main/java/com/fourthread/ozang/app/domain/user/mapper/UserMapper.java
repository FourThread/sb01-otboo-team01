package com.fourthread.ozang.app.domain.user.mapper;

import com.fourthread.ozang.app.domain.user.dto.data.UserDto;
import com.fourthread.ozang.app.domain.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

  UserDto toDto(User user);
}
