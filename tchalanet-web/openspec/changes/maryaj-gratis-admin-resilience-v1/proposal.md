# Maryaj gratis admin resilience v1

## Why

The Maryaj gratis admin page combines promotion data with tenant-game configuration. A temporary
failure while reading the game configuration currently makes the whole optional promotion page fail.
Action failures also expose raw server titles/details instead of using the web error contract.

## What

- Keep campaign loading as the page-owned required slice.
- Treat tenant-game readiness and campaign detail enrichment as section-owned optional slices.
- Normalize page, section, and action failures through `ProblemDetail` and translated safe copy.
- Add localized page-level copy for the Maryaj gratis route.

## Non-goals

- Changing promotion eligibility, quantity, or selection business rules.
- Changing seller-terminal or mobile Maryaj flows.
- Replacing the existing reactive form in this legacy page.
