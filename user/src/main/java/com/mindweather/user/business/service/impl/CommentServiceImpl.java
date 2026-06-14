package com.mindweather.user.business.service.impl;

import com.mindweather.user.business.service.CommentService;
import com.mindweather.user.entity.Comment;
import com.mindweather.user.repository.CommentRepository;
import com.mindweather.user.repository.PostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public CommentServiceImpl(CommentRepository commentRepository, PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }

    @Override
    public List<Map<String, Object>> getComments(Long postId, int page, int pageSize) {
        List<Comment> comments = commentRepository.findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(
                postId, PageRequest.of(page - 1, pageSize));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Comment c : comments) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("postId", c.getPostId());
            item.put("userId", c.getUserId());
            item.put("content", c.getContent());
            item.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");
            result.add(item);
        }
        return result;
    }

    @Override
    public Long addComment(Long userId, Long postId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("评论内容不能为空");
        }
        if (content.length() > 200) {
            throw new IllegalArgumentException("评论内容不能超过200字");
        }
        // 验证帖子存在
        postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("投稿不存在"));

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content.trim());
        comment.setCreatedAt(LocalDateTime.now());
        comment.setIsDeleted(false);
        comment = commentRepository.save(comment);

        log.info("评论成功: commentId={}, postId={}, userId={}", comment.getId(), postId, userId);
        return comment.getId();
    }

    @Override
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("评论不存在"));
        if (!comment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除他人的评论");
        }
        comment.setIsDeleted(true);
        commentRepository.save(comment);
        log.info("评论已软删除: commentId={}, userId={}", commentId, userId);
    }
}
