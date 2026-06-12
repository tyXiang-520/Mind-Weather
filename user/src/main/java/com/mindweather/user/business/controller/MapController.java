package com.mindweather.user.business.controller;

import com.mindweather.user.business.service.MapDisplayService;
import com.mindweather.user.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/map")
public class MapController {

    private final MapDisplayService mapDisplayService;

    public MapController(MapDisplayService mapDisplayService) {
        this.mapDisplayService = mapDisplayService;
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> data = mapDisplayService.getOverview();
        return Result.success(data);
    }

    @GetMapping("/areas")
    public Result<Map<String, Object>> areas() {
        Map<String, Object> data = mapDisplayService.getAreaList();
        return Result.success(data);
    }
}
