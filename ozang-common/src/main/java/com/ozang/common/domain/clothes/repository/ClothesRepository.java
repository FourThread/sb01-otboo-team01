package com.ozang.common.domain.clothes.repository;

import com.ozang.common.domain.clothes.entity.Clothes;
import io.lettuce.core.dynamic.annotation.Param;
import java.util.Collection;
import java.util.List;
import com.ozang.common.domain.clothes.repository.query.ClothesRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import org.springframework.data.jpa.repository.Query;

public interface ClothesRepository extends JpaRepository<Clothes, UUID>, ClothesRepositoryCustom {

  List<Clothes> findByIdIn(Collection<UUID> ids);

  List<Clothes> findAllByIdIn(Collection<UUID> ids);

  List<Clothes> findAllByOwnerId(UUID ownerId);

  @Query("SELECT c FROM Clothes c " +
      "LEFT JOIN FETCH c.attributes a " +
      "LEFT JOIN FETCH a.definition " +
      "WHERE c.ownerId = :ownerId")
  List<Clothes> findAllByOwnerIdWithAttributes(@Param("ownerId") UUID ownerId);

  @Query("SELECT c FROM Clothes c " +
      "LEFT JOIN FETCH c.attributes a " +
      "LEFT JOIN FETCH a.definition " +
      "WHERE c.id IN :clothesIds")
  List<Clothes> findAllByIdInWithAttributes(@Param("clothesIds") List<UUID> clothesIds);
}
