package com.ozang.web.user.mapper;

import com.ozang.web.user.dto.data.UserDto;
import com.ozang.web.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

  UserDto toDto(User user);
}
