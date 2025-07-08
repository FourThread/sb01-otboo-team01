package com.fourthread.ozang.module.domain.clothes.service;

import com.fourthread.ozang.module.domain.clothes.dto.requeset.ClothesCreateRequest;
import com.fourthread.ozang.module.domain.clothes.dto.requeset.ClothesUpdateRequest;
import com.fourthread.ozang.module.domain.clothes.dto.response.*;
import com.fourthread.ozang.module.domain.clothes.entity.Clothes;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesAttributeDefinition;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesType;
import com.fourthread.ozang.module.domain.clothes.exception.ClothesAttributeDefinitionException;
import com.fourthread.ozang.module.domain.clothes.exception.ClothesException;
import com.fourthread.ozang.module.domain.clothes.mapper.ClothesMapper;
import com.fourthread.ozang.module.domain.clothes.repository.ClothesAttributeDefinitionRepository;
import com.fourthread.ozang.module.domain.clothes.repository.ClothesRepository;
import com.fourthread.ozang.module.domain.storage.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClothesServiceTest {

    private UUID clothesId;
    private UUID ownerId;
    private UUID definitionId;
    private ClothesAttributeDefinition definition;
    private Clothes existingClothes;
    private ClothesDto updatedDto;


    private ClothesDto clothesDto;

    @Mock
    private ClothesRepository clothesRepository;

    @Mock
    private ClothesAttributeDefinitionRepository definitionRepository;

    @Mock
    private ClothesMapper clothesMapper;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private ClothesService clothesService;

    @BeforeEach
    void setUp() {
        clothesId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        definitionId = UUID.randomUUID();

        definition = new ClothesAttributeDefinition("컬러", List.of("화이트", "블랙"));
        ReflectionTestUtils.setField(definition, "id", definitionId);

        existingClothes = Clothes.builder()
                .ownerId(ownerId)
                .name("기존 이름")
                .type(ClothesType.TOP)
                .build();
        ReflectionTestUtils.setField(existingClothes, "id", clothesId);

        updatedDto = new ClothesDto(
                clothesId,
                ownerId,
                "수정된 셔츠",
                null, // 이미지 없음
                ClothesType.TOP,
                List.of(new ClothesAttributeWithDefDto(
                        definitionId,
                        "컬러",
                        List.of("화이트", "블랙"),
                        "블랙"))
        );

        clothesDto = new ClothesDto(
                UUID.randomUUID(),
                ownerId,
                "여름 셔츠",
                null, // 이미지 없음
                ClothesType.TOP,
                List.of(new ClothesAttributeWithDefDto(
                        definitionId,
                        "컬러",
                        List.of("화이트", "블랙"),
                        "화이트"
                ))
        );
    }

    @DisplayName("옷을 등록할 수 있다.")
    @Test
    void clothes_create() {
        //given
        ClothesAttributeDto attributeDto = new ClothesAttributeDto(definitionId, "화이트");

        ClothesCreateRequest request = new ClothesCreateRequest(
                ownerId,
                "여름 셔츠",
                ClothesType.TOP,
                List.of(attributeDto)
        );

        given(definitionRepository.findById(definitionId)).willReturn(Optional.of(definition));
        given(clothesMapper.toDto(any(Clothes.class))).willReturn(clothesDto);

        //when
        ClothesDto result = clothesService.create(request, null);

        //then
        assertThat(result).isEqualTo(clothesDto);
        then(clothesRepository).should().save(any(Clothes.class));
        then(imageService).should(never()).uploadImage(any());
    }

    @DisplayName("이미지와 함께 옷을 등록할 수 있다.")
    @Test
    void clothes_create_with_image() {
        //given
        ClothesAttributeDto attributeDto = new ClothesAttributeDto(definitionId, "화이트");
        ClothesCreateRequest request = new ClothesCreateRequest(
            ownerId,
            "여름 셔츠",
            ClothesType.TOP,
            List.of(attributeDto)
        );

        MockMultipartFile imageFile = new MockMultipartFile(
            "image",
            "test-image.jpg",
            "image/jpeg",
            "test image content".getBytes()
        );

        String expectedImageUrl = "https://test-bucket.s3.amazonaws.com/clothes/test-image.jpg";

        given(definitionRepository.findById(definitionId)).willReturn(Optional.of(definition));
        given(imageService.uploadImage(imageFile)).willReturn(expectedImageUrl);
        given(clothesMapper.toDto(any(Clothes.class))).willReturn(clothesDto);

        //when
        ClothesDto result = clothesService.create(request, imageFile);

        //then
        assertThat(result).isEqualTo(clothesDto);
        then(clothesRepository).should().save(any(Clothes.class));
        then(imageService).should().uploadImage(imageFile);
    }


    @DisplayName("속성 정의 ID가 존재하지 않으면 의상 등록에 실패한다.")
    @Test
    void create_fail_definition_not_found() {
        //given
        UUID invalidDefinitionId = UUID.randomUUID();

        ClothesCreateRequest invalidRequest = new ClothesCreateRequest(
                ownerId,
                "여름 셔츠",
                ClothesType.TOP,
                List.of(new ClothesAttributeDto(invalidDefinitionId, "화이트"))
        );

        given(definitionRepository.findById(invalidDefinitionId)).willReturn(Optional.empty());

        //when then
        assertThatThrownBy(() -> clothesService.create(invalidRequest, null))
                .isInstanceOf(ClothesAttributeDefinitionException.class);

        then(clothesRepository).should(never()).save(any());
    }

    @DisplayName("의상을 수정할 수 있다")
    @Test
    void update_success() {
        //given
        ClothesAttributeDto attrDto = new ClothesAttributeDto(definitionId, "블랙");
        ClothesUpdateRequest request = new ClothesUpdateRequest("수정된 셔츠", ClothesType.TOP, List.of(attrDto));

        given(clothesRepository.findById(clothesId)).willReturn(Optional.of(existingClothes));
        given(definitionRepository.findById(definitionId)).willReturn(Optional.of(definition));
        given(clothesMapper.toDto(any())).willReturn(updatedDto);

        //when
        ClothesDto result = clothesService.update(clothesId, request, null);

        //then
        assertThat(result).isEqualTo(updatedDto);
        then(clothesRepository).should(never()).save(any());
    }

    @DisplayName("속성 정의 ID가 존재하지 않으면 수정에 실패한다")
    @Test
    void update_fail_definition_not_found() {
        //given
        UUID invalidDefId = UUID.randomUUID();
        ClothesUpdateRequest request = new ClothesUpdateRequest("수정된 셔츠", ClothesType.TOP,
                List.of(new ClothesAttributeDto(invalidDefId, "블랙")));

        given(clothesRepository.findById(clothesId)).willReturn(Optional.of(existingClothes));
        given(definitionRepository.findById(invalidDefId)).willReturn(Optional.empty());

        //when then
        assertThatThrownBy(() -> clothesService.update(clothesId, request, null))
                .isInstanceOf(ClothesAttributeDefinitionException.class);
        then(clothesRepository).should(never()).save(any());
    }


    @DisplayName("의상 ID가 존재하지 않으면 수정에 실패한다.")
    @Test
    void update_fail_clothes_not__found() {
        //given
        ClothesUpdateRequest request = new ClothesUpdateRequest("수정된 셔츠", ClothesType.TOP,
                List.of(new ClothesAttributeDto(definitionId, "블랙")));

        given(clothesRepository.findById(clothesId)).willReturn(Optional.empty());

        //when then
        assertThatThrownBy(() -> clothesService.update(clothesId, request, null))
                .isInstanceOf(ClothesException.class);
        then(definitionRepository).should(never()).findById(any());
    }

    @DisplayName("의상을 삭제할 수 있다.")
    @Test
    void clothes_delete() {
        //given
        given(clothesRepository.findById(clothesId)).willReturn(Optional.of(existingClothes));

        //when
        clothesService.delete(clothesId);

        //then
        then(clothesRepository).should().delete(existingClothes);
    }

    @DisplayName("존재하지 않은 옷 id로 삭제를 할 수 없다.")
    @Test
    void delete_fail_clothes_not_found() {
        //given
        UUID invalidId = UUID.randomUUID();
        given(clothesRepository.findById(invalidId)).willReturn(Optional.empty());

        //when then
        assertThatThrownBy(() -> clothesService.delete(invalidId))
                .isInstanceOf(ClothesException.class);
        then(clothesRepository).should(never()).delete(any());
    }

    @DisplayName("의상 목록을 커서 기반으로 조회할 수 있다.")
    @Test
    void findAll_shouldReturnSinglePage_whenDataExists() {
        //given
        String sortBy = "CREATED_AT";
        String sortDirection = "DESCENDING";
        String cursor = "2025-07-01T10:00:00";
        UUID idAfter = null;
        int limit = 10;

        Clothes clothes = Clothes.builder()
                .ownerId(ownerId)
                .name("테스트 옷")
                .type(ClothesType.TOP)
                .build();
        ReflectionTestUtils.setField(clothes, "id", clothesId);
        ReflectionTestUtils.setField(clothes, "createdAt", LocalDateTime.parse("2025-07-01T09:00:00"));

        when(clothesRepository.findAllByCondition(
                eq(ownerId), eq(cursor), eq(idAfter), eq(limit + 1), eq(ClothesType.TOP), eq(sortBy), eq(SortDirection.DESCENDING)
        )).thenReturn(List.of(clothes));

        when(clothesRepository.countByOwnerAndType(ownerId, ClothesType.TOP)).thenReturn(1);
        when(clothesMapper.toDto(clothes)).thenReturn(clothesDto);

        //when
        ClothesDtoCursorResponse response = clothesService.findAll(
                ownerId, cursor, idAfter, limit, ClothesType.TOP, sortBy, sortDirection
        );

        //then
        assertThat(response.data()).hasSize(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.sortBy()).isEqualTo("CREATED_AT");
        assertThat(response.sortDirection()).isEqualTo("DESCENDING");
    }

    @DisplayName("지원하지 않는 정렬 조건으로 의상 목록 조회를 할 수 없다.")
    @Test
    void findAll_shouldThrowException_whenSortByIsInvalid() {
        //given
        String invalidSortBy = "INVALID_FIELD";
        String sortDirection = "ASCENDING";
        String cursor = null;
        UUID idAfter = null;
        int limit = 10;

        //when then
        assertThatThrownBy(() -> clothesService.findAll(
                ownerId, cursor, idAfter, limit, ClothesType.TOP, invalidSortBy, sortDirection
        )).isInstanceOf(IllegalArgumentException.class);
    }

}