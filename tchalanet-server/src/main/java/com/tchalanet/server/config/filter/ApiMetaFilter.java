package com.tchalanet.server.config.filter;

import static com.tchalanet.server.constants.AppConstants.API_VERSION_HEADER;
import static com.tchalanet.server.constants.AppConstants.API_VERSION_V1;

import com.tchalanet.server.constants.AppConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiMetaFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {

    String apiVersion = req.getHeader(API_VERSION_HEADER); // ex: v1

    // Exemple de vérif: refuser si front annonce v2 sur endpoint v1
    if (apiVersion != null && !req.getRequestURI().startsWith(AppConstants.API_BASE_PATH_VERSION)) {
      res.sendError(HttpStatus.BAD_REQUEST.value(), "API version mismatch");
      return;
    }

    // Propager dans les logs (MDC), si tu utilises SLF4J
    // MDC.put("appVersion", appVersion);

    // Exposer la version servie en réponse (utile en debug)
    res.setHeader(API_VERSION_HEADER, API_VERSION_V1);
    chain.doFilter(req, res);
  }
}
