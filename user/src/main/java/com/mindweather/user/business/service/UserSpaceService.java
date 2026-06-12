package com.mindweather.user.business.service;

import java.util.List;
import java.util.Map;

public interface UserSpaceService {

    Map<String, Object> getTodayWeather(Long userId);

    Map<String, Object> getMyMapData(Long userId);

    List<Map<String, String>> getEmotionTrend(Long userId, String period);

    Map<String, Object> getWeatherCalendar(Long userId, String month);

    Map<String, Integer> getAreaPostDistribution(Long userId);
}
