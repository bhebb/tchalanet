# Terminal Binding — RETIRÉ

> **Statut** : RETIRÉ — ne plus référencer ce flow dans le nouveau code.  
> **Date de retrait** : 2026-06-20  
> **Remplacé par** : le provisioning Firebase du SellerTerminal. Il n'y a plus de binding device séparé.

## Historique

L'ancien flow "Terminal Binding" liait un device physique (via OTP challenge) à un Terminal déclaré par l'admin. C'était un prérequis avant d'ouvrir une session POS.

Ce modèle est retiré avec la suppression du concept `Terminal` indépendant.

## Remplacement

L'identité et la confiance d'un SellerTerminal sont établies par son compte Firebase permanent (créé au provisioning). L'app POS s'authentifie avec le PIN du SellerTerminal — c'est la seule forme d'authentification requise.

Il n'y a pas de challenge OTP, pas de `TerminalBinding`, pas de `device_fingerprint`.

## Voir

- [SellerTerminal Provisioning](./seller-onboarding.md) — flow actuel de création et activation
- [Authentication flow §4](../../01-architecture/flows/authentication-flow.md#4-path-seller_terminal) — auth Firebase POS
