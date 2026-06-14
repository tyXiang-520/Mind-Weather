package com.mindweather.user.business.controller;

import com.mindweather.user.business.service.CommentService;
import com.mindweather.user.common.ErrorCode;
import com.mindweather.user.common.Result;
import com.mindweather.user.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /** 获取某投稿的评论 */
    @GetMapping("/posts/{postId}/comments")
    public Result<Map<String, Object>> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(Map.of(
            "comments", commentService.getComments(postId, page, pageSize)
        ));
    }

    /** 发表评论 */
    @PostMapping("/posts/{postId}/comments")
    public Result<Map<String, Object>> addComment(
            @PathVariable Long postId,
            @RequestBody Map<String, String> body) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ErrorCode.UNAUTHORIZED);
        }
        Long commentId = commentService.addComment(userId, postId, body.get("content"));
        return Result.success(Map.of("commentId", commentId));
    }

    /** 删除评论 */
    @DeleteMapping("/comments/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ErrorCode.UNAUTHORIZED);
        }
        commentService.deleteComment(userId, commentId);
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
