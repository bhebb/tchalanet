# Seller onboarding et operation de vente

Ce document complete le change `tchalanet-security-login-terminal` sur le point qui etait encore trop implicite : un terminal trusted ne suffit pas a vendre. La vente doit aussi etre rattachee a un `seller` metier actif et assigne a l'outlet de l'operation.

## 1. Principe

Le login identifie un `User`.

Le terminal trusted identifie le contexte operationnel : terminal, outlet, session et source de confiance.

Le seller identifie la personne metier qui porte la vente, ses droits commerciaux, ses commissions et son assignation terrain.

Regle centrale :

```text
Le client ne fournit jamais sellerId.
Spring le resout depuis tenantId + actorUserId + outletId + salesSessionId.
```

## 2. Onboarding d'un seller

### 2.1 Pre-requis

- tenant actif ;
- outlet actif et autorise a vendre ;
- utilisateur Keycloak/bootstrap cree pour le vendeur ;
- permissions d'administration seller/terminal pour l'operateur qui fait l'onboarding.

### 2.2 Creation seller

Commande cible/deja presente :

```text
CreateSellerCommand(tenantId, displayName, code, userId?)
```

Effets :

- cree un seller metier dans `core.seller` ;
- le status initial doit permettre une activation explicite selon la policy tenant ;
- `code` est un identifiant metier lisible par le tenant ;
- `userId` peut etre fourni a la creation ou lie plus tard.

Invariants :

- un seller appartient a un tenant ;
- le display name est obligatoire ;
- le seller doit etre `ACTIVE` pour vendre.

### 2.3 Liaison seller/user

Commande cible/deja presente :

```text
LinkSellerToUserCommand(tenantId, sellerId, userId)
```

Regle :

- le `User` porte l'authentification et les roles ;
- le `Seller` porte l'identite commerciale et les regles metier ;
- la vente utilise les deux, mais seul le serveur fait le lien.

### 2.4 Assignation seller/outlet

Commande cible/deja presente :

```text
AssignSellerToOutletCommand(tenantId, sellerId, outletId, startsAt)
```

Effets :

- cree une assignation active ou planifiee entre le seller et l'outlet ;
- cette assignation est le lien metier requis pour vendre dans cet outlet ;
- la vente persiste `sellerId` et `sellerAssignmentId` pour garder la preuve du rattachement au moment de l'operation.

Invariants :

- le seller doit etre actif pour etre eligible a la vente ;
- l'assignation doit etre active au moment de la vente ;
- l'outlet de l'assignation doit matcher l'outlet du contexte POS/session ;
- une fin d'assignation ne modifie pas les tickets historiques.

### 2.5 Commission et politique commerciale

Commande cible/deja presente :

```text
SetSellerCommissionPolicyCommand(...)
```

La commission est separee du terminal :

- le terminal prouve la capacite technique de vendre ;
- le seller prouve qui vend et sous quelle politique commerciale ;
- les calculs de commission doivent se baser sur le seller/assignment resolu serveur, jamais sur un champ client.

### 2.6 Terminal du seller

L'onboarding seller se termine par l'activation du contexte operationnel :

1. creer ou choisir un terminal `PHYSICAL + POS` ou `VIRTUAL + MOBILE` ;
2. assigner le terminal au user du seller ;
3. creer un challenge d'activation ;
4. verifier le challenge et creer le binding ;
5. ouvrir ou selectionner une session de vente ;
6. le seller peut vendre seulement si toutes les validations seller + terminal + session passent.

## 3. Flow operation de vente

### 3.1 Entree HTTP

Endpoint actuel :

```http
POST /tenant/tickets
Authorization: Bearer <access-token>
Idempotency-Key: <stable-operation-key>
X-Terminal-Id: <terminal-id>
X-Outlet-Id: <outlet-id>
X-Sales-Session-Id: <session-id>
X-Device-Binding: <signed-binding-token>
```

La requete contient les lignes de ticket. Elle ne contient pas `sellerId`, `tenantId`, `userId` de reference, ni secret de binding.

### 3.2 Ordre fail-fast cible

1. Authentifier le Bearer token.
2. Resoudre tenant et actor depuis le contexte serveur.
3. Lire l'idempotency key via `platform.idempotence`.
4. Resoudre le contexte operationnel.
5. Refuser la vente si le contexte est `CLIENT_CLAIM` ou `NONE`.
6. Valider terminal, binding, assignment user, capability et outlet.
7. Valider session de vente pour l'action `SELL_TICKET_ONLINE`.
8. Resoudre le seller via `ResolveSellerForOperationQuery`.
9. Valider cutoff, lignes, jeux, limites, autonomie et promotions.
10. Persister ticket, lignes, charges, sellerId et sellerAssignmentId.
11. Publier les evenements after-commit et auditer succes/echec sensible.

### 3.3 Resolution POS/session

Le code de vente appelle :

```text
PosSaleContextResolver.resolve(ctx)
```

Ce resolver doit appeler :

```text
ctx.trustedOperationalContextRequired()
ResolvePosOperationContextQuery(..., PosOperationAction.SELL_TICKET_ONLINE)
```

But :

- garantir que le contexte vient d'une source trusted ;
- obtenir un `ValidatedPosOperationContext` serveur ;
- empecher une vente basee uniquement sur des headers client.

### 3.4 Resolution seller

Le code de vente appelle :

```text
SaleSellerContextResolver.resolve(pos)
```

qui demande :

```text
ResolveSellerForOperationQuery(
  tenantId,
  actorUserId,
  outletId,
  salesSessionId
)
```

Reponses attendues :

- success : `sellerId`, `sellerAssignmentId`, outletId, status seller, status assignation ;
- `seller.no_seller_for_user` si aucun seller n'est lie au user ;
- `seller.not_active` si le seller ne peut pas vendre ;
- `seller.not_assigned_to_outlet` si le seller n'est pas assigne a l'outlet.

### 3.5 Vente POS physique

Conditions minimales :

- terminal `PHYSICAL + POS` actif ;
- binding `POS_DEVICE` actif, signe et compatible ;
- terminal assigne au user ;
- outlet du terminal/session actif ;
- seller actif lie au user ;
- seller assigne au meme outlet ;
- session ouverte ;
- permission `ticket.sell`.

### 3.6 Vente telephone

Conditions minimales :

- terminal `VIRTUAL + MOBILE` actif ;
- binding `MOBILE_APP` actif, signe et compatible ;
- entitlement tenant `PHONE_SALES_ENABLED` ;
- capability `SELL_PHONE` ;
- seller actif lie au user ;
- seller assigne a l'outlet/session utilise pour l'operation ;
- permission `ticket.sell.phone`.

### 3.7 Donnees persistantes de preuve

Un ticket vendu doit garder les references operationnelles serveur :

- `tenantId` ;
- `ticketId` ;
- `terminalId` ;
- `outletId` ;
- `salesSessionId` ;
- `sellerId` ;
- `sellerAssignmentId` ;
- `actorUserId` ou `performedBy` selon le modele local ;
- idempotency key/scope ;
- timestamps serveur.

Ces references sont la base de l'audit, des commissions, de la reconciliation et des disputes.

## 4. Ce qui reste a implementer

- validation complete terminal/binding/capability avant vente ;
- enforcement explicite `ticket.sell.phone` + entitlement telephone ;
- audit success/denial pour vente refusee ;
- tests d'integration POS pairing -> trusted sale -> seller resolution ;
- tests d'integration virtual phone activation -> phone sale -> seller resolution.
