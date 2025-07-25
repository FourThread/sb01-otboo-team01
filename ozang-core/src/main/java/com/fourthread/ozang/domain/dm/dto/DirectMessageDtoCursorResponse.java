package com.fourthread.ozang.domain.dm.dto;

import com.fourthread.ozang.domain.dm.dto.DmItems;
import com.fourthread.ozang.domain.feed.entity.SortBy;
import com.fourthread.ozang.domain.feed.entity.SortDirection;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record DirectMessageDtoCursorResponse (

  List<DmItems> data,
  String nextCursor,
  UUID nextIdAfter,
  Boolean hasNext,
  Long totalCount,
  SortBy sortBy,
  SortDirection sortDirection

) {

}
