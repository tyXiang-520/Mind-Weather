package com.mindweather.user.business.service;

import java.util.Map;

public interface StatsService {

    /** 全校天气分布 */
    Map<String, Object> getWeatherDistribution();

    /** 某分区天气分布 */
    Map<String, Object> getWeatherDistributionByZone(String zoneId);

    /** 今日投稿统计（全校） */
    Map<String, Object> getTodayStats();

    /** 用户个人统计 */
    Map<String, Object> getMyStats(Long userId);
}
