# Android Emulator — Setup dev local (problèmes rencontrés et solutions)

Ce document explique les 4 problèmes techniques rencontrés lors du premier lancement de l'app Flutter sur émulateur Android, et pourquoi chaque solution est la bonne.

---

## Problème 1 — Build échoue : `flutter_appauth compiled against android-31`

### Ce qui s'est passé

Gradle (le système de build Android) a refusé de compiler l'app avec cette erreur :

```
flutter_appauth is currently compiled against android-31.
Dependency 'androidx.fragment:fragment:1.7.1' requires compileSdk ≥ 34.
```

### Pourquoi

Chaque plugin Flutter Android a son propre fichier de build qui déclare contre quelle version du SDK Android il est compilé. Le plugin `flutter_appauth` (v8) était configuré avec `compileSdkVersion 31` (Android 12), mais ses dépendances internes (fragments, lifecycle, etc.) exigent au minimum Android 14 (SDK 34).

Il y a deux couches :
- **Le projet principal** (`android/app/build.gradle.kts`) : contrôle l'app elle-même.
- **Les plugins** (ex: `flutter_appauth`) : ont leur propre `build.gradle` qu'on ne modifie pas directement.

### La solution

Dans `android/app/build.gradle.kts`, passer `compileSdk` à 36 :

```kotlin
android {
    compileSdk = 36
    ...
}
```

Et dans `android/build.gradle.kts` (le fichier racine), forcer tous les plugins à utiliser 36 via un hook `afterEvaluate` :

```kotlin
subprojects {
    afterEvaluate {
        extensions.findByType<com.android.build.gradle.BaseExtension>()?.apply {
            compileSdkVersion(36)
        }
    }
}
```

Le `afterEvaluate` s'exécute **après** que chaque plugin a chargé son propre `build.gradle`. Il écrase donc la valeur 31 hardcodée dans `flutter_appauth` sans modifier le plugin directement.

> **Important :** `compileSdk` contrôle uniquement quelles APIs Java/Kotlin sont disponibles pendant la compilation. Ça ne change pas la version Android minimale requise pour faire tourner l'app (`minSdk`) ni le comportement à l'exécution (`targetSdk`). C'est rétro-compatible.

---

## Problème 2 — `only https connections are permitted` (crash natif)

### Ce qui s'est passé

En cliquant sur "Se connecter", l'app crashait avec :

```
java.lang.IllegalArgumentException: only https connections are permitted
at net.openid.appauth.connectivity.DefaultConnectionBuilder.openConnection
```

### Pourquoi

`flutter_appauth` utilise la librairie Android officielle `AppAuth` (de Google/OpenID Foundation). Cette librairie implémente le protocole OAuth 2.0 / PKCE pour l'authentification sécurisée.

La première chose qu'elle fait au login, c'est télécharger le **document de découverte OpenID** (`/.well-known/openid-configuration`) depuis Keycloak. Ce document lui indique les endpoints d'auth, de token, etc.

AppAuth **refuse d'effectuer cette requête en HTTP**. Ce refus est codé en dur dans le code Java de la librairie, à la ligne :

```java
// DefaultConnectionBuilder.java:51
Preconditions.checkArgument(uri.getScheme().equals("https"),
    "only https connections are permitted");
```

Ce refus arrive **avant** même d'ouvrir une socket réseau. Le fichier `network_security_config.xml` (qui contrôle ce qu'Android autorise sur le réseau) n'a aucun effet ici — la vérification est dans du code Java, pas dans la couche réseau Android.

Notre config locale utilisait `http://10.0.2.2:8082` (Keycloak en HTTP), ce qui déclenchait ce refus systématiquement.

### La solution

Keycloak doit être atteint **en HTTPS**. Localement, c'est Traefik qui joue ce rôle : il reçoit les connexions HTTPS sur `auth.localtest.me` et les proxifie vers Keycloak en HTTP en interne.

On a donc besoin de tunneler le port HTTPS de Traefik vers l'émulateur (voir Problème 3).

---

## Problème 3 — `adb reverse tcp:443 tcp:443` : `Permission denied`

### Ce qui s'est passé

Pour que l'émulateur Android puisse atteindre Traefik sur le Mac, on utilise `adb reverse` — un tunnel qui redirige un port du device vers un port du Mac.

```
adb: error: cannot bind listener: Permission denied
```

### Pourquoi

`adb reverse` fonctionne en créant un listener sur le Mac. Or, **les ports en-dessous de 1024 sont "privilégiés"** sur Linux et macOS — seul `root` peut les utiliser. Le port 443 (HTTPS standard) est donc inaccessible à un process utilisateur normal, même pour `adb`.

