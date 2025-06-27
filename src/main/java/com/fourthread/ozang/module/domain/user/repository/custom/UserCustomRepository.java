package com.fourthread.ozang.module.domain.user.repository.custom;

import com.fourthread.ozang.module.domain.feed.dto.dummy.SortDirection;
import com.fourthread.ozang.module.domain.user.dto.response.UserCursorPageResponse;
import com.fourthread.ozang.module.domain.user.dto.type.Role;
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
