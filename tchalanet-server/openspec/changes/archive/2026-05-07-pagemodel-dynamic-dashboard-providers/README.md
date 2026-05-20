# pagemodel-dynamic-dashboard-providers OpenSpec bundle v2

This bundle updates the previous PageModel dynamic providers OpenSpec by adding a mandatory first task:

1. Decompose large PageModel JSON into reusable sub-JSON fragments for header, footer, sidebar, menus and links.
2. Add a generic `json_file` dynamic provider.
3. Load fragments through a whitelist registry.
4. Parse JSON through the existing non-deprecated `JsonUtils` in `common`.
5. Normalize dynamic provider sources in snake_case.

Apply path suggestion:

```text
openspec/changes/pagemodel-dynamic-dashboard-providers/
```
