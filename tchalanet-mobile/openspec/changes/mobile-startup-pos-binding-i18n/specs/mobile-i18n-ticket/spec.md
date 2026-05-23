# Spec: Mobile/backend i18n and ticket document text resolution

## ADDED Requirements

### Requirement: Flutter localizations must handle stable app UI labels
The Flutter app SHALL use Flutter localizations/ARB for stable UI labels owned by the app.

#### Scenario: Stable UI label
- **GIVEN** the app displays a local navigation action such as Settings or Logout
- **WHEN** the locale is `fr-HT`
- **THEN** the app SHALL resolve the label from Flutter localizations when it is not backend-driven

### Requirement: Backend-driven texts must expose keys and fallbacks
Backend-returned actions, blockers, warnings, and notices SHALL include stable keys and fallback text.

#### Scenario: Backend blocker
- **GIVEN** `profile/current` returns `DEVICE_NOT_ENROLLED`
- **WHEN** it includes a blocker
- **THEN** the blocker SHALL include `code`, `messageKey`, `fallbackMessage`, and optional `params`

#### Scenario: Backend action
- **GIVEN** `profile/current` returns action `OPEN_SESSION`
- **WHEN** the action is serialized
- **THEN** it SHALL include `code`, `labelKey`, `fallbackLabel`, and `enabled`

### Requirement: Mobile app must resolve backend keys with fallback
The mobile app SHALL display backend-driven text using a backend dictionary when available and fallback text otherwise.

#### Scenario: Dictionary contains key
- **GIVEN** the app has a backend i18n dictionary containing `seller.blocker.session_closed`
- **WHEN** a blocker references that key
- **THEN** the app SHALL display the dictionary value

#### Scenario: Dictionary missing key
- **GIVEN** the app does not have the referenced backend key
- **WHEN** a blocker includes `fallbackMessage`
- **THEN** the app SHALL display the fallback message

### Requirement: catalog.i18n resolution must be scope-aware
The backend SHALL resolve i18n using `I18nOverrideLevel.GLOBAL` and `I18nOverrideLevel.TENANT` with API scope safeguards.

#### Scenario: Tenant scope resolves global plus tenant
- **GIVEN** a tenant context for tenant A
- **AND** global key `page.home.title=Bienvenue sur Tchalanet`
- **AND** tenant A override `page.home.title=Bienvenue chez Toto`
- **WHEN** `resolveLocale(locale, ctx)` is called
- **THEN** the result SHALL contain `page.home.title=Bienvenue chez Toto`

#### Scenario: Platform scope resolves global only
- **GIVEN** platform scope with super admin visibility
- **AND** tenant A override `page.home.title=Chez A`
- **AND** tenant B override `page.home.title=Chez B`
- **WHEN** `resolveLocale(locale, platformCtx)` is called
- **THEN** the result SHALL include GLOBAL values only
- **AND** SHALL NOT merge tenant A or tenant B overrides implicitly

#### Scenario: Platform explicit tenant preview
- **GIVEN** platform scope
- **AND** tenant A override exists
- **AND** tenant B override exists
- **WHEN** `resolveLocaleForTenant(locale, tenantA)` is called
- **THEN** the result SHALL include GLOBAL plus tenant A values
- **AND** SHALL NOT include tenant B values

### Requirement: PageModel may use tenant-level i18n overrides
PageModel text keys SHALL be resolvable through the same scope-aware i18n catalog.

#### Scenario: Tenant customizes public home title
- **GIVEN** PageModel block references `page.public_home.hero.title`
- **AND** tenant override sets it to `Bienvenue chez Toto`
- **WHEN** the public home model is resolved for that tenant
- **THEN** the text SHALL resolve to `Bienvenue chez Toto`

### Requirement: Tenant/outlet identity data must not be stored as translations
Official tenant and outlet identity fields SHALL remain in tenant/outlet/theme/config domains, not in i18n keys.

#### Scenario: Tenant display name
- **GIVEN** tenant official display name is `Chez Toto`
- **WHEN** a ticket header is assembled
- **THEN** the tenant display name SHALL come from tenant config/branding data
- **AND** SHALL NOT be resolved as an i18n translation key

#### Scenario: Page marketing text
- **GIVEN** a page hero title says `Bienvenue chez Toto`
- **WHEN** it is tenant-specific marketing copy
- **THEN** it MAY be resolved through a tenant-level i18n/PageModel key

### Requirement: Ticket/PDF/print text must be resolved by backend
Ticket, PDF, and printed receipt labels SHALL be resolved by the backend and passed to the renderer as a document model.

#### Scenario: Ticket document renderer
- **GIVEN** a ticket receipt is generated
- **WHEN** the PDF/print renderer receives the document model
- **THEN** it SHALL receive resolved labels
- **AND** SHALL NOT call i18n resolution itself
- **AND** SHALL NOT rely on Flutter translations

### Requirement: Sold ticket must snapshot critical labels
On final sale, the backend SHALL store the ticket language and critical labels required for reprint/audit consistency.

#### Scenario: Reprint after tenant override changes
- **GIVEN** a ticket was sold with `ticket.footer.thank_you=Mèsi`
- **AND** the tenant later changes the override to `Mèsi anpil`
- **WHEN** the old ticket is reprinted
- **THEN** the reprint SHALL use the label snapshot from sale time
- **AND** SHALL NOT silently use the new override

### Requirement: Ticket language must be resolved once for final sale
The backend SHALL resolve ticket language deterministically before final ticket creation.

#### Scenario: Ticket language resolution
- **GIVEN** request language is absent
- **AND** user preferred language is absent
- **AND** outlet default language is `ht-HT`
- **WHEN** sell creates the ticket
- **THEN** ticket language SHALL be `ht-HT`

#### Scenario: Consistent ticket labels
- **GIVEN** ticket language is `ht-HT`
- **WHEN** ticket labels are resolved
- **THEN** all functional ticket labels SHALL use `ht-HT` values or fallback chain
- **AND** SHALL NOT mix arbitrary French/English labels unless they are names/proper nouns
