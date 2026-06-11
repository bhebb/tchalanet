# Migration inventory

## Completion criteria per routed screen

A routed screen is migrated when:

- it has one screen-level ViewModel;
- the View renders state and forwards intents only;
- the ViewModel depends on Repositories/Use Cases, not Services/storage;
- state category, lifetime, disposal, reset and effects are explicit;
- user-visible strings resolve through i18n;
- styling uses Material 3 and Tchalanet semantic tokens/components;
- ViewModel and appropriate widget/golden tests pass.

## Screen matrix

| Route/surface | Current state | Main migration work | Priority |
| --- | --- | --- | --- |
| `/login` | Auth ViewModel uses typed i18n failure and shared login action; draw providers and draw copy remain in View | separate public draw presentation from auth, finish i18n, dedicated composite screen state | P1 |
| `/forbidden` | migrated: screen ViewModel, i18n, shared blocked feedback state | preserve contract with widget coverage | Done |
| `/pos` cashier home | read providers call Service; large View; feature-to-feature auth import | home Repository/ViewModel, effects, i18n, shared cards/states | P1 |
| `/pos/setup` | ViewModel calls Service directly | Repository boundary, immutable UI state, i18n | P1 |
| `/pos/session/open` | ViewModel calls Service/storage and invalidates another feature | session Repository/Use Case, typed effect, reset rules, i18n | P1 |
| `/sell` | ViewModel calls Services; View coordinates home/provider/terminal state | sell Repository/Use Case, screen-scoped lifecycle, typed effects, i18n | P0 |
| `/pos/sell/success` | View calls print/share actions and other feature providers | success ViewModel/effects, shared components, i18n | P1 |
| `/pos/history` | View-local provider calls Service; local filtering | history Repository/ViewModel, filters in UI state, i18n | P1 |
| `/pos/tickets/:ticketId` | View-local provider calls Service; cancel action incomplete | detail Repository/ViewModel/effects, i18n | P0 |
| `/pos/scan` | ViewModel declared in View file and calls Service | dedicated Repository/ViewModel, payout effect/flow, i18n | P1 |
| `/pos/profile` | stub | define feature before implementation | P3 |
| `/pos/notifications` | migrated: auto-disposed screen ViewModel, Repository boundary, i18n, semantic Material 3 components, widget coverage | preserve paging/read/archive contract and extend golden coverage with the shared theme suite | Done |

## Cross-cutting violations to remove

- `core/network` imports cashier operational-context feature code.
- Views call Services, secure storage, printing, clipboard, and navigation effects
  directly without typed ViewModel effects.
- ViewModels call Services/storage and invalidate providers owned by other features.
- Screen-scoped providers are not consistently auto-disposed.
- User-facing strings remain hardcoded across screens.
- Existing screens use semantic status tokens inconsistently and lack golden coverage.

## Migration order

1. Sell and ticket detail correctness/security flows.
2. Cashier home, operational context, and session.
3. Auth/login.
4. History, success, print/share, and scan.
5. Remaining stubs and non-critical screens.
