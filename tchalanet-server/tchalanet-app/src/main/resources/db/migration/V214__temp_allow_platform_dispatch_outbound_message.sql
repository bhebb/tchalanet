-- Temporary pre-go-live policy adjustment:
-- communication dispatch is a platform/system job and must process tenant-scoped
-- outbound messages while recording platform-owned delivery attempts.
ALTER POLICY outbound_message_rls_all ON outbound_message
  USING (
    public.allow_platform_cross_tenant_select()
    OR (
      public.current_tenant() IS NOT NULL
      AND tenant_id = public.current_tenant()
      AND (public.deleted_visibility() = 'all'
        OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
        OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
    )
    OR (tenant_id IS NULL AND public.allow_platform_cross_tenant_select())
  )
  WITH CHECK (
    public.allow_platform_cross_tenant_select()
    OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant())
    OR (tenant_id IS NULL AND public.allow_platform_cross_tenant_select())
  );
