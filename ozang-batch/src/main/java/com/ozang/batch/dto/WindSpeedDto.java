package com.ozang.batch.dto;

import com.ozang.batch.dto.type.WindStrength;

public record WindSpeedDto(
     Double speed,
     WindStrength asWord
) {}