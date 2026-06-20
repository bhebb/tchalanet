# SellerTerminal Provisioning — Flow

> Processus de création et d'activation d'un SellerTerminal.  
> Remplace l'ancien "Seller Onboarding" (Seller + Terminal + Session séparés — retirés).  
> Domaine canonique : `tchalanet-server/tchalanet-core/.../sellerterminal/`

---

## Concepts clés

```
SellerTerminal = acteur de vente unique (identité Firebase + droits de vente + unité de facturation)
Outlet         = groupement géographique optionnel
```

Un SellerTerminal n'a pas besoin d'un Outlet pour vendre.  
Un SellerTerminal n'a pas besoin d'une session ouverte pour vendre.

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

POST /api/v1/admin/seller-terminals/{id}/pin-reset
body: { reason: "PIN_LOST" | "SELLER_CHANGED" | "SUSPECTED_COMPROMISE" | "ADMIN_CORRECTION" | "OTHER" }

→ Réponse (une seule fois) : { temporaryPin: "<6 chiffres>", mustChangePin: true, ... }
→ Firebase password réinitialisé
→ mustChangePin = true

L'admin remet le nouveau PIN au vendeur hors système.
Le vendeur suit le flow "Premier login" (étapes 4–8) pour changer son PIN.
```

---

## États du SellerTerminal

| Statut | Signification |
|---|---|
| `ACTIVE` | Actif, peut vendre |
| `INACTIVE` | Inactif (cas historique) |
| `BLOCKED` | Bloqué temporairement par admin (réversible) |
| `DISABLED` | Désactivé définitivement |

---

## Invariants

- `mustChangePin = true` bloque toutes les actions de vente.
- Le PIN temporaire n'est jamais stocké en clair en DB ni loggué.
- Le PIN est retourné **une seule fois** dans la réponse de `/pin-reset` puis effacé.
- Un SellerTerminal `DISABLED` ne peut pas être réactivé via `/pin-reset` (409).
- Un SellerTerminal sans identité Firebase ne peut pas faire `/pin-reset` (409).

---

## Sous-flows référencés

- Auth POS → [authentication-flow](../../01-architecture/flows/authentication-flow.md#4-path-seller_terminal)
- Vente ticket → [sell-ticket](./sell-ticket.md)
