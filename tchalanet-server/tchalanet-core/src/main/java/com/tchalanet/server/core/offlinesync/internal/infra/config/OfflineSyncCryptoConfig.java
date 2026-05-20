package com.tchalanet.server.core.offlinesync.internal.infra.config;

import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineCryptoPort;
import com.tchalanet.server.core.offlinesync.internal.infra.crypto.Ed25519OfflineCryptoAdapter;
import com.tchalanet.server.core.offlinesync.internal.infra.crypto.OfflineCryptoProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Wires the offlinesync Ed25519 crypto adapter when the module feature flag is on.
 */
@Configuration
@ConditionalOnProperty(prefix = "tch.offlinesync", name = "enabled", havingValue = "true")
public class OfflineSyncCryptoConfig {

    @Bean
    public OfflineCryptoPort offlineCryptoPort(
        OfflineCryptoProperties properties, Environment environment
    ) {
        return new Ed25519OfflineCryptoAdapter(properties, environment);
    }
}
