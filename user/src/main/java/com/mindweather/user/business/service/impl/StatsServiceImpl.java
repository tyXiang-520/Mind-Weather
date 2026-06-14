package com.mindweather.user.business.service.impl;

import com.mindweather.user.business.config.ZoneConfig;
import com.mindweather.user.business.service.StatsService;
import com.mindweather.user.entity.Post;
import com.mindweather.user.repository.PostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class StatsServiceImpl implements StatsService {

    private final PostRepository postRepository;
    private final ZoneConfig zoneConfig;

    private static final Map<String, String[]> WEATHER_META = Map.of(
        "sunny", new String[]{"☀️", "晴"},
        "cloudy", new String[]{"⛅", "多云"},
        "overcast", new String[]{"☁️", "阴"},
        "rainy", new String[]{"🌧️", "小雨"},
        "heavy_rain", new String[]{"⛈️", "暴雨"},
        "thunderstorm", new String[]{"🌩️", "雷暴"},
        "snow", new String[]{"❄️", "雪"}
    );

    public StatsServiceImpl(PostRepository postRepository, ZoneConfig zoneConfig) {
        this.postRepository = postRepository;
        this.zoneConfig = zoneConfig;
    }

    @Override
    public Map<String, Object> getWeatherDistribution() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<Object[]> counts = postRepository.countWeatherSince(todayStart);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", LocalDate.now().toString());

        // 各天气计数
        Map<String, Object> weatherMap = new LinkedHashMap<>();
        long total = counts.stream().mapToLong(r -> (Long) r[1]).sum();
        for (Object[] row : counts) {
            String code = (String) row[0];
            long cnt = (Long) row[1];
            String[] meta = WEATHER_META.getOrDefault(code, new String[]{"⛅", "多云"});
            weatherMap.put(code, Map.of(
                "count", cnt,
                "icon", meta[0],
                "name", meta[1],
                "percent", total > 0 ? Math.round(cnt * 100.0 / total) : 0
            ));
        }
        result.put("weatherDistribution", weatherMap);
        result.put("totalPosts", total);
        return result;
    }

    @Override
    public Map<String, Object> getWeatherDistributionByZone(String zoneId) {
        if (zoneConfig.getZone(zoneId) == null) {
            throw new IllegalArgumentException("无效的分区: " + zoneId);
        }

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<Object[]> allCounts = postRepository.countWeatherByZoneSince(todayStart);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", LocalDate.now().toString());
        result.put("zoneId", zoneId);
        result.put("zoneName", zoneConfig.getZone(zoneId).name());

        // 筛选该分区
        Map<String, Long> zoneMap = new HashMap<>();
        for (Object[] row : allCounts) {
            if (zoneId.equals(row[0])) {
                zoneMap.put((String) row[1], (Long) row[2]);
            }
        }

        long zoneTotal = zoneMap.values().stream().mapToLong(Long::longValue).sum();
        Map<String, Object> weatherMap = new LinkedHashMap<>();
        for (Map.Entry<String, Long> e : zoneMap.entrySet()) {
            String[] meta = WEATHER_META.getOrDefault(e.getKey(), new String[]{"⛅", "多云"});
            weatherMap.put(e.getKey(), Map.of(
                "count", e.getValue(),
                "icon", meta[0],
                "name", meta[1],
                "percent", zoneTotal > 0 ? Math.round(e.getValue() * 100.0 / zoneTotal) : 0
            ));
        }
        result.put("weatherDistribution", weatherMap);
        result.put("totalPosts", zoneTotal);
        return result;
    }

    @Override
    public Map<String, Object> getTodayStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long totalPosts = postRepository.countTodayPosts(todayStart);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", LocalDate.now().toString());
        result.put("totalPosts", totalPosts);

        // 各分区投稿数
        List<Object[]> zoneCounts = postRepository.countByZone();
        Map<String, Long> zoneMap = new LinkedHashMap<>();
        for (Object[] row : zoneCounts) {
            zoneMap.put((String) row[0], (Long) row[1]);
        }
        result.put("zonePostCounts", zoneMap);

        // 热门标签
        List<Post> recentPosts = postRepository.findByIsDeletedFalseAndCreatedAtAfterOrderByCreatedAtDesc(
                LocalDate.now().minusDays(7).atStartOfDay());
        Map<String, Long> tagCounts = new HashMap<>();
        for (Post p : recentPosts) {
            if (p.getTags() != null && !p.getTags().isBlank()) {
                for (String tag : p.getTags().replaceAll("[\\[\\]\"]", "").split(",")) {
                    tag = tag.trim();
                    if (!tag.isEmpty()) {
                        tagCounts.merge(tag, 1L, Long::sum);
                    }
                }
            }
        }
        List<Map<String, Object>> hotTags = tagCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> Map.<String, Object>of("name", e.getKey(), "count", e.getValue()))
                .toList();
        result.put("hotTags", hotTags);

        return result;
    }

    @Override
    public Map<String, Object> getMyStats(Long userId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<Post> todayPosts = postRepository.findByUserIdAndIsDeletedFalseAndCreatedAtAfter(userId, todayStart);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todayPostCount", todayPosts.size());

        // 今日情绪分布
        Map<String, Long> emotionCounts = new HashMap<>();
        for (Post p : todayPosts) {
            emotionCounts.merge(p.getEmotionType() != null ? p.getEmotionType() : "平静", 1L, Long::sum);
        }
        result.put("todayEmotions", emotionCounts);

        // 常去的分区
        Map<String, Long> zoneCounts = new HashMap<>();
        for (Post p : todayPosts) {
            zoneCounts.merge(p.getZoneId(), 1L, Long::sum);
        }
        result.put("visitedZones", zoneCounts);

        // 各建筑投稿数
        List<Object[]> buildingData = postRepository.countByBuildingForUser(userId);
        Map<String, Long> buildingMap = new LinkedHashMap<>();
        for (Object[] row : buildingData) {
            buildingMap.put((String) row[0], ((Number) row[2]).longValue());
        }
        result.put("buildingPostCounts", buildingMap);

        return result;
    }
}
