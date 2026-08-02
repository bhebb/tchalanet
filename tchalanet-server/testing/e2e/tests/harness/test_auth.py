import base64
import json

from tch_e2e.auth import FirebaseEmulatorAuth, env_or_default


def _claims(token: str) -> dict:
    payload = token.split(".")[1]
    padding = "=" * (-len(payload) % 4)
    return json.loads(base64.urlsafe_b64decode(payload + padding))


def test_env_or_default_treats_blank_as_missing(monkeypatch):
    monkeypatch.setenv("TCH_SUPER_ADMIN_USERNAME", " ")

    assert env_or_default("TCH_SUPER_ADMIN_USERNAME", "super_admin") == "super_admin"


def test_firebase_emulator_seeded_super_admin_ignores_blank_identity_secrets(monkeypatch):
    monkeypatch.setenv("TCH_FIREBASE_PROJECT_ID", "demo-tchalanet-local")
    monkeypatch.setenv("TCH_SUPER_ADMIN_USERNAME", "")
    monkeypatch.setenv("TCH_TENANT_ADMIN_USERNAME", "")
    monkeypatch.setenv("TCH_SELLER_USERNAME", "")

    token = FirebaseEmulatorAuth.from_env().password_grant(
        username=env_or_default("TCH_SUPER_ADMIN_USERNAME", "superadmin"), password=""
    )

    claims = _claims(token)
    assert claims["iss"] == "https://securetoken.google.com/demo-tchalanet-local"
    assert claims["aud"] == "demo-tchalanet-local"
    assert claims["sub"] == "00000000-0000-0000-0000-000000010001"
    assert claims["email"] == "super_admin@localtest.me"


def test_firebase_emulator_accepts_seeded_superadmin_alias(monkeypatch):
    monkeypatch.delenv("TCH_SUPER_ADMIN_USERNAME", raising=False)

    token = FirebaseEmulatorAuth.from_env().password_grant(username="superadmin", password="")

    assert _claims(token)["sub"] == "00000000-0000-0000-0000-000000010001"
