# Codex review prompt — draw_channel business schedule boundary

Review the implementation for the OpenSpec change `use-draw-channel-business-schedule`.

Focus on these decisions:

1. `result_slot` is global and provider/result oriented.
2. `draw_channel` is tenant-scoped commercial configuration for selling a slot.
3. `draw_channel.cutoff_sec` is the tenant/channel sales cutoff rule.
4. `draw.scheduled_at` and `draw.cutoff_at` are generated snapshots.
5. `SellTicketCommand` must check `now < draw.cutoffAt`.
6. Close due logic must close `OPEN` draws where `cutoff_at <= now`.
7. Fetch/apply/settle must not confuse `result_slot` and `draw_channel` responsibilities.

Look for:

- accidental use of global scheduler cutoff to calculate tenant sales cutoff;
- recalculation of cutoff during sell/close instead of using draw snapshot;
- `result_slot` being polluted with tenant business config;
- missing `draw_time`, `timezone`, `cutoff_sec`, or `days_of_week` in `DrawChannelCalendarRow`;
- provisioning/seed code that creates draw channels without cutoff values;
- any need for migrations that was not explicitly discussed.

Do not redesign the whole draw pipeline. Suggest minimal changes to align the implementation with the spec.
