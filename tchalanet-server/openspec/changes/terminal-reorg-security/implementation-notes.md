# Implementation notes for agents

## Do not introduce these mistakes

- Do not place business logic in controllers.
- Do not store clear OTP codes.
- Do not let controller hash binding credentials.
- Do not let sales/payout/offlinesync read terminal binding JPA repositories.
- Do not sign backend grants with the POS public key.
- Do not rely only on signature validation; still validate terminal, outlet, session, seller, business rules.
- Do not skip nonce/timestamp anti-replay.

## Suggested names

```text
TerminalDeviceProofVerifier
TerminalSignaturePayloadCanonicalizer
TerminalNonceReplayGuard
TerminalPublicKeyHasher
TerminalBindingCredentialHasher
ServerSigningApi
SignedOfflineGrantView
OfflineGrantCanonicalizer
```

## Algorithms

Recommended V1 signing algorithm: `Ed25519`.

Store algorithm with the public key:

```text
public_key_algorithm = ED25519
public_key = base64 DER/SPKI or PEM
public_key_hash = sha256(canonical public key bytes)
```

## Flutter client abstractions

```dart
abstract class TerminalKeyStore {
  Future<TerminalKeyPairInfo> createOrLoadBindingKey();
  Future<String> signPayload(String canonicalPayload);
  Future<String> getPublicKey();
  Future<String> getPublicKeyAlgorithm();
}

abstract class BackendPublicKeyStore {
  Future<BackendPublicKey?> findByKeyId(String keyId);
  Future<void> refreshKeys();
}
```

Start with a simple implementation if needed, but keep the abstraction ready for Android Keystore.
