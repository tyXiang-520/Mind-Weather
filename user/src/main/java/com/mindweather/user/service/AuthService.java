package com.mindweather.user.service;

import com.mindweather.user.common.BusinessException;
import com.mindweather.user.common.ErrorCode;
import com.mindweather.user.entity.User;
import com.mindweather.user.repository.UserRepository;
import com.mindweather.user.security.JWTManager;
import com.mindweather.user.utils.NicknameGenerator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JWTManager jwtManager;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, JWTManager jwtManager, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtManager = jwtManager;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(NicknameGenerator.generateNickname());
        user.setCreatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    public String login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        return jwtManager.generateToken(user.getId());
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public User getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof User)) {
                return null;
            }
            User principal = (User) auth.getPrincipal();
            // JWT filter 只设了 id，需要从数据库加载完整数据
            return principal.getId() != null ? findById(principal.getId()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public void changePassword(User user, String oldPassword, String newPassword) {
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("原密码错误");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("新密码至少6位");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public boolean validateToken(String token) {
        return jwtManager.verifyToken(token);
    }
}
