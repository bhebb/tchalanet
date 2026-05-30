# Terminal Binding — Flow

> Processus de liaison entre un terminal déclaré et un appareil/application POS ou mobile.  
> Référence technique : `tchalanet-server/docs/conventions/context/operational-context.md`  
> Domaine canonique : `tchalanet-server/core/terminal/DOMAIN_TERMINAL.md`

---

## Pourquoi

Un terminal peut être créé par un admin sans qu'un appareil réel y soit encore lié. Le binding est la preuve durable qu'un device ou une application a été associé à ce terminal après une procédure de vérification.

Sans binding actif, le terminal ne peut pas être utilisé pour des opérations sensibles (sell, payout, offline).

---

## Pré-requis

- Terminal créé par l'admin (`REGISTERED` ou `PENDING_ACTIVATION`)
- Seller/opérateur authentifié et connu du système
- Tenant valide

---

## Flow : POS Device / Mobile App

```
Admin déclare le terminal
  └─ POST /admin/terminals
     → Terminal créé (REGISTERED)
     → Admin assigne outlet et user si nécessaire

Activation : création du challenge
  └─ POST /tenant/terminals/{terminalId}/activation-challenges
     → CreateTerminalActivationChallengeCommand
     → OTP 6 chiffres généré (codeHash stocké, code livré selon deliveryMode)
     → Challenge : PENDING / TTL court
     → Terminal : PENDING_ACTIVATION

Activation : vérification du challenge
  └─ POST /tenant/terminals/{terminalId}/activation-challenges/{challengeId}/verify
     → VerifyTerminalActivationChallengeCommand
       - clearCode
       - bindingType : POS_DEVICE | MOBILE_APP
       - bindingPublicKey (si device signature activée)
       - deviceFingerprintHash
       - bindingCredential
     → Handler vérifie : challenge valide, non expiré, non consommé, codeHash match
     → Handler crée TerminalBinding (ACTIVE)
     → Challenge marqué CONSUMED
     → Terminal → ACTIVE
```

---

## Flow : Admin Selection (Web / Back-office)

```
Admin sélectionne un terminal dans l'interface
  └─ POST /tenant/me/operational-context/select
     → Source : ADMIN_SELECTION
     → TerminalBinding de type ADMIN_SELECTION créé
     → Pas de challenge OTP requis
```

---

## États du binding

| Statut | Signification |
|---|---|
| `ACTIVE` | Binding valide, terminal utilisable |
| `REVOKED` | Révoqué explicitement (sécurité, réassignation) |
| `EXPIRED` | TTL dépassé (si applicable) |

---

## Validation lors d'une action sensible

Après binding, chaque action sensible (sell, payout) vérifie :

```
1. trusted operational context
2. terminal ACTIVE et appartient au tenant
3. binding ACTIVE et compatible avec l'opération
4. capability terminal requise présente
5. seller assignment actif
6. outlet/session match
```

Voir fail-fast complet : `docs/conventions/context/operational-context.md#fail-fast-order`

---

## Sous-flows référencés

- Après binding → [session-opening](./session-opening.md) *(TODO)*
- Après session → [sell-ticket](./sell-ticket.md)
- Voir contexte opérationnel → [role-flows](./role-login-flow.visual.html)
