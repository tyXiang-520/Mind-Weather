package com.mindweather.user.controller;

import com.mindweather.user.common.Result;
import com.mindweather.user.dto.LoginRequest;
import com.mindweather.user.dto.RegisterRequest;
import com.mindweather.user.dto.UserInfoResponse;
import com.mindweather.user.entity.User;
import com.mindweather.user.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    @GetMapping("/profile")
    public Result<UserInfoResponse> profile() {
        User user = authService.getCurrentUser();
        if (user == null) {
            return Result.error(401, "未登录");
        }
        return Result.success(toResponse(user, null));
    }

    @PutMapping("/profile")
    public Result<UserInfoResponse> updateProfile(@RequestBody Map<String, Object> updates) {
        User user = authService.getCurrentUser();
        if (user == null) {
            return Result.error(401, "未登录");
        }
        if (updates.containsKey("nickname")) {
            user.setNickname((String) updates.get("nickname"));
        }
        if (updates.containsKey("avatar")) {
            user.setAvatar((String) updates.get("avatar"));
        }
        if (updates.containsKey("defaultAnonymous")) {
            user.setDefaultAnonymous((Boolean) updates.get("defaultAnonymous"));
        }
        user = authService.saveUser(user);
        return Result.success(toResponse(user, null));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> request) {
        User user = authService.getCurrentUser();
        if (user == null) {
            return Result.error(401, "未登录");
        }
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");
        if (oldPassword == null || newPassword == null) {
            return Result.error(400, "请提供旧密码和新密码");
        }
        authService.changePassword(user, oldPassword, newPassword);
        return Result.success(null);
    }

    private UserInfoResponse toResponse(User user, String token) {
        UserInfoResponse response = new UserInfoResponse();
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setDefaultAnonymous(user.getDefaultAnonymous());
        response.setToken(token);
        return response;
    }
}
