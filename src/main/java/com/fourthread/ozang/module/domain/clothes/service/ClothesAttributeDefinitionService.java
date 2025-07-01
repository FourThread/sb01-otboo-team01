package com.fourthread.ozang.module.domain.clothes.service;


import com.fourthread.ozang.module.domain.clothes.dto.requeset.ClothesAttributeDefCreateRequest;
import com.fourthread.ozang.module.domain.clothes.dto.requeset.ClothesAttributeDefUpdateRequest;
import com.fourthread.ozang.module.domain.clothes.dto.response.ClothesAttributeDefDto;
import com.fourthread.ozang.module.domain.clothes.dto.response.CursorPageResponseClothesAttributeDefDto;
import com.fourthread.ozang.module.domain.clothes.dto.response.SortBy;
import com.fourthread.ozang.module.domain.clothes.dto.response.SortDirection;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesAttributeDefinition;
import com.fourthread.ozang.module.domain.clothes.mapper.ClothesAttributeDefinitionMapper;
import com.fourthread.ozang.module.domain.clothes.repository.ClothesAttributeDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClothesAttributeDefinitionService {

    private final ClothesAttributeDefinitionRepository definitionRepository;
    private final ClothesAttributeDefinitionMapper definitionMapper;

    @Transactional
    public ClothesAttributeDefDto create(ClothesAttributeDefCreateRequest request) {
        if (definitionRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("이미 존재하는 속성 이름입니다."); //TODO 커스텀 예외처리
        }
        ClothesAttributeDefinition clothesAttributeDefinition = new ClothesAttributeDefinition(request.name(), request.selectableValues());
        ClothesAttributeDefinition save = definitionRepository.save(clothesAttributeDefinition);
        return definitionMapper.toDto(save);
    }

    @Transactional
    public ClothesAttributeDefDto update(UUID definitionId, ClothesAttributeDefUpdateRequest request) {
        ClothesAttributeDefinition definition = definitionRepository.findById(definitionId)
                .orElseThrow(() -> new IllegalArgumentException("속성 정의가 존재하지 않습니다.")); //TODO 커스텀 예외처리

        if (definitionRepository.existsByNameAndIdNot(request.name(), definitionId)) {
            throw new IllegalArgumentException("해당 속성정의 이름이 이미 존재합니다."); //TODO 커스텀 예외처리
        }

        definition.update(request.name(), request.selectableValues());
        return definitionMapper.toDto(definition);
    }

    @Transactional
    public ClothesAttributeDefDto delete(UUID definitionId) {
        ClothesAttributeDefinition definition = definitionRepository.findById(definitionId)
                .orElseThrow(() -> new IllegalArgumentException("속성 정의가 존재하지 않습니다.")); //TODO 커스텀 예외처리
        ClothesAttributeDefDto dto = definitionMapper.toDto(definition);
        definitionRepository.delete(definition);
        return dto;
    }

    @Transactional(readOnly = true)
    public CursorPageResponseClothesAttributeDefDto findAll(
            String cursor,
            UUID idAfter,
            int limit,
            String sortBy,
            String sortDirection,
            String keywordLike
    ) {
        SortBy sortByEnum = SortBy.from(sortBy);
        SortDirection sortDirectionEnum = SortDirection.from(sortDirection);

        validateCursorFormatIfIdSorting(sortByEnum, cursor);

        List<ClothesAttributeDefinition> results = definitionRepository.findAllByCondition(
                cursor, idAfter, limit + 1, sortByEnum, sortDirectionEnum, keywordLike
        );

        boolean hasNext = results.size() > limit;
        List<ClothesAttributeDefinition> pageContent = hasNext ? results.subList(0, limit) : results;

        UUID nextId = hasNext ? pageContent.get(pageContent.size() - 1).getId() : null;
        String nextCursor = hasNext ? getCursorValue(pageContent.get(pageContent.size() - 1), sortByEnum) : null;

        int totalCount = definitionRepository.countByCondition(keywordLike);

        List<ClothesAttributeDefDto> dtoList = pageContent.stream()
                .map(def -> new ClothesAttributeDefDto(def.getId(), def.getName(), def.getSelectableValues()))
                .toList();

        return new CursorPageResponseClothesAttributeDefDto(
                dtoList,
                nextCursor,
                nextId,
                hasNext,
                totalCount,
                sortByEnum.name(),
                sortDirectionEnum.name()
        );
    }

    private String getCursorValue(ClothesAttributeDefinition def, SortBy sortBy) {
        return switch (sortBy) {
            case NAME -> def.getName();
            case ID -> def.getId().toString();
        };
    }

    private void validateCursorFormatIfIdSorting(SortBy sortBy, String cursor) {
        if (sortBy == SortBy.ID && cursor != null) {
            try {
                UUID.fromString(cursor); // 포맷 검증만
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("cursor는 UUID 형식이어야 합니다."); //TODO 커스텀 예외처리
            }
        }
    }
}
