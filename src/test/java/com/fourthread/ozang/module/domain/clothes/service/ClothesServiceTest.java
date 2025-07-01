package com.fourthread.ozang.module.domain.clothes.service;

import com.fourthread.ozang.module.domain.clothes.dto.requeset.ClothesCreateRequest;
import com.fourthread.ozang.module.domain.clothes.dto.response.ClothesAttributeDto;
import com.fourthread.ozang.module.domain.clothes.dto.response.ClothesAttributeWithDefDto;
import com.fourthread.ozang.module.domain.clothes.dto.response.ClothesDto;
import com.fourthread.ozang.module.domain.clothes.entity.Clothes;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesAttributeDefinition;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesType;
import com.fourthread.ozang.module.domain.clothes.mapper.ClothesMapper;
import com.fourthread.ozang.module.domain.clothes.repository.ClothesAttributeDefinitionRepository;
import com.fourthread.ozang.module.domain.clothes.repository.ClothesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ClothesServiceTest {

    private UUID ownerId;
    private UUID definitionId;
    private ClothesAttributeDefinition definition;

    private ClothesDto clothesDto;

    @Mock
    private ClothesRepository clothesRepository;

    @Mock
    private ClothesAttributeDefinitionRepository definitionRepository;

    @Mock
    private ClothesMapper clothesMapper;

    @InjectMocks
    private ClothesService clothesService;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        definitionId = UUID.randomUUID();

        definition = new ClothesAttributeDefinition("컬러", List.of("화이트", "블랙"));
        ReflectionTestUtils.setField(definition, "id", definitionId);


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
                .isInstanceOf(IllegalArgumentException.class); //TODO 커스텀 예외처리

        then(clothesRepository).should(never()).save(any());
    }
}