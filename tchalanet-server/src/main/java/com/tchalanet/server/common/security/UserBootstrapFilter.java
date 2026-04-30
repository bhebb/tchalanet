package com.tchalanet.server.common.security;

import static com.tchalanet.server.common.constant.ContextKeys.BOOTSTRAPPED_APP_USER_ID;

import com.tchalanet.server.common.types.enums.UserStatus;
import com.tchalanet.server.common.types.id.IdGenerator;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
@EnableConfigurationProperties(UserBootstrapFilter.UserBootstrapProperties.class)
public class UserBootstrapFilter extends OncePerRequestFilter {

    private static final String UPSERT_SQL =
        """
        INSERT INTO app_user (
          id,
          keycloak_sub,
          username,
          email,
          display_name,
          status,
          last_login_at,
          created_at,
          updated_at
        )
        VALUES (?, ?, ?, ?, ?, ?, now(), now(), now())
        ON CONFLICT (keycloak_sub) WHERE deleted_at IS NULL DO UPDATE SET
          username = COALESCE(EXCLUDED.username, app_user.username),
          email = COALESCE(EXCLUDED.email, app_user.email),
          display_name = COALESCE(EXCLUDED.display_name, app_user.display_name),
          last_login_at = now(),
          updated_at = now()
        WHERE app_user.deleted_at IS NULL
        RETURNING id, status
        """;

    private final JdbcTemplate jdbcTemplate;
    private final IdGenerator idGenerator;
    private final UserBootstrapProperties properties;

    public UserBootstrapFilter(
        @Qualifier("rawDataSource") DataSource rawDataSource,
        IdGenerator idGenerator,
        UserBootstrapProperties properties) {
        this.jdbcTemplate = new JdbcTemplate(rawDataSource);
        this.idGenerator = idGenerator;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
        @Nonnull HttpServletRequest request,
        @Nonnull HttpServletResponse response,
        @Nonnull FilterChain filterChain)
        throws ServletException, IOException {

        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (!isJwtAuthentication(auth)) {
            filterChain.doFilter(request, response);
            return;
        }

        var jwt = (Jwt) auth.getPrincipal();
        var keycloakSub = parseSubject(jwt);

        if (keycloakSub == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid authenticated subject");
            return;
        }

        var user = upsertUser(jwt, keycloakSub);

        if (user == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "User bootstrap failed");
            return;
        }

        if (user.status() != UserStatus.ACTIVE) {
            log.info("User bootstrap denied: appUserId={} status={}", user.id(), user.status());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "User is not active");
            return;
        }

        request.setAttribute(BOOTSTRAPPED_APP_USER_ID, user.id());
        filterChain.doFilter(request, response);
    }

    private boolean isJwtAuthentication(Authentication auth) {
        return auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Jwt;
    }

    private UUID parseSubject(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT subject is not a UUID");
            return null;
        }
    }

    private BootstrappedUser upsertUser(Jwt jwt, UUID keycloakSub) {
        var username =
            firstNonBlank(jwt.getClaimAsString("preferred_username"), jwt.getClaimAsString("username"));

        var email = blankToNull(jwt.getClaimAsString("email"));
        var displayName = firstNonBlank(jwt.getClaimAsString("name"), username, email);

        var id = idGenerator.newUuid();
        var defaultStatus = properties.defaultStatus();

        var users =
            jdbcTemplate.query(
                UPSERT_SQL,
                (rs, rowNum) -> mapBootstrappedUser(rs),
                id,
                keycloakSub,
                username,
                email,
                displayName,
                defaultStatus.name());

        return users.isEmpty() ? null : users.getFirst();
    }

    private BootstrappedUser mapBootstrappedUser(ResultSet rs) throws SQLException {
        return new BootstrappedUser(
            rs.getObject("id", UUID.class),
            UserStatus.valueOf(rs.getString("status")));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            var normalized = blankToNull(value);
            if (normalized != null) return normalized;
        }
        return null;
    }

    private String blankToNull(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    private record BootstrappedUser(UUID id, UserStatus status) {}

    @ConfigurationProperties(prefix = "app.security.user-bootstrap")
    public record UserBootstrapProperties(UserStatus defaultStatus) {
        public UserBootstrapProperties {
            if (defaultStatus == null) {
                defaultStatus = UserStatus.ACTIVE;
            }
        }
    }
}
