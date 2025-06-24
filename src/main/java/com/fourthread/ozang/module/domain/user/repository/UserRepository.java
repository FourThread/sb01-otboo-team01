package com.fourthread.ozang.module.domain.user.repository;

import com.fourthread.ozang.module.domain.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

  boolean existsByEmail(String email);

  boolean existsByName(String username);

  Optional<User> findByName(String name);
}
