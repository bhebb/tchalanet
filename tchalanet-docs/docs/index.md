# Tchalanet Docs

Tchalanet est une plateforme multi-tenant de loterie et POS pour vendre des tickets,
gérer des tirages, régler les résultats, payer les gagnants et opérer des réseaux de vente.

---

## Par où commencer ?

**Opérateur ou admin ?** → [Guide opérateur](02-functional/guides/operator-admin-guide.md)

**Agent terrain (vendeur / caissier) ?** → [Guide agent terrain](02-functional/guides/field-agent-guide.md)

**Développeur backend ?** → [Guide développeur backend](99-reference/backend-conventions.md)

**Nouveau sur le projet ?** → [Qu'est-ce que Tchalanet ?](00-overview/what-is-tchalanet.md)

**Besoin de la carte système ?** → [Carte système](00-overview/system-map.md)

---

## Parcours par rôle

### Opérateur / admin

1. [Qu'est-ce que Tchalanet ?](00-overview/what-is-tchalanet.md)
2. [Onboarding tenant](02-functional/flows/tenant-onboarding.md)
3. [Ouverture de session](02-functional/flows/session-opening.md)
4. [Réconciliation](02-functional/flows/reconciliation.md)
5. [Règlement (settlement)](02-functional/flows/settlement.md)

### Agent terrain (vendeur)

1. [Liaison terminal](02-functional/flows/terminal-binding.md)
2. [Ouverture de session](02-functional/flows/session-opening.md)
3. [Vente de ticket](02-functional/flows/sell-ticket.md)
4. [Vérification de ticket](02-functional/flows/verify-ticket.md)
5. [Paiement terrain (payout)](02-functional/flows/payout-field-flow.md)

### Développeur backend

1. [Architecture backend](server-docs/ARCHITECTURE.md)
2. [Backend Playbook](server-docs/PLAYBOOK.md)
3. [Command / Query handlers](server-docs/conventions/command_query_handlers.md)
4. [Guide développeur backend](99-reference/backend-conventions.md)

### Développeur mobile / POS

1. [Architecture mobile](mobile-docs/ARCHITECTURE.md)
2. [Sell ticket flow](02-functional/flows/sell-ticket.md)
3. [Payout flow](02-functional/flows/payout-field-flow.md)
4. [Offline sync](02-functional/flows/offline-sync.md)

---

## Zones principales

### Guide utilisateur
Parcours guidés pour opérateurs et agents terrain — self-serve, sans solliciter l'équipe.

### Métier
Flows système complets : vente ticket, tirage, payout, réconciliation, onboarding.

### Architecture
Cartes des composants, modèle de sécurité, décisions d'architecture (ADR).

### Technique
Conventions backend, conventions mobile, OpenSpec en cours.

### Opérations
Déploiement, stack locale, infrastructure.

### Référence interne
Guidelines, versions, où vit la vérité, maps AGENTS.
