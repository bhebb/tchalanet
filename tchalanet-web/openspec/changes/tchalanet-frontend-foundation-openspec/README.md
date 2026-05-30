# Tchalanet Frontend Foundation OpenSpecs

This ZIP contains two separated OpenSpec changes:

- `init-web-platform-foundation` — Angular/Nx web foundation.
- `init-mobile-pos-foundation` — Flutter/POS mobile foundation.

The goal is to make the first frontend implementation safe and incremental:

1. create shared contracts/types first;
2. prove runtime foundations: auth, roles, settings, i18n, theme, PageModel where relevant;
3. avoid building a large abstract design system before real V1 flows;
4. document every dependency/class so package files do not grow without intent.

Important rule:

> Do not add dependencies because a tutorial uses them. Add a dependency only when the corresponding OpenSpec section explains its owner, purpose, alternatives considered, and why built-in framework capability is not enough.
