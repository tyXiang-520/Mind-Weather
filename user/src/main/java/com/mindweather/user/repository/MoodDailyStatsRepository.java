package com.mindweather.user.repository;

import com.mindweather.user.entity.MoodDailyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MoodDailyStatsRepository extends JpaRepository<MoodDailyStats, Long> {

    /** 查询某天某分区的统计 */
    Optional<MoodDailyStats> findByStatDateAndZoneId(LocalDate statDate, String zoneId);

    /** 查询某天所有分区的统计 */
    List<MoodDailyStats> findByStatDate(LocalDate statDate);

    /** 查询某分区某段时间的统计 */
    List<MoodDailyStats> findByZoneIdAndStatDateBetween(String zoneId, LocalDate startDate, LocalDate endDate);

    /** 查询某段时间所有分区的统计 */
    List<MoodDailyStats> findByStatDateBetween(LocalDate startDate, LocalDate endDate);

    /** 查询某天全校各天气的总投稿数 */
    @Query("SELECT m.weatherCode, SUM(m.postCount) FROM MoodDailyStats m WHERE m.statDate = :date GROUP BY m.weatherCode")
    List<Object[]> countWeatherByDate(@Param("date") LocalDate date);

    /** 查询某分区最近 N 天的主导天气 */
    @Query("SELECT m.statDate, m.weatherCode, m.postCount FROM MoodDailyStats m WHERE m.zoneId = :zoneId AND m.statDate >= :since ORDER BY m.statDate ASC")
    List<Object[]> findDailyWeatherTrend(@Param("zoneId") String zoneId, @Param("since") LocalDate since);
}
