# Spec — admin-params-ux-v1

## Contexte

La page **Paramèt** actuelle (`/app/admin/company/settings`) affiche un menu de deux liens :
« Paramèt operasyon yo » (runtime) et « Konfigirasyon » (config). Cette structure en deux niveaux
oblige l'admin à naviguer dans une page de configuration à onglets lourde pour modifier des
éléments simples (format papye, livrezon, kalandriye).

## Objectif

Supprimer la hiérarchie Runtime / Config avec ses onglets. Remplacer par :
- Une page d'aperçu **Paramèt** contenant 3 cartes de résumé cliquables.
- 3 pages d'édition séparées, accessibles via le bouton `Modifye` de chaque carte.
- Migration de la section **Rejyon ak lang** (langues) vers `AdminBusinessProfilePage`.
- Suppression de `AdminLimitsSectionComponent` de la page de configuration
  (les limites sont gérées sur la page centrale `/app/admin/limits`).

## Nouvelles routes

| Route | Composant |
|---|---|
| `settings` | `AdminParamsOverviewPage` (nouveau) |
| `settings/receipt` | `AdminReceiptConfigPage` (nouveau) |
| `settings/delivery` | `AdminDeliveryConfigPage` (nouveau) |
| `settings/calendar` | `AdminCalendarConfigPage` (nouveau) |
| `settings/config` | `AdminConfigPage` (conservé, langue uniquement, temp.) |
| `settings/runtime` | `AdminRuntimePage` (conservé pour compat.) |

## Carte 1 — Resi ak enpresyon

Résumé affiché :
- Enpresyon : Aktif / Inaktif
- Papye : 58 mm / 80 mm / A4
- QR kod : Wi / Non

Source : `tenantConfigResource().document.receipt`

## Carte 2 — Livrezon tikè

Résumé affiché :
- SMS : Aktif / Inaktif
- WhatsApp : Aktif / Inaktif
- Imèl : Aktif / Inaktif

Source : `tenantConfigResource().communication.buyerTicketDelivery`

## Carte 3 — Kalandriye vant

Résumé affiché :
- Ouvert pa defo : Wi / Non
- N jou fèmen
- N jou ferye

Source : `tenantConfigResource().rules.businessCalendar`

## Locale (migration différée)

La section langue (langues supportées + langue par défaut) doit migrer vers
`AdminBusinessProfilePage` dans un PR séparé. Pendant la transition, la locale
reste accessible via `settings/config#locale`.

## Suppression

- `AdminLimitsSectionComponent` retiré de `admin-config.page.html` + son import.
- `AdminSettingsPage` (ancien menu) devient code mort — supprimer dans PR suivant.
