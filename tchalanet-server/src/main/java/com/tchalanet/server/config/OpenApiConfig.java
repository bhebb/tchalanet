package com.tchalanet.server.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(title = "Tchalanet API", version = "${app.version:dev}"),
    servers = {@Server(url = "${app.apiBaseUrl:http://localhost:8081}")},
    security = {@SecurityRequirement(name = "Keycloak")})
@SecurityScheme(
    name = "Keycloak",
    type = SecuritySchemeType.OAUTH2,
    flows =
        @io.swagger.v3.oas.annotations.security.OAuthFlows(
            authorizationCode =
                @io.swagger.v3.oas.annotations.security.OAuthFlow(
                    authorizationUrl = "${app.auth-url}/protocol/openid-connect/auth",
                    tokenUrl = "${app.auth-url}/protocol/openid-connect/token",
                    scopes = {
                      @io.swagger.v3.oas.annotations.security.OAuthScope(
                          name = "openid",
                          description = "OpenID"),
                      @io.swagger.v3.oas.annotations.security.OAuthScope(
                          name = "profile",
                          description = "Profile"),
                      @io.swagger.v3.oas.annotations.security.OAuthScope(
                          name = "email",
                          description = "Email")
                    })))
public class OpenApiConfig {}
