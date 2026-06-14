package com.mindweather.user.business.controller;

import com.mindweather.user.business.service.LikeService;
import com.mindweather.user.common.ErrorCode;
import com.mindweather.user.common.Result;
import com.mindweather.user.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/posts")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    /** 点赞/取消点赞 */
    @PostMapping("/{postId}/like")
    public Result<Map<String, Object>> toggleLike(@PathVariable Long postId) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ErrorCode.UNAUTHORIZED);
        }
        return Result.success(likeService.toggleLike(userId, postId));
    }

    /** 获取点赞状态 */
    @GetMapping("/{postId}/like/status")
    public Result<Map<String, Object>> getLikeStatus(@PathVariable Long postId) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ErrorCode.UNAUTHORIZED);
        }
        return Result.success(likeService.getLikeStatus(userId, postId));
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
