# admin-quick-actions-ux-v1

## Status

Proposed — 2026-08-09

## Why

Audit UX complet du portail admin : 12 parcours analysés, 22 améliorations identifiées. Le portail fonctionne déjà — l'objectif est de simplifier la vie des gestionnaires qui ne connaissent pas les TI, en réduisant la profondeur de navigation, en exposant les actions urgentes, et en éliminant les frictions inutiles.

Constats principaux vérifiés dans le code :

**Profondeur de navigation :**
- Bloquer un numéro = 4 clics (Sidebar "Limites" → Onglet "Numéro" → Carte → Dialog 6 champs)
- Vendre un ticket POS = 3 clics (sélection vendeur obligatoire avant de pouvoir vendre)
- Bloquer un vendeur = 3 clics (caché dans le menu ⋮, alors que "Débloquer" est visible en direct)
- Entrer un résultat = 3 clics + délai 30 min non-bypassable

**Formulaires trop complexes :**
- Bloquer un numéro : 6 décisions pour une action d'urgence (BLOCK/WARN, chips, active, durée, date, preview)
- Maryaj gratis : 3 modes conditionnels (FIXED/PER_PAID_AMOUNT/TIERED) sans descriptions claires, validation par snackbar pas inline
- Création vendeur : 14 champs sans stepper ni progression
- Canaux de tirage : heure en champ texte libre au lieu de time picker

**Incohérences UX :**
- Rapports : 5 pages sans tab bar commune, presets de date seulement sur financier pas sur quotidien
- Jours ouvrables : clic sur un jour du calendrier ouvre un dialog avec champ date texte au lieu de pré-remplir
- Vérification ticket : sélection terminal obligatoire même si le code ticket est globalement unique
- POS : doublons fusionnés silencieusement au lieu d'avertir
- Mélange i18n / hardcode (français et créole mélangés sur le POS, financials, drawer résultat)

**Actions cachées :**
- "Bloquer" vendeur dans le menu ⋮ mais "Débloquer" visible en direct (asymétrie)
- Pas d'actions sur la page détail vendeur (bloquer, reset PIN, tickets)
- Actions lifecycle tirage (Ouvrir/Fermer) seulement dans le menu ⋮
- Bouton "Retry verification" désactivé en permanence dans le drawer résultat (fantôme)

## What

### A — Raccourcis d'urgence (Quick Actions dashboard)

1. **Quick Action "Bloquer un numéro"** sur le dashboard → dialog simplifié : juste le numéro + tirage auto-sélectionné (prochain OPEN). Défauts : BLOCK, today. Lien "Options avancées" vers le dialog complet existant.
2. **"Bloquer un numéro sur ce tirage"** dans le menu contextuel d'un tirage OPEN → même dialog simplifié, tirage pré-rempli.

### B — Actions vendeur accessibles

3. **Bouton "Bloquer" visible** sur la ligne vendeur ACTIVE (symétrique au "Débloquer" déjà exposé pour BLOCKED). Couleur warn.
4. **Barre d'actions page détail vendeur** : Bloquer/Débloquer, Reset PIN, Voir tickets. Conditionnel selon statut.

### C — Actions inline tirages

5. **Boutons inline sur les lignes tirage** : "Ouvrir" pour SCHEDULED, "Fermer" pour OPEN. Les autres actions lifecycle restent dans le menu.

### D — Résultat tirage

6. **Résultat manquant** : proposer un flow `Pwopoze rezilta` quand le résultat automatique
   manque, sans appliquer définitivement un résultat libre côté tenant-admin.
7. **Retirer le bouton "Retry verification"** désactivé (placeholder non implémenté).

### E — POS / Vente de ticket

8. **Pré-sélectionner le dernier terminal utilisé** au lieu de forcer la sélection à chaque fois.
9. **Avertir sur les doublons** au lieu de fusionner silencieusement les mises (toast "Numéro déjà présent, mise cumulée").

### F — Vérification ticket

10. **Terminal optionnel** : si le code ticket est globalement unique, rendre le champ terminal optionnel ou le retirer.

### G — Rapports cohérents

11. **Tab bar entre les 5 rapports** (overview, quotidien, vendeurs, tirages, financier) pour naviguer sans repasser par le sidebar.
12. **Presets de date unifiés** sur tous les rapports : Aujourd'hui, 7 jours, 30 jours, Ce mois.

### H — Formulaires simplifiés

13. **Descriptions claires des modes maryaj gratis** : FIXED → "Chaque acheteur reçoit X maryaj", PER_PAID_AMOUNT → "1 maryaj par X gourdes", TIERED → "Plus il dépense, plus il reçoit".
14. **Validation inline des tiers maryaj** au lieu de snackbar (feedback immédiat sur chevauchement de plages).
15. **Time picker** pour les heures de tirage dans la config canaux (remplacer le champ texte libre).
16. **Pré-remplir la date** depuis le clic calendrier dans la page jours ouvrables.
17. **Bulk fermeture jours ouvrables** : bouton "Fermer une plage" avec 2 date pickers (du/au).

### I — i18n et vocabulaire

