package com.wingsup.warehouse.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {

    /*
     * token -> userId
     */
    private final Map<String, Long> tokens =
            new ConcurrentHashMap<>();

    /*
     * token -> sessionId
     */
    private final Map<String, String> sessions =
            new ConcurrentHashMap<>();

    /**
     * Tạo token mới cho user.
     */
    public String create(Long userId, String sessionId) {

        String token = UUID.randomUUID().toString();

        tokens.put(token, userId);
        sessions.put(token, sessionId);

        return token;
    }

    /**
     * Lấy userId từ token.
     */
    public Long get(String token) {

        return token == null
                ? null
                : tokens.get(token);
    }

    /**
     * Lấy sessionId của token.
     */
    public String getSessionId(String token) {

        return token == null
                ? null
                : sessions.get(token);
    }

    /**
     * Xóa token.
     */
    public void remove(String token) {

        if (token != null) {

            tokens.remove(token);
            sessions.remove(token);
        }
    }
}