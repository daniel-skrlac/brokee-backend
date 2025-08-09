#!/usr/bin/env bash

HOST_PORT="$1"
shift

HOST="${HOST_PORT%%:*}"
PORT="${HOST_PORT##*:}"

echo "Waiting for $HOST:$PORT..."
while ! nc -z "$HOST" "$PORT"; do
  sleep 1
done

echo "$HOST:$PORT is available"
exec "$@"
