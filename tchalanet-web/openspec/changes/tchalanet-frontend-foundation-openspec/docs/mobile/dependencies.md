# Mobile Dependencies Governance

## Rule

Do not add packages to `pubspec.yaml` without documenting them here.

Each dependency must answer:

- What problem does it solve?
- Is it runtime, build, test, or dev-only?
- Which capability owns it?
- What Flutter/Dart built-in alternative was considered?
- What would make us remove or replace it?

## Initial expected dependencies

| Package | Category | Owner capability | Purpose | Alternative considered | Keep / remove trigger |
|---|---|---|---|---|---|
| `flutter_riverpod` | runtime | state | Session, settings, theme, i18n, operational context state | StatefulWidget/inherited widgets only | Keep if shared app state remains needed |
| `go_router` | runtime | routing | Protected routes and redirects | Navigator 2.0 directly | Keep while route rules are declarative |
| `dio` | runtime | api | HTTP client, interceptors, error mapping | `http` package | Keep if interceptors/error handling justify it |
| `flutter_secure_storage` | runtime | auth/storage | Secure token storage | SharedPreferences | Keep for token/secret storage |

## Likely dev tools

| Tool | Category | Purpose | Rule |
|---|---|---|---|
| `flutter analyze` | dev command | Static analysis | Configure early |
| `dart format` / `flutter format` | dev command | Formatting | Configure early |
| Pre-commit hook | dev optional | Fast checks | Add after clean baseline only |

## Dependency request template

```text
Package:
Category: runtime | dev | test | build
Owner capability:
Problem solved:
Why Flutter/Dart built-ins are not enough:
Alternatives considered:
Risks:
Removal/replacement trigger:
```
