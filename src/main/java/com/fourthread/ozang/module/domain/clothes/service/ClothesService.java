package com.fourthread.ozang.module.domain.clothes.service;

import com.fourthread.ozang.module.domain.clothes.dto.requeset.ClothesCreateRequest;
import com.fourthread.ozang.module.domain.clothes.dto.requeset.ClothesUpdateRequest;
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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClothesService {

    private final ClothesRepository clothesRepository;
    private final ClothesAttributeDefinitionRepository definitionRepository;
    private final ClothesMapper clothesMapper;

    @Transactional
    public ClothesDto create(ClothesCreateRequest request, MultipartFile image) {

        //TODO 이미지 저장

        Clothes clothes = Clothes.builder()
                .ownerId(request.ownerId())
                .name(request.name())
                .type(request.type())
                .build();

        addAttributesToClothes(clothes, request.attributes());

        clothesRepository.save(clothes);
        return clothesMapper.toDto(clothes);
    }

    @Transactional
    public ClothesDto update(UUID clothesId, ClothesUpdateRequest request, MultipartFile imageFile) {
        Clothes clothes = clothesRepository.findById(clothesId)
                .orElseThrow(() -> new IllegalArgumentException("의상 정보를 찾을 수 없습니다."));

        clothes.updateNameAndType(request.name(), request.type());
        clothes.clearAttributes();

        addAttributesToClothes(clothes, request.attributes());

        //TODO 이미지 갱신
        //if (imageFile != null && !imageFile.isEmpty()) { }

        return clothesMapper.toDto(clothes);
    }

    private void addAttributesToClothes(Clothes clothes, List<ClothesAttributeDto> attributeDtos) {
        for (ClothesAttributeDto attrDto : attributeDtos) {
            ClothesAttributeDefinition def = definitionRepository.findById(attrDto.definitionId())
                    .orElseThrow(() -> new IllegalArgumentException("정의 정보를 찾을 수 없습니다."));
            ClothesAttribute attribute = new ClothesAttribute(def, attrDto.value());
            clothes.addAttribute(attribute);
        }
    }

    public void delete(UUID clothesId) {
        Clothes clothes = clothesRepository.findById(clothesId)
                .orElseThrow(() -> new IllegalArgumentException("의상 정보를 찾을 수 없습니다."));
        clothesRepository.delete(clothes);
    }
}
