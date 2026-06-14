package com.mindweather.user.business.controller;

import com.mindweather.user.business.service.StatsService;
import com.mindweather.user.common.ErrorCode;
import com.mindweather.user.common.Result;
import com.mindweather.user.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/weather")
    public Result<Map<String, Object>> weatherDistribution(
            @RequestParam(required = false) String zoneId) {
        if (zoneId != null && !zoneId.isBlank()) {
            return Result.success(statsService.getWeatherDistributionByZone(zoneId));
        }
        return Result.success(statsService.getWeatherDistribution());
    }

    @GetMapping("/today")
    public Result<Map<String, Object>> todayStats() {
        return Result.success(statsService.getTodayStats());
    }

    @GetMapping("/my")
    public Result<Map<String, Object>> myStats() {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ErrorCode.UNAUTHORIZED);
        }
        return Result.success(statsService.getMyStats(userId));
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
