package com.fourthread.ozang.domain.dm.repository;

import com.fourthread.ozang.module.domain.dm.entity.DirectMessage;
import com.fourthread.ozang.module.domain.dm.repository.DirectMessageRepositoryCustom;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID>,
    DirectMessageRepositoryCustom {

}
