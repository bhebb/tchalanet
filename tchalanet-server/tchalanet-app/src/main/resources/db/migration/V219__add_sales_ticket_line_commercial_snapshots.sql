ALTER TABLE sales_ticket_line
  ADD COLUMN selection_policy_snapshot varchar(32),
  ADD COLUMN bet_option_label_snapshot varchar(128);
