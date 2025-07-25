package com.ozang.web.dm.dto;

import com.ozang.web.user.dto.data.UserSummary;
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
