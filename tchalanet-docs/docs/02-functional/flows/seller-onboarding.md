# SellerTerminal Provisioning — Flow

> Processus de création et d'activation d'un SellerTerminal.  
> Remplace l'ancien "Seller Onboarding" (Seller + Terminal + Session séparés — retirés).  
> Domaine canonique : `tchalanet-server/tchalanet-core/.../sellerterminal/`

---

## Concepts clés

```
SellerTerminal = acteur de vente unique
  - identité technique stable : terminalCode + email Firebase construit
  - identité vendeur courant : firstName, lastName, displayName, phoneNumber
  - droits de vente + unité de facturation
Outlet         = groupement géographique optionnel
```

Un SellerTerminal n'a pas besoin d'un Outlet pour vendre.  
Un SellerTerminal n'a pas besoin d'une session ouverte pour vendre.

Le SellerTerminal porte donc deux dimensions :

| Dimension | Stable ? | Exemple | Usage |
|---|---:|---|---|
| Identité technique POS | Oui | `HT-001@tchalanet.tchalanet` | Firebase login, contexte de vente, tickets, commission |
| Vendeur actuel | Non | nom, prénom, téléphone | affichage admin/POS, remise du PIN, responsabilité opérationnelle courante |

Quand le vendeur change, l'admin met à jour les informations du vendeur courant et réinitialise le
PIN. L'identité technique Firebase reste celle du SellerTerminal.

---

## Flow : Création par l'admin

```
Admin tenant crée le SellerTerminal
  └─ POST /api/v1/admin/seller-terminals
     body: { terminalCode, displayName, firstName, lastName, phoneNumber, commissionRate, initialPin }
     → SellerTerminalId retourné
     → Identité Firebase provisionnée (email fictif : <terminalCode>@<tenant>.tchalanet)
     → seller_terminal créé : statut ACTIVE, mustChangePin = true
```

**L'admin remet le `initialPin` au vendeur physiquement (hors système).**

---

## Flow : Premier login du SellerTerminal

```
1. Le vendeur ouvre l'app POS mobile.

2. Il s'authentifie avec :
   - Email : <terminalCode>@<tenant>.tchalanet
   - PIN : le PIN temporaire remis par l'admin

3. Firebase valide le PIN → retourne un id_token.

4. L'app envoie GET /api/v1/tenant/seller-terminal/me
   → { ..., mustChangePin: true, ... }

5. L'app envoie GET /api/v1/tenant/cashier/home
   → { requiredStep: { type: "MUST_CHANGE_PIN", ... }, canSell: false }

6. L'app force la navigation vers l'écran "Changer PIN".

7. Le vendeur saisit un nouveau PIN.
   POST /api/v1/tenant/seller-terminal/me/change-pin
   body: { newPin: "<6 chiffres>" }
   → Firebase password mis à jour
   → mustChangePin = false

8. GET /api/v1/tenant/cashier/home → prêt à vendre
```

---

## Flow : Reset PIN par l'admin

```
Admin constate qu'un vendeur a perdu son PIN ou qu'un changement de vendeur a lieu.

Si le vendeur change, l'admin met aussi à jour le profil vendeur courant du SellerTerminal
(firstName, lastName, displayName, phoneNumber).

POST /api/v1/admin/seller-terminals/{id}/pin-reset
body: { reason: "PIN_LOST" | "SELLER_CHANGED" | "SUSPECTED_COMPROMISE" | "ADMIN_CORRECTION" | "OTHER" }

→ Réponse (une seule fois) : { temporaryPin: "<6 chiffres>", mustChangePin: true, ... }
→ Firebase password réinitialisé
→ mustChangePin = true

L'admin remet le nouveau PIN au vendeur hors système.
Le vendeur suit le flow "Premier login" (étapes 4–8) pour changer son PIN.
```

### Durée du PIN temporaire

État actuel : le PIN temporaire affiché après reset devient le password Firebase du SellerTerminal
et reste valide jusqu'à ce que le vendeur le remplace via `/me/change-pin`.

Décision V0 : pas de TTL applicatif. La sécurité repose sur :

- affichage du PIN une seule fois ;
- transmission hors système par l'admin ;
- `mustChangePin=true`, qui bloque la vente du SellerTerminal tant que le vendeur n'a pas remplacé
  le PIN temporaire ;
- possibilité pour l'admin de refaire un reset PIN si le code n'a pas été transmis correctement.

---

## États du SellerTerminal

| Statut | Signification |
|---|---|
| `PENDING` | Créé, identité Firebase non encore liée |
| `ACTIVE` | Actif, peut vendre |
| `BLOCKED` | Bloqué temporairement par admin (réversible) |
| `DISABLED` | Désactivé définitivement |

---

## Invariants

- `mustChangePin = true` bloque les actions de vente quand l'acteur authentifié est le
  `SELLER_TERMINAL` lui-même.
- Exception Admin POS/support : un `APP_USER` admin peut vendre avec un SellerTerminal `ACTIVE`
  sélectionné explicitement même si `mustChangePin = true`, car l'admin n'utilise pas le PIN du
  SellerTerminal.
- L'email Firebase construit appartient à l'identité technique du SellerTerminal ; il ne doit pas
  être remplacé par l'email personnel du vendeur courant.
- Les champs `firstName`, `lastName`, `displayName`, `phoneNumber` décrivent le vendeur courant et
  peuvent changer sans changer l'identité technique du SellerTerminal.
- Le PIN temporaire n'est jamais stocké en clair en DB ni loggué.
- Le PIN est retourné **une seule fois** dans la réponse de `/pin-reset` puis effacé.
- Le PIN temporaire n'a pas de TTL applicatif en V0 ; il reste valide comme password Firebase
  jusqu'au changement de PIN self-service.
- Un SellerTerminal `DISABLED` ne peut pas être réactivé via `/pin-reset` (409).
- Un SellerTerminal sans identité Firebase ne peut pas faire `/pin-reset` (409).

---

## Sous-flows référencés

- Auth POS → [authentication-flow](../../01-architecture/flows/authentication-flow.md#4-path-seller_terminal)
- Vente ticket → [sell-ticket](./sell-ticket.md)
