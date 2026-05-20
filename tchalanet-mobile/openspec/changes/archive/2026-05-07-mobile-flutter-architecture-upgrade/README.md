# OpenSpec change: mobile-flutter-architecture-upgrade

This OpenSpec is a follow-up after migrating Tchalanet mobile out of Nx/Ionic into standalone Flutter.

It standardizes:

- latest stable Flutter SDK policy,
- official Flutter MVVM architecture baseline,
- feature-first folder layout,
- Riverpod/go_router/Dio/secure-storage stack,
- strict repository boundaries to prevent Claude from burning tokens in server/infra/edge/web.

Apply after or alongside `migrate-mobile-from-nx-ionic-to-flutter`.
