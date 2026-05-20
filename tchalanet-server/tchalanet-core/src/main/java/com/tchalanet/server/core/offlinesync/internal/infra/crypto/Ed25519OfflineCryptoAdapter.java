package com.tchalanet.server.core.offlinesync.internal.infra.crypto;

import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineCryptoPort;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

/**
 * Ed25519 implementation of {@link OfflineCryptoPort} using the JDK 17+ native provider.
 *
 * <p>Holds the server private key (used to sign offline grants) and exposes the server
 * public key. Stateless verification for device signatures.
 */
public class Ed25519OfflineCryptoAdapter implements OfflineCryptoPort {

    private static final String ALGORITHM = "Ed25519";
    private static final Logger LOG = LoggerFactory.getLogger(Ed25519OfflineCryptoAdapter.class);
    private static final Set<String> PROD_LIKE_PROFILES = Set.of("prod", "production", "staging");

    private final PrivateKey serverPrivateKey;
    private final String serverPublicKeyB64;

    public Ed25519OfflineCryptoAdapter(OfflineCryptoProperties properties, Environment environment) {
        try {
            KeyPair keyPair = loadOrGenerate(properties, environment);
            this.serverPrivateKey = keyPair.getPrivate();
            this.serverPublicKeyB64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to initialise offlinesync Ed25519 crypto", e);
        }
    }

    @Override
    public String signGrant(byte[] payload) {
        try {
            Signature sig = Signature.getInstance(ALGORITHM);
            sig.initSign(serverPrivateKey);
            sig.update(payload);
            return Base64.getEncoder().encodeToString(sig.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to sign offline grant payload", e);
        }
    }

    @Override
    public boolean verifySubmission(byte[] payload, String signatureB64, String devicePublicKeyB64) {
        if (payload == null || signatureB64 == null || devicePublicKeyB64 == null) return false;
        try {
            PublicKey devicePublicKey = decodePublicKey(devicePublicKeyB64);
            Signature sig = Signature.getInstance(ALGORITHM);
            sig.initVerify(devicePublicKey);
            sig.update(payload);
            return sig.verify(Base64.getDecoder().decode(signatureB64));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            LOG.debug("offlinesync submission signature verification failed", e);
            return false;
        }
    }

    @Override
    public String serverPublicKey() {
        return serverPublicKeyB64;
    }

    // ------------------------------------------------------------------------------------

    private static KeyPair loadOrGenerate(OfflineCryptoProperties props, Environment env)
        throws GeneralSecurityException {
        boolean hasPriv = !isBlank(props.serverPrivateKey());
        boolean hasPub = !isBlank(props.serverPublicKey());

        if (!hasPriv && !hasPub) {
            if (env != null) {
                for (String p : env.getActiveProfiles()) {
                    if (PROD_LIKE_PROFILES.contains(p.toLowerCase())) {
                        throw new IllegalStateException(
                            "Ephemeral Ed25519 key generation is forbidden in profile '" + p
                                + "'. Set tch.offlinesync.crypto.server-private-key and -public-key.");
                    }
                }
            }
            LOG.warn("tch.offlinesync.crypto.server-{private,public}-key not set — generating "
                + "an ephemeral Ed25519 key pair. Acceptable for dev only. POS devices will "
                + "see grants signed by a key that changes at every server restart.");
            return KeyPairGenerator.getInstance(ALGORITHM).generateKeyPair();
        }
        if (hasPriv ^ hasPub) {
            throw new IllegalStateException(
                "tch.offlinesync.crypto must set BOTH server-private-key and server-public-key, "
                + "or NEITHER (dev ephemeral). Partial config is unsafe.");
        }
        PrivateKey priv = decodePrivateKey(props.serverPrivateKey());
        PublicKey pub = decodePublicKey(props.serverPublicKey());
        return new KeyPair(pub, priv);
    }

    private static PrivateKey decodePrivateKey(String pkcs8Base64) throws GeneralSecurityException {
        byte[] pkcs8 = Base64.getDecoder().decode(pkcs8Base64);
        return KeyFactory.getInstance(ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
    }

    private static PublicKey decodePublicKey(String spkiBase64) throws GeneralSecurityException {
        byte[] spki = Base64.getDecoder().decode(spkiBase64);
        return KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(spki));
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
