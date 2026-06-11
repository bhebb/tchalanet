# Mobile Architecture Requirements

## ADDED Requirements

### Requirement: Routed screens use MVVM

Every routed Flutter screen SHALL have exactly one screen-level ViewModel.

#### Scenario: User triggers an action

- **WHEN** a user triggers an action from a routed screen
- **THEN** the View forwards the intent to its ViewModel
- **AND** the ViewModel performs orchestration through a Repository or Use Case
- **AND** the View renders the resulting immutable state

### Requirement: Separation of Concerns defines component responsibilities

Tchalanet Mobile SHALL use Separation of Concerns as its primary architecture principle.

#### Scenario: A responsibility is introduced

- **WHEN** implementation introduces state, behavior, or external data access
- **THEN** it is assigned to the View, ViewModel, Repository, or Service responsible
  for that concern
- **AND** other components interact through well-defined interfaces and boundaries

### Requirement: Layer boundaries are enforced

Views and ViewModels SHALL NOT access external data sources directly.

#### Scenario: A screen needs remote or persisted data

- **WHEN** a screen needs remote, cached, offline, secure-storage, or platform data
- **THEN** its ViewModel calls a Repository or Use Case
- **AND** the Repository coordinates the required data sources
- **AND** the View does not import the data sources

### Requirement: Core remains feature-independent

The `core/` layer SHALL NOT import a feature.

#### Scenario: Shared network composition needs feature context

- **WHEN** shared network composition needs feature-specific context
- **THEN** the app composition root supplies an interface or interceptor
- **AND** `core/` does not import from `features/`

### Requirement: Riverpod is the application state-management policy

Tchalanet Mobile SHALL use Riverpod as its only application state-management and dependency-injection mechanism.

#### Scenario: Application state is introduced

- **WHEN** implementation introduces screen, shared application, persisted, cached,
  offline, or remote state
- **THEN** it follows the documented Riverpod provider and lifetime policy
- **AND** no competing application state-management framework is introduced

### Requirement: State categories have explicit responsibilities

Every state value SHALL be classified as ephemeral View state, screen UI state, shared application state, application data state, or navigation state.

#### Scenario: A View needs state

- **WHEN** a View needs business, persisted, cached, offline, remote, or shared
  application state
- **THEN** it watches state exposed by its ViewModel or responsible app controller
- **AND** it does not maintain a competing local copy

#### Scenario: A View needs ephemeral presentation state

- **WHEN** a View needs focus, animation, scrolling, or text-controller state
- **THEN** it may own that state for its widget lifetime
- **AND** the state does not become application or business truth

### Requirement: ViewModel state is immutable and command-driven

ViewModels SHALL expose immutable typed state and commands as the only public mutation entry points.

#### Scenario: An asynchronous command executes

- **WHEN** a View invokes an asynchronous ViewModel command
- **THEN** the ViewModel exposes explicit loading and success or failure transitions
- **AND** it does not expose mutable collections, raw JSON, or Dio responses

### Requirement: Provider lifetime matches state lifetime

Every Riverpod provider SHALL have a lifetime, disposal, and reset policy matching the state it exposes.

#### Scenario: A routed screen is removed

- **WHEN** a routed screen or flow leaves navigation
- **THEN** its screen-scoped state is automatically disposed by default
- **AND** any exception to disposal is documented and tested

#### Scenario: Authentication or tenant scope ends

- **WHEN** the user logs out or changes tenant
- **THEN** app/session-scoped state from the previous scope is reset
- **AND** no previous operational context or protected data remains exposed

### Requirement: Persistence belongs to Repositories

Riverpod in-memory state SHALL NOT be treated as persistence.

#### Scenario: State must survive restart or support offline behavior

- **WHEN** state must survive process restart, support offline operation, or coordinate
  cache and remote data
- **THEN** a Repository owns persistence, restoration, cache, retry, and conflict
  policy
- **AND** the View and ViewModel do not access storage directly

### Requirement: One-shot effects are separate from durable state

Navigation, dialogs, SnackBars, clipboard, printing, and platform actions SHALL be modeled as one-shot effects.

#### Scenario: A ViewModel requests a UI or platform effect

- **WHEN** a ViewModel determines that an effect is required
- **THEN** the View consumes the typed effect once
- **AND** the effect does not replay unexpectedly after rebuild or navigation

### Requirement: Notification channels remain distinct

Mobile SHALL use distinct models, lifetimes, and presentation policies for persistent
internal notifications, global news, and immediate API notices/errors.

#### Scenario: POS receives notification content

- **WHEN** the content is a tenant or platform internal notification
- **THEN** it follows the persisted read/unread/archive lifecycle
- **AND** sector news is excluded from the POS
- **WHEN** the content is an API notice or error
- **THEN** it is temporary and limited to the current request context
- **AND** an available trace ID remains hidden but can be copied for support

#### Scenario: Authenticated POS refreshes persistent notifications

- **WHEN** a seller logs in, resumes the app, requests refresh, or reaches the polling interval
- **THEN** mobile refreshes the `platform.notification` summary
- **AND** periodic polling occurs at most every 30 minutes
- **AND** the full notification list is fetched only when the center is opened
- **AND** notification summary state is reset on logout
