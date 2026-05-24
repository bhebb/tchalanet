CREATE TABLE agent_zone (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  parent_zone_id uuid NULL REFERENCES agent_zone(id),
  code varchar(80) NOT NULL,
  name varchar(160) NOT NULL,
  zone_type varchar(40) NOT NULL,
  status varchar(24) NOT NULL,
  depth int NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL,
  updated_by uuid NULL,
  CONSTRAINT uq_agent_zone_tenant_code UNIQUE (tenant_id, code),
  CONSTRAINT ck_agent_zone_status CHECK (status IN ('ACTIVE','INACTIVE'))
);
CREATE INDEX idx_agent_zone_tenant_parent ON agent_zone(tenant_id, parent_zone_id);

CREATE TABLE agent (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  parent_agent_id uuid NULL REFERENCES agent(id),
  display_name varchar(180) NOT NULL,
  type varchar(40) NOT NULL,
  status varchar(24) NOT NULL,
  primary_zone_id uuid NOT NULL REFERENCES agent_zone(id),
  owner_user_id uuid NULL,
  depth int NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL,
  updated_by uuid NULL,
  CONSTRAINT ck_agent_type CHECK (type IN ('INTERNAL','AFFILIATE_PARENT','AFFILIATE','INDIVIDUAL')),
  CONSTRAINT ck_agent_status CHECK (status IN ('ACTIVE','SUSPENDED','BLOCKED','CLOSED')),
  CONSTRAINT ck_agent_depth_v1 CHECK (depth BETWEEN 1 AND 2)
);
CREATE INDEX idx_agent_tenant_parent ON agent(tenant_id, parent_agent_id);
CREATE INDEX idx_agent_tenant_zone ON agent(tenant_id, primary_zone_id);

CREATE TABLE agent_zone_mandate (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  agent_id uuid NOT NULL REFERENCES agent(id),
  zone_id uuid NOT NULL REFERENCES agent_zone(id),
  can_sell boolean NOT NULL DEFAULT true,
  can_create_sub_agents boolean NOT NULL DEFAULT false,
  can_create_sellers boolean NOT NULL DEFAULT true,
  can_create_outlets boolean NOT NULL DEFAULT true,
  can_manage_terminals boolean NOT NULL DEFAULT true,
  can_view_reports boolean NOT NULL DEFAULT true,
  max_child_agent_depth int NOT NULL DEFAULT 0,
  max_child_agents int NOT NULL DEFAULT 0,
  max_sellers int NOT NULL DEFAULT 0,
  max_terminals int NOT NULL DEFAULT 0,
  CONSTRAINT uq_agent_zone_mandate UNIQUE (agent_id, zone_id)
);
CREATE INDEX idx_agent_zone_mandate_tenant_zone ON agent_zone_mandate(tenant_id, zone_id);

CREATE TABLE agent_user_assignment (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  agent_id uuid NOT NULL REFERENCES agent(id),
  user_id uuid NOT NULL,
  relation varchar(32) NOT NULL,
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL,
  CONSTRAINT uq_agent_user_relation UNIQUE (tenant_id, agent_id, user_id, relation)
);
CREATE INDEX idx_agent_user_assignment_user ON agent_user_assignment(tenant_id, user_id) WHERE active = true;

ALTER TABLE agent_zone ENABLE ROW LEVEL SECURITY;
ALTER TABLE agent ENABLE ROW LEVEL SECURITY;
ALTER TABLE agent_zone_mandate ENABLE ROW LEVEL SECURITY;
ALTER TABLE agent_user_assignment ENABLE ROW LEVEL SECURITY;
-- Add project-specific RLS policies using existing tenant context SQL helper.
