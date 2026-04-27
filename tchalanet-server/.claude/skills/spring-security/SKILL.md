---
name: spring-security
description: Use when writing security, authentication, authorization, JWT, OAuth2, Keycloak, roles, scopes, or endpoint protection code in tchalanet-server — covers @PreAuthorize, SecurityConfig, JwtDecoder, resource server, TchContextFilter, and token validation.
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

# Spring Security — Tchalanet

> Auth : Spring Security OAuth2 Resource Server · JWT · Keycloak 26.0.7

## Architecture auth

```
Client (Angular/Flutter)
  → Keycloak (auth + émission JWT)
  → tchalanet-server (valide JWT via Resource Server)
      → TchContextFilter (résout scope, tenant, rôles depuis JWT)
      → RLS PostgreSQL (isolation tenant)
```

## Configuration Resource Server

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/public/**").permitAll()
                .requestMatchers("/api/v1/_sdr/**").hasAuthority("SCOPE_internal")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter()))
            )
            .addFilterBefore(tchContextFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

## Scopes API — mapping avec les routes

| Scope      | Route prefix        | Qui y accède                        |
| ---------- | ------------------- | ----------------------------------- |
| `public`   | `/api/v1/public/`   | Anonyme                             |
| `tenant`   | `/api/v1/tenant/`   | Utilisateur authentifié d'un tenant |
| `admin`    | `/api/v1/admin/`    | TENANT_ADMIN ou SUPER_ADMIN         |
| `platform` | `/api/v1/platform/` | SUPER_ADMIN uniquement              |
| `_sdr`     | `/api/v1/_sdr/`     | Service-to-service interne          |

## Rôles Keycloak → Spring Security

```java
// Rôles extraits du JWT par TchContextFilter
// Disponibles via SecurityContextHolder ou TchContext

@PreAuthorize("hasRole('SUPER_ADMIN')")
@PreAuthorize("hasRole('TENANT_ADMIN')")
@PreAuthorize("hasRole('VENDOR')")
@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")

// ✅ Préférer @PreAuthorize sur les méthodes de service
// plutôt que les règles dans SecurityConfig (plus lisible)
```

## JWT — claims Tchalanet

```java
// Claims standards extraits par TchContextFilter
// "tenant_code"  → résolu en UUID via TenantBootstrapLookup
// "sub"          → user ID
// "realm_access.roles" → rôles Keycloak
// "scope"        → scopes OAuth2

// ❌ Ne jamais faire confiance au tenant_id fourni par le client
// ✅ Toujours résoudre depuis le JWT → TenantBootstrapLookup
```

## Tests de sécurité

```java
// ✅ Tests d'intégration avec @WithMockJwtAuth ou Testcontainers Keycloak
@SpringBootTest
@AutoConfigureMockMvc
class TicketSecurityTest {

    @Test
    void shouldReturn401WhenNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/tenant/tickets"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockJwtAuth(authorities = "ROLE_VENDOR")
    void shouldReturn403WhenWrongRole() throws Exception {
        mockMvc.perform(post("/api/v1/admin/draws"))
            .andExpect(status().isForbidden());
    }
}
```

## Checklist sécurité

- [ ] Endpoints `/public/` explicitement permis
- [ ] Endpoints `/_sdr/` protégés par scope `internal`
- [ ] Session stateless (`STATELESS`)
- [ ] CSRF désactivé (API REST stateless)
- [ ] `@PreAuthorize` sur les méthodes sensibles
- [ ] Jamais de tenant_id trusted depuis le client
- [ ] Tests d'intégration sécurité pour chaque rôle critique
- [ ] Aucun secret Keycloak dans le code — via Doppler/env vars
