package com.mindweather.user.business.service;

import java.util.List;
import java.util.Map;

public interface CommentService {

    /** 获取某投稿的评论列表 */
    List<Map<String, Object>> getComments(Long postId, int page, int pageSize);

    /** 发表评论，返回评论ID */
    Long addComment(Long userId, Long postId, String content);

    /** 删除评论 */
    void deleteComment(Long userId, Long commentId);
}
