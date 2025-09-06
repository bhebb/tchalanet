-- variable de session: app.current_tenant
create or replace function set_current_tenant(p uuid)
returns void language plpgsql security definer as $$
begin
  perform set_config('app.current_tenant', p::text, true);
end$$;

-- helper pour lecture de la variable (debug)
create or replace function current_tenant()
returns uuid language sql stable as
$$ select current_setting('app.current_tenant', true)::uuid; $$;

-- Activer RLS et policies (exemples sur tables principales)
do $$
declare t text;
begin
  -- tables qui portent enterprise_id
for t in select unnest(array['outlet','terminal','draw','odds','limit_policy','ticket','audit_event']) loop
             execute format('alter table %I enable row level security', t);
execute format($f$
    create policy rls_%1$s_select on %1$I
        for select using (enterprise_id::text = current_setting('app.current_tenant'));
$f$, t);
execute format($f$
    create policy rls_%1$s_all on %1$I
        for all using (enterprise_id::text = current_setting('app.current_tenant'))
        with check (enterprise_id::text = current_setting('app.current_tenant'));
$f$, t);
end loop;

  -- ticket_line n'a pas enterprise_id: protéger via parent
  perform 1;
end$$;

-- Vue sécurisée pour ticket_line (option)
create or replace view v_ticket_line_secure as
select tl.*
from ticket_line tl
         join ticket t on t.id = tl.ticket_id
where t.enterprise_id::text = current_setting('app.current_tenant');

-- indexes utilitaires (déjà posés dans V1)
