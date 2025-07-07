package com.fourthread.ozang.module.domain.clothes.service;

import com.fourthread.ozang.module.domain.clothes.dto.requeset.ClothesCreateRequest;
import com.fourthread.ozang.module.domain.clothes.dto.requeset.ClothesUpdateRequest;
import com.fourthread.ozang.module.domain.clothes.dto.response.*;
import com.fourthread.ozang.module.domain.clothes.entity.Clothes;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesAttribute;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesAttributeDefinition;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesType;
import com.fourthread.ozang.module.domain.clothes.exception.ClothesAttributeDefinitionException;
import com.fourthread.ozang.module.domain.clothes.exception.ClothesException;
import com.fourthread.ozang.module.domain.clothes.mapper.ClothesMapper;
import com.fourthread.ozang.module.domain.clothes.repository.ClothesAttributeDefinitionRepository;
import com.fourthread.ozang.module.domain.clothes.repository.ClothesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static com.fourthread.ozang.module.common.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesService {

    private final ClothesRepository clothesRepository;
    private final ClothesAttributeDefinitionRepository definitionRepository;
    private final ClothesMapper clothesMapper;
    private final ImageService imageService;

    @Transactional
    public ClothesDto create(ClothesCreateRequest request, MultipartFile image) {

        //TODO ownerId 존재 검증

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = imageService.uploadImage(image);
            log.info("Image uploaded for new clothes. URL: {}", imageUrl);
        }

        Clothes clothes = Clothes.builder()
                .ownerId(request.ownerId())
                .name(request.name())
                .type(request.type())
                .build();

        if (imageUrl != null) {
            clothes.updateImageUrl(imageUrl);
        }

        addAttributesToClothes(clothes, request.attributes());

        clothesRepository.save(clothes);
        return clothesMapper.toDto(clothes);
    }

    @Transactional
    public ClothesDto update(UUID clothesId, ClothesUpdateRequest request, MultipartFile imageFile) {
        Clothes clothes = clothesRepository.findById(clothesId)
                .orElseThrow(() -> new ClothesException(CLOTHES_NOT_FOUND, this.getClass().getSimpleName(), CLOTHES_NOT_FOUND.getMessage()));

        clothes.updateNameAndType(request.name(), request.type());
        clothes.clearAttributes();

        addAttributesToClothes(clothes, request.attributes());

        if (imageFile != null && !imageFile.isEmpty()) {
            String oldImageUrl = clothes.getImageUrl();
            if (oldImageUrl != null && !oldImageUrl.isBlank()) {
                try {
                    imageService.deleteImage(oldImageUrl);
                    log.info("Old image deleted={}", oldImageUrl);
                } catch (Exception e) {
                    log.warn("Failed to delete old image={}", oldImageUrl, e);
                }
            }

            String newImageUrl = imageService.uploadImage(imageFile);
            clothes.updateImageUrl(newImageUrl);
            log.info("New image upload for clothes {}: {}", clothesId, newImageUrl);
        }

        return clothesMapper.toDto(clothes);
    }

    private void addAttributesToClothes(Clothes clothes, List<ClothesAttributeDto> attributeDtos) {
        for (ClothesAttributeDto attrDto : attributeDtos) {
            ClothesAttributeDefinition def = definitionRepository.findById(attrDto.definitionId())
                    .orElseThrow(() -> new ClothesAttributeDefinitionException(CLOTHES_ATTRIBUTE_DEFINITION_NOT_FOUND, this.getClass().getSimpleName(), CLOTHES_ATTRIBUTE_DEFINITION_NOT_FOUND.getMessage()));
            ClothesAttribute attribute = new ClothesAttribute(def, attrDto.value());
            clothes.addAttribute(attribute);
        }
    }

    @Transactional
    public void delete(UUID clothesId) {
        Clothes clothes = clothesRepository.findById(clothesId)
                .orElseThrow(() -> new ClothesException(CLOTHES_NOT_FOUND, this.getClass().getSimpleName(), CLOTHES_NOT_FOUND.getMessage()));

        String imageUrl = clothes.getImageUrl();
        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                imageService.deleteImage(imageUrl);
                log.info("Image deleted for clothes {}: {}", clothesId, imageUrl);
            } catch (Exception e) {
                log.warn("Failed to delete image for clothes {}: {}", clothesId, imageUrl, e);
            }
        }

        clothesRepository.delete(clothes);
    }


    @Transactional(readOnly = true)
    public ClothesDtoCursorResponse findAll(UUID ownerId, String cursor, UUID idAfter, int limit, ClothesType typeEqual, String sortBy, String sortDirection) {

        validateSortBy(sortBy);
        SortDirection direction = SortDirection.from(sortDirection);

        List<Clothes> results = clothesRepository.findAllByCondition(
                ownerId, cursor, idAfter, limit + 1, typeEqual, sortBy, direction
        );

        boolean hasNext = results.size() > limit;
        List<Clothes> pageContent = hasNext ? results.subList(0, limit) : results;

        UUID nextId = hasNext ? pageContent.get(pageContent.size() - 1).getId() : null;
        String nextCursor = hasNext ? getCursorValue(pageContent.get(pageContent.size() - 1), sortBy) : null;

        int totalCount = clothesRepository.countByOwnerAndType(ownerId, typeEqual);

        List<ClothesDto> dtoList = pageContent.stream()
                .map(clothesMapper::toDto)
                .toList();

        return new ClothesDtoCursorResponse(
                dtoList,
                nextCursor,
                nextId,
                hasNext,
                totalCount,
                sortBy.toUpperCase(),
                direction.name()
        );
    }

    private void validateSortBy(String sortBy) {
        if (!"CREATED_AT".equalsIgnoreCase(sortBy)) {
            throw new IllegalArgumentException("지원하지 않는 정렬 필드입니다: " + sortBy);
        }
    }

    private String getCursorValue(Clothes clothes, String sortBy) {
        return switch (sortBy.toUpperCase()) {
            case "CREATED_AT" -> clothes.getCreatedAt().toString();
            default -> throw new IllegalArgumentException("지원하지 않는 커서 필드입니다: " + sortBy);
        };
    }


}
