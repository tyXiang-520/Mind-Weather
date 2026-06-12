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
public class PostTextResponse {

    private Long postId;
    private String emotionType;
    private String weatherCode;
    private String weatherName;
    private String weatherIcon;
    private List<String> tags;
}
