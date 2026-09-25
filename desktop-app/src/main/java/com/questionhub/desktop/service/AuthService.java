package com.questionhub.desktop.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class AuthService {
    // 与旧 Web 版保持一致，原口令可继续使用。
    private static final String PASSWORD_HASH = "a22e9ece2ab512a64dc27badaa811cb7905d3210a57bff20986fde4eb5c0d38e";

    public boolean login(String password) {
        if (password == null || password.trim().isEmpty()) return false;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String hash = HexFormat.of().formatHex(md.digest(password.trim().getBytes(StandardCharsets.UTF_8)));
            return PASSWORD_HASH.equals(hash);
        } catch (Exception e) {
            throw new IllegalStateException("无法校验访问口令", e);
        }
    }
}
