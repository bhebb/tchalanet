create table theme
(
    id             uuid primary key,
    tenant_id      uuid null,
    base_preset_id varchar(128) not null,
    label          varchar(160) not null,
    mode           varchar(10)  not null default 'system', -- light|dark|system
    density        smallint     not null default 0,        -- 0|-1|-2
    palette_json   jsonb        not null default '{}'::jsonb,
    tokens_json    jsonb        not null default '{}'::jsonb,
    css_vars_json  jsonb        not null default '{}'::jsonb,
    status         varchar(20)  not null default 'draft',  -- draft|published|archived
    version        int          not null default 1,
    created_at     timestamptz  not null default now(),
    updated_at     timestamptz  not null default now()
);
create index idx_theme_tenant_status on theme (tenant_id, status);
create index idx_theme_base on theme (base_preset_id);

create table user_preference
(
    user_id    uuid primary key,
    theme_mode varchar(10) null,
    density    smallint null,
    locale     varchar(8) null,
    updated_at timestamptz not null default now()
);
