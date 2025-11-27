# Tchalanet – Mobile (Ionic Angular) Guide (DRAFT)

> This guide is a placeholder for future mobile work. It gives high-level expectations for Copilot when generating Ionic-related code.

---

## 1. Tech Stack

- Ionic + Angular (same major Angular version as web: 20+ when possible).
- Shared Nx workspace with the web app.
- Shared domain models and data-access libraries where it makes sense.

---

## 2. Principles

- Reuse as much logic as possible from the web:
  - data-access libs
  - domain models
  - endpoints
- Mobile-specific:
  - navigation (tabs, router-outlet)
  - offline support (later)
  - POS / cashier flows optimized for touch

---

## 3. UI Guidelines

- Keep screens focused: one main action per view (sell ticket, close session, see stats).
- Use Ionic components (ion-header, ion-content, ion-list, ion-button, etc.).
- Honor Tchalanet theme tokens where possible (bridge CSS vars from web theme).

---

## 4. To Refine Later

This file will be extended when mobile work starts in earnest. For now, mobile generation by Copilot should be conservative and rely on standard Ionic + Angular patterns.