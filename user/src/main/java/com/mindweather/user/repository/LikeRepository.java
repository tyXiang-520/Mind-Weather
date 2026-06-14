package com.mindweather.user.repository;

import com.mindweather.user.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    /** 查询用户是否已点赞某帖子 */
    Optional<Like> findByPostIdAndUserId(Long postId, Long userId);

    /** 某帖子的点赞数 */
    long countByPostId(Long postId);

    /** 某用户点赞的所有帖子 ID */
    List<Long> findPostIdsByUserId(Long userId);

    /** 批量统计帖子点赞数 */
    @Query("SELECT l.postId, COUNT(l) FROM Like l WHERE l.postId IN :postIds GROUP BY l.postId")
    List<Object[]> countByPostIds(@Param("postIds") List<Long> postIds);

    /** 检查用户是否点赞过某帖子 */
    boolean existsByPostIdAndUserId(Long postId, Long userId);

    /** 删除点赞（软删除由应用层处理） */
    void deleteByPostIdAndUserId(Long postId, Long userId);
}
