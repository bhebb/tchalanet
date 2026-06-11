# Guide opérateur / admin

## Ce que cette page répond

Je suis opérateur ou admin Tchalanet. Par où je commence ?

---

## Votre rôle

En tant qu'opérateur ou admin, vous configurez et supervisez le réseau de vente :

- Vous gérez les tenants, outlets et terminaux
- Vous supervisez les sessions, réconciliations et règlements
- Vous pilotez l'onboarding des vendeurs et la liaison des terminaux
- Vous avez accès au back-office et aux rapports

---

## Parcours recommandé

### 1. Démarrer avec Tchalanet

1. [Qu'est-ce que Tchalanet ?](../../00-overview/what-is-tchalanet.md) — comprendre le produit et les rôles
2. [Carte système](../../00-overview/system-map.md) — comment les composants s'articulent

### 2. Mettre en place le réseau de vente

| Étape | Flow |
|---|---|
| Créer un nouveau tenant | [Onboarding tenant](../flows/tenant-onboarding.md) |
| Intégrer un vendeur | [Onboarding vendeur](../flows/seller-onboarding.md) |
| Lier un terminal POS | [Liaison terminal](../flows/terminal-binding.md) |
| Sélectionner le POS admin | [Sélection POS admin](../flows/admin-pos-selection.md) |

### 3. Opérer au quotidien

| Action | Flow |
|---|---|
| Ouvrir une session de vente | [Ouverture de session](../flows/session-opening.md) |
| Suivre les ventes du jour | [Réconciliation](../flows/reconciliation.md) |
| Clore et régler la journée | [Règlement (settlement)](../flows/settlement.md) |
| Superviser le tirage | [Tirage (draw execution)](../flows/draw-execution.md) |

---

## Gérer le Maryaj gratuit (promotion par défaut)

> Disponible avec la version « Maryaj gratuit automatique »
> (spec : `tchalanet-server/openspec/changes/maryaj-gratis-auto-selection-v1/`).

Chaque tenant reçoit une campagne `Maryaj gratuit` instanciée depuis le
template plateforme. Elle appartient au tenant — vous pouvez :

| Besoin | Action |
|---|---|
| Un tenant n'en veut pas | Mettre la campagne en **pause** ou la **désactiver** dans l'admin promotion. Effet immédiat sur les ventes suivantes ; les tickets déjà émis ne changent pas. |
| Modifier la mise de base offerte | Éditer le `payoutBaseAmount` de l'effet (valeur fixe en V1 ; un mode multiplicateur est à l'étude). |
| Modifier les conditions | Éditer l'éligibilité de la règle (montant minimum payé, nombre de lignes). |
| Limiter la régénération des numéros | `maxRegenerationsBeforeConfirm` (3 par défaut) — chaque régénération est auditée. |

Règles à connaître :

- Le vendeur ne valide pas la promo : la ligne gratuite apparaît
  automatiquement au preview si la campagne est active.
- Les numéros sont régénérables **avant** confirmation uniquement ; après,
  ils sont figés sur le ticket.
- Pas de Maryaj gratuit en vente offline (V1).

---

## Où vit la vérité

Pour les règles métier et les configurations avancées :

- [Politique documentaire](../../00-guidelines/doc-policy.md)
- [Où vit la vérité](../../00-overview/where-truth-lives.md)

---

## Questions fréquentes

**Je dois approuver une grosse vente** → Voir le flow [Vente de ticket](../flows/sell-ticket.md) — section `PENDING_APPROVAL`.

**Un terminal ne répond plus** → Voir le flow [Liaison terminal](../flows/terminal-binding.md) — section rebinding.

**La réconciliation ne balance pas** → Voir le flow [Réconciliation](../flows/reconciliation.md) — section écarts.
