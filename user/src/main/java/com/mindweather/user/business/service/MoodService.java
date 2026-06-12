package com.mindweather.user.business.service;

import com.mindweather.user.business.dto.PostTextRequest;
import com.mindweather.user.business.dto.PostTextResponse;

import java.util.List;
import java.util.Map;

public interface MoodService {

    PostTextResponse submitTextPost(Long userId, PostTextRequest request);

    List<Map<String, Object>> getMyPosts(Long userId, int page, int pageSize);

    void deletePost(Long userId, Long postId);

    String hashContent(String content);

    boolean isDuplicatePost(Long userId, String contentHash);
}
