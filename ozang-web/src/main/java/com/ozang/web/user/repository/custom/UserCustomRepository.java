package com.ozang.web.user.repository.custom;

import com.ozang.web.feed.entity.SortDirection;
import com.ozang.web.user.dto.response.UserCursorPageResponse;
import com.ozang.web.user.dto.type.Role;
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
