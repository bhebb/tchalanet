# Google Play Compliance Book — Tchalanet Mobile

Dernière revue : 2026-08-14

Ce document prépare la publication de l’application Android Tchalanet (`com.tchalanet.mobile`) sur Google Play. Il est basé sur l’état actuel du dépôt et sur les exigences Google Play consultées le 2026-08-14.

Ce document n’est pas un avis juridique. Avant publication, l’entité juridique exacte du compte développeur Google Play doit être vérifiée et doit apparaître dans la politique de confidentialité publiée.

## Sources officielles Google Play

- User Data / Privacy Policy / Data Safety: <https://support.google.com/googleplay/android-developer/answer/10144311>
- Data Safety form: <https://support.google.com/googleplay/android-developer/answer/10787469>
- Prepare app for review / App content: <https://support.google.com/googleplay/android-developer/answer/9859455>
- Prominent disclosure and consent: <https://support.google.com/googleplay/android-developer/answer/11150561>
- Advertising ID: <https://support.google.com/googleplay/android-developer/answer/6048248>

## URLs publiques à fournir

À exposer sur le domaine de production avant soumission Google Play :

- Privacy Policy: `https://tchalanet.com/public/privacy`
- Terms of Use: `https://tchalanet.com/public/terms`
- Data deletion: `https://tchalanet.com/public/data-deletion`
- Contact: `https://tchalanet.com/public/contact`

Google Play exige une Privacy Policy active, publique, non géobloquée, non PDF, non éditable par l’utilisateur, et accessible dans l’application et dans Play Console.

## Identité de l’application

- App name: Tchalanet
- Android package: `com.tchalanet.mobile`
- Current version in repo: `1.0.0+1`
- Auth provider observed in repo: Firebase Authentication
- Backend/API: Tchalanet API via HTTPS
- Public app links: `tchalanet.com`, `app.tchalanet.com`

À valider avant soumission :

- Nom légal exact du développeur Play Console.
- Adresse email ou mécanisme officiel de contact privacy. La page publique `/public/contact` est présente, mais une adresse dédiée peut réduire les risques de rejet.
- Pays de distribution et lois locales applicables.
- Classification d’âge et restrictions liées aux jeux de loterie.

## Permissions Android observées

Dans `tchalanet-mobile/android/app/src/main/AndroidManifest.xml` :

| Permission | Usage déclaré | Data Safety / disclosure |
|---|---|---|
| `CAMERA` | Scan QR/code ticket | Déclarer accès caméra. Expliquer que l’image sert à lire le code et n’est pas stockée comme photo/vidéo. |
| `BLUETOOTH_SCAN` | Recherche d’imprimantes ticket | Déclarer usage Bluetooth pour impression. |
| `BLUETOOTH_CONNECT` | Connexion imprimante | Déclarer usage Bluetooth pour impression. |
| `BLUETOOTH_ADVERTISE` | Capacité Bluetooth déclarée | Vérifier si réellement nécessaire avant release; retirer si inutile. |
| `ACCESS_FINE_LOCATION` avec `maxSdkVersion=30` | Requis par Android <= 11 pour scan Bluetooth | Déclarer que l’app ne suit pas la localisation; usage technique pour découverte Bluetooth. |

Les manifests debug/profile ajoutent `INTERNET` pour le développement. En release, vérifier le merged manifest, car les plugins réseau/Firebase peuvent aussi déclarer Internet.

Permissions non observées :

- Contacts
- SMS
- Journal d’appels
- Microphone
- Photos / media personnels
- Advertising ID (`com.google.android.gms.permission.AD_ID`)

## SDKs et dépendances à prendre en compte

Observé dans `tchalanet-mobile/pubspec.yaml` :

- `firebase_core`
- `firebase_auth`
- `dio`
- `flutter_secure_storage`
- `shared_preferences`
- `printing`
- `connectivity_plus`
- `flutter_blue_plus`
- `flutter_bluetooth_serial_plus`
- `permission_handler`
- `app_links`
- `mobile_scanner`

Action Play Console : inclure dans Data Safety les données collectées ou partagées par les SDKs intégrés. Google rappelle que le développeur est responsable des pratiques des SDKs tiers.

## Inventaire des données

