package com.mindweather.user.business.service;

import java.util.List;
import java.util.Map;

public interface MapDisplayService {

    Map<String, Object> getOverview();

    Map<String, Object> getAreaList();

    Map<String, String> getCampusMainWeather();

    int getTotalPostsToday();

    List<Map<String, Object>> getHotTags(int topN);
}
