# Tasks

## A — Raccourcis d'urgence

- [ ] Créer `BlockNumberQuickDialogComponent` (dialog simplifié : chips numéro + tirage auto OPEN + défauts BLOCK/today).
- [ ] Ajouter Quick Action "Bloquer un numéro" dans la config page-model dashboard.
- [ ] Adapter `QuickActionsWidget` pour supporter handler dialog (pas seulement navigation route).
- [ ] Ajouter item "Bloquer un numéro sur ce tirage" dans le menu contextuel tirage OPEN.

## B — Actions vendeur

- [ ] Exposer bouton "Bloquer" (warn) sur la ligne vendeur ACTIVE dans `seller-terminal-table`.
- [ ] Ajouter barre d'actions en haut page détail vendeur (Bloquer/Débloquer, Reset PIN, Tickets).

## C — Actions inline tirages

- [ ] Ajouter bouton inline "Ouvrir" (SCHEDULED) et "Fermer" (OPEN) sur les lignes tirage.

## D — Résultat tirage

- [ ] Ajouter flow `Pwopoze rezilta` quand le résultat auto manque, sans application définitive libre par le tenant-admin.
- [ ] Retirer bouton "Retry verification" désactivé du drawer résultat.

## E — POS / vente

- [ ] Pré-sélectionner le dernier terminal utilisé dans `pos-sell.page`.
- [ ] Avertir sur les doublons POS au lieu de fusionner silencieusement (toast).

## F — Vérification ticket

- [ ] Rendre le champ terminal optionnel dans `pos-ticket-verify.page`.

## G — Rapports

- [ ] Ajouter tab bar commune entre les 5 pages rapport.
- [ ] Unifier presets de date (Aujourd'hui, 7j, 30j, Ce mois) sur tous les rapports.

## H — Formulaires simplifiés

- [ ] Ajouter descriptions claires des 3 modes maryaj gratis dans `maryaj-offer-panel`.
- [ ] Validation inline des tiers maryaj (remplacer snackbar).
- [ ] Remplacer champ texte heure par time picker dans `draw-channel-config.dialog`.
- [ ] Pré-remplir date depuis clic calendrier dans `admin-business-days.page`.
- [ ] Ajouter bouton "Fermer une plage" (bulk) dans `admin-business-days.page`.

## I — i18n et vocabulaire métier borlette

- [ ] Corriger les strings hardcodées (POS, financials, drawer résultat) → clés i18n.
- [ ] Renommer dans `surface-admin.json` (ht) : "Tablo debò" → "Akèy", "Limit" → "Kontwòl nimewo", "Konfig kanal" → "Orè tiraj yo", "Barèm" → "Pri jwèt yo".
- [ ] Renommer dans `surface-admin.json` (ht) : "Konfigirasyon" → "Règleman", "Paramèt" → "Règ", "Modèl paj" → "Dekorasyon ekran".
- [ ] Garder "Antrepriz mwen" ou valider "Biznis mwen" après test terrain ; ne pas imposer "Bank mwen" sans validation client.
- [ ] Renommer dans `surface-admin.json` (ht) : "Jou travay" → "Jou louvri / fèmen", "Enskripsyon" → "Premye pa".
- [ ] Renommer dans `common.json` (ht) : "Verifye" reste mais ajouter clé "Tcheke tikè" pour la vérification ticket.
- [ ] Appliquer les équivalents dans `surface-admin.json` (fr) : "Limites" → "Contrôles numéro", "Canaux de tirage" → "Horaires de tirage", etc.
- [ ] Vérifier que les clés domaine (Bolèt, Maryaj, Tiraj, Machann, Komisyon) ne sont PAS modifiées — elles sont déjà correctes.

## J — Commissions & création vendeur

- [ ] Ajouter compteur "X vendeurs affectés" dans `set-default-rate.dialog`.
- [ ] Ajouter sidebar sticky de progression sur `seller-terminal-create-form`.

## K — Design pour faible literacy

- [ ] Transformer `QuickActionsWidget` en grille d'icônes 2×3 (icône 32px + label 2 mots).
- [ ] Créer variante simplifiée du `BlockNumberQuickDialog` : bouton rouge + champ numérique + confirme (pas de BLOCK/WARN, pas de checkbox).
- [ ] Remplacer les snackbars Material par composant feedback visuel fort (flash écran + icône + texte court).
- [ ] Mapper les codes erreur backend vers des messages créoles explicatifs (kisa ki pase + kisa pou fè) dans `errors.json` (ht).

## Validation

- [ ] `pnpm exec tsc -p apps/admin-portal/tsconfig.app.json --noEmit`
- [ ] Test manuel des parcours simplifiés sur le portail admin.
