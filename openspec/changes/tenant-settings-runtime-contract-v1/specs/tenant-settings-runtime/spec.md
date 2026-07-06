## ADDED Requirements

### Requirement: Tenant Settings Ownership Is Layered

Tenant settings SHALL be persisted, validated, and projected by the platform tenant capability,
while tenant-admin web endpoints SHALL remain thin feature/BFF endpoints.

#### Scenario: A setting is added or exposed

- **GIVEN** a tenant setting is proposed for admin edit
- **WHEN** the setting is added to a backend DTO, web form, or mobile/runtime contract
- **THEN** the proposal identifies the runtime, print, fee, calendar, or reporting consumer
- **AND** fields without a proven consumer are treated as compatibility fields or hidden fields.

#### Scenario: Tenant admin saves settings

- **GIVEN** a tenant admin submits a settings change from `/app/admin/settings/config`
- **WHEN** the backend handles the request
- **THEN** the `/admin/*` controller only resolves context, checks permissions, and delegates
- **AND** `platform.tenant` validates and persists the canonical tenant settings JSON.

### Requirement: Settings Saves Preserve Canonical Hidden Fields

Admin settings updates SHALL preserve canonical settings fields that are not editable in the
current form.

#### Scenario: Admin web reads persisted tenant settings

- **GIVEN** a tenant has persisted settings JSON created by provisioning or migration
- **WHEN** admin web reads all settings or one settings section
- **THEN** backend returns the persisted settings document or persisted section
- **AND** backend does not re-merge current classpath defaults into existing tenant settings
- **AND** clients do not synthesize missing sibling sections locally.

#### Scenario: Tenant dashboard carries runtime settings

- **GIVEN** a tenant admin loads the tenant dashboard
- **WHEN** backend resolves the dashboard PageModel response
- **THEN** the response carries a non-rendered `tenant.settings` dynamic payload containing
  canonical tenant runtime columns, supported values, and the persisted settings JSON
- **AND** admin web saves that payload into the global runtime settings store for later
  tenant-scoped forms.

#### Scenario: Admin settings forms use global tenant selectors

- **GIVEN** admin web has tenant settings in the global runtime settings store
- **WHEN** a tenant-scoped form renders language or currency choices
- **THEN** it uses global selectors for supported languages and supported currencies
- **AND** it does not maintain independent hardcoded choice lists in the form component.

#### Scenario: Super admin accesses tenant admin space

- **GIVEN** a super admin is acting in tenant admin space through an effective tenant override
- **WHEN** admin web calls a tenant-scoped `/admin/*` or `/tenant/*` API
- **THEN** backend reads and writes the tenant-owned resource for the effective tenant from
  request context
- **AND** admin web does not send a separate tenant id from form state.

#### Scenario: Existing tenant settings are incomplete

- **GIVEN** an older tenant is missing a required persisted settings section
- **WHEN** readiness or admin setup evaluates tenant settings
- **THEN** the missing persisted data is reported as incomplete or migration-required
- **AND** runtime behavior is not silently changed by applying newer classpath defaults at read time.

#### Scenario: Receipt settings are saved from admin web

- **GIVEN** the persisted receipt config contains a canonical template key
- **WHEN** admin web saves receipt messages, paper size, template/QR, and visibility options
- **THEN** the backend persists the edited values
- **AND** the receipt template key is not removed or blanked by the save
- **AND** tenant display name is not treated as receipt-only configuration.

#### Scenario: Locale policy is saved from admin web

- **GIVEN** the persisted locale config contains supported languages and a fallback chain
- **WHEN** admin web saves locale policy fields
- **THEN** the backend persists the edited policy values
- **AND** supported languages remain present and valid
- **AND** tenant default language/default locale are not overwritten from duplicate JSON fields.

### Requirement: Tenant Locale Defaults Are Column-Owned

Tenant default language and default locale SHALL use `tenant.default_language` and
`tenant.default_locale` as their canonical source. Tenant config JSON SHALL NOT introduce an
independent source of truth for those same values.

#### Scenario: Runtime resolves tenant locale defaults

- **GIVEN** a tenant has `default_language` and `default_locale` columns
- **WHEN** runtime bootstrap, tenant runtime view, or a locale hot path resolves defaults
- **THEN** it uses the tenant columns
- **AND** it does not require parsing `tenant.config.locale.defaultLanguage` or
  `tenant.config.locale.defaultLocale`.

#### Scenario: Legacy JSON duplicate defaults are ignored

- **GIVEN** existing tenant JSON contains `locale.defaultLanguage` or `locale.defaultLocale`
- **WHEN** locale policy is validated or saved
- **THEN** those JSON values are ignored as legacy data
- **AND** they cannot diverge from or overwrite the tenant columns as an independent source.

