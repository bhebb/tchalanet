package com.tchalanet.server.core.offlinesync.internal.application.port.out;

/**
 * Outbound port for Ed25519 crypto operations used by the offlinesync module.
 *
 * <p>Two responsibilities:
 * <ul>
 *   <li><b>Sign grants server-side</b>: the server signs the grant payload so the POS
 *       device can validate authenticity offline before producing sales.</li>
 *   <li><b>Verify submissions device-side</b>: each offline sale is signed by the device
 *       private key; the server verifies with the {@code devicePublicKey} stored on the
 *       corresponding {@code OfflineGrant}.</li>
 * </ul>
 *
 * <p>All keys and signatures are passed as Base64-encoded strings (standard, no padding
 * variant). Public keys are X.509 {@code SubjectPublicKeyInfo} ; private keys are PKCS#8.
 */
public interface OfflineCryptoPort {

    /**
     * Sign {@code payload} with the server private key.
     *
     * @return Base64-encoded Ed25519 signature.
     */
    String signGrant(byte[] payload);

    /**
     * Verify that {@code signatureB64} is a valid Ed25519 signature of {@code payload}
     * produced by the holder of the private key matching {@code devicePublicKeyB64}.
     *
     * @return {@code true} iff verification succeeds; {@code false} on mismatch,
     *         malformed key, or malformed signature.
     */
    boolean verifySubmission(byte[] payload, String signatureB64, String devicePublicKeyB64);

    /**
     * @return the Base64-encoded server public key (X.509 SPKI) — to be distributed to
     *         POS devices so they can verify grants offline.
     */
    String serverPublicKey();
}
