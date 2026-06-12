package com.mindweather.user.business.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWeatherDTO {

    private TodayWeather today;
    private List<MyMapArea> myMapAreas;
    private List<TrendPoint> trend;
    private CalendarData calendar;
    private Map<String, Integer> areaDistribution;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodayWeather {
        private String date;
        private String weatherCode;
        private String weatherIcon;
        private String weatherName;
        private String summary;
        private List<String> tags;
        private Integer postCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyMapArea {
        private String name;
        private String code;
        private String weatherIcon;
        private Integer myPostCount;
        private Double lat;
        private Double lng;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String date;
        private String weatherIcon;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalendarData {
        private String month;
        private List<CalendarDay> days;
        private MonthStats stats;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalendarDay {
        private String date;
        private String weatherIcon;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthStats {
        private Integer sunnyDays;
        private Integer cloudyDays;
        private Integer rainyDays;
        private Integer activeDays;
    }
}