### Requirement: Tenant Supported Values Drive Tenant-Scoped Forms

Supported languages and currency choices SHALL be provided by tenant/platform runtime contracts
and reused by tenant-scoped forms instead of being hardcoded independently in each client.

#### Scenario: Admin creates a user or seller-terminal with a language

- **GIVEN** a tenant has a supported language policy
- **WHEN** admin web, platform web, or mobile needs a language picker for an admin, seller,
  seller-terminal, print, or send flow
- **THEN** the picker options come from the tenant-supported language contract
- **AND** an unsupported hardcoded language cannot be submitted as if it were tenant-valid.

#### Scenario: Admin configures a tenant money value

- **GIVEN** a tenant has canonical currency or supported currency values
- **WHEN** an admin configures send fees or another tenant-scoped money value
- **THEN** the currency options come from the backend tenant/platform contract
- **AND** clients do not maintain their own divergent currency allow-lists.

### Requirement: Tenant Settings UI Is Ordered By Operational Usage

Tenant settings SHALL prioritize operational configuration before default/runtime values.

#### Scenario: Tenant admin opens settings

- **GIVEN** a tenant admin opens the tenant settings surface
- **WHEN** the settings sections are rendered
- **THEN** document/print settings are shown before send options
- **AND** send options are shown before business calendar settings
- **AND** defaults and allowed-value policy settings are shown last.

### Requirement: Tenant Display Name Is Canonical Tenant Identity

Tenant display name SHALL be canonical tenant or brand identity. It SHALL be initialized from
tenant code during provisioning and SHALL be editable by the tenant after provisioning. Receipt
header and footer text SHALL remain document-print configuration unless another consumer is
proven.

#### Scenario: Tenant is provisioned

- **GIVEN** a tenant is provisioned with code `chez-toto`
- **WHEN** the tenant identity is created
- **THEN** canonical display name is initialized from the tenant code
- **AND** receipt config is not the canonical owner of display name.

#### Scenario: Tenant edits display name

- **GIVEN** a provisioned tenant has a canonical display name
- **WHEN** the tenant changes the display name
- **THEN** the change is saved through the tenant/brand identity contract
- **AND** receipts, reports, setup readiness, and summaries read the canonical display name.

#### Scenario: Legacy receipt display name exists

- **GIVEN** existing receipt config contains `document.receipt.displayName`
- **WHEN** the field is saved
- **THEN** V1 admin settings do not write it back
- **AND** runtime, print, setup readiness, and summaries read `tenant.display_name`.

#### Scenario: Receipt header and footer are edited

- **GIVEN** header and footer messages are receipt-specific free text
- **WHEN** tenant admin saves receipt print settings
- **THEN** those messages remain in document-print configuration
- **AND** they are not treated as canonical tenant identity.

### Requirement: Setup Settings Readiness Is Backend-Owned

The global setup settings card SHALL derive its status from backend readiness, using a named
settings readiness rule.

#### Scenario: Setup reads readiness from backend service

- **GIVEN** admin web needs to render the settings setup card
- **WHEN** it requests setup readiness
- **THEN** backend evaluates settings through the tenant settings readiness service
- **AND** the response contains stable section status and reason codes
- **AND** admin web does not duplicate tenant settings field validation locally.

#### Scenario: Backend computes section readiness

- **GIVEN** tenant settings contain identity, defaults, print, send, and calendar sections
- **WHEN** backend computes settings readiness
- **THEN** each section returns a named readiness state and missing reasons
- **AND** optional nullable fields do not make the section incomplete
- **AND** required structural fields without safe defaults do make the section incomplete.

#### Scenario: Optional receipt messages are empty

- **GIVEN** tenant receipt print settings have valid template, paper size, and print options
- **AND** receipt header and footer are null or blank
- **WHEN** backend computes print readiness
- **THEN** print settings remain configured
- **AND** receipt rendering omits the custom header/footer messages.

#### Scenario: Print structural options are missing

- **GIVEN** tenant receipt print settings are missing paper size, template key, or an enabled
  structural option without a server default
- **WHEN** backend validates or computes readiness
- **THEN** print settings are incomplete or invalid
- **AND** the response identifies the missing structural field.

#### Scenario: Send channel is disabled

- **GIVEN** a send channel is disabled for the tenant
- **WHEN** backend validates or computes send-options readiness
- **THEN** the disabled channel does not require fee amount or payer values
- **AND** an enabled channel still requires valid amount, currency, and payer.

#### Scenario: Calendar has no exceptions

