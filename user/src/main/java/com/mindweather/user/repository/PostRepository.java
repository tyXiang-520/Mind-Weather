package com.mindweather.user.repository;

import com.mindweather.user.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /** 用户的投稿列表（按时间倒序） */
    List<Post> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** 某建筑的投稿列表 */
    List<Post> findByBuildingNameAndIsDeletedFalseOrderByCreatedAtDesc(String buildingName, Pageable pageable);

    /** 某分区的投稿列表 */
    List<Post> findByZoneIdAndIsDeletedFalseOrderByCreatedAtDesc(String zoneId, Pageable pageable);

    /** 今日投稿总数 */
    @Query("SELECT COUNT(p) FROM Post p WHERE p.isDeleted = false AND p.createdAt >= :since")
    long countTodayPosts(@Param("since") LocalDateTime since);

    /** 各分区投稿数 */
    @Query("SELECT p.zoneId, COUNT(p) FROM Post p WHERE p.isDeleted = false GROUP BY p.zoneId")
    List<Object[]> countByZone();

    /** 各建筑投稿数 */
    @Query("SELECT p.buildingName, p.weatherCode, COUNT(p) FROM Post p WHERE p.isDeleted = false AND p.userId = :userId GROUP BY p.buildingName, p.weatherCode")
    List<Object[]> countByBuildingForUser(@Param("userId") Long userId);

    /** 今天某用户在某建筑的投稿数（防刷） */
    @Query("SELECT COUNT(p) FROM Post p WHERE p.userId = :userId AND p.buildingName = :buildingName AND p.createdAt >= :since AND p.isDeleted = false")
    long countUserPostsTodayAtBuilding(@Param("userId") Long userId, @Param("buildingName") String buildingName, @Param("since") LocalDateTime since);

    /** 获取某时间之后的所有有效投稿（用于天气聚合） */
    List<Post> findByIsDeletedFalseAndCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime since);

    /** 某分区某时间之后的投稿 */
    List<Post> findByZoneIdAndIsDeletedFalseAndCreatedAtAfter(String zoneId, LocalDateTime since);

    /** 某用户某时间之后的投稿 */
    List<Post> findByUserIdAndIsDeletedFalseAndCreatedAtAfter(Long userId, LocalDateTime since);

    /** 统计各分区各天气的数量 */
    @Query("SELECT p.zoneId, p.weatherCode, COUNT(p) FROM Post p WHERE p.isDeleted = false AND p.createdAt >= :since GROUP BY p.zoneId, p.weatherCode")
    List<Object[]> countWeatherByZoneSince(@Param("since") LocalDateTime since);

    /** 统计各天气总数 */
    @Query("SELECT p.weatherCode, COUNT(p) FROM Post p WHERE p.isDeleted = false AND p.createdAt >= :since GROUP BY p.weatherCode")
    List<Object[]> countWeatherSince(@Param("since") LocalDateTime since);

    /** 统计各情绪总数 */
    @Query("SELECT p.emotionType, COUNT(p) FROM Post p WHERE p.isDeleted = false AND p.createdAt >= :since GROUP BY p.emotionType")
    List<Object[]> countEmotionSince(@Param("since") LocalDateTime since);
}
