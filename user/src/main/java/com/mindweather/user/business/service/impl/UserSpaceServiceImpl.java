package com.mindweather.user.business.service.impl;

import com.mindweather.user.business.service.UserSpaceService;
import com.mindweather.user.entity.Post;
import com.mindweather.user.repository.PostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class UserSpaceServiceImpl implements UserSpaceService {

    private final PostRepository postRepository;

    public UserSpaceServiceImpl(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    public Map<String, Object> getTodayWeather(Long userId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<Post> todayPosts = postRepository.findByUserIdAndIsDeletedFalseAndCreatedAtAfter(userId, todayStart);

        Map<String, Object> result = new LinkedHashMap<>();

        if (todayPosts.isEmpty()) {
            result.put("icon", "⛅");
            result.put("name", "多云");
            result.put("code", "cloudy");
            result.put("summary", "今天还没有记录心情");
            return result;
        }

        // 取频率最高的天气
        Map<String, Long> weatherCounts = new HashMap<>();
        for (Post p : todayPosts) {
            weatherCounts.merge(p.getWeatherCode() != null ? p.getWeatherCode() : "cloudy", 1L, Long::sum);
        }
        String topWeather = weatherCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("cloudy");

        result.put("icon", getWeatherIcon(topWeather));
        result.put("name", getWeatherName(topWeather));
        result.put("code", topWeather);
        result.put("summary", "今天在 " + todayPosts.size() + " 个地点记录了心情");
        return result;
    }

    private String getWeatherIcon(String code) {
        return switch (code != null ? code : "") {
            case "sunny" -> "☀️"; case "cloudy" -> "⛅"; case "overcast" -> "☁️";
            case "rainy" -> "🌧️"; case "heavy_rain" -> "⛈️"; case "thunderstorm" -> "🌩️";
            default -> "⛅";
        };
    }

    private String getWeatherName(String code) {
        return switch (code != null ? code : "") {
            case "sunny" -> "晴"; case "cloudy" -> "多云"; case "overcast" -> "阴";
            case "rainy" -> "小雨"; case "heavy_rain" -> "暴雨"; case "thunderstorm" -> "雷暴";
            default -> "多云";
        };
    }

    @Override
    public Map<String, Object> getMyMapData(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Object[]> buildingData = postRepository.countByBuildingForUser(userId);
        Map<String, String> buildingWeathers = new LinkedHashMap<>();
        for (Object[] row : buildingData) {
            buildingWeathers.put((String) row[0], (String) row[1]);
        }
        result.put("buildingWeathers", buildingWeathers);
        return result;
    }

    @Override
    public List<Map<String, String>> getEmotionTrend(Long userId, String period) {
        return List.of();
    }

    @Override
    public Map<String, Object> getWeatherCalendar(Long userId, String month) {
        return Map.of("calendar", List.of());
    }

    @Override
    public Map<String, Integer> getAreaPostDistribution(Long userId) {
        List<Object[]> buildingData = postRepository.countByBuildingForUser(userId);
        Map<String, Integer> distribution = new LinkedHashMap<>();
        for (Object[] row : buildingData) {
            distribution.put((String) row[0], ((Number) row[2]).intValue());
        }
        return distribution;
    }
}
