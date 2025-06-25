package com.fourthread.ozang.module.domain.user.mapper;

import com.fourthread.ozang.module.domain.user.dto.data.ProfileDto;
import com.fourthread.ozang.module.domain.user.entity.Profile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProfileMapper {

  ProfileDto toDto(Profile profile);
}
