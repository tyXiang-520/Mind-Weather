package com.mindweather.user.service;

import com.mindweather.user.common.BusinessException;
import com.mindweather.user.common.ErrorCode;
import com.mindweather.user.entity.User;
import com.mindweather.user.repository.UserRepository;
import com.mindweather.user.security.JWTManager;
import com.mindweather.user.utils.NicknameGenerator;
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

    public boolean validateToken(String token) {
        return jwtManager.verifyToken(token);
    }
}
