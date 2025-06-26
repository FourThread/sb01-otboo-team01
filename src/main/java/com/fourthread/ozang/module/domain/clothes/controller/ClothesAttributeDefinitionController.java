package com.fourthread.ozang.module.domain.clothes.controller;

import com.fourthread.ozang.module.domain.clothes.dto.requeset.ClothesAttributeDefCreateRequest;
import com.fourthread.ozang.module.domain.clothes.dto.requeset.ClothesAttributeDefUpdateRequest;
import com.fourthread.ozang.module.domain.clothes.dto.response.ClothesAttributeDefDto;
import com.fourthread.ozang.module.domain.clothes.service.ClothesAttributeDefinitionService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/clothes/attribute-defs")
@RequiredArgsConstructor
public class ClothesAttributeDefinitionController {

    private final ClothesAttributeDefinitionService definitionService;

    //TODO 어드민 사용자만 의상 속성을 정의(생성, 수정, 삭제)할 수 있다.

    @PostMapping
    public ResponseEntity<ClothesAttributeDefDto> create(
            @RequestBody @Validated ClothesAttributeDefCreateRequest request) {

        ClothesAttributeDefDto response = definitionService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{definitionId}")
    public ResponseEntity<ClothesAttributeDefDto> update(
            @PathVariable @NotNull UUID definitionId,
            @RequestBody @Validated ClothesAttributeDefUpdateRequest request) {

        ClothesAttributeDefDto response = definitionService.update(definitionId, request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @DeleteMapping("/{definitionId}")
    public ResponseEntity<ClothesAttributeDefDto> delete(
            @PathVariable @NotNull UUID definitionId) {
        ClothesAttributeDefDto response = definitionService.delete(definitionId);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(response);
        //TODO 204 인데 바디 값을 보내야함 -> api 스펙 이상하다.
    }


}