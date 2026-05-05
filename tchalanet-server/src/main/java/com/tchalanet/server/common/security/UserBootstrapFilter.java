package com.tchalanet.server.common.security;

import com.tchalanet.server.common.types.enums.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

import static com.tchalanet.server.common.constant.ContextKeys.BOOTSTRAPPED_APP_USER_ID;

@Component
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties(UserBootstrapProperties.class)
public class UserBootstrapFilter extends OncePerRequestFilter {

    private final JdbcTemplate jdbc;
    private final UserBootstrapProperties props;

    private static final String FIND_SQL =
        """
        select id, status
        from app_user
        where keycloak_sub = ?
          and deleted_at is null
        limit 1
        """;

    private static final String TOUCH_SQL =
        """
        update app_user
        set last_login_at = now(), updated_at = now()
        where id = ?
        """;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain)
        throws ServletException, IOException {

        if (!props.enabled()) {
            chain.doFilter(request, response);
            return;
        }

        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Jwt jwt)) {
            chain.doFilter(request, response);
            return;
        }

        var sub = parseSub(jwt);
        if (sub == null) {
            response.sendError(403, "Invalid JWT subject");
            return;
        }

        var user = findUser(sub);

        if (user == null) {
            response.sendError(403, "User not provisioned");
            return;
        }

        if (user.status() != UserStatus.ACTIVE) {
            response.sendError(403, "User not active");
            return;
        }

        if (props.updateLastLogin()) {
            jdbc.update(TOUCH_SQL, user.id());
        }

        request.setAttribute(BOOTSTRAPPED_APP_USER_ID, user.id());

        chain.doFilter(request, response);
    }

    private BootstrappedUser findUser(UUID sub) {
        var list =
            jdbc.query(
                FIND_SQL,
                (rs, i) -> new BootstrappedUser(
                    rs.getObject("id", UUID.class),
                    UserStatus.valueOf(rs.getString("status"))
                ),
                sub);

        return list.isEmpty() ? null : list.getFirst();
    }

    private UUID parseSub(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (Exception e) {
            log.warn("Invalid JWT sub");
            return null;
        }
    }

    private record BootstrappedUser(UUID id, UserStatus status) {}
}
