# Tchalanet Public Draw Results OpenSpec Pack v2

This pack updates the previous publicdrawresults OpenSpec to use one internal query with `includeHistory` and `historyLimit`.

Key rule:

```text
PageModel public home -> includeHistory=false
Details/mobile/terminal -> includeHistory=true, historyLimit 5/10
Advanced search -> separate paginated history query
```

Copy `openspec/changes/add-public-drawresults` into the backend repo OpenSpec changes directory.
