package com.tchalanet.server.filter;

import com.tchalanet.server.services.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.Audited;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    //    private final JdbcTemplate jdbc; // permet d'exécuter du SQL simple sur la connexion en cours
    @Autowired
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            userService.upsertUserFromJwt(jwt);

            Object ent = jwt.getClaim("active_enterprise_id");
            if (ent != null) {
                // Pose le tenant pour la session DB actuelle (pool Hikari)
//                jdbc.update("select set_current_tenant(?::uuid)", ent.toString());
            }
        }
        chain.doFilter(request, response);
    }
}
