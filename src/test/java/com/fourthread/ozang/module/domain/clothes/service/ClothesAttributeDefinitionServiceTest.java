package com.fourthread.ozang.module.domain.clothes.service;

import com.fourthread.ozang.module.domain.clothes.dto.requeset.ClothesAttributeDefCreateRequest;
import com.fourthread.ozang.module.domain.clothes.dto.response.ClothesAttributeDefDto;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesAttributeDefinition;
import com.fourthread.ozang.module.domain.clothes.mapper.ClothesAttributeDefinitionMapper;
import com.fourthread.ozang.module.domain.clothes.repository.ClothesAttributeDefinitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ClothesAttributeDefinitionServiceTest {

    private UUID ClothesAttributeDefinitionId;
    private ClothesAttributeDefinition clothesAttributeDefinition;
    private ClothesAttributeDefDto clothesAttributeDefDto;


    @Mock
    private ClothesAttributeDefinitionRepository repository;

    @Mock
    private ClothesAttributeDefinitionMapper mapper;

    @InjectMocks
    private ClothesAttributeDefinitionService service;


    @BeforeEach
    void setUp() {
        ClothesAttributeDefinitionId = UUID.randomUUID();

        clothesAttributeDefinition = new ClothesAttributeDefinition("두께감", List.of("얇음", "보통", "두꺼움"));
        ReflectionTestUtils.setField(clothesAttributeDefinition, "id", ClothesAttributeDefinitionId);

        clothesAttributeDefDto = new ClothesAttributeDefDto(
                ClothesAttributeDefinitionId,
                clothesAttributeDefinition.getName(),
                clothesAttributeDefinition.getSelectableValues()
        );
    }

    @DisplayName("의상 속성 정의를 등록할 수 있다.")
    @Test
    void ClothesAttributeDefCreate() {
        //given
        String name = "두께감";
        List<String> selectableValues = List.of("얇음", "보통", "두꺼움");
        ClothesAttributeDefCreateRequest request = new ClothesAttributeDefCreateRequest(name, selectableValues);

        given(repository.save(any())).willReturn(clothesAttributeDefinition);
        given(mapper.toDto(clothesAttributeDefinition)).willReturn(clothesAttributeDefDto);

        //when
        ClothesAttributeDefDto result = service.create(request);

        //then
        assertThat(result).isEqualTo(clothesAttributeDefDto);
    }

}