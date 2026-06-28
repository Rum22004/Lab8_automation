package edu.itc.cloud.web;

import edu.itc.cloud.model.User;
import edu.itc.cloud.service.AccessDeniedException;
import edu.itc.cloud.service.UserService;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Minimal in-memory bearer-token store for the starter. Good enough to drive
 * per-user isolation in tests; swap for Spring Security + JWT in production.
 */
@Component
public class AuthService {

    private final UserService users;
    private final ConcurrentMap<String, Long> tokenToUserId = new ConcurrentHashMap<>();

    public AuthService(UserService users) {
        this.users = users;
    }

    /** Issue a fresh token for a user id. */
    public String issueToken(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenToUserId.put(token, userId);
        return token;
    }

    public void revoke(String token) {
        if (token != null) {
            tokenToUserId.remove(token);
        }
    }

    /** Resolve the user from an {@code Authorization: Bearer <token>} header. */
    public User currentUser(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        Long userId = tokenToUserId.get(token);
        if (userId == null) {
            throw new AccessDeniedException("Missing or invalid token");
        }
        return users.require(userId);
    }

    public String extractToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException("Missing or invalid token");
        }
        return authorizationHeader.substring("Bearer ".length()).trim();
    }
}
