package com.ozang.common.domain.clothes.repository;

import com.ozang.common.domain.clothes.entity.ClothesAttributeDefinition;
import com.ozang.common.domain.clothes.repository.query.ClothesAttributeDefinitionRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClothesAttributeDefinitionRepository extends JpaRepository<ClothesAttributeDefinition, UUID>, ClothesAttributeDefinitionRepositoryCustom {

    boolean existsByName(String name);

    //수정 중복 체크용 - 자기 자신 제외
    boolean existsByNameAndIdNot(String name, UUID id);

}
