package com.tchalanet.server.resolver;

import com.tchalanet.server.properties.ApiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import static com.tchalanet.server.constants.AppConstants.TENANT_ID_CLAIMS;

@Component
@RequiredArgsConstructor
public class TenantResolver {

    private final ApiProperties properties;

    public String resolveTenant(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            Object details = auth.getPrincipal();
            if (details instanceof Jwt jwt) {
                // ⚠️ clé du claim selon config Keycloak (ex: tenant, tenant_id)
                String tenant = jwt.getClaimAsString(TENANT_ID_CLAIMS);
                if (tenant != null && !tenant.isBlank()) {
                    return tenant;
                }
            }
        }
        return properties.defaultTenant(); // fallback
    }
}
