"""Notification visibility for tenant and platform administration audiences."""
from __future__ import annotations

import uuid

import pytest

from tch_e2e.api_response import assert_ok, get_data
from tch_e2e.auth import E2EAuth
from tch_e2e.client import ApiClient
from tch_e2e.config import SeedIds


def _matching_dedupe_keys(client: ApiClient, path: str, dedupe_key: str) -> set[str]:
    response = client.get(path, params={"q": dedupe_key, "page": 0, "size": 20})
    data = get_data(response)
    items = data.get("items") or data.get("content") or []
    return {
        str(item.get("dedupeKey"))
        for item in items
        if isinstance(item, dict) and item.get("dedupeKey")
    }


def _create_owner_client(
    tenant_admin_client: ApiClient, keycloak: E2EAuth, base_url: str
) -> ApiClient:
    if not hasattr(keycloak, "mint") or not hasattr(keycloak, "uid_for_email"):
        pytest.skip("Notification audience E2E requires the Firebase Auth Emulator.")

    suffix = uuid.uuid4().hex[:10]
    email = f"e2e-notification-owner-{suffix}@test.test"
    response = tenant_admin_client.post(
        "/admin/identity/users",
        json={
            "email": email,
            "firstName": "E2E",
            "lastName": "Owner",
            "role": "TENANT_OWNER",
        },
    )
    assert_ok(response)

    firebase = keycloak
    subject = firebase.uid_for_email(email)  # type: ignore[attr-defined]
    token = firebase.mint(subject=subject, email=email)  # type: ignore[attr-defined]
    return ApiClient(base_url=base_url, token=token)


@pytest.mark.L2
@pytest.mark.auth_context
def test_tenant_owner_and_admin_see_tenant_notice_but_superadmin_does_not(
    base_url: str,
    keycloak: E2EAuth,
    seed_ids: SeedIds,
    super_admin_client: ApiClient,
    tenant_admin_client: ApiClient,
) -> None:
    """A tenant-admin audience is visible to both tenant roles, never to platform scope."""
    owner_client = _create_owner_client(tenant_admin_client, keycloak, base_url)
    suffix = uuid.uuid4().hex
    dedupe_key = f"e2e.notification.audience:{suffix}"
    try:
        created = super_admin_client.post(
            f"/platform/tenants/{seed_ids.tenant_id}/notifications/announcements",
            json={
                "sourceId": suffix,
                "dedupeKey": dedupe_key,
                "audienceType": "TENANT_ADMINS",
                "severity": "INFO",
                "kind": "INFO",
                "category": "SYSTEM",
                "titleText": "E2E notification audience",
                "messageText": "Tenant owner and admin must both see this notice.",
                "translations": {
                    "en": {
                        "title": "E2E notification audience",
                        "body": "Tenant owner and admin must both see this notice.",
                    },
                    "fr": {
                        "title": "Audience de notification E2E",
                        "body": "Le proprietaire et l'admin du tenant doivent voir cet avis.",
                    },
                    "ht": {
                        "title": "Notifikasyon E2E",
                        "body": "Pwopriyete a ak admin tenant lan dwe we avi sa a.",
                    },
                },
                "payload": {"scenario": "notification-audience", "run": suffix},
                "actionType": "E2E_NOTIFICATION_AUDIENCE",
                "actionUrl": "/app/admin",
                "channels": ["WEB"],
            },
        )
        assert_ok(created, expected=(200, 201))

        assert dedupe_key in _matching_dedupe_keys(
            tenant_admin_client, "/tenant/me/notifications", dedupe_key
        ), "TENANT_ADMIN cannot see its tenant notification"
        assert dedupe_key in _matching_dedupe_keys(
            owner_client, "/tenant/me/notifications", dedupe_key
        ), "TENANT_OWNER cannot see the same tenant notification"
        assert dedupe_key not in _matching_dedupe_keys(
            super_admin_client, "/platform/notifications", dedupe_key
        ), "SUPER_ADMIN received a tenant-scoped notification in platform scope"
    finally:
        owner_client.close()
