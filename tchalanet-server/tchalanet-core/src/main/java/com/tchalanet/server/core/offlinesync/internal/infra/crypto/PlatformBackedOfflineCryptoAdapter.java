package com.tchalanet.server.core.offlinesync.internal.infra.crypto;

import com.tchalanet.server.common.crypto.Ed25519SignatureVerifier;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineCryptoPort;
import com.tchalanet.server.platform.keymanagement.api.BackendPublicKeyApi;
import com.tchalanet.server.platform.keymanagement.api.ServerSigningApi;
import com.tchalanet.server.platform.keymanagement.api.model.ServerSigningPurpose;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements {@link OfflineCryptoPort} by delegating server-side signing to
 * {@code platform.keymanagement}. Device-side verification is handled via the
 * shared {@link Ed25519SignatureVerifier} in {@code common.crypto}.
 */
@RequiredArgsConstructor
public class PlatformBackedOfflineCryptoAdapter implements OfflineCryptoPort {

    private static final Ed25519SignatureVerifier VERIFIER = new Ed25519SignatureVerifier();
    private static final Logger LOG = LoggerFactory.getLogger(PlatformBackedOfflineCryptoAdapter.class);

    private final ServerSigningApi serverSigningApi;
    private final BackendPublicKeyApi backendPublicKeyApi;

    @Override
    public String signGrant(byte[] payload) {
        return serverSigningApi.sign(ServerSigningPurpose.OFFLINE_GRANT, payload).signature();
    }

    @Override
    public boolean verifySubmission(byte[] payload, String signatureB64, String devicePublicKeyB64) {
        if (payload == null || signatureB64 == null || devicePublicKeyB64 == null) return false;
        try {
            var spki = Base64.getDecoder().decode(devicePublicKeyB64);
            var sig  = Base64.getDecoder().decode(signatureB64);
            return VERIFIER.verify(spki, payload, sig);
        } catch (IllegalArgumentException e) {
            LOG.debug("offlinesync submission signature verification failed: bad base64", e);
            return false;
        }
    }

    @Override
    public String serverPublicKey() {
        var keys = backendPublicKeyApi.listActivePublicKeys().keys();
        if (keys.isEmpty()) {
            throw new IllegalStateException("No active backend public keys available");
        }
        return keys.get(0).publicKey();
    }
}
