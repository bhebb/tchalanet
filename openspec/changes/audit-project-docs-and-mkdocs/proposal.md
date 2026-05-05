# Change: Audit project docs and reorganize MkDocs

## Why

Tchalanet now has documentation spread across multiple projects and contexts:

- `tchalanet-server`
- `tchalanet-web`
- `tchalanet-mobile`
- `tchalanet-edge-service`
- `tchalanet-infra`
- `tchalanet-docs`
- global `openspec/`
- component OpenSpecs
- AI-agent context files

This creates several risks:

- duplicated architecture rules;
- stale Markdown files;
- obsolete decisions still loaded by AI agents;
- MkDocs becoming either incomplete or overloaded;
- OpenSpec context packs becoming too large;
- developers not knowing the canonical source of truth.

The goal is to create a global documentation governance pass without moving code or changing runtime behavior.

## What

This change will:

- count every `.md` file in the repository;
- classify Markdown files by owner/component and purpose;
- detect likely duplicates and obsolete docs;
- define canonical sources of truth;
- reorganize `tchalanet-docs` as the published documentation portal;
- keep detailed docs near the owning component;
- link from MkDocs to component docs instead of copying everything;
- define how MkDocs references OpenSpec specs, context packs, ADRs, domain docs, and implementation docs;
- produce a cleanup plan with safe archive/move/delete steps.

## Impact

- Documentation only.
- No runtime behavior change.
- No Docker, backend, frontend, mobile, or infra behavior change.
- Some docs may be moved, renamed, archived, or linked.
- MkDocs navigation may change.
- AI context loading becomes lighter because global OpenSpec remains a router, not a copy of all details.

## Non-goals

- Do not rewrite all documentation content in this change.
- Do not delete docs without first producing an inventory and archive plan.
- Do not merge component OpenSpecs into the global OpenSpec.
- Do not make `tchalanet-docs` the only place where technical docs live.
- Do not load every doc into AI-agent context.

## Principles

- Docs near code are canonical for implementation details.
- MkDocs is the published portal and navigation layer.
- OpenSpec context packs are lightweight routers.
- Component OpenSpecs stay close to their component/project.
- Duplicates are resolved by ownership, not by copying everything into one place.
