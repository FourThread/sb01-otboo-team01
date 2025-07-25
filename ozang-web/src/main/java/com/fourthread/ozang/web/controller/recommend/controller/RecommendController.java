package com.fourthread.ozang.domain.recommend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fourthread.ozang.module.domain.recommend.dto.RecommendationDto;
import com.fourthread.ozang.module.domain.recommend.service.RecommendClothesService;
import com.fourthread.ozang.module.domain.security.userdetails.UserDetailsImpl;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendController {

  private final RecommendClothesService recommendService;

  @GetMapping
  public RecommendationDto recommend(
      @RequestParam UUID weatherId,
      @AuthenticationPrincipal UserDetailsImpl userDetails
  ) throws JsonProcessingException {

    return recommendService.recommend(weatherId, userDetails.getPayloadDto().userId());
  }

}
