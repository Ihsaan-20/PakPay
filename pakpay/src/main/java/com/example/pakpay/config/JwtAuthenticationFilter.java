package com.example.pakpay.config;

import com.example.pakpay.repository.UserTokenRepository;
import com.example.pakpay.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final UserTokenRepository tokenRepository;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    // Dynamic list for bypassing security
    private final List<String> excludedPaths = List.of("/api/auth/**"); 

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, 
            @NonNull HttpServletResponse response, 
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        String servletPath = request.getServletPath();
        
        // 1. DYNAMIC BYPASS: Puri /api/auth chain ko azadi do
        if (excludedPaths.stream().anyMatch(path -> pathMatcher.match(path, servletPath))) {
            filterChain.doFilter(request, response);
            return;
        }
    	
    	try {
            final String authHeader = request.getHeader("Authorization");

            // 2. Authorization Header Check
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            final String jwt = authHeader.substring(7);
            final String mobileNumber = jwtService.extractMobileNumber(jwt);

            // 3. Authenticate user if not already done
            if (mobileNumber != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                boolean isTokenValidInDb = tokenRepository.findByAccessToken(jwt)
                        .map(t -> !t.isExpired() && !t.isRevoked())
                        .orElse(false);

                if (jwtService.isTokenValid(jwt, mobileNumber) && isTokenValidInDb) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            mobileNumber, 
                            null, 
                            new ArrayList<>() // Empty roles for now
                    );
                    
                    // Ye line zaroori hai details pass karne ke liye
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token Expired", "Session khatam ho gaya hai.");
        } catch (Exception e) {
            // Log the actual error to console
            System.err.println("JWT Filter Error: " + e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden", "Access Denied: " + e.getMessage());
        }
    }

    // Helper method to send JSON errors instead of empty 403
    private void sendErrorResponse(HttpServletResponse response, int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\": \"%s\", \"message\": \"%s\"}", error, message));
    }
}