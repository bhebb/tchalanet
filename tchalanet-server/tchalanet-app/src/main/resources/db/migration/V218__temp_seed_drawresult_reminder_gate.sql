select set_config('app.current_tenant', '', true);
select set_config('app.deleted_visibility', 'active', true);
select set_config('app.api_scope', 'platform', true);
select set_config('app.is_super_admin', 'true', true);

insert into app_setting (
  level,
  namespace,
  setting_key,
  value_type,
  setting_value,
  active,
  exposure
)
select
  'GLOBAL',
  'batch.gate',
  'drawresult:reminder:run',
  'BOOLEAN',
  'true',
  true,
  'INTERNAL'
where not exists (
  select 1
    from app_setting
   where level = 'GLOBAL'
     and namespace = 'batch.gate'
     and setting_key = 'drawresult:reminder:run'
);

select set_config('app.current_tenant', '', true);
select set_config('app.deleted_visibility', 'active', true);
select set_config('app.api_scope', '', true);
select set_config('app.is_super_admin', 'false', true);
