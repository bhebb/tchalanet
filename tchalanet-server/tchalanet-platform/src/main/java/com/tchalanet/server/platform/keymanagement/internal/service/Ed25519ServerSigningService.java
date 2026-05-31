package com.tchalanet.server.platform.keymanagement.internal.service;

import com.tchalanet.server.platform.keymanagement.api.BackendPublicKeyApi;
import com.tchalanet.server.platform.keymanagement.api.ServerSigningApi;
import com.tchalanet.server.platform.keymanagement.api.model.BackendPublicKeySetView;
import com.tchalanet.server.platform.keymanagement.api.model.BackendPublicKeyView;
import com.tchalanet.server.platform.keymanagement.api.model.ServerSignatureResult;
import com.tchalanet.server.platform.keymanagement.api.model.ServerSigningPurpose;
import com.tchalanet.server.platform.keymanagement.internal.config.KeyManagementProperties;
import jakarta.annotation.PostConstruct;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Ed25519ServerSigningService implements ServerSigningApi, BackendPublicKeyApi {

    private static final String ALGORITHM = "Ed25519";
    private static final String KEY_FORMAT = "SPKI_BASE64";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final Set<String> PROD_PROFILES = Set.of("prod", "production", "staging");
    private static final Logger LOG = LoggerFactory.getLogger(Ed25519ServerSigningService.class);

    private final KeyManagementProperties properties;
    private final Environment environment;

    private PrivateKey privateKey;
    private String activePublicKeySpki;
    private final Instant serviceStartedAt = Instant.now();

    @PostConstruct
    void init() {
        var cfg = properties.serverSigning();
        boolean hasPriv = cfg.privateKeyPkcs8Base64() != null && !cfg.privateKeyPkcs8Base64().isBlank();
        boolean hasPub  = cfg.publicKeySpkiBase64()  != null && !cfg.publicKeySpkiBase64().isBlank();

        if (!hasPriv && !hasPub) {
            for (String profile : environment.getActiveProfiles()) {
                if (PROD_PROFILES.contains(profile.toLowerCase())) {
                    throw new IllegalStateException(
                        "Ephemeral key generation is forbidden in profile '" + profile
                        + "'. Set TCH_SERVER_SIGNING_ED25519_PRIVATE_KEY_PKCS8_BASE64 and TCH_SERVER_SIGNING_ED25519_PUBLIC_KEY_SPKI_BASE64.");
                }
            }
            LOG.warn("tch.keymanagement.server-signing keys not configured — generating an ephemeral " +
                     "Ed25519 key pair. Grants signed with this key will be unverifiable after restart. " +
                     "Acceptable for local dev only.");
            loadEphemeral();
            return;
        }

        if (hasPriv ^ hasPub) {
            throw new IllegalStateException(
                "tch.keymanagement.server-signing must set BOTH private-key-pkcs8-base64 and " +
                "public-key-spki-base64, or NEITHER (dev ephemeral). Partial config is unsafe.");
        }

        loadConfigured(cfg.privateKeyPkcs8Base64(), cfg.publicKeySpkiBase64());
    }

    private void loadConfigured(String privB64, String pubB64) {
        try {
            var kf = KeyFactory.getInstance(ALGORITHM);
            privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privB64)));
            // Validate public key is parseable
            kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(pubB64)));
            activePublicKeySpki = pubB64;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Failed to load backend signing key from config", e);
        }
    }

    private void loadEphemeral() {
        try {
            KeyPair kp = KeyPairGenerator.getInstance(ALGORITHM).generateKeyPair();
            privateKey = kp.getPrivate();
            activePublicKeySpki = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to generate ephemeral Ed25519 key pair", e);
        }
    }

    @Override
    public ServerSignatureResult sign(ServerSigningPurpose purpose, byte[] canonicalPayload) {
        try {
            var sig = java.security.Signature.getInstance(ALGORITHM);
            sig.initSign(privateKey);
            sig.update(canonicalPayload);
            var signatureBase64Url = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(sig.sign());
            return new ServerSignatureResult(
                signatureBase64Url,
                ALGORITHM,
                properties.serverSigning().activeKeyId()
            );
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new IllegalStateException("Signing failed", e);
        }
    }

    @Override
    public BackendPublicKeySetView listActivePublicKeys() {
        var cfg = properties.serverSigning();
        var key = new BackendPublicKeyView(
            cfg.activeKeyId(),
            cfg.algorithm(),
            KEY_FORMAT,
            activePublicKeySpki,
            serviceStartedAt,
            null,
            STATUS_ACTIVE
        );
        return new BackendPublicKeySetView(cfg.activeKeyId(), List.of(key));
    }
}
