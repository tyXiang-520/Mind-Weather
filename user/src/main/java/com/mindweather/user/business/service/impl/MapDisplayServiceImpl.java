package com.mindweather.user.business.service.impl;

import com.mindweather.user.business.config.ZoneConfig;
import com.mindweather.user.business.service.MapDisplayService;
import com.mindweather.user.entity.Post;
import com.mindweather.user.repository.PostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MapDisplayServiceImpl implements MapDisplayService {

    private final PostRepository postRepository;
    private final ZoneConfig zoneConfig;

    // 天气对应的 emoji 和中文名
    private static final Map<String, String[]> WEATHER_META = Map.of(
        "sunny", new String[]{"☀️", "晴"},
        "cloudy", new String[]{"⛅", "多云"},
        "overcast", new String[]{"☁️", "阴"},
        "rainy", new String[]{"🌧️", "小雨"},
        "heavy_rain", new String[]{"⛈️", "暴雨"},
        "thunderstorm", new String[]{"🌩️", "雷暴"}
    );

    public MapDisplayServiceImpl(PostRepository postRepository, ZoneConfig zoneConfig) {
        this.postRepository = postRepository;
        this.zoneConfig = zoneConfig;
    }

    @Override
    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        // 全校主天气（基于各分区投稿加权）
        overview.put("campusMainWeather", getCampusMainWeather());
        overview.put("totalPostsToday", getTotalPostsToday());
        overview.put("hotTags", getHotTags(10));
        overview.put("emotionDistribution", getEmotionDistribution(todayStart));

        // 12 分区数据（含各分区真实天气）
        List<Object[]> zoneWeatherCounts = postRepository.countWeatherByZoneSince(todayStart);
        Map<String, Map<String, Long>> zoneWeatherMap = new HashMap<>();
        for (Object[] row : zoneWeatherCounts) {
            String zid = (String) row[0];
            String wcode = (String) row[1];
            long cnt = (Long) row[2];
            zoneWeatherMap.computeIfAbsent(zid, k -> new HashMap<>()).put(wcode, cnt);
        }

        List<Object[]> zonePostCounts = postRepository.countByZone();
        Map<String, Long> zonePostTotal = new HashMap<>();
        for (Object[] row : zonePostCounts) {
            zonePostTotal.put((String) row[0], (Long) row[1]);
        }

        List<Map<String, Object>> areaList = new ArrayList<>();
        for (ZoneConfig.ZoneInfo zone : zoneConfig.getAllZones()) {
            Map<String, Long> weatherCounts = zoneWeatherMap.getOrDefault(zone.id(), Map.of());
            String dominantWeather = getDominantWeather(weatherCounts);

            Map<String, Object> areaData = new LinkedHashMap<>();
            areaData.put("id", zone.id());
            areaData.put("name", zone.name());
            areaData.put("code", zone.id());
            areaData.put("buildingCount", zone.buildings().size());
            areaData.put("postCount", zonePostTotal.getOrDefault(zone.id(), 0L));
            areaData.put("weatherCode", dominantWeather);
            areaData.put("weatherIcon", getWeatherIcon(dominantWeather));
            areaData.put("weatherName", getWeatherName(dominantWeather));
            areaData.put("dominantEmotion", getDominantEmotion(zone.id(), todayStart));
            areaData.put("emotionDistribution", getZoneEmotionDistribution(zone.id(), todayStart));
            areaData.put("buildings", zone.buildings());
            areaList.add(areaData);
        }
        overview.put("areas", areaList);
        return overview;
    }

    @Override
    public Map<String, Object> getAreaList() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> areaList = new ArrayList<>();
        for (ZoneConfig.ZoneInfo zone : zoneConfig.getAllZones()) {
            Map<String, Object> areaData = new LinkedHashMap<>();
            areaData.put("id", zone.id());
            areaData.put("name", zone.name());
            areaData.put("code", zone.id());
            areaData.put("buildings", zone.buildings());
            areaList.add(areaData);
        }
        result.put("areas", areaList);
        return result;
    }

    @Override
    public Map<String, String> getCampusMainWeather() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<Object[]> counts = postRepository.countWeatherSince(todayStart);

        if (counts.isEmpty()) {
            return Map.of("weatherCode", "cloudy", "weatherIcon", "⛅", "weatherName", "多云");
        }

        // 找出现次数最多的天气
        String topWeather = counts.stream()
            .max(Comparator.comparingLong(r -> (Long) r[1]))
            .map(r -> (String) r[0])
            .orElse("cloudy");

        return Map.of(
            "weatherCode", topWeather,
            "weatherIcon", getWeatherIcon(topWeather),
            "weatherName", getWeatherName(topWeather)
        );
    }

    @Override
    public int getTotalPostsToday() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        return (int) postRepository.countTodayPosts(todayStart);
    }

    @Override
    public List<Map<String, Object>> getHotTags(int topN) {
        // 从最近一周的投稿中提取标签，按频率排序
        LocalDateTime weekAgo = LocalDate.now().minusDays(7).atStartOfDay();
        List<Post> recentPosts = postRepository.findByIsDeletedFalseAndCreatedAtAfterOrderByCreatedAtDesc(weekAgo);

        Map<String, Long> tagCounts = new HashMap<>();
        for (Post post : recentPosts) {
            if (post.getTags() != null && !post.getTags().isBlank()) {
                for (String tag : post.getTags().split(",")) {
                    tag = tag.trim();
                    if (!tag.isEmpty()) {
                        tagCounts.merge(tag, 1L, Long::sum);
                    }
                }
            }
        }

        return tagCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(topN)
            .map(e -> Map.<String, Object>of("name", e.getKey(), "count", e.getValue()))
            .collect(Collectors.toList());
    }

    // ═══════════ 辅助方法 ═══════════

    private Map<String, Object> getEmotionDistribution(LocalDateTime since) {
        List<Object[]> emotionCounts = postRepository.countEmotionSince(since);
        Map<String, Object> dist = new LinkedHashMap<>();
        long total = emotionCounts.stream().mapToLong(r -> (Long) r[1]).sum();

        for (Object[] row : emotionCounts) {
            String emotion = (String) row[0];
            long count = (Long) row[1];
            dist.put(emotion, Map.of("count", count, "percent", total > 0 ? Math.round(count * 100.0 / total) : 0));
        }
        return dist;
    }

    private Map<String, Object> getZoneEmotionDistribution(String zoneId, LocalDateTime since) {
        List<Post> zonePosts = postRepository.findByZoneIdAndIsDeletedFalseAndCreatedAtAfter(zoneId, since);
        Map<String, Long> counts = new HashMap<>();
        for (Post p : zonePosts) {
            counts.merge(p.getEmotionType() != null ? p.getEmotionType() : "未知", 1L, Long::sum);
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        Map<String, Object> dist = new LinkedHashMap<>();
        counts.forEach((emotion, count) ->
            dist.put(emotion, Map.of("count", count, "percent", total > 0 ? Math.round(count * 100.0 / total) : 0))
        );
        return dist;
    }

    private String getDominantWeather(Map<String, Long> counts) {
        if (counts.isEmpty()) return "cloudy";
        return counts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("cloudy");
    }

    private String getDominantEmotion(String zoneId, LocalDateTime since) {
        List<Post> zonePosts = postRepository.findByZoneIdAndIsDeletedFalseAndCreatedAtAfter(zoneId, since);
        Map<String, Long> emotionCounts = new HashMap<>();
        for (Post p : zonePosts) {
            emotionCounts.merge(p.getEmotionType() != null ? p.getEmotionType() : "平静", 1L, Long::sum);
        }
        return emotionCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("平静");
    }

    private String getWeatherIcon(String code) {
        String[] meta = WEATHER_META.get(code);
        return meta != null ? meta[0] : "⛅";
    }

    private String getWeatherName(String code) {
        String[] meta = WEATHER_META.get(code);
        return meta != null ? meta[1] : "多云";
    }
}
