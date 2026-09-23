package com.speakup.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

/**
 * Encapsulates caller identity for ownership enforcement.
 * Resolves to AUTHENTICATED (user ID from validated JWT) or GUEST (X-Guest-Id),
 * with authenticated identity always taking precedence.
 */
public class CallerContext {

    public enum CallerType {
        AUTHENTICATED,
        GUEST,
        ANONYMOUS
    }

    private final CallerType type;
    private final Long userId;
    private final String guestId;

    private CallerContext(CallerType type, Long userId, String guestId) {
        this.type = type;
        this.userId = userId;
        this.guestId = guestId;
    }

    public static CallerContext authenticated(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null for authenticated caller");
        }
        return new CallerContext(CallerType.AUTHENTICATED, userId, null);
    }

    public static CallerContext guest(String guestId) {
        String cleanGuestId = guestId != null ? guestId.trim() : null;
        if (cleanGuestId == null || cleanGuestId.isBlank()) {
            return new CallerContext(CallerType.ANONYMOUS, null, null);
        }
        return new CallerContext(CallerType.GUEST, null, cleanGuestId);
    }

    public static CallerContext anonymous() {
        return new CallerContext(CallerType.ANONYMOUS, null, null);
    }

    /**
     * Resolves CallerContext adhering to security precedence:
     * If valid authenticated principal is present -> AUTHENTICATED (ignoring guest header).
     * Else if X-Guest-Id header is present -> GUEST.
     * Else -> ANONYMOUS.
     */
    public static CallerContext resolve(UserPrincipal principal, String guestHeader) {
        if (principal != null && principal.getId() != null) {
            return CallerContext.authenticated(principal.getId());
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal p) {
            return CallerContext.authenticated(p.getId());
        }

        if (guestHeader != null && !guestHeader.isBlank()) {
            return CallerContext.guest(guestHeader);
        }

        return CallerContext.anonymous();
    }

    public boolean isAuthenticated() {
        return type == CallerType.AUTHENTICATED;
    }

    public boolean isGuest() {
        return type == CallerType.GUEST;
    }

    public boolean isAnonymous() {
        return type == CallerType.ANONYMOUS;
    }

    public CallerType getType() {
        return type;
    }

    public Long getUserId() {
        return userId;
    }

    public String getGuestId() {
        return guestId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CallerContext that = (CallerContext) o;
        return type == that.type && Objects.equals(userId, that.userId) && Objects.equals(guestId, that.guestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, userId, guestId);
    }

    @Override
    public String toString() {
        return "CallerContext{" +
                "type=" + type +
                ", userId=" + userId +
                ", guestId='" + guestId + '\'' +
                '}';
    }
}
