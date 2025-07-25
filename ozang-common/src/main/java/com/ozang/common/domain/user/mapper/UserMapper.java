package com.ozang.common.domain.user.mapper;

import com.ozang.common.domain.user.dto.data.UserDto;
import com.ozang.common.domain.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

  UserDto toDto(User user);
}
