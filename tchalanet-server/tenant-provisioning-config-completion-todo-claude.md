# TODO technique — Compléter tenant : provisioning / configuration / cycle de vie

**Statut :** brouillon technique (pas un OpenSpec change)
**Contexte produit :** voir `admin-tenant-next-steps-claude.md` (décisions V0 déjà actées — ne pas les recontredire sans validation explicite).
**Généré :** 2026-07-03, à partir d'une relecture complète du code actuel (backend `tchalanet-server`, web `platform-portal` + `admin-portal`).

Convention des cases : `[ ]` à faire, `[~]` partiellement fait / à vérifier, `[x]` déjà fait (listé pour contexte, ne pas refaire).

---

## A. Backend — Provisioning (`TenantProvisioningOrchestrator`)

- [x] Création tenant + admin initial + readiness recalculée
- [x] Seeding catalog profil `DEFAULT_HAITI_LOTTERY`/`DEMO` (jeux, odds, draw channels, limites par défaut)
- [ ] **Auto-appliquer un plan par défaut à la création.** Aucun plan n'est attaché aujourd'hui ; `POST /platform/subscriptions/{id}/apply` existe (`PlatformSubscriptionController`) mais n'est jamais appelé par l'orchestrateur. Sans plan, `EntitlementService.requireLimitAtMost` échoue dès la création d'un `SellerTerminal`.
  - Décision requise : plan par défaut fixe (ex. `DEMO` pendant onboarding interne, `STARTER` en production) vs. champ `planCode` optionnel dans `TenantProvisioningRequest` avec fallback.
  - Si 4xx attendu en l'absence de limite plutôt qu'un 500 interne (`ProblemRest.internal`), corriger `EntitlementService` en parallèle (voir section C).
  - Note : `Outlet` n'existe plus comme entité (confirmé — aucune `class Outlet`/`OutletJpaEntity`). L'acteur bloqué par l'absence de plan est `SellerTerminal`, pas "outlet". Corriger la terminologie partout ci-dessous.
- [ ] **Nettoyer `defaultCommissionRate` hors-commande.** Actuellement persisté via `tenantPersistence.updateDefaultCommissionRate(...)` après `createTenant(...)` (`TenantProvisioningOrchestrator.java:82-86`, marqué `//todo pass dans create tenant`). Ajouter le champ à `CreateTenantRequest` et le faire porter par `TenantConfigService.createTenant` en une seule opération.
- [ ] **Preview vs provision : garder synchronisés.** `preview()` et `provision()` dérivent chacun `includedDomains`/`expectedReadinessSections`/`nextSteps` séparément — si un plan par défaut est ajouté, mettre à jour les deux chemins (`TenantProvisioningOrchestrator.java:59-66` et `136-150`) pour que le preview annonce fidèlement ce qui sera fait.
- [ ] **Re-tester `GET /platform/plans`.** Signalé 500 le 2026-06-01 (mémoire), code actuel (`PlanAdminController.list()`) semble correct — vérifier en environnement réel avant de considérer le bug clos ou de bâtir l'UI d'application de plan dessus.

## B. Backend — Config tenant post-création

- [x] `/admin/tenant` (tenant-admin) : GET/PUT identité, GET/PUT adresse
- [x] `/admin/tenant-config` (tenant-admin) : GET settings/communication/document, PUT `internal-settings` (remplacement complet)
- [x] `/platform/tenants/{id}` (super-admin) : PUT identité (name/timezone/currency uniquement)
- [ ] **Décider si le super-admin doit pouvoir éditer adresse/settings, ou rester lecture-seule** (le doc produit dit lecture-seule V0 côté super-admin — confirmer que c'est toujours voulu avant d'ajouter des endpoints `/platform/tenants/{id}/address` ou `/internal-settings`). Ne pas construire ces endpoints sans validation produit.
- [ ] **`PUT /admin/tenant-config/internal-settings` fait un remplacement complet du JSON**, pas de merge partiel. Risque : un tenant-admin qui PATCH juste `document.receipt` écrase `communication`/`rules`/`locale` s'il ne renvoie pas l'objet complet. Décider : merge côté serveur (deep-merge comme au provisioning) ou endpoints par section (`/internal-settings/communication`, `/internal-settings/receipt`, etc.) — cohérent avec `getCommunication`/`getDocument` déjà découpés en lecture.
- [ ] **`updateTenantIdentity` ne couvre pas `type` ni `code`.** Si un changement de type post-création doit être possible (ex. AMBULANT → BORLETTE en cas de croissance), ajouter le champ + règles de transition. Sinon documenter explicitement que `type`/`code` sont immuables.

## C. Backend — Plans / Abonnements / Droits d'usage

