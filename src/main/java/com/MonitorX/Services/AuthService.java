package com.MonitorX.Services;

import org.springframework.stereotype.Service;
import com.MonitorX.Repository.MonitoringRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private final MonitoringRepository repository;
    private final Map<String, String> activeSessions = new ConcurrentHashMap<>();

    public AuthService(MonitoringRepository repository) {
        this.repository = repository;
    }

    public Optional<String> login(String username, String password) {
        Optional<String> dbHash = repository.getOperatorPasswordHash(username);
        if (dbHash.isEmpty()) {
            return Optional.empty();
        }

        String inputHash = hashPassword(password);
        if (dbHash.get().equals(inputHash)) {
            String token = UUID.randomUUID().toString();
            activeSessions.put(token, username);
            return Optional.of(token);
        }

        return Optional.empty();
    }

    public void logout(String token) {
        activeSessions.remove(token);
    }

    public boolean isValidSession(String token) {
        return activeSessions.containsKey(token);
    }

    public Optional<String> getUsernameForToken(String token) {
        return Optional.ofNullable(activeSessions.get(token));
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
