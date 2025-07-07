package com.fourthread.ozang.module.domain.dm.dto;

import com.fourthread.ozang.module.domain.user.dto.data.UserSummary;
import java.time.LocalDateTime;
import java.util.UUID;

public record DmItems (

  UUID id,
  LocalDateTime createdAt,
  UserSummary sender,
  UserSummary receiver,
  String content

) {

}
