package com.mindweather.user.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostTextRequest {

    @NotBlank(message = "投稿内容不能为空")
    @Size(max = 500, message = "投稿内容不能超过500字")
    private String content;

    /** 建筑名（来自 GLB 模型），如 "北大楼"、"图书馆" */
    @NotBlank(message = "建筑名称不能为空")
    private String buildingName;

    /** 分区 ID，A-L */
    @NotBlank(message = "分区不能为空")
    private String zoneId;
}
