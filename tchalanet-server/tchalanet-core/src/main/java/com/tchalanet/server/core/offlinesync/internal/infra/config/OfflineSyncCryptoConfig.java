package com.tchalanet.server.core.offlinesync.internal.infra.config;

import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineCryptoPort;
import com.tchalanet.server.core.offlinesync.internal.infra.crypto.PlatformBackedOfflineCryptoAdapter;
import com.tchalanet.server.platform.keymanagement.api.BackendPublicKeyApi;
import com.tchalanet.server.platform.keymanagement.api.ServerSigningApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "tch.offlinesync", name = "enabled", havingValue = "true")
public class OfflineSyncCryptoConfig {

    @Bean
    public OfflineCryptoPort offlineCryptoPort(
        ServerSigningApi serverSigningApi,
        BackendPublicKeyApi backendPublicKeyApi
    ) {
        return new PlatformBackedOfflineCryptoAdapter(serverSigningApi, backendPublicKeyApi);
    }
}
