# Codex Review Prompt — add-public-drawresults

Review the implementation of `openspec/changes/add-public-drawresults`.

Check these points carefully:

1. Public PageModel result widget uses `core.drawresult` query, not tenant-scoped `core.draw`.
2. `ListPublicDrawResultSlotsQuery` exists with:
   - `slotKeys`
   - `provider`
   - `includeHistory`
   - `historyLimit`
3. `includeHistory=false`:
   - forces history limit to 0
   - does not execute history lookup
   - returns `history = List.of()` or PageModel omits history after mapping
4. `includeHistory=true`:
   - defaults invalid limit to 5
   - caps limit to 10
   - returns recent history per slot
5. Public reader starts from `result_slot` and `draw_result` only.
6. Public reader does not use tenant draw/draw_channel lifecycle.
7. Public reader does not call `TchContext.requireTenantId()`.
8. Next/countdown calculation uses injected `Clock`, slot timezone, and draw time.
9. Public DTOs do not expose internal IDs unless intentionally approved.
10. Advanced history search is a separate paginated/range-limited query.
11. Existing `core.draw` queries are not deleted unless confirmed unused.

If issues are found, propose minimal patches aligned with the OpenSpec.
