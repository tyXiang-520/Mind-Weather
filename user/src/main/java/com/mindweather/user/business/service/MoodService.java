package com.mindweather.user.business.service;

import com.mindweather.user.business.dto.PostTextRequest;
import com.mindweather.user.business.dto.PostTextResponse;

import java.util.List;
import java.util.Map;

public interface MoodService {

    PostTextResponse submitTextPost(Long userId, PostTextRequest request);

    List<Map<String, Object>> getMyPosts(Long userId, int page, int pageSize);

    void deletePost(Long userId, Long postId);

    List<Map<String, Object>> getBuildingPosts(String buildingName, int page, int pageSize);

    List<Map<String, Object>> getZonePosts(String zoneId, int page, int pageSize);

    String hashContent(String content);

    boolean isDuplicatePost(Long userId, String contentHash);
}
