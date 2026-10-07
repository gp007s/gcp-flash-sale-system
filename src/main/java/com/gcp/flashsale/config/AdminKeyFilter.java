package com.gcp.flashsale.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * The AWS project left the admin endpoints open. Our VMs have a public address,
 * so the admin and debug endpoints need the header X-Admin-Key.
 * If no key is configured (env ADMIN_API_KEY), those endpoints stay locked.
 */
@Component
public class AdminKeyFilter extends OncePerRequestFilter {

    private final String adminKey;

    public AdminKeyFilter(@Value("${flashsale.admin-key:}") String adminKey) {
        this.adminKey = adminKey;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean protectedPath = path.startsWith("/api/admin")
                || path.startsWith("/gcs")
                || path.equals("/pubsub/send");
        return !protectedPath;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader("X-Admin-Key");
        if (adminKey.isBlank() || provided == null || !sameKey(provided)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/plain");
            response.getWriter().write("Forbidden");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean sameKey(String provided) {
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                adminKey.getBytes(StandardCharsets.UTF_8));
    }
}