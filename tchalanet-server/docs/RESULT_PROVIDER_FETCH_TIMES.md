# Result provider fetch times

Small operational reference for staging result ingestion before MEP.

Observed on staging from `public.draw_result` and `public.result_slot` on 2026-08-21.

## Rules for MEP

- `IL`, `MN`, and `TN` are manual result providers for now.
- Manual providers must not be fetched automatically.
- Automatic fetch should be enabled only for providers that have produced stable confirmed results on staging.
- Fetch windows should stay conservative: one queued fetch per slot/date, no tight retry loop when a result is not actionable.
- Prefer Ops-triggered on-demand fetch for production troubleshooting instead of increasing provider polling.

## Automatic providers observed on staging

Draw time is the slot local time from `result_slot`.
`fetched_at` is stored by Postgres as UTC.

The UTC draw time below is the observed summer-time conversion for the staging sample window.
It will shift when provider local time changes for daylight saving time.

| Provider | Slot | Draw time | Slot timezone | Draw UTC | Observed fetched_at UTC | Delay after draw | Status |
| --- | --- | ---: | --- | ---: | --- | --- | --- |
| CA | CA_MID | 13:00 | America/Los_Angeles | 20:00 | 20:15-20:20 | 15-20 min | confirmed daily |
| CA | CA_EVE | 18:30 | America/Los_Angeles | 01:30 D+1 | 01:35-02:00 D+1 | 5-30 min | confirmed daily after draw |
| FL | FL_MID | 13:30 | America/New_York | 17:30 | 17:45-18:05 | 15-35 min | confirmed daily |
| FL | FL_EVE | 22:45 | America/New_York | 02:45 D+1 | 02:50 D+1 | 5 min | confirmed daily after draw |
| GA | GA_MID | 12:29 | America/New_York | 16:29 | 16:35-17:25 | 6-56 min | confirmed daily |
| GA | GA_EVE | 18:59 | America/New_York | 22:59 | 23:05-23:20 | 6-21 min | confirmed daily |
| GA | GA_LATE | 23:34 | America/New_York | 03:34 D+1 | 03:50-04:00 D+1 | 16-26 min | confirmed daily after draw |
| MI | MI_MID | 12:59 | America/Detroit | 16:59 | 17:20 | 21 min | confirmed daily |
| MI | MI_EVE | 19:29 | America/Detroit | 23:29 | 23:50-00:00 D+1 | 21-31 min | confirmed daily |
| MO | MO_MID | 12:45 | America/Chicago | 17:45 | 17:55-18:45 | 10-60 min | confirmed daily |
| MO | MO_EVE | 21:00 | America/Chicago | 02:00 D+1 | 02:15-02:20 D+1 | 15-20 min | confirmed daily after draw |
| NJ | NJ_MID | 12:59 | America/New_York | 16:59 | 17:05 | 6 min | confirmed daily |
| NJ | NJ_EVE | 22:57 | America/New_York | 02:57 D+1 | 03:05-03:15 D+1 | 8-18 min | confirmed daily after draw |
| NY | NY_MID | 14:30 | America/New_York | 18:30 | 18:50-19:00 | 20-30 min | confirmed daily |
| NY | NY_EVE | 22:30 | America/New_York | 02:30 D+1 | 02:50-03:00 D+1 | 20-30 min | confirmed daily after draw |
| OH | OH_MID | 12:29 | America/New_York | 16:29 | 16:50-17:05 | 21-36 min | confirmed daily |
| OH | OH_EVE | 19:29 | America/New_York | 23:29 | 23:50-00:15 D+1 | 21-46 min | confirmed daily |
| PA | PA_MID | 13:35 | America/New_York | 17:35 | 17:40-18:55 | 5-80 min | confirmed daily |
| PA | PA_EVE | 19:00 | America/New_York | 23:00 | 23:15-23:35 | 15-35 min | confirmed daily |
| TX | TX_1000 | 10:00 | America/Chicago | 15:00 | 15:15-15:20 | 15-20 min | confirmed on scheduled days |
| TX | TX_1227 | 12:27 | America/Chicago | 17:27 | 17:35 | 8 min | confirmed on scheduled days |
| TX | TX_1800 | 18:00 | America/Chicago | 23:00 | 23:15-23:20 | 15-20 min | confirmed on scheduled days |
| TX | TX_2212 | 22:12 | America/Chicago | 03:12 D+1 | 03:30-03:35 D+1 | 18-23 min | confirmed on scheduled days |

## Manual providers

These slots exist in `result_slot`, but they should be handled manually until a provider integration is explicitly enabled and validated.

| Provider | Slot | Draw time | Slot timezone | Current handling |
| --- | --- | ---: | --- | --- |
| IL | IL_MID | 12:40 | America/Chicago | manual, no automatic fetch |
| IL | IL_EVE | 21:22 | America/Chicago | manual, no automatic fetch |
| MN | MN_EVE | 18:17 | America/Chicago | manual, no automatic fetch |
| TN | TN_MID | 12:29 | America/Chicago | manual, no automatic fetch |
| TN | TN_EVE | 22:00 | America/Chicago | manual, no automatic fetch |

## Production rollout recommendation

- Start production with automatic fetch enabled only for the observed stable providers.
- Disable automatic fetch for manual providers in configuration or source selection.
- Add an Ops action to fetch a single provider slot/date on demand.
- Rate-limit on-demand fetch by provider and slot/date.
- Do not retry indefinitely when a provider returns no actionable result.
- Keep a daily Ops report for missing results by provider/slot/date.
