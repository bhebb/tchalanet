# DOC FONCTIONNELLE — Sécurité login, mobile, POS et transactions Tchalanet

## 1. Vision produit

Tchalanet offre un avantage fort aux tenants : vendre des tickets depuis un POS physique ou depuis un téléphone, tout en gardant une sécurité transactionnelle stricte.

La promesse fonctionnelle est :

```text
Vendre partout, mais jamais sans contexte vérifié.
```

Tchalanet ne sécurise pas seulement les comptes utilisateurs. Tchalanet sécurise le contexte de vente : utilisateur, tenant, terminal, appareil, outlet, session, permissions, idempotence et audit.

## 2. Règle d’or

Une transaction sensible ne doit jamais reposer uniquement sur le fait qu’un utilisateur est connecté.

Pour vendre un ticket, payer un gain, synchroniser des ventes offline ou demander un offline grant, la plateforme doit vérifier :

1. identité valide ;
2. tenant valide ;
3. rôle / permission valide ;
4. terminal actif ;
5. terminal assigné à l’utilisateur ;
6. appareil ou terminal virtuel activé ;
7. outlet actif ;
8. seller actif et assigné à l'outlet ;
9. session de vente ouverte ;
10. contexte opérationnel trusted ;
11. idempotency key ;
12. audit fonctionnel.

## 3. Surfaces applicatives

### 3.1 Web Angular

Utilisé pour :

- tenant admin ;
- super admin ;
- configuration ;
- dashboards ;
- supervision ;
- reporting.

Le web utilise un login fort via Keycloak : username/email + password, avec MFA recommandé pour tenant admin et obligatoire pour super admin.

Par défaut, un admin web n’a pas de terminal opérationnel. Il peut administrer, mais ne peut pas vendre sans entrer explicitement dans un mode opérateur / POS.

### 3.2 Flutter POS physique

Utilisé par les vendeurs/caissiers sur un appareil dédié ou semi-dédié.

Le POS physique nécessite :

- un terminal `PHYSICAL + POS` ;
- une assignation user ;
- un seller métier actif lié au user ;
- une assignation seller-outlet active ;
- un outlet ;
- un device binding signé ;
- une session de vente ;
- la permission `ticket.sell`.

### 3.3 Flutter mobile / vente téléphone

Utilisé par un vendeur autorisé à vendre par téléphone.

La vente téléphone nécessite :

- un terminal virtuel `VIRTUAL + MOBILE` ;
- un entitlement tenant `PHONE_SALES_ENABLED` ;
- une assignation user ;
- un seller métier actif lié au user ;
- une assignation seller-outlet active ;
- un binding virtuel signé ;
- une session compatible ;
- la permission `ticket.sell.phone`.

## 4. Identité et auth locale

### 4.1 Keycloak

Keycloak reste la source d’identité :

- username/email/password pour admin ;
- téléphone ou username court + secret initial pour vendeur mobile/POS ;
- tokens OAuth/OIDC pour Angular et Flutter ;
- refresh token pour éviter une reconnexion complète à chaque ouverture.

### 4.2 Face ID / empreinte / PIN local

La biométrie ne remplace pas Keycloak.

Elle sert à déverrouiller localement l’application et le coffre sécurisé contenant le refresh token ou les secrets locaux.

Flow utilisateur :

```text
Première connexion : téléphone/username + password -> Keycloak -> tokens.
Puis : activer Face ID / empreinte / PIN local.
Ouverture suivante : Face ID -> refresh token -> nouveau access token -> API Spring.
```

Si le refresh token est absent, expiré ou révoqué, l’utilisateur doit repasser par Keycloak.

## 5. Activation POS et mobile

### 5.1 POS physique

1. L’admin tenant crée un terminal POS.
2. L’admin assigne le terminal à un user et à un outlet.
3. Le POS affiche ou scanne un code d’appairage / QR code.
4. Le vendeur se connecte via Keycloak.
5. Spring vérifie user, terminal, outlet et tenant.
6. Spring crée un `SIGNED_DEVICE_BINDING`.
7. Le POS est prêt à vendre.

### 5.2 Terminal virtuel téléphone

1. Le tenant doit avoir `PHONE_SALES_ENABLED`.
2. L’admin crée ou active un terminal virtuel pour le vendeur.
3. L’activation se fait par code admin, email OTP ou SMS OTP selon la policy tenant.
4. Spring crée un virtual binding signé.
5. Le vendeur peut ouvrir le mode vente téléphone.

Note produit : le SMS OTP ne doit pas être utilisé à chaque login. Il est réservé à l’activation, au changement d’appareil, au reset sécurité ou à une suspicion de fraude.

## 6. Rôles et permissions

### 6.1 Rôles

- `SUPER_ADMIN` : supervision globale, opérations platform, override audité.
- `TENANT_ADMIN` : administration d’un tenant.
- `OPERATOR` : opérations internes tenant.
- `CASHIER` : vente POS/mobile.
- `SYSTEM` : tâches techniques contrôlées.

### 6.2 Permissions minimales

```text
ticket.sell
ticket.sell.phone
ticket.cancel
payout.pay
offline.grant
offline.sync
terminal.create
terminal.assign
terminal.activate
terminal.lock
terminal.revoke
session.open
session.close
admin.pos_mode
```

La règle UI : Angular/Flutter peut masquer des boutons selon les droits connus, mais Spring doit toujours revalider côté serveur.

## 7. Transactions sensibles

### 7.1 Vente ticket

Une vente ticket exige :

- `Authorization: Bearer <token>` ;
- terminal context trusted ;
- permission `ticket.sell` ou `ticket.sell.phone` ;
- session ouverte ;
- `Idempotency-Key` ;
- audit.

### 7.2 Payout

Un payout exige :

- identité valide ;
- permission payout ;
- terminal/outlet/session compatibles si paiement au comptoir ;
- audit fort ;
- éventuellement justification si opération sensible.

### 7.3 Offline

Une vente offline ne devient pas ticket réel tant que le serveur ne l’a pas validée au sync.

Le sync doit valider :

- offline grant ;
- terminal autorisé offline ;
- signature ;
- cutoff ;
- draw status ;
- idempotence ;
- limites offline ;
- audit.

## 8. Message commercial

Tchalanet peut être positionné comme :

> Une plateforme de vente multi-tenant sécurisée, conçue pour les réalités terrain : vendeurs mobiles, POS physiques, connexions instables, mais avec une sécurité transactionnelle forte.

Formulation forte :

```text
Un login ouvre une session utilisateur.
Un terminal trusted ouvre une capacité de vendre.
Une session opérationnelle valide permet une transaction.
```

## 9. MVP fonctionnel

Le MVP doit garantir :

1. pas de vente sans terminal ;
2. pas de vente sans session ;
3. pas de vente sans idempotency key ;
4. pas de vente depuis `CLIENT_CLAIM` ;
5. POS physique = `SIGNED_DEVICE_BINDING` ;
6. vente téléphone = terminal virtuel activé ;
7. terminal assigné à un user ;
8. permission Spring obligatoire ;
9. audit sur toutes les actions sensibles ;
10. RLS tenant obligatoire ;
11. auth locale = déverrouillage, pas autorisation métier ;
12. OTP = activation / reset / risque, pas login quotidien.
