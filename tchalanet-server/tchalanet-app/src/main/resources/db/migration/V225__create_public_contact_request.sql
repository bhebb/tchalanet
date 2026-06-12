-- Contact request reference sequence
CREATE SEQUENCE contact_request_ref_seq
    START WITH 1
    INCREMENT BY 1;

-- Public contact request table (platform-level, non-tenanted)
CREATE TABLE public_contact_request
(
    id                     uuid         PRIMARY KEY,
    reference              varchar(32)  NOT NULL,
    intent                 varchar(40)  NOT NULL,
    full_name              varchar(160) NOT NULL,
    phone                  varchar(64)  NOT NULL,
    email                  varchar(160),
    organization_name      varchar(180),
    city                   varchar(120),
    country                varchar(120),
    outlet_count           integer,
    preferred_contact_time varchar(120),
    message                text         NOT NULL,
    consent_to_contact     boolean      NOT NULL,
    status                 varchar(40)  NOT NULL,
    internal_notes         text,
    external_tool          varchar(80),
    external_reference     varchar(160),
    exported_at            timestamptz,
    source_page            varchar(160),
    -- BaseEntity audit fields
    created_at             timestamptz,
    updated_at             timestamptz,
    created_by             uuid,
    updated_by             uuid,
    deleted_at             timestamptz,
    deleted_by             uuid,
    version                bigint       NOT NULL DEFAULT 0,
    CONSTRAINT public_contact_request_reference_uq UNIQUE (reference)
);

CREATE INDEX ix_contact_request__status     ON public_contact_request (status);
CREATE INDEX ix_contact_request__intent     ON public_contact_request (intent);
CREATE INDEX ix_contact_request__created_at ON public_contact_request (created_at DESC);

-- Envers audit table
CREATE TABLE public_contact_request_aud
(
    id                     uuid         NOT NULL,
    rev                    integer      NOT NULL,
    revtype                smallint,
    created_at             timestamptz,
    created_by             uuid,
    updated_at             timestamptz,
    updated_by             uuid,
    deleted_at             timestamptz,
    deleted_by             uuid,
    version                bigint,
    reference              varchar(32),
    intent                 varchar(40),
    full_name              varchar(160),
    phone                  varchar(64),
    email                  varchar(160),
    organization_name      varchar(180),
    city                   varchar(120),
    country                varchar(120),
    outlet_count           integer,
    preferred_contact_time varchar(120),
    message                text,
    consent_to_contact     boolean,
    status                 varchar(40),
    internal_notes         text,
    external_tool          varchar(80),
    external_reference     varchar(160),
    exported_at            timestamptz,
    source_page            varchar(160),
    exported_at_mod        boolean,
    CONSTRAINT pk_public_contact_request_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_public_contact_request_aud__revinfo FOREIGN KEY (rev) REFERENCES revinfo (rev)
);
