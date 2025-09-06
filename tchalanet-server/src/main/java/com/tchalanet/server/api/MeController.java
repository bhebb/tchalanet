package com.tchalanet.server.api;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class MeController {

    @GetMapping("/me")
    public Map<String,Object> me(@AuthenticationPrincipal Jwt jwt) {
        // roles depuis realm_access
        List<String> roles = Optional.ofNullable(jwt.getClaimAsMap("realm_access"))
                .map(m -> (List<String>) m.getOrDefault("roles", List.of()))
                .orElse(List.of());

        String activeEnterpriseId = jwt.getClaimAsString("active_enterprise_id");
        //todo set tenant
//        Object tenants = jwt.getClaim("tenants"); // si JSON, spring la mappe déjà en type Map/List

        return Map.of(
                "sub", jwt.getSubject(),
                "roles", roles,
                "active_enterprise_id", activeEnterpriseId,
//                "tenants", tenants,
                "is_super_admin", roles.contains("SUPER_ADMIN")
        );
    }
}
