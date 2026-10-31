#!/usr/bin/env bash
# Runs the notification delivery ITs (*IT) against Mailpit (SMTP) and Mockoon (SMS gateway and webhook).
# Both are started here, on 127.0.0.1 only, and stopped when the script ends, so nothing keeps running on the machine.
# Usage: bash jenkins/run-notification-its.sh   (needs Node 18+ and Mailpit; Mockoon CLI is fetched by npx)
set -euo pipefail

cd "$(dirname "$0")/.."

# The ports are the ones the *IT classes and src/test/resources/application.properties use.
MAILPIT_SMTP_PORT=1025
MAILPIT_API_PORT=8025
MOCKOON_PORT=8027
MOCKOON_CLI_VERSION=9.10.0
MOCK_FILE=src/test/resources/mockoon/notification-mocks.json
LOG_DIR=target/notification-its
MVN_NOT="${MVN_NOT:-./mvnw -B -Pnotification}"

die() {
  echo "$1" >&2
  exit 1
}

is_listening() {
  (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null
}

# First run of npx downloads Mockoon CLI, hence the long wait.
wait_for_port() {
  local service="$1" port="$2" pid="$3" log="$4"
  for _ in $(seq 1 180); do
    is_listening "$port" && return 0
    if ! kill -0 "$pid" 2>/dev/null; then
      tail -n 20 "$log" >&2 || true
      die "$service stopped before listening on port $port. Log: $log"
    fi
    sleep 1
  done
  tail -n 20 "$log" >&2 || true
  die "$service did not listen on port $port within 180 seconds. Log: $log"
}

# Each service runs in its own process group: stopping npx alone would leave Mockoon running.
pids=""
stop_services() {
  for pid in $pids; do
    kill -TERM -- "-$pid" 2>/dev/null || true
  done
  for pid in $pids; do
    wait "$pid" 2>/dev/null || true
  done
}

command -v node >/dev/null || die "Node is not installed on this machine (18 or newer is needed by Mockoon CLI)."
command -v npx >/dev/null || die "npx is not installed on this machine (it comes with Node)."
[ "$(node -p 'process.versions.node.split(".")[0]')" -ge 18 ] || die "Mockoon CLI needs Node 18 or newer, found $(node -v)."
command -v mailpit >/dev/null || die "Mailpit is not installed on this machine: https://mailpit.axllent.org/docs/install/"
[ -f "$MOCK_FILE" ] || die "Mockoon environment not found: $MOCK_FILE"

for port in "$MAILPIT_SMTP_PORT" "$MAILPIT_API_PORT" "$MOCKOON_PORT"; do
  if is_listening "$port"; then
    die "Port $port is already in use. Stop the Mailpit or Mockoon running on this machine, then run this again."
  fi
done

# Environment variables win over application.properties: these would send a real SMS instead of calling Mockoon.
unset NOTIFICATION_SMS_GATEWAY_URL NOTIFICATION_SMS_USERNAME NOTIFICATION_SMS_PASSWORD
export MOCKOON_ADMIN_TOKEN="${MOCKOON_ADMIN_TOKEN:-notification-its}"

mkdir -p "$LOG_DIR"
trap stop_services EXIT

set -m
mailpit --listen "127.0.0.1:$MAILPIT_API_PORT" --smtp "127.0.0.1:$MAILPIT_SMTP_PORT" >"$LOG_DIR/mailpit.log" 2>&1 &
mailpit_pid=$!
npx --yes --package="@mockoon/cli@$MOCKOON_CLI_VERSION" mockoon-cli start --data "$MOCK_FILE" \
  --port "$MOCKOON_PORT" --hostname 127.0.0.1 --admin-api-token "$MOCKOON_ADMIN_TOKEN" \
  --log-transaction --disable-log-to-file >"$LOG_DIR/mockoon.log" 2>&1 &
mockoon_pid=$!
set +m
pids="$mailpit_pid $mockoon_pid"

wait_for_port "Mailpit SMTP" "$MAILPIT_SMTP_PORT" "$mailpit_pid" "$LOG_DIR/mailpit.log"
wait_for_port "Mailpit API" "$MAILPIT_API_PORT" "$mailpit_pid" "$LOG_DIR/mailpit.log"
wait_for_port "Mockoon" "$MOCKOON_PORT" "$mockoon_pid" "$LOG_DIR/mockoon.log"

# The notification profile allows an empty selection, so it is refused here: a wrong name must not pass as green.
# JaCoCo is off so this run does not overwrite the coverage report of the earlier stages.
$MVN_NOT test -Dtest='*NotificationDeliveryIT,ScheduledNotificationCheckIT' \
  -Dsurefire.failIfNoSpecifiedTests=true -Djacoco.skip=true
