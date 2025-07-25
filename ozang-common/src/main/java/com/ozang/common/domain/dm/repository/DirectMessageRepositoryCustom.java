package com.ozang.common.domain.dm.repository;

import com.ozang.common.domain.dm.dto.DirectMessageDtoCursorRequest;
import com.ozang.common.domain.dm.dto.DmItems;
import java.util.List;

public interface DirectMessageRepositoryCustom {

  List<DmItems> retrieveDm(DirectMessageDtoCursorRequest request);

  Long count(DirectMessageDtoCursorRequest request);

}
