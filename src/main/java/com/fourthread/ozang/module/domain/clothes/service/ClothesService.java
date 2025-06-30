package com.fourthread.ozang.module.domain.clothes.service;

import com.fourthread.ozang.module.domain.clothes.dto.requeset.ClothesCreateRequest;
import com.fourthread.ozang.module.domain.clothes.dto.response.ClothesAttributeDto;
import com.fourthread.ozang.module.domain.clothes.dto.response.ClothesDto;
import com.fourthread.ozang.module.domain.clothes.entity.Clothes;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesAttribute;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesAttributeDefinition;
import com.fourthread.ozang.module.domain.clothes.mapper.ClothesMapper;
import com.fourthread.ozang.module.domain.clothes.repository.ClothesAttributeDefinitionRepository;
import com.fourthread.ozang.module.domain.clothes.repository.ClothesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ClothesService {

    private final ClothesRepository clothesRepository;
    private final ClothesAttributeDefinitionRepository definitionRepository;
    private final ClothesMapper clothesMapper;

    @Transactional
    public ClothesDto create(ClothesCreateRequest request, MultipartFile image) {

        Clothes clothes = new Clothes(
                request.ownerId(),
                request.name(),
                request.type(),
                null //TODO 이미지 저장
        );

        for (ClothesAttributeDto attrDto : request.attributes()) {
            ClothesAttributeDefinition definition = definitionRepository.findById(attrDto.definitionId())
                    .orElseThrow(() -> new IllegalArgumentException("정의 ID가 잘못됨: " + attrDto.definitionId()));

            ClothesAttribute attribute = new ClothesAttribute(definition, attrDto.value());
            clothes.addAttribute(attribute);
        }

        clothesRepository.save(clothes);
        return clothesMapper.toDto(clothes);
    }
}
