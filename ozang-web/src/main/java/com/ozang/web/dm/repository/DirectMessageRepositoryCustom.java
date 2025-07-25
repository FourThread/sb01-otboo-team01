package com.ozang.web.dm.repository;

import com.ozang.web.dm.dto.DirectMessageDtoCursorRequest;
import com.ozang.web.dm.dto.DmItems;
import java.util.List;

public interface DirectMessageRepositoryCustom {

  List<DmItems> retrieveDm(DirectMessageDtoCursorRequest request);

  Long count(DirectMessageDtoCursorRequest request);

}
