package com.mindweather.user.business.service;

import com.mindweather.user.business.dto.WeatherResultDTO;

import java.util.List;
import java.util.Map;

public interface WeatherMappingService {

    WeatherResultDTO mapEmotionToWeather(String emotionType);

    WeatherResultDTO getWeatherByCode(String weatherCode);

    List<Map<String, String>> getAllMappingRules();

    WeatherResultDTO calculateDominantWeather(Map<String, Double> emotionDistribution);
}
