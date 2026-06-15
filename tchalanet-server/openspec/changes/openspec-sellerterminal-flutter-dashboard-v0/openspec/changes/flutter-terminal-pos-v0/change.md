# Change: Flutter Terminal POS V0

## Status

Proposed

## Why

The target seller devices are Android POS terminals with integrated thermal printers. A responsive web terminal may validate the business flow quickly, but native Flutter Android is the better long-term seller terminal client for:

- integrated printer support;
- kiosk-like UX;
- simple seller flow;
- secure local token storage;
- future offline potential.

V0 keeps admin on web/SSR and uses Flutter only for seller terminal sales.

## What Changes

- Define Flutter Android POS as the seller terminal client strategy.
- Keep admin configuration, stats, results, odds, limits, and reports in web.
- Flutter authenticates as a SellerTerminal through Firebase technical user credentials.
- Flutter consumes backend terminal APIs that are client-neutral.
- Flutter prints tickets from backend receipt payloads.
- No offline sales in V0.

## Scope

Flutter app:

- terminal login;
- terminal status/me;
- sale entry;
- ticket preview/confirm;
- recent tickets;
- reprint;
- receipt printing.

Backend contracts:

- terminal actor bootstrap/me;
- controlled sale endpoints;
- print payload.

Out of scope:

- admin dashboard in Flutter;
- odds/limits configuration in Flutter;
- manual result entry in Flutter;
- offline sales;
- push notifications;
- native billing.
