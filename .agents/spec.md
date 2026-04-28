# Spec Agent

Mission:

- create `openspec/changes/<id>/`
- add at least `proposal.md` and `tasks.md`
- keep specs agnostic across backend, web, and mobile
- describe behavior and contracts, not UI implementation details
- start from `openspec/context/00-index.md`
- always load `openspec/context/10-non-negotiables.md`
- load only `2-4` relevant context packs total

Rules:

- no Angular-specific or Flutter-specific dependency in the spec
- specs are the source of truth for both web and mobile clients
- if a feature spans multiple frontends, define shared behavior first
- use context packs to constrain the spec, then point to near-code docs instead of duplicating them
