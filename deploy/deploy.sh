#!/usr/bin/env bash
# Deploys a jar to one environment and checks it answers; restores the previous jar if it does not.
# Usage: deploy/deploy.sh <dev|uat|prod> <path-to-jar>
set -euo pipefail

env="${1:?environment (dev|uat|prod)}"
jar="${2:?path to jar}"

case "$env" in
  dev)  port=8081 ;;
  uat)  port=8082 ;;
  prod) port=8083 ;;
  *) echo "Unknown environment: $env" >&2; exit 2 ;;
esac

target="/opt/lms/${env}"
service="lms@${env}"

# The health URL is public in every profile (permitAll in SecurityConfig).
wait_until_up() {
  for _ in $(seq 1 40); do
    if curl -fsS -o /dev/null "http://localhost:${port}/api-docs"; then
      return 0
    fi
    sleep 3
  done
  return 1
}

if [ -f "${target}/app.jar" ]; then
  cp "${target}/app.jar" "${target}/app.jar.prev"
fi
cp "$jar" "${target}/app.jar"
sudo systemctl restart "$service"

if wait_until_up; then
  echo "${env} is up on port ${port}"
  exit 0
fi

echo "${env} did not answer on port ${port}" >&2
if [ -f "${target}/app.jar.prev" ]; then
  echo "Restoring the previous jar" >&2
  cp "${target}/app.jar.prev" "${target}/app.jar"
  sudo systemctl restart "$service"
fi
exit 1
