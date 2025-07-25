package com.fourthread.ozang.domain.clothes.repository;

import com.fourthread.ozang.module.domain.clothes.entity.ClothesAttributeDefinition;
import com.fourthread.ozang.module.domain.clothes.repository.query.ClothesAttributeDefinitionRepositoryCustom;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClothesAttributeDefinitionRepository extends JpaRepository<ClothesAttributeDefinition, UUID>, ClothesAttributeDefinitionRepositoryCustom {

    boolean existsByName(String name);

    //수정 중복 체크용 - 자기 자신 제외
    boolean existsByNameAndIdNot(String name, UUID id);

}
