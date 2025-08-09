#!/bin/sh
set -e

echo "Waiting for Keycloak realm '$REALM' to be ready..."
until python - <<'PY'
import os, sys, requests
try:
    r = requests.get(
        f"{os.environ['KEYCLOAK_BASE']}/realms/{os.environ['REALM']}/.well-known/openid-configuration",
        timeout=5
    )
    r.raise_for_status()
except Exception:
    sys.exit(1)
PY
do
  echo "Realm '$REALM' not ready yet..."
  sleep 2
done
echo "Keycloak realm '$REALM' is ready!"

export TOKEN_URL="$KEYCLOAK_BASE/realms/$REALM/protocol/openid-connect/token"

echo "Requesting access token..."
ACCESS_TOKEN=$(python - <<'PY'
import os, requests
r = requests.post(
    os.environ["TOKEN_URL"],
    data={
        "grant_type": "password",
        "client_id": os.environ["CLIENT_ID"],
        "username": os.environ["USERNAME"],
        "password": os.environ["PASSWORD"],
    },
    timeout=20
)
r.raise_for_status()
print(r.json()["access_token"])
PY
)

echo "Token acquired for realm '$REALM'."

echo "Waiting for OpenAPI schema at '$OPENAPI_URL'..."
until python - <<'PY'
import os, sys, requests
try:
    r = requests.get(os.environ['OPENAPI_URL'], timeout=5)
    if r.status_code < 500:
        sys.exit(0)
except Exception:
    pass
sys.exit(1)
PY
do
  echo "OpenAPI schema not ready yet..."
  sleep 2
done
echo "OpenAPI schema is ready!"

echo "Running Schemathesis tests..."
schemathesis run "$OPENAPI_URL" \
  --url "$BASE_URL" \
  --header "Authorization: Bearer $ACCESS_TOKEN" \
  --checks=all \
  --rate-limit=10/m \
  --workers=4 \
  --wait-for-schema=60
