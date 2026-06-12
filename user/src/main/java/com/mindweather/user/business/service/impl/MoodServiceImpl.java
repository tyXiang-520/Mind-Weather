package com.mindweather.user.business.service.impl;

import com.mindweather.user.business.config.ZoneConfig;
import com.mindweather.user.business.dto.EmotionResultDTO;
import com.mindweather.user.business.dto.PostTextRequest;
import com.mindweather.user.business.dto.PostTextResponse;
import com.mindweather.user.business.dto.WeatherResultDTO;
import com.mindweather.user.business.service.EmotionAnalysisService;
import com.mindweather.user.business.service.MoodService;
import com.mindweather.user.business.service.WeatherMappingService;
import com.mindweather.user.entity.Post;
import com.mindweather.user.repository.PostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
public class MoodServiceImpl implements MoodService {

    private final PostRepository postRepository;
    private final EmotionAnalysisService emotionAnalysisService;
    private final WeatherMappingService weatherMappingService;
    private final ZoneConfig zoneConfig;

    private static final int MAX_CONTENT_LENGTH = 500;

    public MoodServiceImpl(PostRepository postRepository,
                           EmotionAnalysisService emotionAnalysisService,
                           WeatherMappingService weatherMappingService,
                           ZoneConfig zoneConfig) {
        this.postRepository = postRepository;
        this.emotionAnalysisService = emotionAnalysisService;
        this.weatherMappingService = weatherMappingService;
        this.zoneConfig = zoneConfig;
    }

    @Override
    public PostTextResponse submitTextPost(Long userId, PostTextRequest request) {
        String content = sanitizeContent(request.getContent());
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("投稿内容不能为空");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("投稿内容不能超过" + MAX_CONTENT_LENGTH + "字");
        }

        // 验证建筑名
        String buildingName = request.getBuildingName();
        if (!zoneConfig.isValidBuilding(buildingName)) {
            throw new IllegalArgumentException("无效的建筑名称: " + buildingName);
        }

        String zoneId = zoneConfig.getZoneId(buildingName);

        // 敏感内容检测
        if (emotionAnalysisService.containsSensitiveContent(content)) {
            throw new IllegalArgumentException("内容包含敏感信息，请修改后重试");
        }

        // 防刷：同一用户同一建筑一天最多 3 条
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayCount = postRepository.countUserPostsTodayAtBuilding(userId, buildingName, todayStart);
        if (todayCount >= 3) {
            throw new IllegalArgumentException("今天已在这个建筑投稿" + todayCount + "次，明天再来吧~");
        }

        // AI 情绪分析
        EmotionResultDTO emotionResult = emotionAnalysisService.analyze(content);
        String emotionType = emotionResult.getEmotionType();
        String weatherCode = emotionResult.getWeatherCode();
        List<String> tags = emotionResult.getTags();

        // 如果 AI 没返回 weatherCode，用本地映射
        if (weatherCode == null) {
            WeatherResultDTO weather = weatherMappingService.mapEmotionToWeather(emotionType);
            weatherCode = weather.getWeatherCode();
        }

        // 存入数据库
        Post post = new Post();
        post.setUserId(userId);
        post.setContent(content);
        post.setBuildingName(buildingName);
        post.setZoneId(zoneId);
        post.setWeatherCode(weatherCode);
        post.setEmotionType(emotionType);
        post.setTags(tags != null ? String.join(",", tags) : "");
        post.setCreatedAt(LocalDateTime.now());
        post.setIsDeleted(false);

        post = postRepository.save(post);

        log.info("投稿成功: postId={}, userId={}, building={}, zone={}, weather={}",
                post.getId(), userId, buildingName, zoneId, weatherCode);

        WeatherResultDTO weather = weatherMappingService.mapEmotionToWeather(emotionType);
        PostTextResponse response = new PostTextResponse();
        response.setPostId(post.getId());
        response.setEmotionType(emotionType);
        response.setWeatherCode(weatherCode);
        response.setWeatherName(weather.getWeatherName());
        response.setWeatherIcon(weather.getWeatherIcon());
        response.setTags(tags);
        return response;
    }

    @Override
    public List<Map<String, Object>> getMyPosts(Long userId, int page, int pageSize) {
        List<Post> posts = postRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(
                userId, PageRequest.of(page - 1, pageSize));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Post post : posts) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("postId", post.getId());
            item.put("content", post.getContent());
            item.put("buildingName", post.getBuildingName());
            item.put("zoneId", post.getZoneId());
            item.put("emotionType", post.getEmotionType());
            item.put("weatherCode", post.getWeatherCode());
            item.put("weatherIcon", getWeatherIcon(post.getWeatherCode()));
            item.put("weatherName", getWeatherName(post.getWeatherCode()));
            item.put("tags", post.getTags() != null ? List.of(post.getTags().split(",")) : List.of());
            item.put("createdAt", post.getCreatedAt() != null ? post.getCreatedAt().toString() : "");
            result.add(item);
        }
        return result;
    }

    @Override
    public void deletePost(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("投稿不存在"));
        if (!post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除他人的投稿");
        }
        post.setIsDeleted(true);
        postRepository.save(post);
        log.info("投稿已软删除: postId={}, userId={}", postId, userId);
    }

    @Override
    public String hashContent(String content) {
        // 防刷改为数据库查询实现，不再需要 hash
        return Integer.toHexString(content.hashCode());
    }

    @Override
    public boolean isDuplicatePost(Long userId, String contentHash) {
        // 防刷逻辑已移到 submitTextPost 的数据库查询
        return false;
    }

    private String sanitizeContent(String raw) {
        if (raw == null) return null;
        return raw.trim().replaceAll("\\s+", " ");
    }

    private String getWeatherIcon(String code) {
        return switch (code != null ? code : "") {
            case "sunny" -> "☀️";
            case "cloudy" -> "⛅";
            case "overcast" -> "☁️";
            case "rainy" -> "🌧️";
            case "heavy_rain" -> "⛈️";
            case "thunderstorm" -> "🌩️";
            default -> "⛅";
        };
    }

    private String getWeatherName(String code) {
        return switch (code != null ? code : "") {
            case "sunny" -> "晴";
            case "cloudy" -> "多云";
            case "overcast" -> "阴";
            case "rainy" -> "小雨";
            case "heavy_rain" -> "暴雨";
            case "thunderstorm" -> "雷暴";
            default -> "多云";
        };
    }
}
