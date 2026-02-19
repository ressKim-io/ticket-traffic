#!/bin/bash
# SportsTix Database Initialization
# Creates per-service databases and restricted users (least privilege principle).
# Flyway uses postgres superuser for DDL; app users get DML only.

set -euo pipefail

# Service definitions: name, database, user, password env var
SERVICES=(
  "auth:sportstix_auth:auth_user:${AUTH_DB_PASSWORD:-auth_pass}"
  "game:sportstix_game:game_user:${GAME_DB_PASSWORD:-game_pass}"
  "booking:sportstix_booking:booking_user:${BOOKING_DB_PASSWORD:-booking_pass}"
  "payment:sportstix_payment:payment_user:${PAYMENT_DB_PASSWORD:-payment_pass}"
  "admin:sportstix_admin:admin_user:${ADMIN_DB_PASSWORD:-admin_pass}"
)

for entry in "${SERVICES[@]}"; do
  IFS=':' read -r svc db user pass <<< "$entry"

  echo "=== Setting up $svc: database=$db, user=$user ==="

  # 1. Create database (skip if exists)
  psql -v ON_ERROR_STOP=0 -U "$POSTGRES_USER" -d postgres -c "CREATE DATABASE $db;" 2>/dev/null || true

  # 2. Create app user (skip if exists)
  psql -v ON_ERROR_STOP=0 -U "$POSTGRES_USER" -d postgres -c "CREATE USER $user WITH PASSWORD '$pass';" 2>/dev/null || true

  # 3. Restrict connections: only the owner and the app user can connect
  psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d postgres <<-EOSQL
    REVOKE CONNECT ON DATABASE $db FROM PUBLIC;
    GRANT CONNECT ON DATABASE $db TO $user;
EOSQL

  # 4. Grant DML privileges on current and future objects
  psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$db" <<-EOSQL
    GRANT USAGE ON SCHEMA public TO $user;
    ALTER DEFAULT PRIVILEGES FOR USER $POSTGRES_USER IN SCHEMA public
      GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO $user;
    ALTER DEFAULT PRIVILEGES FOR USER $POSTGRES_USER IN SCHEMA public
      GRANT USAGE, SELECT ON SEQUENCES TO $user;
EOSQL

  echo "=== Done: $svc ==="
done

echo "All databases and users initialized successfully."
