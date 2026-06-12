package com.mindweather.user.business.service;

import com.mindweather.user.business.dto.EmotionResultDTO;

public interface EmotionAnalysisService {

    EmotionResultDTO analyze(String text);

    boolean containsSensitiveContent(String text);
}
