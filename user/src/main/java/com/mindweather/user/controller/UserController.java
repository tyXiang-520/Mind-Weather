package com.mindweather.user.controller;

import com.mindweather.user.common.Result;
import com.mindweather.user.dto.ChangePasswordRequest;
import com.mindweather.user.entity.User;
import com.mindweather.user.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/profile")
    public Result<Map<String, Object>> profile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) auth.getPrincipal();
        User user = authService.findById(principal.getId());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getId());
        data.put("email", user.getEmail());
        data.put("nickname", user.getNickname());
        data.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
        return Result.success(data);
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) auth.getPrincipal();
        User user = authService.findById(principal.getId());
        authService.changePassword(user, request.getOldPassword(), request.getNewPassword());
        return Result.success(null);
    }
}
