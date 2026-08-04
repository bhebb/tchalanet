-- Archive deletion is a platform operation, but RLS must remain enabled for all normal traffic.
-- The application sets app.archive_cleanup only inside a transactional, authorized purge call.
CREATE OR REPLACE FUNCTION public.allow_archive_cleanup() RETURNS boolean AS $$
  SELECT public.is_super_admin()
    AND public.api_scope() = 'platform'
    AND coalesce(current_setting('app.archive_cleanup', true), '') = 'true';
$$ LANGUAGE sql STABLE;

DROP POLICY draw_rls_all ON draw;
CREATE POLICY draw_rls_all ON draw
  FOR ALL
  USING (
    public.allow_archive_cleanup()
    OR (
      public.current_tenant() IS NOT NULL
      AND tenant_id = public.current_tenant()
      AND (public.deleted_visibility() = 'all'
        OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
        OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
    )
  )
  WITH CHECK (
    public.allow_archive_cleanup()
    OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant())
  );

DROP POLICY stats_draw_rls_all ON stats_draw;
CREATE POLICY stats_draw_rls_all ON stats_draw
  FOR ALL
  USING (public.allow_archive_cleanup() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()))
  WITH CHECK (public.allow_archive_cleanup() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

DROP POLICY sales_ticket_rls_all ON sales_ticket;
CREATE POLICY sales_ticket_rls_all ON sales_ticket
  FOR ALL
  USING (
    public.allow_archive_cleanup()
    OR (
      public.current_tenant() IS NOT NULL
      AND tenant_id = public.current_tenant()
      AND (public.deleted_visibility() = 'all'
        OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
        OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
    )
  )
  WITH CHECK (
    public.allow_archive_cleanup()
    OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant())
  );

DROP POLICY sales_ticket_line_rls_all ON sales_ticket_line;
CREATE POLICY sales_ticket_line_rls_all ON sales_ticket_line
  FOR ALL
  USING (
    public.allow_archive_cleanup()
    OR (
      public.current_tenant() IS NOT NULL
      AND tenant_id = public.current_tenant()
      AND (public.deleted_visibility() = 'all'
        OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
        OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
    )
  )
  WITH CHECK (
    public.allow_archive_cleanup()
    OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant())
  );

DROP POLICY sales_ticket_charge_rls_all ON sales_ticket_charge;
CREATE POLICY sales_ticket_charge_rls_all ON sales_ticket_charge
  FOR ALL
  USING (
    public.allow_archive_cleanup()
    OR (
      public.current_tenant() IS NOT NULL
      AND tenant_id = public.current_tenant()
      AND (public.deleted_visibility() = 'all'
        OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
        OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
    )
  )
  WITH CHECK (
    public.allow_archive_cleanup()
    OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant())
  );
