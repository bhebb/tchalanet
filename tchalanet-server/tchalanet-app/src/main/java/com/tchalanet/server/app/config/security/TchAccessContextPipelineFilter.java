package com.tchalanet.server.app.config.security;

import com.tchalanet.server.platform.accesscontrol.api.AccessResolutionStep;
import com.tchalanet.server.platform.identity.api.IdentityBootstrapStep;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TchAccessContextPipelineFilter extends OncePerRequestFilter {

    private final IdentityBootstrapStep identityBootstrapStep;
    private final AccessResolutionStep accessResolutionStep;


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.startsWith("/public/")
            || path.startsWith("/api/v1/public/")
            || path.startsWith("/actuator/health")
            || path.startsWith("/api/v1/actuator/health")
            || path.startsWith("/swagger-ui")
            || path.startsWith("/api/v1/swagger-ui")
            || path.startsWith("/v3/api-docs")
            || path.startsWith("/openapi")
            || path.startsWith("/api/v1/openapi")
            || path.equals("/error")
            || path.equals("/api/v1/error");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        identityBootstrapStep.bootstrap(request);
        accessResolutionStep.resolve(request);
        filterChain.doFilter(request, response);
    }
}
