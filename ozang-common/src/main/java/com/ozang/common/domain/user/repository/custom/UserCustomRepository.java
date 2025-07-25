package com.ozang.common.domain.user.repository.custom;

import com.ozang.common.domain.feed.entity.SortDirection;
import com.ozang.common.domain.user.dto.response.UserCursorPageResponse;
import com.ozang.common.domain.user.dto.type.Role;
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