- **GIVEN** tenant business calendar has a valid default-open value and recurring weekdays policy
- **AND** no one-off business-day override rows exist
- **WHEN** backend computes calendar readiness
- **THEN** calendar settings remain configured
- **AND** date-specific behavior falls back to recurring/default policy.

#### Scenario: Tenant has no holiday rules

- **GIVEN** tenant business calendar has valid default-open and closed-weekday policy
- **AND** the tenant has selected no recurring holiday rules
- **WHEN** backend computes calendar readiness
- **THEN** calendar settings remain configured
- **AND** no holiday closure is applied by default.

#### Scenario: Tenant settings are still default

- **GIVEN** a tenant still has default or incomplete settings according to the backend rule
- **WHEN** admin web renders the setup checklist
- **THEN** the settings card is not READY
- **AND** its CTA routes to the settings surface that can resolve the gap.

### Requirement: Mobile Consumes Tenant Settings Read-Only Through Runtime

Mobile SHALL consume tenant settings through runtime bootstrap/state contracts and SHALL NOT
edit tenant settings directly.

#### Scenario: Runtime settings change after login

- **GIVEN** an authenticated mobile session is polling runtime state
- **WHEN** the backend reports a settings version change
- **THEN** mobile performs the bounded tenant runtime re-bootstrap
- **AND** applies safe typed settings fallbacks for missing or unsupported fields.

### Requirement: Tenant Calendar Settings Are Managed Separately From Provider Calendars

Tenant business-calendar settings SHALL manage tenant-level ability to sell, seller-terminal
operational availability SHALL remain seller-terminal-owned, and provider result-slot calendars
SHALL remain platform-owned and global.

#### Scenario: Tenant admin configures recurring closed weekdays

- **GIVEN** a tenant admin opens tenant settings
- **WHEN** they edit business-calendar rules
- **THEN** the settings surface exposes recurring tenant rules such as default-open and closed
  weekdays
- **AND** the save updates `rules.businessCalendar` without touching provider calendars.

#### Scenario: Tenant admin chooses recurring holidays

- **GIVEN** a tenant admin opens tenant calendar settings
- **WHEN** they choose known holidays from the Haiti V0 template or add a custom recurring holiday
- **THEN** the holiday is stored as a tenant-owned recurring month/day rule without a year
- **AND** the tenant can decide which suggested holidays apply.

#### Scenario: Tenant admin adds a one-off holiday or closure

- **GIVEN** a tenant admin needs to close or open the tenant for one concrete date
- **WHEN** they add a calendar exception
- **THEN** the exception is stored with a concrete date including the year
- **AND** it takes precedence over recurring holidays, closed weekdays, and default-open fallback.

#### Scenario: Calendar policy is evaluated for a date

- **GIVEN** backend checks whether the tenant may sell on a date
- **WHEN** calendar policy is evaluated
- **THEN** a one-off business-day override wins first
- **AND** recurring holiday rules are checked before closed weekdays
- **AND** closed weekdays are checked before `defaultOpen`.

#### Scenario: Holiday sales flag exists in legacy config

- **GIVEN** legacy config contains `rules.businessCalendar.holidaySalesAllowed`
- **WHEN** tenant calendar V1 is rendered or saved
- **THEN** the boolean is hidden and removed from the active JSON contract
- **AND** explicit tenant holiday rules determine holiday closures.

#### Scenario: Tenant admin manages one-off closures

- **GIVEN** a tenant admin needs to close the whole commerce on a specific date
- **WHEN** they use the calendar exceptions entry point from settings
- **THEN** the operation writes tenant-level `business_day_override` rows
- **AND** no `result_slot_calendar_override` row is created.

#### Scenario: Seller-terminal availability is changed

- **GIVEN** a seller-terminal must be blocked, disabled, or made ineligible for sales
- **WHEN** an admin changes seller-terminal operational availability
- **THEN** the operation uses seller-terminal-owned status or eligibility flows
- **AND** it does not create tenant settings JSON or provider calendar overrides.

#### Scenario: Platform admin manages provider no-draw days

- **GIVEN** a provider has no draw for a result slot on a date or recurring holiday
- **WHEN** a platform admin updates the provider calendar
- **THEN** the operation writes `result_slot_calendar_override`
- **AND** no tenant settings JSON or tenant business-day override is changed.

#### Scenario: Tenant admin consults provider no-draw days read-only

- **GIVEN** a tenant admin opens the catalog result-slot calendar
- **WHEN** provider calendar overrides are displayed
- **THEN** the admin can read `result_slot_calendar_override`
- **AND** create, update, and delete actions remain reserved to super admins.
