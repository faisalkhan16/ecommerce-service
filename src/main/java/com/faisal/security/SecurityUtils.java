package com.faisal.security;

import com.faisal.dto.AuthUser;
import com.faisal.enums.Role;
import com.faisal.exception.BadRequestException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Objects;

public class SecurityUtils {

    private SecurityUtils() {}

    public static AuthUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BadRequestException("Unauthenticated request");
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof Jwt jwt) {
            Long userId = readLongClaim(jwt, "userId");
            String email = jwt.getSubject();
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null || roles.isEmpty()) {
                throw new BadRequestException("roles claim is missing or empty");
            }
            Role role = Role.valueOf(roles.get(0));

            return new AuthUser(userId, role, email);
        }

        if (principal instanceof AuthUser user) {
            return user;
        }

        throw new BadRequestException("Unsupported authentication principal: " + principal.getClass().getName());
    }

    private static Long readLongClaim(Jwt jwt, String claim) {
        Object raw = jwt.getClaims().get(claim);
        if (raw == null) {
            throw new BadRequestException("Missing claim: " + claim);
        }
        if (raw instanceof Number n) {
            return n.longValue();
        }
        if (raw instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ex) {
                throw new BadRequestException("Invalid numeric claim " + claim);
            }
        }
        throw new BadRequestException("Unsupported claim type for " + claim);
    }
}