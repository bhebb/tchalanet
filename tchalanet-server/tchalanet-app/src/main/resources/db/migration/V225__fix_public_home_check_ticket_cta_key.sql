SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', 'platform', true);
SELECT set_config('app.is_super_admin', 'true', true);

UPDATE page_model_template
SET model = jsonb_set(
  model,
  '{content,widgets,home.checkTicket,props,ctaKey}',
  to_jsonb('public.check.cta'::text),
  false
)
WHERE logical_id = 'public.home'
  AND deleted_at IS NULL
  AND model #>> '{content,widgets,home.checkTicket,props,ctaKey}' IN ('cta', 'public.ticket.cta');

UPDATE page_model
SET model = jsonb_set(
  model,
  '{content,widgets,home.checkTicket,props,ctaKey}',
  to_jsonb('public.check.cta'::text),
  false
)
WHERE logical_id = 'public.home'
  AND deleted_at IS NULL
  AND model #>> '{content,widgets,home.checkTicket,props,ctaKey}' IN ('cta', 'public.ticket.cta');

SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', '', true);
SELECT set_config('app.is_super_admin', 'false', true);
