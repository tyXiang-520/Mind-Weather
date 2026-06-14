package com.mindweather.user.business.enums;

import com.mindweather.user.business.dto.WeatherResultDTO;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public enum WeatherTypeEnum {

    SUNNY("sunny", "☀️", "晴天"),
    CLOUDY("cloudy", "⛅", "多云"),
    OVERCAST("overcast", "☁️", "阴天"),
    RAINY("rainy", "🌧️", "雨天"),
    HEAVY_RAIN("heavy_rain", "⛈️", "暴雨"),
    THUNDERSTORM("thunderstorm", "🌩️", "雷暴");

    private final String code;
    private final String icon;
    private final String name;

    private static final Map<String, WeatherTypeEnum> EMOTION_MAP = Map.of(
            "愉悦", SUNNY,
            "开心", SUNNY,
            "平静", CLOUDY,
            "疲惫", OVERCAST,
            "压力", OVERCAST,
            "低落", RAINY,
            "悲伤", RAINY,
            "焦虑", HEAVY_RAIN,
            "崩溃", THUNDERSTORM,
            "愤怒", THUNDERSTORM
    );

    WeatherTypeEnum(String code, String icon, String name) {
        this.code = code;
        this.icon = icon;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

    public static Optional<WeatherTypeEnum> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(type -> type.code.equalsIgnoreCase(code))
                .findFirst();
    }

    public static Optional<WeatherTypeEnum> fromEmotion(String emotionType) {
        if (emotionType == null || emotionType.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(EMOTION_MAP.get(emotionType));
    }

    public static Map<String, WeatherTypeEnum> getEmotionMap() {
        return Map.copyOf(EMOTION_MAP);
    }

    public WeatherResultDTO toWeatherResult() {
        return WeatherResultDTO.builder()
                .weatherCode(this.code)
                .weatherIcon(this.icon)
                .weatherName(this.name)
                .build();
    }
}
