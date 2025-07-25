package com.ozang.web.user.repository;

import com.ozang.web.user.entity.User;
import com.ozang.web.user.repository.custom.UserCustomRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID>, UserCustomRepository {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  boolean existsByName(String username);

  Optional<User> findByName(String name);

  List<User> findAllByIdIn(Collection<UUID> ids);
  
  @Query("SELECT u.id FROM User u")
  Set<UUID> findAllUserIds();
}
