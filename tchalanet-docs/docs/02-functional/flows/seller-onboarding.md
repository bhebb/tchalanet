# Seller Onboarding — Flow

> Processus de création et d'activation d'un seller (machann/vendeur transactionnel).  
> Domaine canonique : `tchalanet-server/openspec/changes/tchalanet-commercial-network-v1/docs/domains/DOMAIN_SELLER.md`  
> Décision : `tchalanet-commercial-network-v1/docs/decisions/ADR-SELLER-OUTLET-PROMOTION-V1.md`

---

## Concepts clés

```
User     = authentification (Keycloak)
Seller   = identité métier / machann / vendeur transactionnel
Outlet   = canal, lieu, institution de vente
Cashier  = écran/flow POS
```

Un seller peut exister avant d'avoir un login user. Un user peut être lié à un seller existant après coup.

---

## Flow : Création seller

```
Admin tenant crée le seller
  └─ POST /admin/sellers
     → Seller créé : ACTIVE
     → seller_id, display_name, code (optionnel), user_id (optionnel)
```

---

## Flow : Lier un user au seller

```
Admin lie un user existant au seller
  └─ PATCH /admin/sellers/{sellerId}/user
     → seller.user_id = userId
     → Le user peut maintenant s'authentifier comme seller
```

---

## Flow : Assigner un outlet au seller

```
Admin assigne le seller à un outlet
  └─ POST /admin/sellers/{sellerId}/outlet-assignments
     → SellerOutletAssignment créé (ACTIVE)
     → Historisé : starts_at = maintenant

Changement d'outlet :
  → Ancien assignment fermé (ends_at, ENDED)
  → Nouvel assignment ouvert
  → Les anciens tickets gardent le seller_assignment_id original
```

---

## Flow : Seller prêt à vendre

Une fois le seller créé et assigné à un outlet, pour pouvoir vendre :

1. User du seller se connecte (Keycloak)
   - Voir [role-flows](./role-login-flow.visual.html) — Seller POS
2. Terminal bindé à l'appareil
   - Voir [terminal-binding](./terminal-binding.md)
3. Session POS ouverte
   - Voir [session-opening](./session-opening.md) *(TODO)*
4. Opération `sell` disponible

---

## États du seller

| Statut | Signification |
|---|---|
| `ACTIVE` | Seller actif, peut vendre |
| `SUSPENDED` | Temporairement suspendu |
| `INACTIVE` | Désactivé |

---

## États d'un assignment outlet

| Statut | Signification |
|---|---|
| `ACTIVE` | Assignment courant |
| `ENDED` | Clôturé (changement d'outlet ou fin) |
| `SUSPENDED` | Suspendu temporairement |

---

## Invariants

- Un seller peut être sans user — il peut exister pour représenter un point de vente sans login.
- Un seller peut avoir plusieurs assignments outlet dans l'historique, un seul ACTIVE à la fois.
- La commission est snapshotée dans `sales` au moment de la vente — pas recalculée.

---

## Sous-flows référencés

- Après seller actif + outlet assigné → [terminal-binding](./terminal-binding.md)
- Après binding terminal → [session-opening](./session-opening.md) *(TODO)*
- Après session → [sell-ticket](./sell-ticket.md)
