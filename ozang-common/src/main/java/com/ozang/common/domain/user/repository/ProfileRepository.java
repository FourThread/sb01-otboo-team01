package com.ozang.common.domain.user.repository;

import com.ozang.common.domain.user.entity.Profile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

  Optional<Profile> findByUserId(UUID userId);

}
