UPDATE tenant
SET config = jsonb_set(
    config,
    '{rules,businessCalendar,holidays}',
    '[]'::jsonb,
    true
)
WHERE deleted_at IS NULL
  AND config IS NOT NULL
  AND config #> '{rules,businessCalendar,holidays}' IS NULL;
