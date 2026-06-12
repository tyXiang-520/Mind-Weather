package com.mindweather.user.business.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MapWeatherDTO {

    private WeatherResultDTO campusMainWeather;
    private Integer totalPostsToday;
    private List<HotTag> hotTags;
    private List<AreaInfo> areas;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HotTag {
        private String name;
        private Integer count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AreaInfo {
        private Integer id;
        private String name;
        private String code;
        private BigDecimal lat;
        private BigDecimal lng;
        private String weatherCode;
        private String weatherIcon;
        private String weatherName;
        private Integer postCount;
        private String dominantEmotion;
        private List<String> topTags;
        private Map<String, Double> emotionDistribution;
    }
}
