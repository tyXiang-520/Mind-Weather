package com.mindweather.user.business.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindweather.user.business.dto.EmotionResultDTO;
import com.mindweather.user.business.service.EmotionAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek v4 Pro AI 情绪分析服务
 *
 * 通过 DeepSeek Chat Completion API 分析文本情绪，
 * 映射到 MindWeather 天气类型。
 *
 * API 文档: https://api-docs.deepseek.com/
 */
@Slf4j
@Primary
@Service
public class DeepSeekEmotionService implements EmotionAnalysisService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${mindweather.ai.api-key:your-api-key-here}")
    private String apiKey;

    @Value("${mindweather.ai.api-url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${mindweather.ai.model:deepseek-chat}")
    private String model;

    // 敏感词简单过滤列表
    private static final List<String> SENSITIVE_WORDS = List.of(
        // 可根据需要扩展
    );

    public DeepSeekEmotionService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public EmotionResultDTO analyze(String text) {
        // 先尝试调用 DeepSeek API，失败则 fallback 本地分析
        try {
            return analyzeWithAI(text);
        } catch (Exception e) {
            log.warn("DeepSeek API 调用失败，使用本地 fallback 分析: {}", e.getMessage());
            return analyzeLocally(text);
        }
    }

    /**
     * 调用 DeepSeek API 进行情绪分析
     */
    private EmotionResultDTO analyzeWithAI(String text) throws Exception {
        String systemPrompt = """
            你是一个情绪分析助手。分析用户输入的文本，返回 JSON 格式结果。
            不要包含 markdown 代码块标记，只返回纯 JSON。

            天气映射规则：
            - sunny（晴）：开心、兴奋、满足、幸福、充满希望
            - cloudy（多云）：平静、淡定、中性、悠闲
            - overcast（阴）：疲惫、困倦、无聊、迷茫
            - rainy（雨）：悲伤、难过、思念、emo
            - heavy_rain（暴雨）：焦虑、压力大、崩溃、抓狂
            - thunderstorm（雷暴）：愤怒、生气、嫉妒、烦躁

            返回格式：
            {
              "emotionType": "情绪类型",
              "weatherCode": "天气代码",
              "confidence": 0.0~1.0,
              "tags": ["标签1", "标签2"]
            }
            """;

        String userPrompt = "分析以下文本的情绪，返回 JSON：\n" + text;

        Map<String, Object> requestBody = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ),
            "temperature", 0.3,
            "max_tokens", 200
        );

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("DeepSeek API 返回 " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String content = root.path("choices").get(0).path("message").path("content").asText();

        // 清理可能的 markdown 包裹
        content = content.replaceAll("```json\\s*", "").replaceAll("```", "").trim();

        JsonNode result = objectMapper.readTree(content);

        EmotionResultDTO dto = new EmotionResultDTO();
        dto.setEmotionType(result.path("emotionType").asText("平静"));
        dto.setWeatherCode(result.path("weatherCode").asText("cloudy"));
        dto.setConfidence(result.path("confidence").asDouble(0.5));

        List<String> tags = new ArrayList<>();
        result.path("tags").forEach(tag -> tags.add(tag.asText()));
        dto.setTags(tags);

        log.info("DeepSeek 分析完成: emotion={}, weather={}, tags={}", dto.getEmotionType(), dto.getWeatherCode(), tags);
        return dto;
    }

    /**
     * 本地简单情绪分析（API 不可用时的 fallback）
     */
    private EmotionResultDTO analyzeLocally(String text) {
        EmotionResultDTO dto = new EmotionResultDTO();
        List<String> tags = new ArrayList<>();

        // 基于关键词的简单规则
        if (containsAny(text, "开心","快乐","高兴","太好","棒","喜欢","爱","幸福","阳光","美好")) {
            dto.setEmotionType("愉悦");
            dto.setWeatherCode("sunny");
            dto.setConfidence(0.7);
        } else if (containsAny(text, "累","困","疲惫","无聊","烦","躺平","摆烂","迷茫")) {
            dto.setEmotionType("疲惫");
            dto.setWeatherCode("overcast");
            dto.setConfidence(0.7);
        } else if (containsAny(text, "哭","难过","伤心","emo","抑郁","失落","思念")) {
            dto.setEmotionType("低落");
            dto.setWeatherCode("rainy");
            dto.setConfidence(0.7);
        } else if (containsAny(text, "焦虑","紧张","压力","ddl","考试","慌","怕","卷")) {
            dto.setEmotionType("焦虑");
            dto.setWeatherCode("heavy_rain");
            dto.setConfidence(0.7);
        } else if (containsAny(text, "气","怒","操","恶心","差","烂","坑")) {
            dto.setEmotionType("愤怒");
            dto.setWeatherCode("thunderstorm");
            dto.setConfidence(0.6);
        } else {
            dto.setEmotionType("平静");
            dto.setWeatherCode("cloudy");
            dto.setConfidence(0.5);
        }

        // 提取标签
        if (text.contains("考试") || text.contains("ddl")) tags.add("考试周");
        if (text.contains("食堂") || text.contains("吃")) tags.add("美食");
        if (text.contains("图书馆") || text.contains("自习")) tags.add("自习");
        if (text.contains("操场") || text.contains("运动") || text.contains("跑")) tags.add("运动");
        if (text.contains("宿舍")) tags.add("宿舍生活");
        if (text.contains("实习") || text.contains("工作")) tags.add("实习");
        if (text.contains("毕业")) tags.add("毕业季");
        dto.setTags(tags);

        return dto;
    }

    @Override
    public boolean containsSensitiveContent(String text) {
        String lower = text.toLowerCase();
        for (String word : SENSITIVE_WORDS) {
            if (lower.contains(word)) return true;
        }
        return false;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
