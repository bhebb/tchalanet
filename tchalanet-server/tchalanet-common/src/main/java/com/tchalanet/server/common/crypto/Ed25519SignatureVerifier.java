package com.tchalanet.server.common.crypto;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public final class Ed25519SignatureVerifier implements SignatureVerifier {

    @Override
    public boolean verify(byte[] spkiPublicKey, byte[] canonicalPayload, byte[] signature) {
        try {
            var keyFactory = KeyFactory.getInstance("Ed25519");
            var publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(spkiPublicKey));
            var sig = java.security.Signature.getInstance("Ed25519");
            sig.initVerify(publicKey);
            sig.update(canonicalPayload);
            return sig.verify(signature);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | SignatureException e) {
            return false;
        }
    }
}
