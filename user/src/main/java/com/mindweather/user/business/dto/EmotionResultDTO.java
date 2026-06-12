package com.mindweather.user.business.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionResultDTO {

    private String emotionType;
    private String emotionTag;
    private String weatherCode;
    private Double confidence;
    private List<String> tags;
}
