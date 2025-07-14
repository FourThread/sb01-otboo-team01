package com.fourthread.ozang.module.domain.clothes.controller;

import com.fourthread.ozang.module.domain.clothes.dto.requeset.ClothesAttributeDefCreateRequest;
import com.fourthread.ozang.module.domain.clothes.dto.requeset.ClothesAttributeDefUpdateRequest;
import com.fourthread.ozang.module.domain.clothes.dto.response.ClothesAttributeDefDto;
import com.fourthread.ozang.module.domain.clothes.dto.response.CursorPageResponseClothesAttributeDefDto;
import com.fourthread.ozang.module.domain.clothes.service.ClothesAttributeDefinitionService;
import com.fourthread.ozang.module.domain.security.userdetails.UserDetailsImpl;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody @Validated ClothesAttributeDefCreateRequest request) {

        UUID userId = userDetails.getPayloadDto().userId();
        ClothesAttributeDefDto response = definitionService.create(request, userId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{definitionId}")
    public ResponseEntity<ClothesAttributeDefDto> update(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable @NotNull UUID definitionId,
            @RequestBody @Validated ClothesAttributeDefUpdateRequest request) {

        UUID userId = userDetails.getPayloadDto().userId();
        ClothesAttributeDefDto response = definitionService.update(definitionId, request, userId);
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

    @GetMapping
    public ResponseEntity<CursorPageResponseClothesAttributeDefDto> findAll(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) UUID idAfter,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASCENDING") String sortDirection,
            @RequestParam(required = false) String keywordLike
    ) {
        CursorPageResponseClothesAttributeDefDto result =
                definitionService.findAll(cursor, idAfter, limit, sortBy, sortDirection, keywordLike);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(result);
    }
}