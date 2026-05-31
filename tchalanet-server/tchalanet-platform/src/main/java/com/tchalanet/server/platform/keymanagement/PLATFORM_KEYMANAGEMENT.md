# Platform Capability `platform.keymanagement` — Gestion des clés de signature serveur

> **Archetype** : Application Service Module.  
> **Fichier** : `tchalanet-platform/src/main/java/com/tchalanet/server/platform/keymanagement/PLATFORM_KEYMANAGEMENT.md`

---

## 1. Rôle

**Responsabilité principale**

Posséder et exposer les clés de signature cryptographique côté serveur (backend), de sorte que nul autre module ne gère de matériel clé privé directement.

**Ce que ce module fait**

- Signer des payloads canoniques avec la clé privée serveur (Ed25519) via `ServerSigningApi`.
- Exposer les clés publiques serveur actives via `BackendPublicKeyApi` + endpoint public `GET /public/security/backend-signing-keys`.
- Charger la clé depuis la configuration (`tch.keymanagement.server-signing.*`) au démarrage et échouer rapidement si absente.
- Permettre au POS de bootstrapper et de vérifier les grants offline sans contacter le serveur en runtime.

**Ce que ce module ne fait pas**

- Gérer les clés publiques des devices POS/mobile — c'est `core.terminal` (stocké dans `terminal_binding`).
- Vérifier les signatures device — c'est `OfflineCryptoPort.verifySubmission` dans `core.offlinesync`.
- Gérer des secrets non liés à la signature (tokens, credentials, secrets utilisateurs).
- HSM/KMS (V2+), rotation automatique de clé (V2+), audit de signing (à ajouter si requis par compliance).

---

## 2. Structure

```text
platform/keymanagement/
  api/
    ServerSigningApi.java          ← signer un payload canonique avec la clé privée serveur
    BackendPublicKeyApi.java       ← lister les clés publiques serveur actives
    model/
      ServerSigningPurpose.java    ← enum (OFFLINE_GRANT, …)
      ServerSignatureResult.java   ← (signature: String, algorithm: String, keyId: String)
      BackendPublicKeyView.java    ← (keyId, algorithm, publicKeyFormat, publicKey, validFrom, validUntil, status)
      BackendPublicKeySetView.java ← (activeKeyId, keys: List<BackendPublicKeyView>)
  internal/
    config/
      KeyManagementProperties.java ← @ConfigurationProperties (keys nullable; validation in service)
    service/
      Ed25519ServerSigningService.java ← implémente les deux interfaces, Java 25 native Ed25519
    web/
      BackendPublicKeysController.java ← GET /public/security/backend-signing-keys (no auth)
```

---

## 3. API publique (`api/`)

```java
public interface ServerSigningApi {
    ServerSignatureResult sign(ServerSigningPurpose purpose, byte[] canonicalPayload);
}

public interface BackendPublicKeyApi {
    BackendPublicKeySetView listActivePublicKeys();
}
```

`core.offlinesync` injecte uniquement `ServerSigningApi` — jamais `Ed25519ServerSigningService`.  
`BackendPublicKeyApi` est consommée uniquement par `BackendPublicKeysController` (interne) et peut être exposée à d'autres modules si nécessaire.

**Interdit dans api/** : JPA entities, classes internal, `@ConfigurationProperties`.

---

## 4. Configuration V1

```yaml
tch:
  keymanagement:
    server-signing:
      active-key-id: ${TCH_SERVER_SIGNING_KEY_ID:server-signing-key-2026-01}
      algorithm: ED25519
      private-key-pkcs8-base64: ${TCH_SERVER_SIGNING_ED25519_PRIVATE_KEY_PKCS8_BASE64}
      public-key-spki-base64: ${TCH_SERVER_SIGNING_ED25519_PUBLIC_KEY_SPKI_BASE64}
```

- La clé privée ne doit jamais apparaître dans les logs, les réponses API ou les exceptions.
- `Ed25519ServerSigningService` valide les clés au `@PostConstruct`. Si absentes **et profil non prod/staging** → génère une paire éphémère avec un log WARNING (comportement local-dev, miroir de l'ancien `Ed25519OfflineCryptoAdapter`). En prod/staging, l'absence de clé fait échouer le démarrage.
- Ne jamais committer de vraie clé. `application-local-ide.yaml` ne contient pas de clé — l'éphémère est généré en mémoire.

**Évolution prévue :**
```
V1: env/secret injecté (actuel)
V2: Vault / cloud secrets manager / KMS-backed signer
V3: HSM/KMS signing sans matériel clé dans la JVM
```

---

## 5. Endpoint public

```http
GET /public/security/backend-signing-keys
```

- Aucune authentification requise (les clés publiques ne sont pas secrètes).
- HTTPS obligatoire.
- Le POS bootstrap ces clés au démarrage et les cache par `keyId`.
- Si un grant arrive avec un `keyId` inconnu, le POS rafraîchit via cet endpoint avant de rejeter.

Réponse :
```json
{
  "activeKeyId": "server-signing-key-2026-01",
  "keys": [
    {
      "keyId": "server-signing-key-2026-01",
      "algorithm": "ED25519",
      "publicKeyFormat": "SPKI_BASE64",
      "publicKey": "...",
      "validFrom": "2026-05-30T00:00:00Z",
      "validUntil": null,
      "status": "ACTIVE"
    }
  ]
}
```

---

## 6. Transactions

Ce module ne démarre pas de transaction. `sign()` est une opération purement en mémoire (crypto + return). `listActivePublicKeys()` est aussi en mémoire (lecture de la config). Aucun accès DB en V1.

---

## 7. Dépendances

**Autorisé** :
- `common.*` (Hashing, types)

**Interdit** :
- `core.*`
- `features.*`
- `platform.<autre>.internal`

---

## 8. Impact sur les flows

`core.offlinesync.RequestOfflineGrantCommandHandler` signe le grant via `ServerSigningApi.sign(OFFLINE_GRANT, canonicalPayload)` — la réponse inclut `grantSignature` et `serverPublicKey` pour que le POS puisse vérifier offline.

Voir aussi `DOMAIN_OFFLINESYNC.md §6` et `DOMAIN_TERMINAL.md §Device Proof`.

---

## 9. Points d'attention

- **Pas de RLS** : ce module n'a pas de tables en V1, donc pas de filtre tenant.
- **Context non requis** : `sign()` n'a pas besoin de `TchRequestContext` (opération pure crypto).
- **Rotation manuelle en V1** : changer les env vars + redéployer. Le `keyId` dans les grants permet au POS de sélectionner la bonne clé même après rotation.
- **`serviceStartedAt` utilisé comme `validFrom`** pour les clés exposées — il se réinitialise au déploiement. Si une date stable est requise (compliance), configurer `active-key-id` avec une date encodée.
