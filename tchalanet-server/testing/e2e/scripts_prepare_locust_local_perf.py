"""Prepare local-perf seller-terminal identities for Locust.

This is intentionally local-only. It mirrors active seller terminals into
``seller_terminal_external_identity`` as provider ``LOCAL_PERF`` with
``external_subject = seller_terminal.id`` so Locust can mint seller-terminal JWTs
without loading Firebase Auth Emulator.
"""
from __future__ import annotations

import os
import subprocess
import textwrap
from pathlib import Path


def _env(name: str, default: str) -> str:
    value = os.environ.get(name)
    return value if value and value.strip() else default


def main() -> None:
    container = _env("TCH_LOCUST_POSTGRES_CONTAINER", "tchl-postgres-dev")
    user = _env("TCH_LOCUST_POSTGRES_USER", "postgres")
    password = _env("TCH_LOCUST_POSTGRES_PASSWORD", "devpass")
    database = _env("TCH_LOCUST_POSTGRES_DB", "tchalanet_db")
    issuer = _env("TCH_LOCAL_JWT_ISSUER", "tchalanet-local")
    issuer_sql = issuer.replace("'", "''")
    mark_ready = _env("TCH_LOCUST_MARK_TERMINALS_READY", "true").lower() in {
        "1",
        "true",
        "yes",
    }

    sql = textwrap.dedent(
        f"""
        INSERT INTO seller_terminal_external_identity (
          id, tenant_id, seller_terminal_id, provider, issuer, external_subject,
          created_at, updated_at
        )
        SELECT
          gen_random_uuid(), st.tenant_id, st.id, 'LOCAL_PERF', '{issuer_sql}', st.id::text,
          now(), now()
        FROM seller_terminal st
        WHERE st.status = 'ACTIVE'
          AND st.deleted_at IS NULL
          AND NOT EXISTS (
            SELECT 1
            FROM seller_terminal_external_identity existing
            WHERE existing.provider = 'LOCAL_PERF'
              AND existing.issuer = '{issuer_sql}'
              AND existing.external_subject = st.id::text
          );

        {"UPDATE seller_terminal SET must_change_pin = false, updated_at = now() WHERE status = 'ACTIVE' AND deleted_at IS NULL;" if mark_ready else ""}

        SELECT provider, issuer, count(*) AS bound_terminals
        FROM seller_terminal_external_identity
        WHERE provider = 'LOCAL_PERF'
          AND issuer = '{issuer_sql}'
        GROUP BY provider, issuer
        ORDER BY provider, issuer;
        """
    ).strip()

    command = [
        "docker",
        "exec",
        "-e",
        f"PGPASSWORD={password}",
        container,
        "psql",
        "-h",
        "127.0.0.1",
        "-U",
        user,
        "-d",
        database,
        "-v",
        "ON_ERROR_STOP=1",
        "-c",
        sql,
    ]
    subprocess.run(command, check=True)

    selected = subprocess.run(
        [
            "docker",
            "exec",
            "-e",
            f"PGPASSWORD={password}",
            container,
            "psql",
            "-h",
            "127.0.0.1",
            "-U",
            user,
            "-d",
            database,
            "-At",
            "-F",
            "\t",
            "-c",
            textwrap.dedent(
                f"""
                SELECT st.tenant_id, t.code, st.id, st.terminal_code, st.display_name
                FROM seller_terminal st
                JOIN tenant t ON t.id = st.tenant_id
                WHERE st.status = 'ACTIVE'
                  AND st.deleted_at IS NULL
                  AND EXISTS (
                    SELECT 1
                    FROM seller_terminal_external_identity existing
                    WHERE existing.provider = 'LOCAL_PERF'
                      AND existing.issuer = '{issuer_sql}'
                      AND existing.external_subject = st.id::text
                  )
                ORDER BY
                  (
                    SELECT count(*)
                    FROM draw d
                    WHERE d.tenant_id = st.tenant_id
                      AND d.status = 'OPEN'
                  ) DESC,
                  st.created_at,
                  st.id
                LIMIT 1;
                """
            ).strip(),
        ],
        check=True,
        capture_output=True,
        text=True,
    ).stdout.strip()

    if selected:
        tenant_id, tenant_code, terminal_id, terminal_code, display_name = selected.split("\t")
        target = Path("target/locust/local_perf.env")
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(
            "\n".join(
                [
                    f"TCH_LOAD_TENANT_ID={tenant_id}",
                    f"TCH_LOAD_TENANT_CODE={tenant_code}",
                    f"TCH_LOAD_SELLER_TERMINAL_ID={terminal_id}",
                    f"TCH_LOAD_SELLER_TERMINAL_CODE={terminal_code}",
                    f"TCH_LOAD_SELLER_TERMINAL_NAME={display_name}",
                    "",
                ]
            ),
            encoding="utf-8",
        )
        print(f"Wrote {target} for Locust seller-terminal discovery.")


if __name__ == "__main__":
    main()
