# Spec — mobile-offline-branchlet

## Intent

Prepare the Flutter POS app for future offline mode without implementing full offline sales/sync in this change.

## Concepts

```text
OfflineGrant
  server-issued, signed authorization with limits and expiresAt

OfflineSession
  local seller-confirmed offline period, always <= grant.expiresAt
```

## Rules

- Offline limits are always lower than online limits.
- Seller may choose a local offline duration only within the remaining server grant duration.
- Seller cannot extend a grant without internet.
- Offline mode must be explicitly confirmed when no internet is detected.
- Offline grant does not guarantee final acceptance of offline sales at sync.

## Startup behavior

When online:

```text
login/profile OK
-> POS context OK
-> silently request/refresh offline grant if eligible
-> store grant securely if returned
-> do not block POS if grant is denied
```

## Periodic refresh

When online:

```text
refresh grant periodically
allow seller to tap “Mettre à jour autorisation”
```

## No internet behavior

If no network:

```text
check local valid grant
if absent/expired -> show offline unavailable
if valid -> ask seller to confirm offline mode and duration
```

## Required warning copy

```text
Vous êtes hors ligne.
Les ventes seront enregistrées localement et devront être synchronisées.
Certaines ventes peuvent être rejetées si les règles serveur ne sont plus respectées.

[Continuer hors ligne]
[Annuler]
```

## Offline status UI

Display permanently while offline:

```text
HORS LIGNE
Dernière autorisation
Expiration
Ventes à synchroniser
Limite restante estimée
```

## Acceptance criteria

- App has offline package/branchlet structure.
- App displays grant status placeholder.
- App can represent grant unavailable/available/expired/sync required states.
- App asks for seller confirmation before entering offline mode.
- No actual offline ticket sale is required in this change.