| Donnée | Collectée/transmise hors appareil | Partagée avec tiers | Usage |
|---|---:|---:|---|
| Email ou identifiant de connexion | Oui | Firebase Auth, backend Tchalanet | Authentification, accès compte |
| Mot de passe/PIN | Transmis au service d’authentification lors de la connexion | Firebase Auth | Connexion; non stocké en clair dans l’app |
| Jetons d’authentification | Oui, stockés localement en secure storage | Firebase/Auth backend | Maintien de session, API |
| Rôles, permissions, organisation, vendeur, terminal | Oui | Backend Tchalanet | Contrôle d’accès et contexte opérationnel |
| Téléphone vendeur | Oui si profil renseigné | Backend Tchalanet | Profil, support, gestion opérationnelle |
| Téléphone client | Oui seulement si option SMS/ticket activée | Backend Tchalanet et prestataire SMS si activé | Envoi d’information ticket |
| Tickets, lignes de jeu, mises, tirages, résultats | Oui | Backend Tchalanet | Vente, vérification, rapports |
| Code public / QR scanné | Oui | Backend Tchalanet | Vérification ticket |
| Image caméra | Non conservée comme photo/vidéo par Tchalanet | Non | Lecture locale du QR/code |
| Imprimante Bluetooth sélectionnée | Stockée localement | Non par défaut | Impression ticket |
| Langue choisie | Stockée localement | Non par défaut | Préférence utilisateur |
| IP, horodatages, device/app version, request IDs | Oui | Hébergement/monitoring | Sécurité, diagnostic, prévention abus |
| Identifiant publicitaire | Non observé | Non | Non utilisé |

## Réponses proposées — Data Safety

### Data collection and security

- Does your app collect or share user data? **Yes**
- Is all user data collected by your app encrypted in transit? **Yes**, assuming production APIs are HTTPS only.
- Do you provide a way for users to request data deletion? **Yes**: `/public/data-deletion`.
- Is account creation available? **Yes**, accounts are provisioned/authorized for organizations; deletion requests must be supported.

### Data types à déclarer

Sélection recommandée, à vérifier dans Play Console selon les libellés exacts disponibles :

- Personal info:
  - Email address: collected, app functionality/account management.
  - Phone number: collected when profile or SMS option is used, app functionality/account management/communications.
  - Name: collected if entered in contact/support forms or account profile.
- Financial info:
  - Purchase history or transaction-like business records may not map cleanly to consumer purchases. Tickets, stakes and lottery operations should be disclosed under app activity or other app-specific data if Play Console offers the appropriate category. Do not mark Google Play in-app purchases unless used.
- App activity:
  - App interactions: ticket sale, scan, verification, draw/result views.
  - Other user-generated content or other actions: operational lottery entries and ticket data if Play Console category applies.
- App info and performance:
  - Crash logs/diagnostics only if a crash/monitoring SDK is enabled. Currently Firebase Crashlytics is not observed; do not declare Crashlytics unless added.
  - Diagnostics: technical logs/request metadata if transmitted.
- Device or other IDs:
  - Device identifiers only if Firebase/SDKs or backend logs collect an installation/device ID. Verify merged SDK behavior before submission.
- Location:
  - Only if Play Console asks due to `ACCESS_FINE_LOCATION` on Android <= 11. Explain Bluetooth discovery; no location tracking.

### Data sharing

Declare sharing where data is processed by:

- Firebase Authentication for auth;
- infrastructure/hosting/security providers;
- SMS/communication provider if SMS feature is enabled;
- the user’s organization/manager for operational records.

Do not declare advertising or sale of data unless an ad/marketing SDK is introduced.

## Store listing disclosures

Suggested short disclosure for review notes:

> Tchalanet is a lottery operations app for authorized managers, sellers and terminals. The app uses camera permission to scan ticket QR codes, Bluetooth permissions to connect ticket printers, and Android location permission only on Android 11 and earlier because it is technically required for Bluetooth discovery. The app does not track user location, does not access contacts/SMS/call logs/microphone/photos, and does not use advertising ID.

Restricted access instructions:

> The manager/seller areas require a provisioned account. Provide Google reviewers with a test account and instructions for login, ticket scan fallback using manual code entry, and printer screens without requiring a physical printer.

## Public policy coverage checklist

- [x] Privacy Policy names Tchalanet and mobile package context.
- [x] Privacy Policy explains collected data categories.
- [x] Privacy Policy includes sharing categories.
- [x] Privacy Policy includes secure handling.
- [x] Privacy Policy includes retention and deletion.
- [x] Privacy Policy includes cookies/local storage.
- [x] Privacy Policy explains camera/Bluetooth/location permissions.
- [x] Data deletion page exists.
- [x] Terms mention accounts, terminals, acceptable use and mobile permissions.
- [ ] Confirm legal developer entity and privacy contact.
- [ ] Confirm production URLs are reachable without authentication and without geoblocking.
- [ ] Confirm in-app links to Privacy Policy and Data deletion before release.
- [ ] Verify merged Android release manifest before upload.
- [ ] Complete Google Play Data Safety form from the final release artifact and SDK inventory.

## Release-blocking checks

Before Google Play upload:

```bash
cd tchalanet-mobile
flutter build appbundle --release
```

Then inspect merged permissions from the release artifact or Android build intermediates. If `AD_ID`, media, contacts, SMS, microphone, or call-log permissions appear unexpectedly, either remove the dependency/permission or update the policy and Data Safety declarations.
