package com.fourthread.ozang.module.domain.user;

import com.fourthread.ozang.module.domain.user.entity.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

}
