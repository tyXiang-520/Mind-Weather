package com.mindweather.user.business.service.impl;

import com.mindweather.user.business.dto.AreaWeatherDTO;
import com.mindweather.user.business.dto.WeatherResultDTO;
import com.mindweather.user.business.service.AreaWeatherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AreaWeatherServiceImpl implements AreaWeatherService {

    private static final String DEFAULT_WEATHER_CODE = "cloudy";
    private static final String DEFAULT_WEATHER_ICON = "⛅";
    private static final String DEFAULT_WEATHER_NAME = "多云";

    @Override
    public AreaWeatherDTO aggregate(List<WeatherResultDTO> weatherList) {
        if (weatherList == null || weatherList.isEmpty()) {
            log.debug("天气记录列表为空，返回默认聚合结果");
            return buildEmptyResult();
        }

        final int totalCount = weatherList.size();

        Map<String, Long> weatherCounts = weatherList.stream()
                .map(WeatherResultDTO::getWeatherCode)
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        Map.Entry<String, Long> dominantEntry = weatherCounts.entrySet().stream()
                .max(Comparator
                        .comparing(Map.Entry<String, Long>::getValue)
                        .thenComparing(Map.Entry::getKey))
                .orElse(null);

        final String dominantCode = dominantEntry != null
                ? dominantEntry.getKey()
                : DEFAULT_WEATHER_CODE;

        WeatherResultDTO dominantWeather = weatherList.stream()
                .filter(w -> dominantCode.equals(w.getWeatherCode()))
                .findFirst()
                .orElse(WeatherResultDTO.builder()
                        .weatherCode(DEFAULT_WEATHER_CODE)
                        .weatherIcon(DEFAULT_WEATHER_ICON)
                        .weatherName(DEFAULT_WEATHER_NAME)
                        .build());

        final double total = totalCount;
        Map<String, Double> weatherDistribution = weatherCounts.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Math.round(e.getValue() / total * 100.0) / 100.0,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        log.debug("区域天气聚合完成: total={}, dominant={}, distribution={}",
                totalCount, dominantCode, weatherDistribution);

        return AreaWeatherDTO.builder()
                .dominantWeather(dominantWeather)
                .postCount(totalCount)
                .weatherDistribution(weatherDistribution)
                .build();
    }

    private AreaWeatherDTO buildEmptyResult() {
        return AreaWeatherDTO.builder()
                .dominantWeather(WeatherResultDTO.builder()
                        .weatherCode(DEFAULT_WEATHER_CODE)
                        .weatherIcon(DEFAULT_WEATHER_ICON)
                        .weatherName(DEFAULT_WEATHER_NAME)
                        .build())
                .postCount(0)
                .weatherDistribution(Map.of())
                .build();
    }
}
