package com.mindweather.user.security;

import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class JWTManager {

    private static final long TOKEN_EXPIRE_MS = 24 * 60 * 60 * 1000L;

    public String generateToken(Long userId) {
        long expireTime = System.currentTimeMillis() + TOKEN_EXPIRE_MS;
        String payload = userId + ":" + expireTime;
        return "mock-token-" + Base64.getEncoder().encodeToString(payload.getBytes());
    }

    public boolean verifyToken(String token) {
        try {
            String decoded = decodePayload(token);
            long expireTime = Long.parseLong(decoded.split(":")[1]);
            return System.currentTimeMillis() < expireTime;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            String decoded = decodePayload(token);
            long expireTime = Long.parseLong(decoded.split(":")[1]);
            return System.currentTimeMillis() >= expireTime;
        } catch (Exception e) {
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        try {
            String decoded = decodePayload(token);
            return Long.parseLong(decoded.split(":")[0]);
        } catch (Exception e) {
            return null;
        }
    }

    private String decodePayload(String token) {
        String encoded = token.replace("mock-token-", "");
        return new String(Base64.getDecoder().decode(encoded));
    }
}
