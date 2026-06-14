package com.mindweather.user.business.service.impl;

import com.mindweather.user.business.service.LikeService;
import com.mindweather.user.entity.Like;
import com.mindweather.user.repository.LikeRepository;
import com.mindweather.user.repository.PostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;

    public LikeServiceImpl(LikeRepository likeRepository, PostRepository postRepository) {
        this.likeRepository = likeRepository;
        this.postRepository = postRepository;
    }

    @Override
    public Map<String, Object> toggleLike(Long userId, Long postId) {
        // 验证帖子存在
        postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("投稿不存在"));

        Optional<Like> existing = likeRepository.findByPostIdAndUserId(postId, userId);
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            log.info("取消点赞: postId={}, userId={}", postId, userId);
            return Map.of("liked", false, "likeCount", likeRepository.countByPostId(postId));
        } else {
            Like like = new Like();
            like.setPostId(postId);
            like.setUserId(userId);
            like.setCreatedAt(LocalDateTime.now());
            likeRepository.save(like);
            log.info("点赞成功: postId={}, userId={}", postId, userId);
            return Map.of("liked", true, "likeCount", likeRepository.countByPostId(postId));
        }
    }

    @Override
    public Map<String, Object> getLikeStatus(Long userId, Long postId) {
        boolean liked = likeRepository.existsByPostIdAndUserId(postId, userId);
        long count = likeRepository.countByPostId(postId);
        return Map.of("liked", liked, "likeCount", count);
    }
}
