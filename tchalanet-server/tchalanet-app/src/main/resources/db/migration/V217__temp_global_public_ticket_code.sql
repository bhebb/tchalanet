-- Public ticket verification receives only public_code, so the code must identify one ticket
-- across tenants. Keep ticket_code and verification_code tenant-scoped.

ALTER TABLE sales_ticket
  DROP CONSTRAINT IF EXISTS uk_sales_ticket__public_code;

ALTER TABLE sales_ticket
  DROP CONSTRAINT IF EXISTS uk_sales_ticket_public_code;

ALTER TABLE sales_ticket
  ADD CONSTRAINT uk_sales_ticket__public_code UNIQUE (public_code);
