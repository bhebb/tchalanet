# Tasks

## A — Flux de vente

- [x] Remplacer `GestureDetector` par `InkWell` sur le widget `_Chip` dans `cashier_sell_page.dart` (~ligne 1609). Conserver `minHeight: 48`.
- [x] Changer navigation "Nouveau ticket" de `/pos` vers `/sell` avec `extra: {'drawId': currentDrawId}` dans `cashier_sell_success_page.dart`.
- [x] Remplacer `width: 112` / `width: 152` par `Flexible` ou `LayoutBuilder` sur les champs de sélection numérique dans `cashier_sell_page.dart`.

## B — Corrections localisation

- [x] Remplacer `'Copier code'` (ligne 197 de `cashier_sell_success_page.dart`) par `translations.translate('pos.sale_completion.copy_code')`.
- [x] Ajouter clé `pos.sale_completion.copy_code` dans les fichiers i18n ht ("Kopye kòd"), fr ("Copier code"), en ("Copy code").
- [x] Retirer `fallback: 'Tout li'` dans `notification_center_page.dart` — vérifier que la clé `notifications.center.mark_all_read` existe dans les 3 locales.
- [x] Retirer `fallback: 'Tiraj'` dans `seller_terminal_stats_page.dart` — vérifier que la clé `pos.reports.draw` existe dans les 3 locales.
- [x] Remplacer `_fmtDateTime` dans `cashier_ticket_detail_page.dart` par `MaterialLocalizations.formatMediumDate` + `formatTimeOfDay`.
- [x] Remplacer le format date hardcodé dans `notification_center_page.dart` par le même pattern localisé.

## C — Scan / vérification ticket

- [x] Retirer le container 160dp QR placeholder et le texte invite dans `cashier_scan_page.dart`. Garder uniquement le champ texte.
- [x] Désactiver bouton "Verifye" quand le champ code est vide : `onPressed: code.trim().isEmpty ? null : _verify`.

## D — Dashboard améliorations

- [x] Remplacer `SizedBox.shrink()` en erreur/vide dans la section "dernier ticket" de `cashier_home_page.dart` par un `FeedbackState(compact: true)` avec message traduit.
- [x] Extraire le `Timer.periodic(1s)` et `_now` dans un widget `_DrawCountdown` avec son propre `State` pour isoler les rebuilds du countdown.

## E — Historique et rapports

- [x] Ajouter option "Lòt dat" (date picker) au `SegmentedButton` de `cashier_history_page.dart`.
- [x] Ajouter bouton "Wè plis" en bas de la liste tickets quand `tickets.length >= 50`, avec message indiquant la troncature.
- [x] Afficher label "Total jounen an" au-dessus des StatCards dans `seller_terminal_stats_page.dart` quand un filtre par tirage est actif.

## F — Résultats tirage

- [x] Condenser les filtres de `seller_terminal_results_page.dart` : chips horizontaux scrollables pour la période + dropdown cascading unique provider→slot.
- [x] Sélectionner le preset "7 jou" par défaut au lieu de laisser aucun preset actif.

## G — Profil et settings

- [x] Ajouter `CircularProgressIndicator` 16dp sur le toggle pendant l'appel `_updateSettings` dans `seller_terminal_profile_page.dart`, et snackbar succès/erreur.
- [x] Enrichir `forbidden_page.dart` : afficher la raison de blocage depuis l'API runtime + bouton contact admin (téléphone ou WhatsApp).

## H — Maintenance

- [x] Créer `showLogoutConfirmation(BuildContext, WidgetRef)` dans `design_system/components/`.
- [x] Remplacer les 3 copies du dialog logout (change_pin_page, cashier_home_page, seller_terminal_profile_page) par l'appel partagé.

## Validation

- [x] `flutter analyze` sans erreurs
- [ ] Test manuel des parcours sur émulateur Android (360×720 pour Sunmi V2)
- [x] Vérifier les 3 locales (ht, fr, en) sur les écrans modifiés
