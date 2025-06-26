package com.fourthread.ozang.module.domain.clothes.service;

import com.fourthread.ozang.module.domain.clothes.dto.requeset.ClothesAttributeDefCreateRequest;
import com.fourthread.ozang.module.domain.clothes.dto.requeset.ClothesAttributeDefUpdateRequest;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

    @DisplayName("이미 존재하는 속성 정의 이름으로 등록할 수 없다.")
    @Test
    void ClothesAttributeDefCreate_fail_whenNameDuplicated() {
        // given
        String name = "두께감";
        List<String> selectableValues = List.of("얇음", "보통", "두꺼움");
        ClothesAttributeDefCreateRequest request = new ClothesAttributeDefCreateRequest(name, selectableValues);

        given(repository.existsByName(name)).willReturn(true);

        // when then
        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class); //TODO 추후 예외 수정
        then(repository).should(never()).save(any());
    }

    @DisplayName("의상 속성 정의를 수정할 수 있다.")
    @Test
    void ClothesAttributeDefUpdate() {
        //given
        UUID id = clothesAttributeDefinition.getId();
        ClothesAttributeDefUpdateRequest request = new ClothesAttributeDefUpdateRequest("두께감", List.of("얇음", "보통", "두꺼움"));

        given(repository.findById(id)).willReturn(Optional.of(clothesAttributeDefinition));
        given(repository.existsByNameAndIdNot("두께감", id)).willReturn(false);
        given(mapper.toDto(clothesAttributeDefinition)).willReturn(clothesAttributeDefDto);

        //when
        ClothesAttributeDefDto result = service.update(id, request);

        //then
        assertThat(result).isEqualTo(clothesAttributeDefDto);
    }

    @DisplayName("존재하지 않는 속성 정의 ID로 수정할 수 없다.")
    @Test
    void update_fail_whenDefinitionNotFound() {
        //given
        UUID id = UUID.randomUUID();
        ClothesAttributeDefUpdateRequest request = new ClothesAttributeDefUpdateRequest("두께감", List.of("얇음", "보통", "두꺼움"));

        given(repository.findById(id)).willReturn(Optional.empty());

        //when then
        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("이미 존재하는 속성 정의 이름으로 수정할 수 없다.")
    @Test
    void update_fail_whenNameIsDuplicated() {
        //given
        UUID id = clothesAttributeDefinition.getId();
        ClothesAttributeDefUpdateRequest request = new ClothesAttributeDefUpdateRequest("촉감", List.of("부드러움", "뻣뻣함"));

        given(repository.findById(id)).willReturn(Optional.of(clothesAttributeDefinition));
        given(repository.existsByNameAndIdNot("촉감", id)).willReturn(true);

        //when then
        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("의상 속성 정의를 삭제할 수 있다.")
    @Test
    void ClothesAttributeDefDelete() {
        //given
        UUID id = clothesAttributeDefinition.getId();

        given(repository.findById(id)).willReturn(Optional.of(clothesAttributeDefinition));
        given(mapper.toDto(clothesAttributeDefinition)).willReturn(clothesAttributeDefDto);

        //when
        ClothesAttributeDefDto result = service.delete(id);

        //then
        assertThat(result).isEqualTo(clothesAttributeDefDto);
        verify(repository).delete(clothesAttributeDefinition);
    }

    @DisplayName("존재하지 않는 속성 정의는 삭제할 수 없다.")
    @Test
    void delete_fail_whenDefinitionNotFound() {
        //given
        UUID id = UUID.randomUUID();
        given(repository.findById(id)).willReturn(Optional.empty());

        //when then
        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).delete(any());
    }
}