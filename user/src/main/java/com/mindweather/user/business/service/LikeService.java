package com.mindweather.user.business.service;

import java.util.Map;

public interface LikeService {

    /** 点赞/取消点赞，返回当前状态 */
    Map<String, Object> toggleLike(Long userId, Long postId);

    /** 获取点赞状态 */
    Map<String, Object> getLikeStatus(Long userId, Long postId);
}
