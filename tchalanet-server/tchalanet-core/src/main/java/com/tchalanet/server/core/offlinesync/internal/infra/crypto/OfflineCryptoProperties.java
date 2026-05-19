package com.tchalanet.server.core.offlinesync.internal.infra.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for the offlinesync crypto module.
 *
 * <p>Two valid combinations at boot:
 * <ul>
 *   <li>Both keys set → use the operator-provided server key pair.</li>
 *   <li>Both keys absent → generate an ephemeral key pair (dev only, log a warning).</li>
 * </ul>
 * Any partial setup (one key only) is rejected at startup.
 *
 * @param serverPrivateKey Base64-encoded PKCS#8 Ed25519 private key.
 * @param serverPublicKey  Base64-encoded X.509 {@code SubjectPublicKeyInfo} Ed25519 public key.
 */
@ConfigurationProperties(prefix = "tch.offlinesync.crypto")
public record OfflineCryptoProperties(String serverPrivateKey, String serverPublicKey) {}
