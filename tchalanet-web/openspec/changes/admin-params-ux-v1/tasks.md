# Tasks — admin-params-ux-v1

Checkpoint obligatoire : lire ce fichier en début de session et cocher en temps réel (`[ ]` → `[x]`).

---

## 1. Routes

- [x] Créer la route `settings` → `AdminParamsOverviewPage`
- [x] Créer la route `settings/receipt` → `AdminReceiptConfigPage`
- [x] Créer la route `settings/delivery` → `AdminDeliveryConfigPage`
- [x] Créer la route `settings/calendar` → `AdminCalendarConfigPage`
- [x] Conserver `settings/config` (AdminConfigPage) pour compat locale
- [x] Conserver `settings/runtime` (AdminRuntimePage) pour compat

---

## 2. Page d'aperçu — `AdminParamsOverviewPage`

- [x] Remplacer l'ancien menu (2 liens) par 3 cartes de résumé
- [x] Carte Resi : enabled, paperSize, qrCode
- [x] Carte Livrezon : SMS / WhatsApp / Imèl enabled
- [x] Carte Kalandriye : defaultOpen, closedWeekdayCount, holidayCount
- [x] Bouton `Modifye` sur chaque carte → routerLink vers l'éditeur
- [x] Lien de retour (backRoute) : setup ou business-profile selon `?from=setup`
- [x] Support queryParams `{ from: 'setup' }` propagé aux liens Modifye

---

## 3. Pages d'édition

### 3.1 `AdminReceiptConfigPage`

- [x] Extraire la logique de l'onglet Receipt de `AdminConfigPage`
- [x] Formulaire : enabled, headerMessage, footerMessage, defaultPaperSize, showQrCode
- [x] Mutation : `updateSettingsSection('document', ...)`
- [x] Readiness banner (section 'print')
- [x] Bouton retour → `settings` (ou setup)

### 3.2 `AdminDeliveryConfigPage`

- [x] Extraire la logique de l'onglet Communication de `AdminConfigPage`
- [x] Formulaire : smsEnabled/Amount/Currency/PaidBy, whatsapp..., email...
- [x] Mutation : `updateSettingsSection('communication', ...)`
- [x] Readiness banner (section 'send')
- [x] Layout mobile : `config-page__channel-fields` répond bien en 1 col

### 3.3 `AdminCalendarConfigPage`

- [x] Extraire la logique de l'onglet Calendar de `AdminConfigPage`
- [x] Formulaire : defaultOpen, closedWeekdays, holidayTemplateKeys, custom holidays
- [x] Mutation : `updateSettingsSection('rules', ...)`
- [x] Readiness banner (section 'calendar')
- [x] Lien « Jou eksepsyon yo » → `/app/admin/business-days`

---

## 4. Nettoyage `AdminConfigPage`

- [x] Retirer `AdminLimitsSectionComponent` du template
- [x] Retirer `AdminLimitsSectionComponent` de l'array `imports` du composant
- [ ] Retirer l'onglet locale (après migration vers BusinessProfile)

---

## 5. Migration Locale → BusinessProfile (PR suivant)

- [ ] Ajouter `TenantParametersApiService` dans `AdminBusinessProfilePage`
- [ ] Ajouter section "Rejyon ak lang" avec localeForm (supportedLanguages, fallbackLanguage)
- [ ] Mutation save locale : `updateSettingsSection('locale', ...)`
- [ ] Effacement onglet locale dans `AdminConfigPage`
- [ ] i18n pour la nouvelle section (HT/FR/EN)

---

## 6. i18n

- [x] Ajouter clés `admin.settings.overview.*` (HT/FR/EN)
- [ ] Ajouter clés `admin.businessProfile.locale.*` (HT/FR/EN) — tâche 5

---

## 7. Suppression (PR suivant)

- [ ] Supprimer `AdminSettingsPage` (admin-settings.page.ts/.html/.scss)
- [ ] Supprimer `AdminRuntimePage` si non utilisé ailleurs

---

## 8. Gates Before Done

- [ ] `pnpm nx lint admin-portal` — 0 erreurs
- [ ] `pnpm nx test admin-portal` — tests verts
- [ ] `pnpm nx build admin-portal` — build clean
- [ ] Validation 360 px mobile sur staging
