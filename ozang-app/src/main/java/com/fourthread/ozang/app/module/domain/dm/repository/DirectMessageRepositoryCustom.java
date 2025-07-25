package com.fourthread.ozang.module.domain.dm.repository;

import com.fourthread.ozang.module.domain.dm.dto.DirectMessageDtoCursorRequest;
import com.fourthread.ozang.module.domain.dm.dto.DmItems;
import java.util.List;

public interface DirectMessageRepositoryCustom {

  List<DmItems> retrieveDm(DirectMessageDtoCursorRequest request);

  Long count(DirectMessageDtoCursorRequest request);

}
