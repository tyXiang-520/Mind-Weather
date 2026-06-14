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
 * 多提供商 AI 情绪分析服务
 *
 * Fallback 链: deepseek → pollinations → local
 * 所有提供商使用 OpenAI 兼容格式
 */
@Slf4j
@Primary
@Service
public class DeepSeekEmotionService implements EmotionAnalysisService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${mindweather.ai.pollinations.enabled:true}")
    private boolean pollinationsEnabled;

    @Value("${mindweather.ai.pollinations.api-url:https://text.pollinations.ai/openai}")
    private String pollinationsApiUrl;

    @Value("${mindweather.ai.pollinations.model:openai}")
    private String pollinationsModel;

    @Value("${mindweather.ai.deepseek.enabled:true}")
    private boolean deepseekEnabled;

    @Value("${mindweather.ai.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${mindweather.ai.deepseek.api-url:https://api.deepseek.com/v1/chat/completions}")
    private String deepseekApiUrl;

    @Value("${mindweather.ai.deepseek.model:deepseek-chat}")
    private String deepseekModel;

    private static final List<String> SENSITIVE_WORDS = List.of();

    private static final String SYSTEM_PROMPT = """
        你是一个情绪分析助手。分析用户输入的文本情绪，只返回纯JSON，不要markdown代码块标记。

        情绪→天气映射（按优先级排序）：
        - 愤怒/烦躁/生气 → thunderstorm（雷暴）
        - 焦虑/压力大/崩溃/抓狂 → heavy_rain（暴雨）
        - 悲伤/难过/emo/失落 → rainy（雨）
        - 开心/兴奋/幸福/满足 → sunny（晴）
        - 疲惫/困倦/无聊/迷茫 → overcast（阴）
        - 平静/淡定/中性/悠闲 → cloudy（多云）

        返回JSON格式：
        {"emotionType":"情绪类型","weatherCode":"天气代码","confidence":0.0~1.0,"tags":["标签1","标签2"]}

        示例输入："今天好开心！" → {"emotionType":"愉悦","weatherCode":"sunny","confidence":0.9,"tags":["开心"]}
        示例输入："累死了想睡觉" → {"emotionType":"疲惫","weatherCode":"overcast","confidence":0.8,"tags":["疲惫"]}
        示例输入："要考试了好焦虑" → {"emotionType":"焦虑","weatherCode":"heavy_rain","confidence":0.8,"tags":["考试"]}
        示例输入："气死我了" → {"emotionType":"愤怒","weatherCode":"thunderstorm","confidence":0.8,"tags":["生气"]}
        示例输入："有点想家了" → {"emotionType":"低落","weatherCode":"rainy","confidence":0.7,"tags":["想家"]}
        """;

    public DeepSeekEmotionService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public EmotionResultDTO analyze(String text) {
        // Fallback 链: deepseek(效果好) → pollinations(免Key) → local
        if (deepseekEnabled && deepseekApiKey != null && !deepseekApiKey.isBlank()) {
            try {
                return callAI(text, deepseekApiUrl, deepseekApiKey, deepseekModel, "DeepSeek");
            } catch (Exception e) {
                log.warn("DeepSeek API 调用失败: {}", e.getMessage());
            }
        }

        if (pollinationsEnabled) {
            try {
                return callAI(text, pollinationsApiUrl, null, pollinationsModel, "Pollinations");
            } catch (Exception e) {
                log.warn("Pollinations API 调用失败: {}", e.getMessage());
            }
        }

        log.info("所有 AI API 不可用，使用本地 fallback 分析");
        return analyzeLocally(text);
    }

    private EmotionResultDTO callAI(String text, String apiUrl, String apiKey, String model, String provider) throws Exception {
        String userPrompt = "分析以下文本的情绪，返回 JSON：\n" + text;

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.3,
                "max_tokens", 200
        );

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        // Pollinations AI 不需要 API Key
        if (apiKey != null && !apiKey.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
        }

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException(provider + " API 返回 " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String content = root.path("choices").get(0).path("message").path("content").asText();

        content = content.replaceAll("```json\\s*", "").replaceAll("```", "").trim();

        JsonNode result = objectMapper.readTree(content);

        EmotionResultDTO dto = new EmotionResultDTO();
        dto.setEmotionType(result.path("emotionType").asText("平静"));
        dto.setWeatherCode(result.path("weatherCode").asText("cloudy"));
        dto.setConfidence(result.path("confidence").asDouble(0.5));

        List<String> tags = new ArrayList<>();
        result.path("tags").forEach(tag -> tags.add(tag.asText()));
        dto.setTags(tags);

        log.info("{} 分析完成: emotion={}, weather={}, tags={}", provider, dto.getEmotionType(), dto.getWeatherCode(), tags);
        return dto;
    }

    private EmotionResultDTO analyzeLocally(String text) {
        EmotionResultDTO dto = new EmotionResultDTO();
        List<String> tags = new ArrayList<>();

        // 优先检查强情绪（愤怒 > 崩溃 > 低落 > 愉悦 > 疲惫），最后才是平静
        if (containsAny(text, "气","怒","操","草","靠","妈的","恶心","差","烂","坑","服了","无语","烦死了","受不了","TMD","tmd","淦")) {
            dto.setEmotionType("愤怒");
            dto.setWeatherCode("thunderstorm");
            dto.setConfidence(0.7);
        } else if (containsAny(text, "焦虑","紧张","压力","ddl","考试","慌","怕","卷","截止","期限","要死","完了","崩溃","麻了","破防","救命","受不了")) {
            dto.setEmotionType("焦虑");
            dto.setWeatherCode("heavy_rain");
            dto.setConfidence(0.7);
        } else if (containsAny(text, "哭","难过","伤心","emo","抑郁","失落","思念","想家","孤独","泪","悲伤")) {
            dto.setEmotionType("低落");
            dto.setWeatherCode("rainy");
            dto.setConfidence(0.7);
        } else if (containsAny(text, "开心","快乐","高兴","太好","棒","喜欢","爱","幸福","阳光","美好","不错","充实","满足","享受","最美","赞","爽","好开心","哈哈","嘻嘻","嘿嘿","nice")) {
            dto.setEmotionType("愉悦");
            dto.setWeatherCode("sunny");
            dto.setConfidence(0.7);
        } else if (containsAny(text, "累","困","疲惫","无聊","烦","躺平","摆烂","迷茫","还行","一般","想睡觉","顶不住","熬","没意思","随便","要命")) {
            dto.setEmotionType("疲惫");
            dto.setWeatherCode("overcast");
            dto.setConfidence(0.7);
        } else {
            dto.setEmotionType("平静");
            dto.setWeatherCode("cloudy");
            dto.setConfidence(0.5);
        }

        if (text.contains("考试") || text.contains("ddl") || text.contains("截止")) tags.add("考试周");
        if (text.contains("食堂") || text.contains("吃") || text.contains("饭")) tags.add("美食");
        if (text.contains("图书馆") || text.contains("自习") || text.contains("学习")) tags.add("自习");
        if (text.contains("操场") || text.contains("运动") || text.contains("跑") || text.contains("健身")) tags.add("运动");
        if (text.contains("宿舍") || text.contains("寝室")) tags.add("宿舍生活");
        if (text.contains("实习") || text.contains("工作") || text.contains("面试")) tags.add("实习");
        if (text.contains("毕业") || text.contains("论文")) tags.add("毕业季");
        if (text.contains("课") || text.contains("老师") || text.contains("教授")) tags.add("课程");
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
