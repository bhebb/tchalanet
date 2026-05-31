package com.tchalanet.server.core.terminal.internal.infra.config;

import com.tchalanet.server.common.crypto.Ed25519SignatureVerifier;
import com.tchalanet.server.common.crypto.SignatureVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TerminalCryptoConfig {

    @Bean
    public SignatureVerifier terminalSignatureVerifier() {
        return new Ed25519SignatureVerifier();
    }
}
