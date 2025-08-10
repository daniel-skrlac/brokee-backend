#!/bin/sh
set -e

URL="$1"
shift

echo "Waiting for Keycloak at $URL..."
until curl -fsS "$URL" >/dev/null 2>&1; do
    echo "Keycloak not ready yet..."
    sleep 2
done

echo "Keycloak is ready!"
exec "$@"
