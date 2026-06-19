package com.tchalanet.server.core.uslottery.internal.infra.external.oh.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tchalanet.server.core.uslottery.internal.infra.config.UsLotteryProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
@Slf4j
public class OhioAuthClient {

    private final RestClient ohAuthRestClient;
    private final UsLotteryProperties props;

    public OhioAuthClient(
        @Qualifier("ohAuthRestClient")
        RestClient rest,
        UsLotteryProperties props) {
        this.ohAuthRestClient = Objects.requireNonNull(rest);
        this.props = Objects.requireNonNull(props);
    }

    public Optional<String> login() {
        var cfg = props.getProviders() == null ? null : props.getProviders().get("oh");

        if (cfg == null
            || StringUtils.isBlank(cfg.getAuthPath())
            || StringUtils.isBlank(cfg.getAuthUsername())
            || StringUtils.isBlank(cfg.getAuthPassword())) {

            log.warn("oh-auth skipped reason=missing_auth_config");
            return Optional.empty();
        }

        try {
            var response = ohAuthRestClient.post()
                .uri(cfg.getAuthPath())
                .contentType(MediaType.valueOf("application/json-patch+json"))
                .body(Map.of(
                    "userName", cfg.getAuthUsername(),
                    "password", cfg.getAuthPassword()
                ))
                .retrieve()
                .body(OhioLoginResponse.class);

            var token = response == null ? null : response.token();

            if (StringUtils.isBlank(token)) {
                log.warn("oh-auth failed reason=empty_token");
                return Optional.empty();
            }

            return Optional.of(token.trim());

        } catch (Exception ex) {
            log.warn("oh-auth failed err={}", ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OhioLoginResponse(
        @JsonProperty("token") String token,
        @JsonProperty("accessToken") String accessToken,
        @JsonProperty("bearerToken") String bearerToken
    ) {
        public String token() {
            if (StringUtils.isNotBlank(token)) {
                return token;
            }
            if (StringUtils.isNotBlank(accessToken)) {
                return accessToken;
            }
            return bearerToken;
        }
    }
}
