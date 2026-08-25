package com.wcs.monitor.common;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {

    public record Session(Long userId, String username, String realName, String role) {
    }

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public String create(Long userId, String username, String realName, String role) {
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, new Session(userId, username, realName, role));
        return token;
    }

    public Session get(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return sessions.get(token);
    }

    public void remove(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }
}
