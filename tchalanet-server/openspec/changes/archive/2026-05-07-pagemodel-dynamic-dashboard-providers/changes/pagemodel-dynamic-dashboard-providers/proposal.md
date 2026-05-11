# OpenSpec Change — PageModel Dynamic Providers + JSON Fragment Provider

## Change ID

`pagemodel-dynamic-dashboard-providers`

## Why

Tchalanet already has the correct dynamic PageModel mechanism:

- `PageModelDynamicProvider`
- `PageModelDynamicResolver`
- `PageDynamicPayload`
- `WidgetDynamicError`

The next step is not to create a second BFF provider system. The next step is to make the existing dynamic resolver more useful by:

1. Decomposing large PageModel JSON documents into reusable JSON fragments.
2. Loading repeated static/semi-static fragments through a generic dynamic provider.
3. Adding business/data providers for public home, public results, cashier dashboard, tenant admin dashboard, and superadmin dashboard.

This keeps the PageModel template readable, avoids duplication between dashboards, and lets header/sidebar/footer/menu/link sections evolve independently.

## What

### 1. Decompose PageModel JSON into sub-JSON fragments

Move repeated shell/navigation fragments out of large PageModel JSON documents into classpath JSON files:

- header links
- footer links
- sidebar links
- menu entries
- support links
- legal links
- public navigation links
- private navigation links by role/profile

The PageModel will reference those fragments with:

```json
{
  "binding": {
    "mode": "dynamic",
    "source": "json_file"
  },
  "props": {
    "file_key": "private_sidebar_cashier"
  }
}
```

### 2. Create a generic JSON file provider

Add a provider:

```text
source = json_file
providerKey = json_file
```

It must load only whitelisted classpath fragments through a registry. It must not accept raw paths from PageModel JSON.

It must use the existing non-deprecated `JsonUtils` class from `common` rather than direct deprecated ObjectMapper helpers or duplicated JSON parsing utilities.

### 3. Add dynamic providers for real data

Add providers for:

- public home
- public draw results page
- cashier dashboard
- tenant admin dashboard
- superadmin dashboard

Each provider must call application queries/services through stable application APIs. Providers must not access repositories/entities directly and must not implement business invariants.

## Non-goals

- Do not replace the existing `PageModelDynamicResolver`.
- Do not create a parallel `PublicHomePageModelProvider` that reconstructs the whole page.
- Do not pass raw file paths from templates.
- Do not move business logic into `features.pagemodel`.
- Do not make `features.pagemodel` hexagonal.
- Do not expose internal IDs in public payloads.

## Impact

- Smaller PageModel JSON templates.
- Shared shell/navigation fragments across private dashboards.
- Safer static JSON reuse through `json_file` provider.
- Clear provider/source registry for future dashboard work.
- One consistent mechanism for dynamic payloads.
