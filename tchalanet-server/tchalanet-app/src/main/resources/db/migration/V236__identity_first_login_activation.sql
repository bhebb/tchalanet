ALTER TABLE app_user
  ADD COLUMN IF NOT EXISTS must_change_password boolean NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS must_complete_profile boolean NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS first_login_completed_at timestamptz,
  ADD COLUMN IF NOT EXISTS temporary_credential_issued_at timestamptz;
