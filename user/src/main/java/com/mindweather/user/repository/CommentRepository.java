package com.mindweather.user.repository;

import com.mindweather.user.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** 某帖子的评论列表（按时间正序） */
    List<Comment> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(Long postId, Pageable pageable);

    /** 某帖子的评论数 */
    long countByPostIdAndIsDeletedFalse(Long postId);

    /** 某用户的评论列表 */
    List<Comment> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** 批量统计帖子评论数 */
    @Query("SELECT c.postId, COUNT(c) FROM Comment c WHERE c.postId IN :postIds AND c.isDeleted = false GROUP BY c.postId")
    List<Object[]> countByPostIds(@Param("postIds") List<Long> postIds);

    /** 最近留言（带用户昵称、头像和投稿建筑） */
    @Query(value = "SELECT c.id, c.content, c.created_at, u.nickname, u.avatar, p.building_name " +
           "FROM comments c JOIN users u ON c.user_id = u.id JOIN posts p ON c.post_id = p.id " +
           "WHERE c.is_deleted = false AND p.is_deleted = false " +
           "ORDER BY c.created_at DESC " +
           "LIMIT :limit", nativeQuery = true)
    List<Object[]> findRecentCommentsWithUser(@Param("limit") int limit);
}
