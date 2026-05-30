# Tenant Onboarding — Flow

> Provisioning d'un nouveau tenant par un super-admin.  
> Spec : `tchalanet-server/openspec/specs/tenant-provisioning/spec.md`  
> Package : `features.platformadmin.tenantonboarding`

---

## Principe

Le provisioning est un flow platform-admin, pas un flow dashboard.  
L'orchestrateur appelle les domaines/APIs propriétaires — il n'insère jamais directement dans les tables.

```
Super-admin provisionne → chaque domaine reçoit une commande → statut readiness retourné
```

---

## Profils V1

| Profil | Usage |
|---|---|
| `MINIMAL` | Tenant vide, configuration manuelle requise |
| `DEFAULT_HAITI_LOTTERY` | Configuration standard loterie Haïti |
| `DEMO` | Tenant de démonstration |

---

## Flow : Preview (lecture seule)

```
POST /platform/tenant-onboarding/preview
  → Retourne :
    - domaines qui seraient provisionnés
    - warnings (dépendances manquantes)
    - données non copiées
    - readiness attendue
  → Aucune donnée écrite
```

---

## Flow : Provisioning

```
POST /platform/tenant-onboarding/provision
  → Orchestrateur appelle dans l'ordre :

  1. platform / tenantconfig
     → Tenant base créé (id, code, nom, timezone, locale)

  2. platform.identity
     → Admin user initial créé

  3. catalog.theme / platform.tenanttheme
     → Thème/apparence appliqué selon profil

  4. catalog.settings / platform.tenantconfig
     → Settings par défaut appliqués

  5. catalog.i18n
     → Overrides i18n selon profil/locale

  6. catalog.game
     → Jeux activés selon profil

  7. catalog.pricing (owner pricing)
     → Pricing par défaut

  8. catalog.drawchannel
     → Draw channels selon profil

  9. core.promotion
     → Templates de promotion selon profil

  10. core.limitpolicy
      → Templates de limit policy selon profil

  11. core.pagemodel
      → PageModels dupliqués depuis templates globaux

  → Résultat : statut par domaine + readiness + next steps
```

---

## Ce que le provisioning ne copie pas

- Tickets, ventes, sessions, paiements
- Terminal bindings
- Users (sauf admin/demo initiaux)
- Audit, notifications, stats
- Ledger, offline submissions

---

## Readiness après provisioning

Après provisioning réussi, le tenant n'est pas encore opérationnel POS. Les étapes suivantes restent nécessaires :

1. Créer outlets
2. Créer terminals + binding
3. Créer sellers + outlet assignments
4. Ouvrir une session POS de test

---

## Sous-flows référencés

- Outlets : *(TODO — admin flow)*
- Terminals : [terminal-binding](./terminal-binding.md)
- Sellers : [seller-onboarding](./seller-onboarding.md)
- Session POS : [session-opening](./session-opening.md) *(TODO)*
