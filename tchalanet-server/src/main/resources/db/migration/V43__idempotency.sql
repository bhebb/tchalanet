-- V43: Idempotency table
create table if not exists idempotency_record (
  id uuid primary key,
  tenant_id uuid not null,
  scope text not null,
  idem_key text not null,
  request_hash text not null,
  status text not null, -- IN_PROGRESS | COMPLETED | FAILED
  resource_id uuid null,
  response_json jsonb null,
  expires_at timestamptz not null,
  version bigint default 0 not null,
  deleted_at timestamptz null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by uuid null,
  updated_by uuid null
);

create unique index if not exists ux_idem_tenant_scope_key
  on idempotency_record(tenant_id, scope, idem_key);

create index if not exists ix_idem_expires_at
  on idempotency_record(expires_at);
