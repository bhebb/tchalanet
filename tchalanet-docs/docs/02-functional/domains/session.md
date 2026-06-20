# Session POS — RETIRÉ

> **Statut** : RETIRÉ — ne plus référencer ce domaine dans le nouveau code.  
> **Date de retrait** : 2026-06-20  
> **Remplacé par** : le concept de session POS est supprimé. `SellerTerminal` est l'acteur opérationnel direct sans session.

## Historique

L'entité `SalesSession` modélisait une fenêtre comptable d'une période de vente liée à un terminal+outlet+seller.

Ce modèle est retiré dans Tchalanet V0+ : un `SellerTerminal` actif peut vendre directement sans ouvrir de session.

## Tables DB

Les tables `sales_session` et `sales_session_aud` restent en DB pour préserver l'historique des données existantes. Elles ne doivent pas être utilisées pour de nouvelles ventes.

## Migration

Les nouvelles ventes référencent `seller_terminal_id` directement (pas de `sales_session_id`).

## Voir

- `tchalanet-docs/docs/02-functional/domains/terminal.md` — domaine SellerTerminal actuel
- `tchalanet-docs/docs/00-guidelines/glossary.md#sellerterminal` — vocabulaire canonique
