package com.tchalanet.server.platform.keymanagement.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "tch.keymanagement")
public record KeyManagementProperties(ServerSigning serverSigning) {

    public KeyManagementProperties {
        if (serverSigning == null) {
            serverSigning = new ServerSigning("server-signing-key-dev", "ED25519", null, null);
        }
    }

    public record ServerSigning(
        @DefaultValue("server-signing-key-dev") String activeKeyId,
        @DefaultValue("ED25519") String algorithm,
        String privateKeyPkcs8Base64,
        String publicKeySpkiBase64
    ) {}
}
