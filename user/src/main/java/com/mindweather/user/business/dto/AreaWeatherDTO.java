package com.mindweather.user.business.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AreaWeatherDTO {

    private WeatherResultDTO dominantWeather;
    private Integer postCount;
    private Map<String, Double> weatherDistribution;
}
