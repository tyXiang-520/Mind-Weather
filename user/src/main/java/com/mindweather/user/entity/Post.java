package com.mindweather.user.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "posts", indexes = {
    @Index(name = "idx_posts_user_id", columnList = "user_id"),
    @Index(name = "idx_posts_zone_id", columnList = "zone_id"),
    @Index(name = "idx_posts_building_name", columnList = "building_name"),
    @Index(name = "idx_posts_created_at", columnList = "created_at")
})
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 500)
    private String content;

    /** 建筑名（来自 GLB 模型），如 "北大楼"、"图书馆" */
    @Column(name = "building_name", nullable = false, length = 100)
    private String buildingName;

    /** 分区 ID（A-L）*/
    @Column(name = "zone_id", nullable = false, length = 1)
    private String zoneId;

    /** 情绪分析结果：sunny/cloudy/overcast/rainy/heavy_rain/thunderstorm */
    @Column(name = "weather_code", length = 20)
    private String weatherCode;

    /** 情绪标签：愉悦/平静/焦虑/低落/疲惫... */
    @Column(name = "emotion_type", length = 50)
    private String emotionType;

    /** 提取的关键词标签，JSON 数组字符串：["考试周","ddl"] */
    @Column(length = 1000)
    private String tags;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
    }
}
