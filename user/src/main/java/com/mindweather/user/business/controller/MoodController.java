package com.mindweather.user.business.controller;

import com.mindweather.user.business.dto.PostTextRequest;
import com.mindweather.user.business.dto.PostTextResponse;
import com.mindweather.user.business.service.MoodService;
import com.mindweather.user.common.ErrorCode;
import com.mindweather.user.common.Result;
import com.mindweather.user.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/posts")
public class MoodController {

    private final MoodService moodService;

    public MoodController(MoodService moodService) {
        this.moodService = moodService;
    }

    @PostMapping("/text")
    public Result<PostTextResponse> submitTextPost(@RequestBody PostTextRequest request) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ErrorCode.UNAUTHORIZED);
        }
        PostTextResponse response = moodService.submitTextPost(userId, request);
        return Result.success(response);
    }

    @GetMapping("/my")
    public Result<List<Map<String, Object>>> getMyPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ErrorCode.UNAUTHORIZED);
        }
        List<Map<String, Object>> posts = moodService.getMyPosts(userId, page, pageSize);
        return Result.success(posts);
    }

    @GetMapping("/building")
    public Result<List<Map<String, Object>>> getBuildingPosts(
            @RequestParam String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        List<Map<String, Object>> posts = moodService.getBuildingPosts(name, page, pageSize);
        return Result.success(posts);
    }

    @GetMapping("/zone")
    public Result<List<Map<String, Object>>> getZonePosts(
            @RequestParam String zoneId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        List<Map<String, Object>> posts = moodService.getZonePosts(zoneId, page, pageSize);
        return Result.success(posts);
    }

    @DeleteMapping("/{postId}")
    public Result<Void> deletePost(@PathVariable Long postId) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ErrorCode.UNAUTHORIZED);
        }
        moodService.deletePost(userId, postId);
        return Result.success(null);
    }

    private Long getUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof User)) {
                return null;
            }
            return ((User) auth.getPrincipal()).getId();
        } catch (Exception e) {
            return null;
        }
    }
}
