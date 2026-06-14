package com.mindweather.user.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mood_daily_stats", indexes = {
    @Index(name = "idx_mood_stats_date", columnList = "stat_date"),
    @Index(name = "idx_mood_stats_zone", columnList = "zone_id"),
    @Index(name = "idx_mood_stats_date_zone", columnList = "stat_date, zone_id")
})
public class MoodDailyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "zone_id", nullable = false, length = 1)
    private String zoneId;

    @Column(name = "weather_code", nullable = false, length = 20)
    private String weatherCode;

    @Column(name = "post_count", nullable = false)
    private Integer postCount;

    @Column(name = "unique_users", nullable = false)
    private Integer uniqueUsers;
}
