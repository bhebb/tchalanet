# Session POS — RETIRÉ

> **Statut** : RETIRÉ — ne plus référencer ce flow dans le nouveau code.  
> **Date de retrait** : 2026-06-20  
> **Remplacé par** : il n'y a plus de session POS à ouvrir.

## Historique

L'ancien flow d'ouverture de session POS établissait une fenêtre comptable (SalesSession) liée à un Terminal + Outlet + Seller. Ouvrir une session était un prérequis avant toute vente.

Ce modèle est retiré avec la suppression du concept `SalesSession`.

## Remplacement

Un `SellerTerminal` authentifié (`ACTOR_SELLER_TERMINAL`) peut vendre directement.  
L'écran POS home (`GET /api/v1/tenant/cashier/home`) retourne l'état de préparation sans notion de session.

Le seul `requiredStep` qui peut bloquer la vente est `MUST_CHANGE_PIN` (après provisioning ou reset PIN admin).

## Voir

- [SellerTerminal Provisioning](./seller-onboarding.md) — flow actuel
- Cashier home : `features.cashier` → `CashierHomeService`
