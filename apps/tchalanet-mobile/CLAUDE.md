# Claude — apps/tchalanet-mobile

Legacy mobile app router. Prefer the current mobile project router at
`../../tchalanet-mobile/AGENTS.md` unless the task explicitly targets this app.

Read first:

- `../../AGENTS.md`
- `../../VERSIONS.md`
- `../../tchalanet-mobile/AGENTS.md`
- files explicitly mentioned in the task

Context rule:

- Keep this scope small.
- Do not introduce legacy hybrid assumptions.
- Keep mobile details in `tchalanet-mobile/` when the current Flutter app owns them.

Commands:

```bash
flutter pub get
flutter test
flutter build apk --release
```
