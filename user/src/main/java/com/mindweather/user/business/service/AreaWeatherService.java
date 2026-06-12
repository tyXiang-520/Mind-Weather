package com.mindweather.user.business.service;

import com.mindweather.user.business.dto.AreaWeatherDTO;
import com.mindweather.user.business.dto.WeatherResultDTO;

import java.util.List;

public interface AreaWeatherService {

    AreaWeatherDTO aggregate(List<WeatherResultDTO> weatherList);
}
