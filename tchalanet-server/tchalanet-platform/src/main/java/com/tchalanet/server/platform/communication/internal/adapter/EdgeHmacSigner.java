package com.tchalanet.server.platform.communication.internal.adapter;

import com.tchalanet.server.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;

/** Signs internal edge-service requests with HMAC-SHA256. */
@Component
@RequiredArgsConstructor
public class EdgeHmacSigner {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final JsonUtils jsonUtils;
    private final Clock clock;

    public SignedRequest sign(String secret, Object request) {
        try {
            var rawJsonBody = jsonUtils.toJson(request);
            var timestamp = clock.instant().toString();
            var payloadToSign = timestamp + "." + rawJsonBody;

            var mac = Mac.getInstance(HMAC_SHA256);
            var secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);

            var signatureBytes = mac.doFinal(payloadToSign.getBytes(StandardCharsets.UTF_8));
            var signature = "sha256=" + bytesToHex(signatureBytes);

            return new SignedRequest(timestamp, signature, rawJsonBody);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to sign edge request", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        var hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = Character.forDigit(v >>> 4, 16);
            hexChars[i * 2 + 1] = Character.forDigit(v & 0x0F, 16);
        }
        return new String(hexChars);
    }

    public record SignedRequest(
        String timestamp,
        String signature,
        String rawJsonBody
    ) {}
}
