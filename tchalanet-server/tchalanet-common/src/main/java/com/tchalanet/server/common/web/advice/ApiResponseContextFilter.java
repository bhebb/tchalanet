package com.tchalanet.server.common.web.advice;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/** Clears ApiResponseContext ThreadLocal at the end of each HTTP request. */
public class ApiResponseContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } finally {
            ApiResponseContext.clear();
        }
    }
}
