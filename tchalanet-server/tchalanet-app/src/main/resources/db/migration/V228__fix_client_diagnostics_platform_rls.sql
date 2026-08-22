CREATE POLICY client_diagnostic_policy_rls_platform_write ON client_diagnostic_policy
  FOR ALL
  USING (public.allow_platform_cross_tenant_select())
  WITH CHECK (public.allow_platform_cross_tenant_select());

CREATE POLICY client_diagnostic_event_rls_platform_write ON client_diagnostic_event
  FOR ALL
  USING (public.allow_platform_cross_tenant_select())
  WITH CHECK (public.allow_platform_cross_tenant_select());
