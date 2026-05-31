package com.tchalanet.server.common.crypto;

public interface SignatureVerifier {

    boolean verify(byte[] spkiPublicKey, byte[] canonicalPayload, byte[] signature);
}