- [x] `POST /platform/subscriptions/{tenantId}/apply` et `/change` existent (`PlatformSubscriptionController`)
- [x] `GET /platform/subscriptions/{tenantId}` (resolve) existe
- [x] **Séparation V0 entitlement/permission clarifiée dans le code :**
  - Entitlement = feature/quota porté par le plan du tenant (`billing_plan.features_json`, `billing_plan.limits_json`, `@RequiredFeature`, `@RequiredQuota`).
  - Permission = autorisation de l'utilisateur/acteur (`permission`, `role_permission`, `@RequiresPermission`, `hasPermission`).
  - Pour une action admin sensible, il peut falloir les deux : exemple création seller-terminal = permission `seller_terminal.manage` + quota `limits.seller_terminals.max`.
- [x] **Seeds plan alignés V0 :** quotas `limits.admin_users.max`, `limits.seller_terminals.max`, `limits.draw_channels.max`, `limits.promotion_rules.max`; suppression des quotas `outlets`, `users`, `terminals`, `mobile_devices`, `offline`, `exports` dans `V201`. Le quota mobile sera réintroduit quand une table/usage provider mobile device existe.
- [x] **DEMO large pour onboarding/test :** admin users `9999`, seller terminals `9999`, draw channels `999`, promotion rules `999`; features admin/promotions/theme incluses. Le quota mobile device reste volontairement absent tant qu'il n'a pas de table/usage provider.
- [x] **Rôles V0 réduits aux humains admin :** plus de seed `app_role` pour cashier/opérateur/system, plus de user local `cashier`, plus de constantes `TchRole` associées. Les surfaces `CASHIER_WEB`/i18n restent des noms legacy d'écran, pas des rôles. Les rôles humains V0 sont `TENANT_OWNER`, `TENANT_ADMIN`, `SUPER_ADMIN`; les batch/schedulers restent `TchActorType.SYSTEM`, pas un rôle.
- [x] **Permissions V0 consolidées :** `V202` ne seed que les rôles `SUPER_ADMIN`, `TENANT_OWNER`, `TENANT_ADMIN`; le catalogue inclut les permissions admin réellement utilisées par les guards (`user.*`, `role.*`, `seller_terminal.*`, `draw.*`, `draw_result.*`, `draw_channel.*`, `ticket.*`, `tenant.*`, `theme.*`, `archive.*`, etc.) et les grants couvrent owner/admin sans réintroduire cashier/opérateur/system. Les permissions self POS/seller-terminal (`seller_terminal.me.read`, `ticket.read_own`, etc.) restent injectées par l'acteur seller-terminal, pas grantées aux rôles humains.
- [x] **HT_NUMERO retiré :** plus de seed game ni provisioning default.
- [x] **Quota enforcement actif :** création admin tenant vérifie `limits.admin_users.max`; création seller-terminal vérifie `limits.seller_terminals.max`.
- [ ] **Pas de listener automatique** sur la création de tenant pour appliquer un plan (voir A). Décider où vit cette logique : dans l'orchestrateur (appel direct à `ApplyTenantPlanCommand`) ou via un `@EventListener` sur `TenantStatusChangedEvent`/tenant-created — privilégier l'orchestrateur pour rester explicite (cohérent avec "hard rule: calls owning domain APIs only" déjà en commentaire de classe).
- [ ] **Erreur "entitlement manquant" doit être un 4xx métier, pas un 500.** Vérifier `EntitlementService.requireLimitAtMost` — actuellement lève `ProblemRest.internal`. Un tenant sans plan qui tente de créer un `SellerTerminal` doit recevoir un message actionnable ("Aucun plan actif — appliquez un plan avant de créer des seller-terminals"), pas une erreur interne.
- [ ] **Droits d'usage (entitlements) : pas d'endpoint de lecture dédié pour l'UI tenant-detail.** Vérifier s'il existe une query pour lister les features/limites effectives d'un tenant (probablement dérivable de `SubscriptionView` + `PlanView.limits`) ; sinon exposer une vue agrégée `GET /platform/tenants/{id}/entitlements`.
- [ ] **Quota draw channels à brancher plus tard.** La clé `limits.draw_channels.max` existe dans les plans, mais la création actuelle de draw channel est un endpoint platform super-admin (`/platform/draw-channels`). Appliquer `@RequiredQuota` seulement quand un endpoint tenant-admin crée/configure des channels tenant-scoped.
- [ ] **Thème/reçus V0.** Thème default toujours disponible; modification de preset/settings gated par `theme.preset_selection` + permission `theme.manage`. Header/footer reçu restent defaults. Ne pas ajouter de feature reçu tant qu'un endpoint de customization reçu n'est pas découpé et réellement annoté.

## D. Web `platform-portal` (super-admin) — Détail tenant

