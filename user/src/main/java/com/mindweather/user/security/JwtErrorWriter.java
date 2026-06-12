package com.mindweather.user.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindweather.user.common.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtErrorWriter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void write(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        Result<Void> result = Result.error(code, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