### La solution

Ajouter un deuxième point d'entrée HTTPS dans Traefik sur le port **8443** (non-privilégié) :

**`traefik.yml`** — nouvel entrypoint :
```yaml
mobile-https:
  address: ":8443"
```

**`docker-compose-traefik.yml`** — exposer le port :
```yaml
ports:
  - "8443:8443"
```

**`10-routers.yaml`** — les routers Keycloak et API écoutent sur les deux entrypoints :
```yaml
keycloak:
  entryPoints: [ websecure, mobile-https ]
```

Ensuite, `adb reverse tcp:8443 tcp:8443` fonctionne sans root car 8443 > 1024.

```bash
# À exécuter une fois par session de dev, avant flutter run
adb reverse tcp:8443 tcp:8443
```

**Comment `adb reverse` fonctionne :**

```
Émulateur Android          Mac (hôte)
──────────────────         ──────────────────────────
auth.localtest.me
  → DNS → 127.0.0.1:8443
  → adb reverse tunnel  →  127.0.0.1:8443 (Traefik)
                                  ↓
                           Keycloak container
```

`auth.localtest.me` résout en `127.0.0.1` partout (c'est le principe de `localtest.me`). Avec le tunnel actif, `127.0.0.1:8443` sur le device est redirigé vers `127.0.0.1:8443` sur le Mac, où Traefik écoute.

---

## Problème 4 — `SSLHandshakeException: connection closed` (certificat non reconnu)

### Ce qui s'est passé

Après le tunnel en place, la connexion HTTPS s'établissait, mais échouait au moment de la négociation TLS :

```
javax.net.ssl.SSLHandshakeException: connection closed
Caused by: java.io.EOFException: connection closed
```

### Pourquoi

En local, Traefik utilise un certificat **généré par `mkcert`**. C'est un outil qui crée une **autorité de certification (CA) locale** sur ta machine et génère des certificats signés par cette CA.

Le problème : Android ne connaît pas cette CA. Son store de certificats de confiance contient uniquement des CAs publiques reconnues (Let's Encrypt, DigiCert, etc.). Quand il reçoit le certificat de Traefik signé par `mkcert`, il dit "je ne connais pas qui a signé ça" et coupe la connexion.

> Analogie : c'est comme si tu recevais un document officiel signé par un notaire dont tu n'as jamais entendu parler — tu refuses de le reconnaître comme valide.

Ce blocage est différent du précédent : la connexion TCP s'établit bien (le tunnel fonctionne), mais la couche TLS/SSL est rejetée.

### La solution

Dire à Android "fais confiance à cette CA spécifique pour les domaines localtest.me".

Android permet ça via `network_security_config.xml` — un fichier XML dans l'app qui étend la liste des CAs de confiance pour certains domaines.

**Étape 1** — Copier le certificat CA de mkcert dans les ressources Android :

```bash
cp "$(mkcert -CAROOT)/rootCA.pem" \
   android/app/src/main/res/raw/mkcert_ca.pem
```

**Étape 2** — Déclarer la confiance dans `network_security_config.xml` :

```xml
<domain-config>
    <domain includeSubdomains="true">localtest.me</domain>
    <trust-anchors>
        <certificates src="@raw/mkcert_ca"/>
        <certificates src="system"/>
    </trust-anchors>
</domain-config>
```

Cela dit à Android : "pour tout ce qui finit en `.localtest.me`, fais confiance à notre CA mkcert locale EN PLUS des CAs système habituelles."

> **Si tu changes de machine ou régénères mkcert** (`mkcert -install` sur un nouveau Mac), tu devras recopier `rootCA.pem` dans `res/raw/mkcert_ca.pem` et rebuilder l'app.

---

## Problème 5 — Keycloak « Client not found » / realm vide après import

### Ce qui s'est passé

Le login Keycloak affichait **« We are sorry... Client not found »**, ou l'API admin retournait `0 clients` alors que le realm `tchalanet-realm.json` en définit 4 (`tchalanet-web`, `tchalanet-api`, `tchalanet-mobile-pos`, `tchalanet-swagger`).

### Pourquoi

En dev, le realm est importé par un container one-shot `keycloak-init` **avant** que le serveur Keycloak principal démarre. Mais si le serveur principal a déjà démarré une première fois (cache Infinispan chargé), un ré-import ultérieur du realm dans la DB n'est **pas** vu par le serveur : son cache mémoire reste sur l'ancien état (vide ou périmé).

Dans les logs Keycloak on voyait alors :
```
type="LOGIN_ERROR", realmName="tchalanet", clientId="tchalanet-mobile-pos", error="client_not_found"
```

### La solution

Redémarrer le serveur Keycloak principal **après** l'import, pour qu'il recharge le realm depuis la DB :

```bash
docker restart tchl-keycloak-dev
# Attendre qu'il soit healthy, puis vérifier :
curl -sk https://auth.localtest.me:8443/realms/tchalanet/.well-known/openid-configuration -o /dev/null -w "%{http_code}\n"  # 200
```

> Voir aussi la note mémoire « keycloak-init cache staleness » : tout ré-import de realm exige un restart du serveur pour purger le cache.

---

## Problème 6 — Login OK mais la redirection ne revient pas dans l'app (« No stored state »)

### Ce qui s'est passé

Après avoir saisi les identifiants et cliqué « Connexion », le Custom Tab Keycloak ne se fermait pas et l'app ne reprenait jamais la main. Les logs répétaient :

```
W/AppAuth: No stored state - unable to handle response
```

### Pourquoi

`MainActivity` était déclarée avec `android:taskAffinity=""` (chaîne vide). Or l'`AuthorizationManagementActivity` d'AppAuth utilise `launchMode="singleTask"` avec l'**affinity par défaut** (= nom du package `com.tchalanet.mobile`).

Conséquence : ces deux activités vivaient dans **deux tâches Android différentes**. Quand AppAuth démarre le flow, il stocke l'état PKCE dans une activité de la tâche A. Au retour du redirect `com.tchalanet.mobile:/oauth2redirect`, Android recrée une `AuthorizationManagementActivity` fraîche dans la tâche B — sans l'état stocké → « No stored state ».

### La solution

Retirer `android:taskAffinity=""` de `MainActivity` dans `AndroidManifest.xml`. Toutes les activités partagent alors la même tâche (affinity = package), et le redirect revient là où l'état PKCE a été stocké.

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:launchMode="singleTop"
    <!-- ❌ supprimé : android:taskAffinity="" -->
    ... >
```

> C'est un changement de manifest (natif) : un **rebuild complet** est nécessaire (`flutter run`), le hot reload/restart ne suffit pas.

---

## Problème 7 — Les appels API ne partent jamais (Dio ne fait pas confiance au CA mkcert)

### Ce qui s'est passé

Après un login réussi, la home POS restait bloquée sur un spinner. Côté serveur, **aucune requête de l'app n'arrivait** (seuls les `/actuator/health` de Docker étaient visibles). Aucune erreur claire côté Dart.

### Pourquoi

C'est le piège le plus subtil. Le `network_security_config.xml` (Problème 4) fait confiance au CA mkcert **uniquement pour la pile réseau Android** (HttpsURLConnection, OkHttp, Chrome Custom Tabs). C'est pourquoi le **login** Keycloak fonctionnait : il passe par un Custom Tab.

Mais le client HTTP **Dio** utilise par défaut l'adapter `dart:io` (`IOHttpClientAdapter`), qui s'appuie sur la **pile TLS propre de Dart (BoringSSL)** — celle-ci charge les CAs système au démarrage et **ignore complètement** `network_security_config.xml`. Le CA mkcert lui est donc invisible.

Résultat : chaque appel HTTPS vers `https://api.localtest.me:8443` échouait au handshake TLS, **avant** même d'atteindre le backend.

```
Custom Tab (login)  → pile réseau Android → CA mkcert trusté    ✅
Dio (appels API)    → pile TLS Dart       → CA mkcert invisible ❌
```

### La solution

En **dev uniquement**, configurer l'adapter Dio pour accepter le cert mkcert sur `*.localtest.me`. Comme l'app tourne aussi sur Web (où `dart:io` n'existe pas), on utilise un **conditional import** :

- `lib/core/network/dev_cert_override.dart` — façade + conditional import
- `dev_cert_override_io.dart` — implémentation `dart:io` (gated `kDebugMode` + host `*.localtest.me`)
- `dev_cert_override_stub.dart` — no-op pour le Web

```dart
// dev_cert_override_io.dart (extrait)
void applyDevCertOverride(Dio dio) {
  if (!kDebugMode) return;
  dio.httpClientAdapter = IOHttpClientAdapter(
    createHttpClient: () {
      final client = HttpClient();
      client.badCertificateCallback = (cert, host, port) =>
          host == 'localtest.me' || host.endsWith('.localtest.me');
      return client;
    },
  );
}
```

> Jamais actif en release (`kDebugMode`) ni hors domaine local. Pour une solution sans bypass, charger `rootCA.pem` dans un `SecurityContext` dédié.

---

## Problème 8 — KC_HOSTNAME, issuer JWT et CORS (cohérence du port :8443)

### Ce qui s'est passé

Après le passage au port `:8443`, deux symptômes distincts :
1. L'API crashait au démarrage : `Unable to resolve the Configuration with the provided Issuer of "https://auth.localtest.me:8443/realms/tchalanet"` puis `Read timed out`.
2. Flutter Web : `blocked by CORS policy: No 'Access-Control-Allow-Origin' header`.

### Pourquoi

1. **Issuer / discovery** : `KC_HOSTNAME` injecte le port dans le claim `iss` des JWT. L'`issuer-uri` Spring doit donc inclure `:8443` pour valider les tokens. MAIS la discovery OIDC (`fromIssuerLocation`) et la récupération des clés se font **depuis le container API**, où `auth.localtest.me` → `127.0.0.1` = le container lui-même (pas Traefik) → timeout. Il faut donc une URL **interne** pour les clés.
2. **CORS** : Flutter Web utilise un port aléatoire (`http://localhost:53501`…) impossible à whitelister un par un.

### La solution

Trois variables à aligner (sources : `envs/dev/keycloak.env` + `envs/dev/.env`) :

| Variable | Valeur |
|---|---|
| `KC_HOSTNAME` | `https://auth.localtest.me:8443` |
| `TCH_KC_ISSUER` | `https://auth.localtest.me:8443/realms/tchalanet` |
| `…JWT_ISSUER_URI` | `https://auth.localtest.me:8443/realms/tchalanet` |

Et dans `application-dev.yaml` :
- `jwk-set-uri: http://keycloak:8080/realms/tchalanet/protocol/openid-connect/certs` (URL **interne** Docker — pas de discovery réseau, pas de TLS)
- `cors.allowed-origins: http://localhost:*,...` via `setAllowedOriginPatterns` (supporte les wildcards)

Le décodeur JWT (`InsecureJwtDecoderConfig`) utilise `NimbusJwtDecoder.withJwkSetUri(...)` + `JwtIssuerValidator` (comparaison de claim, **sans** appel réseau au démarrage) — `JwtValidators.createDefaultWithIssuer()` est à proscrire car il déclenche une discovery OIDC en Spring Security 6.4+.

> **Staging / prod** : même principe — `KC_HOSTNAME` doit inclure le port externe réel, et les trois variables doivent rester alignées. Voir la note mémoire « kc-hostname-port migration ».

> Le routage Traefik sur le port `8443` (entrypoint `mobile-https`) est défini dans `traefik/env/dev.yaml` (template source), **pas** dans `traefik/dynamic/10-routers.yaml` (fichier généré, écrasé à chaque `render-traefik`).

---

## Workflow complet (résumé)

```bash
# Une fois, pour préparer la session de dev
adb reverse tcp:8443 tcp:8443

# Lancer l'app
flutter run --dart-define=POS_DEVICE=true
```

Si Traefik n'est pas démarré :
```bash
docker compose \
  -f tchalanet-infra/compose/docker-compose-traefik.yml \
  -f tchalanet-infra/compose/docker-compose-keycloak.yml \
  up -d
```

---

## Schéma d'ensemble

Auth et API passent par **le même tunnel** `adb reverse tcp:8443` → Traefik (TLS mkcert).

```
Flutter App (Android Emulator)
        │
        │  1. Login → AppAuth (Custom Tab) → pile réseau Android
        │     → CA mkcert trusté via network_security_config
        │
        │  2. Appels API → Dio (dart:io) → pile TLS Dart
        │     → CA mkcert trusté via applyDevCertOverride (dev)
        │
        ▼
127.0.0.1:8443  ──── adb reverse ────▶  Mac: 127.0.0.1:8443
                                               │
                                          Traefik (TLS terminé, cert mkcert)
                                          entrypoints: websecure + mobile-https
                                          ┌────────────┴────────────┐
                                          ▼                         ▼
                                  Keycloak :8080            Spring Boot API :8080
                                  (HTTP interne)            (HTTP interne)
                                          │                         │
                                  ◀ Token JWT ──────        ◀ JSON ─────────
```

Côté serveur, l'API valide les JWT en récupérant les clés depuis l'URL **interne**
`http://keycloak:8080/.../certs` (pas via Traefik), et valide le claim `iss`
(`https://auth.localtest.me:8443/...`) par simple comparaison de chaîne.
