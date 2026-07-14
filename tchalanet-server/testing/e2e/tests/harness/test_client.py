import pytest

from tch_e2e.client import _resolve_timeout


def test_http_timeout_defaults_to_30_seconds(monkeypatch):
    monkeypatch.delenv("TCH_E2E_HTTP_TIMEOUT", raising=False)

    assert _resolve_timeout() == 30.0


def test_http_timeout_can_be_overridden(monkeypatch):
    monkeypatch.setenv("TCH_E2E_HTTP_TIMEOUT", "120")

    assert _resolve_timeout() == 120.0


def test_http_timeout_rejects_invalid_values(monkeypatch):
    monkeypatch.setenv("TCH_E2E_HTTP_TIMEOUT", "0")

    with pytest.raises(ValueError, match="greater than zero"):
        _resolve_timeout()
