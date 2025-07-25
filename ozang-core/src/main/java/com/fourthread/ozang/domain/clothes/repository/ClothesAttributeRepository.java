package com.fourthread.ozang.domain.clothes.repository;

import com.fourthread.ozang.module.domain.clothes.entity.ClothesAttribute;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClothesAttributeRepository extends JpaRepository<ClothesAttribute, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ClothesAttribute ca WHERE ca.definition.id = :definitionId")
    void deleteAllByDefinitionId(@Param("definitionId") UUID definitionId);
}
