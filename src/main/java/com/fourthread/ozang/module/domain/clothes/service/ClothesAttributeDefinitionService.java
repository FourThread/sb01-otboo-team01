package com.fourthread.ozang.module.domain.clothes.service;


import com.fourthread.ozang.module.domain.clothes.dto.requeset.ClothesAttributeDefCreateRequest;
import com.fourthread.ozang.module.domain.clothes.dto.requeset.ClothesAttributeDefUpdateRequest;
import com.fourthread.ozang.module.domain.clothes.dto.response.ClothesAttributeDefDto;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesAttributeDefinition;
import com.fourthread.ozang.module.domain.clothes.mapper.ClothesAttributeDefinitionMapper;
import com.fourthread.ozang.module.domain.clothes.repository.ClothesAttributeDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClothesAttributeDefinitionService {

    private final ClothesAttributeDefinitionRepository definitionRepository;
    private final ClothesAttributeDefinitionMapper definitionMapper;

    public ClothesAttributeDefDto create(ClothesAttributeDefCreateRequest request) {
        if (definitionRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("이미 존재하는 속성 이름입니다."); //TODO 커스텀 예외처리
        }
        ClothesAttributeDefinition clothesAttributeDefinition = new ClothesAttributeDefinition(request.name(), request.selectableValues());
        ClothesAttributeDefinition save = definitionRepository.save(clothesAttributeDefinition);
        return definitionMapper.toDto(save);
    }

    public ClothesAttributeDefDto update(UUID definitionId, ClothesAttributeDefUpdateRequest request) {
        ClothesAttributeDefinition definition = definitionRepository.findById(definitionId)
                .orElseThrow(() -> new IllegalArgumentException("속성 정의가 존재하지 않습니다.")); //TODO 커스텀 예외처리

        if (definitionRepository.existsByNameAndIdNot(request.name(), definitionId)) {
            throw new IllegalArgumentException("해당 속성정의 이름이 이미 존재합니다."); //TODO 커스텀 예외처리
        }

        definition.update(request.name(), request.selectableValues());
        return definitionMapper.toDto(definition);
    }


}
