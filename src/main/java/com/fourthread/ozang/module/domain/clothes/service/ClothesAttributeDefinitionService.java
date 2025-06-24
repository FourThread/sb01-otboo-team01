package com.fourthread.ozang.module.domain.clothes.service;


import com.fourthread.ozang.module.domain.clothes.dto.requeset.ClothesAttributeDefCreateRequest;
import com.fourthread.ozang.module.domain.clothes.dto.response.ClothesAttributeDefDto;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesAttributeDefinition;
import com.fourthread.ozang.module.domain.clothes.mapper.ClothesAttributeDefinitionMapper;
import com.fourthread.ozang.module.domain.clothes.repository.ClothesAttributeDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClothesAttributeDefinitionService {

    private final ClothesAttributeDefinitionRepository repository;
    private final ClothesAttributeDefinitionMapper mapper;

    public ClothesAttributeDefDto create(ClothesAttributeDefCreateRequest request) {
        ClothesAttributeDefinition clothesAttributeDefinition = new ClothesAttributeDefinition(request.name(), request.selectableValues());
        ClothesAttributeDefinition save = repository.save(clothesAttributeDefinition);
        return mapper.toDto(save);
    }
}
