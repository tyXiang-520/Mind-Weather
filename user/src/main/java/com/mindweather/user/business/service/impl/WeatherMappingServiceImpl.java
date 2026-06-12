package com.mindweather.user.business.service.impl;

import com.mindweather.user.business.dto.WeatherResultDTO;
import com.mindweather.user.business.enums.WeatherTypeEnum;
import com.mindweather.user.business.service.WeatherMappingService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WeatherMappingServiceImpl implements WeatherMappingService {

    private static final WeatherTypeEnum DEFAULT_WEATHER = WeatherTypeEnum.CLOUDY;

    @Override
    public WeatherResultDTO mapEmotionToWeather(String emotionType) {
        if (emotionType == null || emotionType.isBlank()) {
            return DEFAULT_WEATHER.toWeatherResult();
        }

        return WeatherTypeEnum.fromEmotion(emotionType)
                .map(WeatherTypeEnum::toWeatherResult)
                .orElse(DEFAULT_WEATHER.toWeatherResult());
    }

    @Override
    public WeatherResultDTO getWeatherByCode(String weatherCode) {
        if (weatherCode == null || weatherCode.isBlank()) {
            return null;
        }

        return WeatherTypeEnum.fromCode(weatherCode)
                .map(WeatherTypeEnum::toWeatherResult)
                .orElse(null);
    }

    @Override
    public List<Map<String, String>> getAllMappingRules() {
        return WeatherTypeEnum.getEmotionMap()
                .entrySet()
                .stream()
                .map(entry -> {
                    Map<String, String> rule = new LinkedHashMap<>();
                    rule.put("emotionType", entry.getKey());
                    rule.put("weatherCode", entry.getValue().getCode());
                    rule.put("weatherIcon", entry.getValue().getIcon());
                    rule.put("weatherName", entry.getValue().getName());
                    return rule;
                })
                .collect(Collectors.toList());
    }

    @Override
    public WeatherResultDTO calculateDominantWeather(Map<String, Double> emotionDistribution) {
        if (emotionDistribution == null || emotionDistribution.isEmpty()) {
            return DEFAULT_WEATHER.toWeatherResult();
        }

        Optional<Map.Entry<String, Double>> dominantEntry = emotionDistribution.entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null)
                .max(Comparator.comparingDouble(Map.Entry::getValue));

        if (dominantEntry.isEmpty() || dominantEntry.get().getValue() <= 0.0) {
            return DEFAULT_WEATHER.toWeatherResult();
        }

        String dominantEmotion = dominantEntry.get().getKey();
        return mapEmotionToWeather(dominantEmotion);
    }
}
