#!/bin/sh
set -e

wait_for_http_200() {
  local name="$1"
  local url="$2"
  echo "Waiting for $name at $url ..."
  until [ "$(curl -s -o /dev/null -w "%{http_code}" "$url")" -eq 200 ]; do
    echo "$name not ready yet..."
    sleep 2
  done
  echo "$name is ready!"
}

wait_for_token() {
  echo "Waiting to obtain Keycloak token for realm '$REALM'..."
  until TOKEN=$(curl -s -X POST \
      "$KEYCLOAK_BASE/realms/$REALM/protocol/openid-connect/token" \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "grant_type=password" \
      -d "client_id=$CLIENT_ID" \
      -d "username=$USERNAME" \
      -d "password=$PASSWORD" \
      | jq -r '.access_token' 2>/dev/null) && [ "$TOKEN" != "null" ] && [ -n "$TOKEN" ]; do
    echo "Token not ready yet..."
    sleep 2
  done
  echo "Obtained Keycloak token!"
}

wait_for_http_200 "Keycloak realm" "$KEYCLOAK_BASE/realms/$REALM/.well-known/openid-configuration"
wait_for_http_200 "Quarkus OpenAPI" "$OPENAPI_URL"
wait_for_token

echo "All dependencies ready. Running Schemathesis..."

TIMESTAMP=$(date +"%Y%m%d-%H%M%S")
REPORT_XML="/reports/schemathesis-report-$TIMESTAMP.xml"

schemathesis run "$OPENAPI_URL" \
  --checks all \
  --url "$BASE_URL" \
  --header "Authorization: Bearer $TOKEN" \
  --rate-limit=10/m \
  --workers=4 \
  --wait-for-schema=60 \
  --max-examples=50 \
  --suppress-health-check=filter_too_much \
  --report junit > "$REPORT_XML" 2>&1 | tee -a /reports/schemathesis-output.log || true

echo "Run complete. XML report saved to $REPORT_XML"
