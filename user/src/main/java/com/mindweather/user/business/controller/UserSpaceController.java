package com.mindweather.user.business.controller;

import com.mindweather.user.business.service.UserSpaceService;
import com.mindweather.user.common.ErrorCode;
import com.mindweather.user.common.Result;
import com.mindweather.user.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/my-weather")
public class UserSpaceController {

    private final UserSpaceService userSpaceService;

    public UserSpaceController(UserSpaceService userSpaceService) {
        this.userSpaceService = userSpaceService;
    }

    @GetMapping("/today")
    public Result<Map<String, Object>> today() {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ErrorCode.UNAUTHORIZED);
        }
        Map<String, Object> data = userSpaceService.getTodayWeather(userId);
        return Result.success(data);
    }

    @GetMapping("/map")
    public Result<Map<String, Object>> myMap() {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ErrorCode.UNAUTHORIZED);
        }
        Map<String, Object> data = userSpaceService.getMyMapData(userId);
        return Result.success(data);
    }

    @GetMapping("/trend")
    public Result<List<Map<String, String>>> trend(
            @RequestParam(defaultValue = "week") String period) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ErrorCode.UNAUTHORIZED);
        }
        List<Map<String, String>> data = userSpaceService.getEmotionTrend(userId, period);
        return Result.success(data);
    }

    @GetMapping("/calendar")
    public Result<Map<String, Object>> calendar(
            @RequestParam String month) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ErrorCode.UNAUTHORIZED);
        }
        Map<String, Object> data = userSpaceService.getWeatherCalendar(userId, month);
        return Result.success(data);
    }

    @GetMapping("/distribution")
    public Result<Map<String, Integer>> distribution() {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ErrorCode.UNAUTHORIZED);
        }
        Map<String, Integer> data = userSpaceService.getAreaPostDistribution(userId);
        return Result.success(data);
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
