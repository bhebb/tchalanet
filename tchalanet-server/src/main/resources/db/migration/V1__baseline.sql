-- === types simples ===
create type ticket_status as enum ('PENDING','WON','LOST','PAID','VOID');

-- === tables maîtres ===
create table enterprise(
                           id uuid primary key,
                           name text not null,
                           tz text not null,
                           currency text not null
);

create table outlet(
                       id uuid primary key,
                       enterprise_id uuid not null references enterprise(id),
                       name text not null,
                       zone text,
                       unique(enterprise_id, name)
);

create table terminal(
                         id uuid primary key,
                         enterprise_id uuid not null references enterprise(id),
                         outlet_id uuid not null references outlet(id),
                         state text not null default 'ACTIVE',
                         last_seen timestamptz
);

-- === tirages / cotes / limites ===
create table draw(
                     id text primary key,
                     enterprise_id uuid not null references enterprise(id),
                     draw_source_code text not null,        -- ex: NY-MID
                     scheduled_at timestamptz not null,
                     cutoff_sec int not null default 120,
                     status text not null default 'SCHEDULED',
                     result_payload jsonb
);
create index ix_draw_ent_time on draw(enterprise_id, scheduled_at);

create table odds(
                     id uuid primary key,
                     enterprise_id uuid not null references enterprise(id),
                     game_code text not null,               -- ex: 1C,2C,3D,4C
                     multiplier numeric(12,4) not null,
                     valid_from timestamptz not null default now(),
                     valid_to timestamptz
);
create index ix_odds_ent_game_valid on odds(enterprise_id, game_code, valid_from desc);

create table limit_policy(
                             id uuid primary key,
                             enterprise_id uuid not null references enterprise(id),
                             scope text not null,                   -- NUMBER, RANGE, AGENT, TERMINAL
                             target text not null,                  -- numéro/pattern/agent/terminal
                             daily_cap numeric(14,2) not null,
                             on_breach text not null default 'BLOCK' -- BLOCK|WARN|ALLOW
);
create index ix_limit_ent_scope_target on limit_policy(enterprise_id, scope, target);

-- === tickets ===
create table ticket(
                       id text primary key,                   -- format généré (ksuid/ulid)
                       enterprise_id uuid not null references enterprise(id),
                       terminal_id uuid not null references terminal(id),
                       created_at timestamptz not null default now(),
                       status ticket_status not null default 'PENDING',
                       total_amount numeric(14,2) not null
);
create index ix_ticket_ent_created on ticket(enterprise_id, created_at);

create table ticket_line(
                            id bigserial primary key,
                            ticket_id text not null references ticket(id) on delete cascade,
                            game_code text not null,
                            selection text not null,               -- ex: "12", "12-34", "xx12x"
                            stake numeric(12,2) not null,
                            odds_snapshot numeric(12,4) not null,
                            potential_payout numeric(14,2) not null
);
create index ix_tline_ticket on ticket_line(ticket_id);

-- === audit applicatif (append-only, optionnel) ===
create table audit_event(
                            id bigserial primary key,
                            ts timestamptz not null default now(),
                            enterprise_id uuid not null,
                            actor_type text not null,              -- USER|TERMINAL|SYSTEM
                            actor_id text not null,
                            entity_type text not null,             -- ODDS|LIMIT|DRAW|TICKET|TERMINAL|PAYMENT
                            entity_id text not null,
                            action text not null,                  -- CREATE|UPDATE|DELETE|STATE_CHANGE|PAY
                            details jsonb not null,
                            ip inet, user_agent text
);
create index ix_audit_ent_ts on audit_event(enterprise_id, ts);