Fichier : `apps/platform-portal/src/app/features/platform/tenants/pages/detail/platform-tenant-detail.page.{ts,html}`

- [x] Onglet Aperçu (identité, adresse en lecture, commission, profil)
- [x] Onglet Administrateurs (liste, lien création)
- [x] Onglet Configuration (locale/calendrier/communication/reçu, lecture seule)
- [x] Activate/Suspend
- [x] **Onglet Abonnement** — branché sur `GET /platform/subscriptions/{tenantId}`, plan/statut/dates, action Appliquer/Changer de plan.
- [x] **Onglet Droits d'usage** — nouvel endpoint `GET /platform/entitlements/{tenantId}` (`PlatformEntitlementController`, pass-through vers `EntitlementApi.getSnapshot`, déjà existant) ; tab câblé (`platform-tenant-detail.page.ts`) affichant plan/abonnement actif/résolu-le + tables génériques features/limites (clé/valeur, pas de libellés codés en dur vu que `EntitlementKeys` semble désynchronisé des vraies clés de plan V0). Pas de vérif E2E possible (pas de backend démarrable dans ce bac à sable).
- [x] **Onglet Seller-terminals** — aucun backend nécessaire : réutilise `PlatformRecipientSellerTerminalsApi` (déjà existant, `/admin/seller-terminals` + header `asTenantAdmin`) ; nouveau composant `tenant-seller-terminals-table` + pagination `tch-pagination`.
- [x] **Onglet Audit** — aucun backend nécessaire : réutilise `PlatformAuditApi.listAuditEvents({ tenantId })` (déjà existant, `GET /platform/audit/logs`) ; nouveau composant `tenant-audit-table` + pagination. Filtres/expand/purge de la page audit complète volontairement omis (tenant déjà fixé, scope tab minimal).
- [ ] **Bouton Archiver absent du détail tenant** (existe côté `TenantAdminController.archive` backend, mais je n'ai pas vu d'action correspondante dans `platform-tenant-detail.page.ts` — seulement `activate`/`suspend`). Vérifier et ajouter si manquant.
- [ ] **"Accéder comme admin" (support access)** — `TenantAdminController.startAdminAccess`/`stopAdminAccess` existent côté backend ; vérifier qu'un CTA est bien câblé dans le header du détail tenant (spec §11.3).

## E. Web `admin-portal` (tenant-admin) — Onboarding / configuration opérationnelle

- [x] Page `setup/pages/complete-config` avec checklist (`REQUIRED_SETUP_SECTION_IDS`, `blockingSteps`)
- [ ] **Vérifier la couverture complète de la checklist attendue** (spec §20.4/§20.5) : `ADDRESS`, `DOCUMENT_RECEIPT`, `DRAW_CHANNELS`, `SELLER_TERMINALS`, `LIMITS`, `ODDS`. Auditer `admin-complete-tenant-config.page.ts` contre cette liste — confirmer qu'aucune étape n'a été oubliée ou renommée sans mise à jour du backend readiness (`TenantReadinessAssembler`).
- [ ] **Édition settings par section** dépend de B (si on découpe l'update en sous-endpoints, adapter le formulaire pour ne poster que la section modifiée au lieu du blob complet).

## F. Vérification / tests

- [ ] E2E : provisionner un tenant `DEFAULT_HAITI_LOTTERY` sans plan → confirmer le comportement actuel (500 ou succès partiel) avant de coder le fix du point A/C.
- [ ] E2E : provisionner puis appliquer un plan manuellement → confirmer que la création d'un `SellerTerminal` fonctionne (référence : `test_tenant_no_seed_onboarding.py`, à étendre plutôt qu'un nouveau fichier).
- [ ] Web : cliquer sur chaque onglet placeholder du détail tenant après implémentation, vérifier `TchErrorPanel`/`TchNotice` sur erreurs backend (pas de `[object Object]`, règle déjà actée dans `admin-tenant-next-steps-claude.md` §22).
- [ ] Re-tester `GET /platform/plans` en live (voir A) et documenter le résultat dans la mémoire de session si le bug persiste.

---

## Ordre suggéré (impact métier d'abord)

1. **C + A.1** — plan par défaut au provisioning + 4xx propre (débloque la vente réelle, c'est LE bloquant métier restant).
2. **A.2** — `defaultCommissionRate` dans la commande de création (dette technique rapide).
3. **D.Abonnement + D.Droits d'usage** — rendre visible côté super-admin ce que C vient de rendre fonctionnel.
4. **D.Seller-terminals + D.Audit** — complète le détail tenant.
5. **B** — décision produit sur l'édition super-admin adresse/settings (à trancher avec l'utilisateur, pas à coder par défaut).
6. **E** — audit de la checklist onboarding tenant-admin (probablement déjà correct, vérification rapide).
