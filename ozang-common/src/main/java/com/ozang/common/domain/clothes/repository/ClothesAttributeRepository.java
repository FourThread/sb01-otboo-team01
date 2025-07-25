package com.ozang.common.domain.clothes.repository;

import com.ozang.common.domain.clothes.entity.ClothesAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ClothesAttributeRepository extends JpaRepository<ClothesAttribute, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ClothesAttribute ca WHERE ca.definition.id = :definitionId")
    void deleteAllByDefinitionId(@Param("definitionId") UUID definitionId);
}