18. **Corriger le mélange i18n / hardcode** sur POS, financials, drawer résultat.
19. **Renommer "Limites" → "Kontwòl nimewo"** dans le sidebar et les i18n (le terme métier borlette, pas le jargon IT).
20. **Vocabulaire sidebar adapté au terrain** — basé sur l'analyse des plateformes réelles (Pakapala/Lesly Center, Paryaj Lakay, Boulpam, MG Borlette Haiti) :
    - "Tablo debò" → "Akèy" (terme universel, utilisé par Paryaj Lakay)
    - "Konfig kanal" → "Orè tiraj yo" (ce sont des horaires de tirage, pas des canaux)
    - "Limit" → "Kontwòl nimewo" (l'action métier, pas le système)
    - "Barèm" → "Pri jwèt yo" (prix des jeux)
    - "Modèl paj" → "Dekorasyon ekran"
    - "Konfigirasyon" → "Règleman"
    - "Paramèt" → "Règ"
    - "Jou travay" → "Jou louvri / fèmen"
    - "Antrepriz mwen" → garder ou valider "Biznis mwen" après test terrain (ne pas imposer
      "Bank mwen" tant que tous les clients ne se reconnaissent pas dans ce mot)
    - "Enskripsyon" → "Premye pa"
    - "Verifye yon tikè" → "Tcheke tikè" (créole oral)
    - Les termes domaine (Bolèt, Maryaj, Tiraj, Rezilta, Machann, Komisyon) sont déjà corrects — ne pas toucher.

### J — Commissions & création vendeur

21. **Compteur "X vendeurs affectés"** sur le dialog de commission par défaut.
22. **Sidebar sticky de progression** sur le formulaire création vendeur (sommaire navigable des 5 sections avec indicateurs de complétion).

### K — Design pour faible literacy (opérateurs terrain)

23. **Quick Actions en grille d'icônes** : transformer les liens textuels du dashboard en cartes 2×3 avec icône dominante 32px + label 2 mots max (pattern MonCash/NatCash que les opérateurs connaissent).
24. **Dialog blocage simplifié : icône + 2 gestes** — bouton rouge "Bloke" + champ numérique + "Konfime". Aucun choix BLOCK/WARN, aucune checkbox, défauts intelligents (BLOCK, today, prochain tirage OPEN). Lien "Plis opsyon" pour le dialog complet.
25. **Feedback visuel fort** : remplacer les snackbars Material par un flash écran plein (vert ✅ succès / rouge ❌ erreur) + vibration mobile + texte court en créole. Impossible à rater.
26. **Erreurs explicatives en créole** : chaque erreur backend doit dire *kisa ki pase* + *kisa pou fè*. Exemple : "Nimewo sa a deja bloke sou tiraj sa a. Ou vle wè règ la?" au lieu de "LIMIT_ASSIGNMENT_CONFLICT".

## Impact

- **Web admin uniquement.** Aucun changement backend — toutes les API existent déjà.
- Nouveau composant : `BlockNumberQuickDialog` (dialog simplifié, ~150 lignes).
- Composants modifiés : `QuickActionsWidget`, `seller-terminal-table`, page détail vendeur, `admin-generated-draws.page`, `generated-draws-table`, `draw-result-drawer`, `pos-sell.page`, `pos-ticket-verify.page`, rapports (5 pages), `upsert-limit-dialog` (labels maryaj), `draw-channel-config.dialog`, `admin-business-days.page`, `admin-seller-configuration`, `seller-terminal-create-form`.
- Fichiers i18n (fr, ht) : nouvelles clés + corrections hardcode.

## Non-goals

- Command palette / recherche globale (effort élevé, change séparé).
- Scan barcode pour vérification ticket (intégration caméra, hors scope).
- Application définitive libre d'un résultat par le tenant-admin sans confirmation Tchalanet.
- Modification du système de limites backend.
- Stepper wizard pour la création vendeur (la sidebar sticky de progression est une alternative plus légère).
- Refonte de la checklist setup en wizard inline.

## Context packs

- `10-non-negotiables.md`
- `30-frontend-rules.md`

## Near-code references

- `tchalanet-web/libs/widgets/src/lib/widgets/surface-admin/quick-actions/quick-actions.widget.ts`
- `tchalanet-web/apps/admin-portal/src/app/features/limits/components/upsert-limit-dialog/`
- `tchalanet-web/apps/admin-portal/src/app/features/seller-terminals/`
- `tchalanet-web/apps/admin-portal/src/app/features/draws/`
- `tchalanet-web/apps/admin-portal/src/app/features/pos/`
- `tchalanet-web/apps/admin-portal/src/app/features/reports/`
- `tchalanet-web/apps/admin-portal/src/app/features/promotions/pages/maryaj-gratis/`
- `tchalanet-web/apps/admin-portal/src/app/features/draw-channels/`
- `tchalanet-web/apps/admin-portal/src/app/features/business-days/`
- `tchalanet-web/apps/admin-portal/src/app/features/seller-configuration/`
- `tchalanet-web/libs/web/shell/src/lib/private-shell/private-navigation.model.ts`
- `tchalanet-web/libs/shared-assets/public/assets/i18n/`
