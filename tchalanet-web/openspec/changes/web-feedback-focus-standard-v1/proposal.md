# Web feedback focus standard

## Problem

Feature pages duplicate DOM queries and post-render focus logic for success and blocking
feedback. That makes accessibility behavior inconsistent and couples page state to a specific
notice element.

## Proposal

Add a shared `tchFeedbackFocus` primitive in `@tch/ui/components`. It owns post-render focus and
viewport scrolling for a feedback surface. `TchNotice`, `TchPageError`, and
`TchFormErrorSummary` use the primitive; feature pages opt into success focus through the notice
API instead of querying DOM elements.

Non-blocking section information and warnings remain local and do not request focus unless their
owner explicitly opts in.

## Scope

- shared feedback focus directive and unit tests;
- standard notice semantics (`status`/`alert` and live-region priority);
- migration of the existing business-profile success flow;
- removal of page-specific DOM focus code;
- documentation and usage rules for future features.
