package com.fourthread.ozang.core.domain.user.repository.custom;

import com.fourthread.ozang.core.domain.feed.entity.SortDirection;
import com.fourthread.ozang.core.domain.user.dto.response.UserCursorPageResponse;
import com.fourthread.ozang.core.domain.user.dto.type.Role;
import java.util.UUID;

public interface UserCustomRepository {

  UserCursorPageResponse searchUsers(
      String cursor,
      UUID idAfter,
      int limit,
      SortDirection sortDirection,
      String emailLike,
      Role roleEqual,
      Boolean locked
  );

}
