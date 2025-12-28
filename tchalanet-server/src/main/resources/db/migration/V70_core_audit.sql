-- DROP TABLE public.revinfo;

CREATE TABLE public.revinfo (
                                rev int4 NOT NULL,
                                tenant_id uuid NULL,
                                rev_timestamp int8 NOT NULL,
                                user_id uuid NULL,
                                CONSTRAINT revinfo_pkey PRIMARY KEY (rev)
);

-- Permissions

ALTER TABLE public.revinfo OWNER TO app_user;
GRANT ALL ON TABLE public.revinfo TO app_user;

-- public.app_role_aud definition

-- Drop table

-- DROP TABLE public.app_role_aud;

CREATE TABLE public.app_role_aud (
                                     id uuid NOT NULL,
                                     rev int4 NOT NULL,
                                     revtype int2 NULL,
                                     code varchar(64) NULL,
                                     description varchar(255) NULL,
                                     "name" varchar(128) NULL,
                                     parent_role_id uuid NULL,
                                     is_system bool NULL,
                                     CONSTRAINT app_role_aud_pkey PRIMARY KEY (id, rev),
                                     CONSTRAINT fkhd8msl9b8usp6k1q9mk6drg4v FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

-- Permissions

ALTER TABLE public.app_role_aud OWNER TO app_user;
GRANT ALL ON TABLE public.app_role_aud TO app_user;


-- public.app_setting_aud definition

-- Drop table

-- DROP TABLE public.app_setting_aud;

CREATE TABLE public.app_setting_aud (
                                        id uuid NOT NULL,
                                        rev int4 NOT NULL,
                                        revtype int2 NULL,
                                        active bool NULL,
                                        "level" varchar(255) NULL,
                                        "namespace" varchar(255) NULL,
                                        outlet_id uuid NULL,
                                        setting_key varchar(255) NULL,
                                        setting_value varchar(255) NULL,
                                        tenant_id uuid NULL,
                                        terminal_id uuid NULL,
                                        value_type varchar(255) NULL,
                                        CONSTRAINT app_setting_aud_level_check CHECK (((level)::text = ANY ((ARRAY['GLOBAL'::character varying, 'TENANT'::character varying, 'OUTLET'::character varying, 'TERMINAL'::character varying])::text[]))),
	CONSTRAINT app_setting_aud_pkey PRIMARY KEY (id, rev),
	CONSTRAINT app_setting_aud_value_type_check CHECK (((value_type)::text = ANY ((ARRAY['STRING'::character varying, 'INT'::character varying, 'LONG'::character varying, 'DECIMAL'::character varying, 'BOOLEAN'::character varying, 'JSON'::character varying])::text[]))),
	CONSTRAINT fkj6h00vs9psxsykcux22mh73o FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

-- Permissions

ALTER TABLE public.app_setting_aud OWNER TO app_user;
GRANT ALL ON TABLE public.app_setting_aud TO app_user;


-- public.app_user_aud definition

-- Drop table

-- DROP TABLE public.app_user_aud;

CREATE TABLE public.app_user_aud (
                                     id uuid NOT NULL,
                                     rev int4 NOT NULL,
                                     revtype int2 NULL,
                                     tenant_id uuid NULL,
                                     approved_at timestamptz(6) NULL,
                                     approved_by uuid NULL,
                                     avatar_url varchar(255) NULL,
                                     display_name varchar(255) NULL,
                                     email varchar(255) NULL,
                                     first_name varchar(255) NULL,
                                     keycloak_id uuid NULL,
                                     last_login_at timestamptz(6) NULL,
                                     last_name varchar(255) NULL,
                                     locale varchar(255) NULL,
                                     phone varchar(255) NULL,
                                     status varchar(255) NULL,
                                     tenant_code varchar(255) NULL,
                                     time_zone varchar(255) NULL,
                                     username varchar(255) NULL,
                                     CONSTRAINT app_user_aud_pkey PRIMARY KEY (id, rev),
                                     CONSTRAINT app_user_aud_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING_APPROVAL'::character varying, 'ACTIVE'::character varying, 'SUSPENDED'::character varying, 'INVITED'::character varying])::text[]))),
	CONSTRAINT fklrwde4gab1o0jmxy358bobg55 FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

-- Permissions

ALTER TABLE public.app_user_aud OWNER TO app_user;
GRANT ALL ON TABLE public.app_user_aud TO app_user;


-- public.draw_aud definition

-- Drop table

-- DROP TABLE public.draw_aud;

CREATE TABLE public.draw_aud (
                                 id uuid NOT NULL,
                                 rev int4 NOT NULL,
                                 revtype int2 NULL,
                                 cutoff_sec int4 NULL,
                                 draw_source varchar(255) NULL,
                                 "locked" bool NULL,
                                 scheduled_at timestamptz(6) NULL,
                                 status varchar(255) NULL,
                                 system_generated bool NULL,
                                 draw_channel_id uuid NULL,
                                 CONSTRAINT draw_aud_pkey PRIMARY KEY (id, rev),
                                 CONSTRAINT draw_aud_status_check CHECK (((status)::text = ANY ((ARRAY['SCHEDULED'::character varying, 'OPEN'::character varying, 'CLOSED'::character varying, 'RESULTED'::character varying, 'SETTLED'::character varying, 'ARCHIVED'::character varying, 'CANCELED'::character varying])::text[]))),
	CONSTRAINT fksdknl6v195ub1athwllhrfg77 FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

-- Permissions

ALTER TABLE public.draw_aud OWNER TO app_user;
GRANT ALL ON TABLE public.draw_aud TO app_user;


-- public.draw_channel_aud definition

-- Drop table

-- DROP TABLE public.draw_channel_aud;

CREATE TABLE public.draw_channel_aud (
                                         id uuid NOT NULL,
                                         rev int4 NOT NULL,
                                         revtype int2 NULL,
                                         active bool NULL,
                                         code varchar(255) NULL,
                                         cutoff_sec int4 NULL,
                                         days_of_week varchar(255) NULL,
                                         draw_time time(0) NULL,
                                         external_channel_code varchar(255) NULL,
                                         external_game_key varchar(255) NULL,
                                         external_provider varchar(255) NULL,
                                         "name" varchar(255) NULL,
                                         sort_order int4 NULL,
                                         tenant_game_id uuid NULL,
                                         timezone varchar(255) NULL,
                                         CONSTRAINT draw_channel_aud_pkey PRIMARY KEY (id, rev),
                                         CONSTRAINT fkn76hvd845yhkt3jiqhu9kgf06 FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

-- Permissions

ALTER TABLE public.draw_channel_aud OWNER TO app_user;
GRANT ALL ON TABLE public.draw_channel_aud TO app_user;


-- public.draw_result_aud definition

-- Drop table

-- DROP TABLE public.draw_result_aud;

CREATE TABLE public.draw_result_aud (
                                        id uuid NOT NULL,
                                        rev int4 NOT NULL,
                                        revtype int2 NULL,
                                        numbers_extra jsonb NULL,
                                        numbers_main jsonb NULL,
                                        overridden_at timestamptz(6) NULL,
                                        overridden_by uuid NULL,
                                        override_reason varchar(255) NULL,
                                        raw_payload jsonb NULL,
                                        "source" varchar(255) NULL,
                                        status varchar(255) NULL,
                                        draw_id uuid NULL,
                                        CONSTRAINT draw_result_aud_pkey PRIMARY KEY (id, rev),
                                        CONSTRAINT fkc8p0miw46gdqh718ilrg4hsrk FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

-- Permissions

ALTER TABLE public.draw_result_aud OWNER TO app_user;
GRANT ALL ON TABLE public.draw_result_aud TO app_user;


-- public.game_aud definition

-- Drop table

-- DROP TABLE public.game_aud;

CREATE TABLE public.game_aud (
                                 id uuid NOT NULL,
                                 rev int4 NOT NULL,
                                 revtype int2 NULL,
                                 active bool NULL,
                                 category varchar(255) NULL,
                                 code varchar(255) NULL,
                                 combination varchar(255) NULL,
                                 description varchar(255) NULL,
                                 max_digits int4 NULL,
                                 min_digits int4 NULL,
                                 "name" varchar(255) NULL,
                                 sort_order int4 NULL,
                                 CONSTRAINT game_aud_pkey PRIMARY KEY (id, rev),
                                 CONSTRAINT fknerhptujtk5wtpdvlsmfptjkj FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

-- Permissions

ALTER TABLE public.game_aud OWNER TO app_user;
GRANT ALL ON TABLE public.game_aud TO app_user;


-- public.i18n_override_aud definition

-- Drop table

-- DROP TABLE public.i18n_override_aud;

CREATE TABLE public.i18n_override_aud (
                                          id uuid NOT NULL,
                                          rev int4 NOT NULL,
                                          revtype int2 NULL,
                                          i18n_key varchar(255) NULL,
                                          i18n_value varchar(255) NULL,
                                          locale varchar(255) NULL,
                                          tenant_id uuid NULL,
                                          CONSTRAINT i18n_override_aud_pkey PRIMARY KEY (id, rev),
                                          CONSTRAINT fkrn1ce8ol108nuqfoay2ximjsr FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

-- Permissions

ALTER TABLE public.i18n_override_aud OWNER TO app_user;
GRANT ALL ON TABLE public.i18n_override_aud TO app_user;


-- public.page_model_aud definition

-- Drop table

-- DROP TABLE public.page_model_aud;

CREATE TABLE public.page_model_aud (
                                       id uuid NOT NULL,
                                       rev int4 NOT NULL,
                                       revtype int2 NULL,
                                       logical_id varchar(255) NULL,
                                       model jsonb NULL,
                                       published_at timestamptz(6) NULL,
                                       schema_version int4 NULL,
                                       "scope" varchar(255) NULL,
                                       slug varchar(255) NULL,
                                       status varchar(255) NULL,
                                       template_id uuid NULL,
                                       CONSTRAINT page_model_aud_pkey PRIMARY KEY (id, rev),
                                       CONSTRAINT page_model_aud_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PUBLISHED'::character varying, 'ARCHIVED'::character varying])::text[]))),
	CONSTRAINT fk9s11d58873ncny4anx6xy4p4i FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

-- Permissions

ALTER TABLE public.page_model_aud OWNER TO app_user;
GRANT ALL ON TABLE public.page_model_aud TO app_user;


-- public.page_model_template_aud definition

-- Drop table

-- DROP TABLE public.page_model_template_aud;

CREATE TABLE public.page_model_template_aud (
                                                id uuid NOT NULL,
                                                rev int4 NOT NULL,
                                                revtype int2 NULL,
                                                description varchar(255) NULL,
                                                is_default bool NULL,
                                                is_system bool NULL,
                                                "label" varchar(255) NULL,
                                                logical_id varchar(255) NULL,
                                                model jsonb NULL,
                                                schema_version int4 NULL,
                                                tenant_id uuid NULL,
                                                CONSTRAINT page_model_template_aud_pkey PRIMARY KEY (id, rev),
                                                CONSTRAINT fk1fte8c6kyc1erajy2x83visw3 FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

-- Permissions

ALTER TABLE public.page_model_template_aud OWNER TO app_user;
GRANT ALL ON TABLE public.page_model_template_aud TO app_user;


-- public.payout_aud definition

-- Drop table

-- DROP TABLE public.payout_aud;

CREATE TABLE public.payout_aud (
                                   id uuid NOT NULL,
                                   rev int4 NOT NULL,
                                   revtype int2 NULL,
                                   amount_cents int8 NULL,
                                   approved_at timestamptz(6) NULL,
                                   currency varchar(3) NULL,
                                   outlet_id uuid NULL,
                                   paid_at timestamptz(6) NULL,
                                   paid_by_user_id uuid NULL,
                                   rejected_at timestamptz(6) NULL,
                                   rejected_reason varchar(255) NULL,
                                   selling_outlet_id uuid NULL,
                                   selling_session_id uuid NULL,
                                   session_id uuid NULL,
                                   status varchar(255) NULL,
                                   terminal_id uuid NULL,
                                   ticket_id uuid NULL,
                                   CONSTRAINT payout_aud_pkey PRIMARY KEY (id, rev),
                                   CONSTRAINT fkrbqxwnos9beae3n6jpkkg51hm FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

-- Permissions

ALTER TABLE public.payout_aud OWNER TO app_user;
GRANT ALL ON TABLE public.payout_aud TO app_user;


-- public.permission_aud definition

-- Drop table

-- DROP TABLE public.permission_aud;

CREATE TABLE public.permission_aud (
                                       code varchar(128) NOT NULL,
                                       rev int4 NOT NULL,
                                       revtype int2 NULL,
                                       category varchar(64) NULL,
                                       description varchar(255) NULL,
                                       "name" varchar(128) NULL,
                                       CONSTRAINT permission_aud_pkey PRIMARY KEY (code, rev),
                                       CONSTRAINT fk8p00qhf8aau42hacp13k6x5hh FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

-- Permissions

ALTER TABLE public.permission_aud OWNER TO app_user;
GRANT ALL ON TABLE public.permission_aud TO app_user;


-- public.plan_aud definition

-- Drop table

-- DROP TABLE public.plan_aud;

CREATE TABLE public.plan_aud (
                                 id uuid NOT NULL,
                                 rev int4 NOT NULL,
                                 revtype int2 NULL,
                                 billing_frequency varchar(16) NULL,
                                 code varchar(64) NULL,
                                 currency varchar(3) NULL,
                                 description varchar(255) NULL,
                                 features jsonb NULL,
                                 "name" varchar(128) NULL,
                                 price_amount numeric(38, 2) NULL,
                                 public_plan bool NULL,
                                 CONSTRAINT plan_aud_billing_frequency_check CHECK (((billing_frequency)::text = ANY ((ARRAY['MONTH'::character varying, 'YEAR'::character varying])::text[]))),
	CONSTRAINT plan_aud_pkey PRIMARY KEY (id, rev),
	CONSTRAINT fkg4e0dy5q4mgpt0dq0uia370bx FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

-- Permissions

ALTER TABLE public.plan_aud OWNER TO app_user;
GRANT ALL ON TABLE public.plan_aud TO app_user;


-- public.pos_session_aud definition

-- Drop table

-- DROP TABLE public.pos_session_aud;

CREATE TABLE public.pos_session_aud (
                                        id uuid NOT NULL,
                                        rev int4 NOT NULL,
                                        revtype int2 NULL,
                                        closed_at timestamptz(6) NULL,
                                        closing_amount numeric(14, 2) NULL,
                                        meta jsonb NULL,
                                        opened_at timestamptz(6) NULL,
                                        opening_float numeric(14, 2) NULL,
                                        outlet_id uuid NULL,
                                        status varchar(16) NULL,
                                        terminal_id uuid NULL,
                                        user_id uuid NULL,
                                        CONSTRAINT pos_session_aud_pkey PRIMARY KEY (id, rev),
                                        CONSTRAINT pos_session_aud_status_check CHECK (((status)::text = ANY ((ARRAY['OPENED'::character varying, 'CLOSED'::character varying, 'SETTLED'::character varying])::text[]))),
	CONSTRAINT fksdf6p05vej9ggcr49uiq6vvq4 FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

-- Permissions

ALTER TABLE public.pos_session_aud OWNER TO app_user;
GRANT ALL ON TABLE public.pos_session_aud TO app_user;


-- public.pos_session_totals_aud definition

-- Drop table

-- DROP TABLE public.pos_session_totals_aud;

CREATE TABLE public.pos_session_totals_aud (
                                               session_id uuid NOT NULL,
                                               rev int4 NOT NULL,
                                               revtype int2 NULL,
                                               created_at timestamptz(6) NULL,
                                               created_by uuid NULL,
                                               deleted_at timestamptz(6) NULL,
                                               gross_margin numeric(14, 2) NULL,
                                               tenant_id uuid NULL,
                                               total_payout numeric(14, 2) NULL,
                                               total_stake numeric(14, 2) NULL,
                                               total_tickets int8 NULL,
                                               updated_at timestamptz(6) NULL,
                                               updated_by uuid NULL,
                                               CONSTRAINT pos_session_totals_aud_pkey PRIMARY KEY (rev, session_id),
                                               CONSTRAINT fkb6fuh3vr9yqhowkhb0o2sxdv6 FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

-- Permissions

ALTER TABLE public.pos_session_totals_aud OWNER TO app_user;
GRANT ALL ON TABLE public.pos_session_totals_aud TO app_user;


-- public.role_permission definition

-- Drop table

-- DROP TABLE public.role_permission;

CREATE TABLE public.role_permission (
                                        permission_code varchar(255) NOT NULL,
                                        created_at timestamptz(6) NULL,
                                        created_by uuid NULL,
                                        deleted_at timestamptz(6) NULL,
                                        updated_at timestamptz(6) NULL,
                                        updated_by uuid NULL,
                                        "version" int8 NOT NULL,
                                        role_id uuid NOT NULL,
                                        CONSTRAINT role_permission_pkey PRIMARY KEY (permission_code, role_id),
                                        CONSTRAINT fk54dlsmecjg5cjcuyxjspj6coo FOREIGN KEY (permission_code) REFERENCES public."permission"(code)
);

-- Permissions

ALTER TABLE public.role_permission OWNER TO app_user;
GRANT ALL ON TABLE public.role_permission TO app_user;


-- public.role_permission_aud definition

-- Drop table

-- DROP TABLE public.role_permission_aud;

CREATE TABLE public.role_permission_aud (
                                            permission_code varchar(255) NOT NULL,
                                            role_id uuid NOT NULL,
                                            rev int4 NOT NULL,
                                            revtype int2 NULL,
                                            CONSTRAINT role_permission_aud_pkey PRIMARY KEY (permission_code, rev, role_id),
                                            CONSTRAINT fk1lylwgm4npbtack8oii44ojfp FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

-- Permissions

ALTER TABLE public.role_permission_aud OWNER TO app_user;
GRANT ALL ON TABLE public.role_permission_aud TO app_user;


-- public.subscription_aud definition

-- Drop table

-- DROP TABLE public.subscription_aud;

CREATE TABLE public.subscription_aud (
                                         id uuid NOT NULL,
                                         rev int4 NOT NULL,
                                         revtype int2 NULL,
                                         billing_external_id varchar(128) NULL,
                                         billing_provider varchar(16) NULL,
                                         cancel_at_period_end bool NULL,
                                         current_period_end timestamptz(6) NULL,
                                         current_period_start timestamptz(6) NULL,
                                         meta jsonb NULL,
                                         status varchar(255) NULL,
                                         plan_id uuid NULL,
                                         CONSTRAINT subscription_aud_billing_provider_check CHECK (((billing_provider)::text = ANY ((ARRAY['STRIPE'::character varying, 'ADYEN'::character varying, 'LOG_ONLY'::character varying, 'NONE'::character varying])::text[]))),
	CONSTRAINT subscription_aud_pkey PRIMARY KEY (id, rev),
	CONSTRAINT subscription_aud_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'TRIALING'::character varying, 'CANCELED'::character varying, 'PAST_DUE'::character varying, 'SUSPENDED'::character varying])::text[]))),
	CONSTRAINT fkni5i9nnkyymy3o3km6rtsb44a FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

-- Permissions

ALTER TABLE public.subscription_aud OWNER TO app_user;
GRANT ALL ON TABLE public.subscription_aud TO app_user;


-- public.tenant_aud definition

-- Drop table

-- DROP TABLE public.tenant_aud;

CREATE TABLE public.tenant_aud (
                                   id uuid NOT NULL,
                                   rev int4 NOT NULL,
                                   revtype int2 NULL,
                                   active_theme_id uuid NULL,
                                   address_id uuid NULL,
                                   code varchar(64) NULL,
                                   currency varchar(3) NULL,
                                   "name" varchar(255) NULL,
                                   status varchar(16) NULL,
                                   timezone varchar(64) NULL,
                                   "type" varchar(32) NULL,
                                   CONSTRAINT tenant_aud_pkey PRIMARY KEY (id, rev),
                                   CONSTRAINT tenant_aud_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'ACTIVE'::character varying, 'SUSPENDED'::character varying, 'REJECTED'::character varying, 'ARCHIVED'::character varying])::text[]))),
	CONSTRAINT tenant_aud_type_check CHECK (((type)::text = ANY ((ARRAY['BORLETTE'::character varying, 'RESEAU'::character varying, 'AMBULANT'::character varying])::text[]))),
	CONSTRAINT fkshcowg257idhnje9o6xyyydwx FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

-- Permissions

ALTER TABLE public.tenant_aud OWNER TO app_user;
GRANT ALL ON TABLE public.tenant_aud TO app_user;


-- public.tenant_user_aud definition

-- Drop table

-- DROP TABLE public.tenant_user_aud;

CREATE TABLE public.tenant_user_aud (
                                        id uuid NOT NULL,
                                        rev int4 NOT NULL,
                                        revtype int2 NULL,
                                        autonomy_level varchar(255) NULL,
                                        is_owner bool NULL,
                                        role_id uuid NULL,
                                        tenant_id uuid NULL,
                                        user_id varchar(255) NULL,
                                        CONSTRAINT tenant_user_aud_pkey PRIMARY KEY (id, rev),
                                        CONSTRAINT fk973hy275rjrqvddjivk7vd1jx FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

-- Permissions

ALTER TABLE public.tenant_user_aud OWNER TO app_user;
GRANT ALL ON TABLE public.tenant_user_aud TO app_user;


-- public.theme_aud definition

-- Drop table

-- DROP TABLE public.theme_aud;

CREATE TABLE public.theme_aud (
                                  id uuid NOT NULL,
                                  rev int4 NOT NULL,
                                  revtype int2 NULL,
                                  base_preset_id varchar(128) NULL,
                                  css_vars_json jsonb NULL,
                                  density int2 NULL,
                                  "label" varchar(160) NULL,
                                  "mode" varchar(10) NULL,
                                  palette_json jsonb NULL,
                                  status varchar(20) NULL,
                                  theme_version int4 NULL,
                                  tokens_json jsonb NULL,
                                  CONSTRAINT theme_aud_mode_check CHECK (((mode)::text = ANY ((ARRAY['LIGHT'::character varying, 'DARK'::character varying, 'SYSTEM'::character varying])::text[]))),
	CONSTRAINT theme_aud_pkey PRIMARY KEY (id, rev),
	CONSTRAINT theme_aud_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PUBLISHED'::character varying, 'ARCHIVED'::character varying])::text[]))),
	CONSTRAINT fkr8ei8lvyl18n7le97r6fdl1ar FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

-- Permissions

ALTER TABLE public.theme_aud OWNER TO app_user;
GRANT ALL ON TABLE public.theme_aud TO app_user;


-- public.user_preference_aud definition

-- Drop table

-- DROP TABLE public.user_preference_aud;

CREATE TABLE public.user_preference_aud (
                                            id uuid NOT NULL,
                                            rev int4 NOT NULL,
                                            revtype int2 NULL,
                                            density int2 NULL,
                                            locale varchar(255) NULL,
                                            theme_mode varchar(255) NULL,
                                            user_id uuid NULL,
                                            CONSTRAINT user_preference_aud_pkey PRIMARY KEY (id, rev),
                                            CONSTRAINT user_preference_aud_theme_mode_check CHECK (((theme_mode)::text = ANY ((ARRAY['LIGHT'::character varying, 'DARK'::character varying, 'SYSTEM'::character varying])::text[]))),
	CONSTRAINT fksggtuxshu4pvqr769qutxmvbd FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

-- Permissions
ALTER TABLE public.user_preference_aud OWNER TO app_user;
GRANT ALL ON TABLE public.user_preference_aud TO app_user;




-- Permissions
GRANT ALL ON SCHEMA public TO pg_database_owner;
GRANT USAGE ON SCHEMA public TO public;
GRANT ALL ON SCHEMA public TO app_user;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT DELETE, INSERT, TRIGGER, SELECT, TRUNCATE, UPDATE, REFERENCES, MAINTAIN ON TABLES TO app_user;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT SELECT, USAGE, UPDATE ON SEQUENCES TO app_user;
