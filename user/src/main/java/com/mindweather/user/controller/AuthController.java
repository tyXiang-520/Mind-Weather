package com.mindweather.user.controller;

import com.mindweather.user.common.Result;
import com.mindweather.user.dto.LoginRequest;
import com.mindweather.user.dto.RegisterRequest;
import com.mindweather.user.dto.UserInfoResponse;
import com.mindweather.user.entity.User;
import com.mindweather.user.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<UserInfoResponse> register(@RequestBody RegisterRequest request) {
        User user = authService.register(request.getEmail(), request.getPassword());
        String token = authService.login(request.getEmail(), request.getPassword());
        return Result.success(toResponse(user, token));
    }

    @PostMapping("/login")
    public Result<UserInfoResponse> login(@RequestBody LoginRequest request) {
        String token = authService.login(request.getEmail(), request.getPassword());
        User user = authService.findByEmail(request.getEmail());
        return Result.success(toResponse(user, token));
    }

    private UserInfoResponse toResponse(User user, String token) {
        UserInfoResponse response = new UserInfoResponse();
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setNickname(user.getNickname());
        response.setToken(token);
        return response;
    }
}
