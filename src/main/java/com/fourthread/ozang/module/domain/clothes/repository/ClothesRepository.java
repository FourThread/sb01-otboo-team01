package com.fourthread.ozang.module.domain.clothes.repository;

import com.fourthread.ozang.module.domain.clothes.entity.Clothes;
import java.util.Collection;
import java.util.List;
import com.fourthread.ozang.module.domain.clothes.repository.query.ClothesRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClothesRepository extends JpaRepository<Clothes, UUID>, ClothesRepositoryCustom {

  List<Clothes> findByIdIn(Collection<UUID> ids);
}
