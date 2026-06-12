package com.mindweather.user.business.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherResultDTO {

    private String weatherCode;
    private String weatherName;
    private String weatherIcon;
}
