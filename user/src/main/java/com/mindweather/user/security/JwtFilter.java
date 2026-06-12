package com.mindweather.user.security;

import com.mindweather.user.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JWTManager jwtManager;
    private final JwtErrorWriter errorWriter;

    public JwtFilter(JWTManager jwtManager, JwtErrorWriter errorWriter) {
        this.jwtManager = jwtManager;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        Long userId = jwtManager.getUserIdFromToken(token);
        if (userId == null) {
            errorWriter.write(response, 2005, "Token无效");
            return;
        }

        if (jwtManager.isTokenExpired(token)) {
            errorWriter.write(response, 2004, "Token已过期");
            return;
        }

        // 用轻量 User 对象作 principal
        User principal = new User();
        principal.setId(userId);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
