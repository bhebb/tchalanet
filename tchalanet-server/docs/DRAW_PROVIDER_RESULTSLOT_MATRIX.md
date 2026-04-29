# Draw Provider / ResultSlot Matrix

> Source of truth for the MVP mapping between US lottery provider game codes, global `result_slot`
> rows, and tenant `draw_channel` rows.

## Runtime Sources

- Provider HTTP/runtime config: `src/main/resources/application-uslottery.yaml`
- Result slots and default tenant channels: `src/main/resources/db/migration/V204__seed_core_game_draw.sql`
- Public read contracts: `catalog.resultslot.api`, `catalog.drawchannel.api`

`application.yaml` must not define `tch.us-lottery.providers.*`; it only imports the provider file.

## Matrix

| ResultSlot | Provider | Timezone         | Draw Time | Days    | Active | Pick3 GameCode     | Pick4 GameCode      | Draw Channel |
| ---------- | -------- | ---------------- | --------- | ------- | ------ | ------------------ | ------------------- | ------------ |
| `NY_MID`   | NY       | America/New_York | 14:30     | MON-SUN | true   | `US_NY_NUM3_MID`   | `US_NY_NUM4_MID`    | `HT_NY_MID`  |
| `NY_EVE`   | NY       | America/New_York | 22:30     | MON-SUN | true   | `US_NY_NUM3_EVE`   | `US_NY_NUM4_EVE`    | `HT_NY_EVE`  |
| `FL_MID`   | FL       | America/New_York | 13:30     | MON-SUN | true   | `US_FL_PICK3_MID`  | `US_FL_PICK4_MID`   | `HT_FL_MID`  |
| `FL_EVE`   | FL       | America/New_York | 22:45     | MON-SUN | true   | `US_FL_PICK3_EVE`  | `US_FL_PICK4_EVE`   | `HT_FL_EVE`  |
| `GA_MID`   | GA       | America/New_York | 12:29     | MON-SUN | true   | `US_GA_CASH3_1229` | `US_GA_CASH4_1229`  | `HT_GA_MID`  |
| `GA_EVE`   | GA       | America/New_York | 18:59     | MON-SUN | true   | `US_GA_CASH3_1859` | `US_GA_CASH4_1859`  | `HT_GA_EVE`  |
| `GA_LATE`  | GA       | America/New_York | 23:34     | MON-SUN | true   | `US_GA_CASH3_2334` | `US_GA_CASH4_2334`  | `HT_GA_LATE` |
| `TN_MID`   | TN       | America/Chicago  | 12:55     | MON-SAT | false  | `US_TN_CASH3_1255` | `US_TN_CASH4_1255`  | `HT_TN_MID`  |
| `TX_1000`  | TX       | America/Chicago  | 10:00     | MON-SAT | true   | `US_TX_PICK3_1000` | `US_TX_DAILY4_1000` | `HT_TX_1000` |
| `TX_1227`  | TX       | America/Chicago  | 12:27     | MON-SAT | true   | `US_TX_PICK3_1227` | `US_TX_DAILY4_1227` | `HT_TX_1227` |
| `TX_1800`  | TX       | America/Chicago  | 18:00     | MON-SAT | true   | `US_TX_PICK3_1800` | `US_TX_DAILY4_1800` | `HT_TX_1800` |
| `TX_2212`  | TX       | America/Chicago  | 22:12     | MON-SAT | true   | `US_TX_PICK3_2212` | `US_TX_DAILY4_2212` | `HT_TX_2212` |

## Orphans And Legacy Codes

| Code              | Decision                                                                                           |
| ----------------- | -------------------------------------------------------------------------------------------------- |
| `US_NY_TAKE5_EVE` | Hors scope MVP. Take 5 uses a different multi-ball format and has no `result_slot`.                |
| `US_FL_LOTTO`     | Hors scope MVP. Lotto uses a 5/6-ball format incompatible with the current pick3/pick4 projection. |
| `US_GA_CASH3_MID` | Legacy code removed from active config; replaced by `US_GA_CASH3_1229`.                            |
| `US_GA_CASH3_EVE` | Legacy code removed from active config; replaced by `US_GA_CASH3_1859`.                            |
| `US_TN_PICK3_MID` | Legacy code removed from active config; replaced by `US_TN_CASH3_1255`.                            |
| `US_TN_PICK3_EVE` | Legacy code removed from active config; replaced by `US_TN_CASH4_1255`.                            |

## Projection Rule

`core.drawresult` resolves Haiti projection from `result_slot.projection_cfg` first. If the slot
configuration is absent or invalid, it falls back to the documented default projection.
