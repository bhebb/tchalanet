# mobile-ux-ergonomics-v1

## Status

Proposed — 2026-08-09

## Why

Audit UX complet de l'app mobile POS : 14 écrans analysés, 18 améliorations identifiées. L'app fonctionne déjà et le design system est solide — l'objectif est d'éliminer les frictions quotidiennes pour les machann qui vendent 200+ tickets/jour sur le terrain, souvent sur des téléphones d'entrée de gamme ou des terminaux Sunmi V2 (360×720dp).

Constats principaux vérifiés dans le code :

**Flux de vente (80% du temps machann) :**
- Les chips de jeu (Bolèt, Maryaj, Loto) utilisent `GestureDetector` au lieu de `InkWell` — aucun feedback tactile (ripple, haptique). Le machann tape et ne sait pas si c'est pris.
- Double confirmation Vérifier → Confirmer : sécuritaire mais lent pour le volume. Chaque tap supplémentaire × 200 ventes/jour = friction cumulative.
- "Nouveau ticket" après une vente navigue vers `/pos` (home) au lieu de `/sell` — le machann doit re-sélectionner un tirage pour vendre à nouveau (+1 tap × 200/jour).
- Largeurs de champ de sélection numérique fixées en dur (112/152px) — risque de clip sur écrans étroits ou accessibilité grande police.

**Écrans secondaires :**
- Placeholder QR sur la page scan : icône QR tappable qui affiche "scan pa disponib". Fausse promesse d'une feature qui n'existe pas.
- Historique tickets limité à aujourd'hui/hier, max 50 sans pagination ni message de troncature.
- Page résultats : 4 filtres empilés consomment ~300dp avant le premier résultat (sur un écran 720dp, il reste 340dp utiles).
- Range par défaut des résultats (2 jours) non communiquée à l'utilisateur.
- Settings sauvegardés immédiatement sans feedback (pas de spinner, pas de confirmation).

**Bugs et incohérences :**
- String hardcodée en français `'Copier code'` sur la page succès (`cashier_sell_success_page.dart:197`).
- Fallback hardcodé `'Tout li'` dans notification center, `'Tiraj'` dans stats page.
- Dates formatées en dur (DD/MM/YYYY HH:MM) au lieu de `MaterialLocalizations.formatMediumDate`.
- Pas de mode sombre (theme unique `TchTheme.light()`).
- Dialog de logout copié-collé 3 fois (change_pin, home, profile).
- Page Forbidden : message générique, pas de raison ni de contact admin.
- Section "dernier ticket" du dashboard : `SizedBox.shrink()` silencieux en erreur/vide.

**Ce qui est déjà bon (ne pas toucher) :**
- Design system tokens (spacing, radius, couleurs sémantiques), composants (PosActionButton, SemanticActionButton, StatCard, StatusBadge, OnlineBadge).
- i18n quasi parfaite (99%+ des strings passent par le système de traduction).
- Vocabulaire métier correct (Bolèt, Maryaj, Machann, Tiraj, Rezilta, Komisyon).
- Auto-advance des segments de sélection numérique.
- Undo inline 8 secondes pour les lignes supprimées.
- Preview ticket en temps réel avec code couleur accepté/rejeté.
- Auto-print sur succès avec receipt settings configurable.
- Sélecteur de langue en noms natifs ("Kreyol", "Francais", "English").
- Adaptive navigation (NavigationBar compact, NavigationRail medium+).

## What

### A — Flux de vente (écran critique)

1. **Chips de jeu : `GestureDetector` → `InkWell`** — ajouter ripple Material et retour sémantique accessible sur tous les chips de `cashier_sell_page.dart` (widget `_Chip`). Conserver le `minHeight: 48` existant.
2. **"Nouveau ticket" → `/sell`** — sur `cashier_sell_success_page.dart`, le bouton "Nouveau ticket" doit naviguer vers `/sell` avec le dernier tirage pré-sélectionné (via `extra: {'drawId': currentDrawId}`), pas vers `/pos`.
3. **Largeurs de sélection dynamiques** — remplacer les `width: 112` / `width: 152` fixes par un `Flexible` ou `LayoutBuilder` pour adapter la largeur aux écrans étroits et à l'accessibilité grande police.

### B — Corrections localisation

4. **Fix "Copier code" hardcodé** — remplacer `'Copier code'` (ligne 197 de `cashier_sell_success_page.dart`) par `translations.translate('pos.sale.copy_code')`. Ajouter la clé dans les 3 locales (ht, fr, en).
5. **Retirer les fallbacks hardcodés** — supprimer les paramètres `fallback: 'Tout li'` et `fallback: 'Tiraj'` dans `notification_center_page.dart` et `seller_terminal_stats_page.dart`. Si les clés i18n manquent, les ajouter.
6. **Dates localisées** — remplacer les `_fmtDateTime` hardcodés (DD/MM/YYYY HH:MM) dans `cashier_ticket_detail_page.dart` et `notification_center_page.dart` par `MaterialLocalizations.of(context).formatMediumDate` + `formatTimeOfDay`.

### C — Scan / vérification ticket

7. **Retirer le placeholder QR trompeur** — supprimer le container 160dp avec icône QR et le texte invite de `cashier_scan_page.dart`. Garder uniquement le champ de saisie manuelle avec label "Tape kòd tikè a". Quand le scan sera implémenté, ajouter le bouton.
8. **Désactiver bouton "Verifye" si champ vide** — `onPressed: code.trim().isEmpty ? null : _verify` au lieu de toujours actif.

