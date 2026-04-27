-- ------------------------------------------------------------
-- RLS: page_model_template
-- - GLOBAL templates (level='GLOBAL' and tenant_id IS NULL) are visible to all tenants
-- - TENANT templates (level='TENANT' and tenant_id=current_tenant()) are visible only to that tenant
-- - Soft delete visibility controlled by app.deleted_visibility
-- ------------------------------------------------------------
ALTER TABLE page_model_template ENABLE ROW LEVEL SECURITY;
ALTER TABLE page_model_template FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS page_model_template_rls_all ON page_model_template;

CREATE POLICY page_model_template_rls_all
ON page_model_template
FOR ALL
USING (
  public.current_tenant() IS NOT NULL
  AND (
    (level = 'GLOBAL' AND tenant_id IS NULL)
    OR
    (level = 'TENANT' AND tenant_id = public.current_tenant())
  )
  AND (
    public.deleted_visibility() = 'all'
    OR (public.deleted_visibility() = 'active'  AND deleted_at IS NULL)
    OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL)
  )
)
WITH CHECK (
  public.current_tenant() IS NOT NULL
  AND (
    (level = 'GLOBAL' AND tenant_id IS NULL)
    OR
    (level = 'TENANT' AND tenant_id = public.current_tenant())
  )
);


-- ==============
-- app_setting
-- ==============
ALTER TABLE app_setting ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_setting FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS app_setting_rls_all ON app_setting;

CREATE POLICY app_setting_rls_all ON app_setting
FOR ALL
USING (
  (
    level = 'GLOBAL'
    AND (
      public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active'  AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL)
    )
  )
  OR
  (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (
      public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active'  AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL)
    )
  )
)
WITH CHECK (
  (level = 'GLOBAL' AND tenant_id IS NULL)
  OR
  (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant())
);


-- ==============
-- i18n_override
-- ==============
ALTER TABLE i18n_override ENABLE ROW LEVEL SECURITY;
ALTER TABLE i18n_override FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS i18n_override_rls_all ON i18n_override;

CREATE POLICY i18n_override_rls_all ON i18n_override
FOR ALL
USING (
  (
    level = 'GLOBAL'
    AND (
      public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active'  AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL)
    )
  )
  OR
  (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (
      public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active'  AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL)
    )
  )
)
WITH CHECK (
  (level = 'GLOBAL' AND tenant_id IS NULL)
  OR
  (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant())
);
