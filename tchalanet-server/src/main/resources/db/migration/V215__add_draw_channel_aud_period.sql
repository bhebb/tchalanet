-- Update audit table for draw_channel to include period field

ALTER TABLE draw_channel_aud
  ADD COLUMN period varchar(32);

COMMENT ON COLUMN draw_channel_aud.period IS 'Audit field for draw_channel.period';