### D — Dashboard améliorations

9. **État vide explicite pour "dernier ticket"** — remplacer `SizedBox.shrink()` par un widget `FeedbackState` compact avec message "Pa gen tikè vann jodi a" et icône ticket quand il n'y a pas de ticket, et un message d'erreur quand le chargement échoue.
10. **Isoler le timer countdown** — extraire la logique `Timer.periodic(1s)` dans un widget `_DrawCountdown` séparé avec son propre `setState`, pour que le rebuild ne touche que les labels de countdown et pas l'arbre entier du dashboard.

### E — Historique et rapports

11. **Date picker sur historique tickets** — ajouter une troisième option "Lòt dat" au `SegmentedButton` qui ouvre un `showDatePicker`, comme la page Rapports le fait déjà.
12. **Pagination tickets (> 50)** — ajouter un bouton "Wè plis" en bas de la liste quand il y a plus de 50 résultats, avec message "Montre 50 sou {{total}} tikè".
13. **Label totaux dans rapports** — quand un filtre par tirage est actif, afficher un label "Total jounen an" au-dessus des StatCards pour que le machann sache que les totaux ne sont pas filtrés.

### F — Résultats tirage

14. **Filtres condensés** — remplacer les 4 contrôles empilés par : chips horizontaux scrollables pour la période (7j, 30j, date custom) + un seul dropdown cascading provider→slot. Objectif : libérer ~150dp de contenu visible.
15. **Période par défaut visible** — sélectionner "7 jou" par défaut au lieu de laisser aucun preset actif.

### G — Profil et settings

16. **Feedback save settings** — ajouter un `CircularProgressIndicator` de 16dp sur le toggle pendant l'appel API, et un bref snackbar "Anregistre!" en succès ou "Echwe" en erreur.
17. **Page Forbidden enrichie** — afficher la raison du blocage (depuis l'API runtime si disponible), et un bouton "Rele admin ou" qui ouvre le numéro de téléphone configuré dans le tenant ou le lien WhatsApp.

### H — Maintenance et qualité

18. **Extraire dialog logout** — créer un `showLogoutConfirmation(BuildContext, WidgetRef)` partagé dans `design_system/components/` et l'appeler depuis les 3 pages qui le dupliquent (change_pin, home, profile).

## Impact

- **App mobile uniquement.** Aucun changement backend — toutes les API existent déjà.
- Pas de nouveaux écrans — modifications sur les écrans existants.
- Fichiers i18n (ht, fr, en) : 3-4 nouvelles clés + corrections hardcode.
- Design system : 1 nouveau composant partagé (`showLogoutConfirmation`), 1 extraction widget (`_DrawCountdown`).
- Fichiers modifiés : `cashier_sell_page.dart` (chips), `cashier_sell_success_page.dart` (navigation + i18n), `cashier_scan_page.dart` (QR + bouton), `cashier_home_page.dart` (timer + vide), `cashier_history_page.dart` (date + pagination), `seller_terminal_stats_page.dart` (label totaux + fallback), `seller_terminal_results_page.dart` (filtres), `seller_terminal_profile_page.dart` (feedback save + logout), `cashier_ticket_detail_page.dart` (date format), `notification_center_page.dart` (date + fallback), `forbidden_page.dart` (raison + contact), `change_pin_page.dart` (logout).

## Non-goals

- Mode sombre (effort significatif, change séparé pour ne pas bloquer les quick wins).
- Mode "vente rapide" fusionnant vérifier+confirmer (nécessite discussion sécurité, change séparé).
- Scan QR / barcode (intégration caméra, change séparé — `cashier-ticket-verification-v1` pourrait le couvrir).
- Annulation de ticket depuis le mobile (décision produit admin-only à valider).
- Export/partage rapports WhatsApp (effort moyen, change séparé).
- Refonte navigation (l'architecture 4 onglets + flux vente est bonne).

## Context packs

- `10-non-negotiables.md`
- `20-flutter-rules.md`

## Near-code references

- `tchalanet-mobile/lib/features/cashier/tickets/presentation/views/cashier_sell_page.dart`
- `tchalanet-mobile/lib/features/cashier/tickets/presentation/views/cashier_sell_success_page.dart`
- `tchalanet-mobile/lib/features/cashier/tickets/presentation/views/cashier_scan_page.dart`
- `tchalanet-mobile/lib/features/cashier/tickets/presentation/views/cashier_history_page.dart`
- `tchalanet-mobile/lib/features/cashier/tickets/presentation/views/cashier_ticket_detail_page.dart`
- `tchalanet-mobile/lib/features/cashier/home/presentation/views/cashier_home_page.dart`
- `tchalanet-mobile/lib/features/cashier/home/presentation/views/seller_terminal_stats_page.dart`
- `tchalanet-mobile/lib/features/cashier/home/presentation/views/seller_terminal_profile_page.dart`
- `tchalanet-mobile/lib/features/draw/presentation/views/seller_terminal_results_page.dart`
- `tchalanet-mobile/lib/features/notifications/presentation/views/notification_center_page.dart`
- `tchalanet-mobile/lib/features/auth/presentation/views/forbidden_page.dart`
- `tchalanet-mobile/lib/features/auth/presentation/views/login_page.dart`
- `tchalanet-mobile/lib/features/auth/presentation/views/change_pin_page.dart`
- `tchalanet-mobile/lib/design_system/components/`
