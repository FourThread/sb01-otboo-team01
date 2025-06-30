package com.fourthread.ozang.module.domain.clothes.controller;

import com.fourthread.ozang.module.domain.clothes.dto.requeset.ClothesCreateRequest;
import com.fourthread.ozang.module.domain.clothes.dto.requeset.ClothesUpdateRequest;
import com.fourthread.ozang.module.domain.clothes.dto.response.ClothesDto;
import com.fourthread.ozang.module.domain.clothes.service.ClothesService;
import lombok.RequiredArgsConstructor;
import org.apache.http.protocol.HTTP;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clothes")
public class ClothesController {

    private final ClothesService clothesService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClothesDto> create(
            @RequestPart("request") @Validated ClothesCreateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        ClothesDto response = clothesService.create(request, imageFile);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping(value = "/{clothesId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClothesDto> update(
            @PathVariable UUID clothesId,
            @RequestPart("request") @Validated ClothesUpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        ClothesDto response = clothesService.update(clothesId, request, imageFile);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable UUID clothesId) {
        clothesService.delete(clothesId);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}
