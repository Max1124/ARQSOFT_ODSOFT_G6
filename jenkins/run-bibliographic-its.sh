#!/usr/bin/env bash
# Runs Bibliographic integration tests with Java 17.
# The tests manage their own local HTTP providers; no Mockoon or real API key is needed.
# Usage: JAVA_HOME=$(bash jenkins/local/find-jdk17.sh) bash jenkins/run-bibliographic-its.sh
set -euo pipefail

cd "$(dirname "$0")/.."

REPORT_DIR=target/surefire-reports-bibliographic-it
mkdir -p "$REPORT_DIR"
# Avoid publishing stale reports after a repeated or interrupted manual run.
rm -f "$REPORT_DIR"/TEST-*.xml "$REPORT_DIR"/*.txt

# Append to the bibliographic unit coverage produced by the preceding Jenkins stage.
bash ./mvnw -B -Pbibliographic test \
  -Dtest='pt/psoft/g1/psoftg1/bibliographicmanagement/**/*IT' \
  -Dsurefire.failIfNoSpecifiedTests=true \
  -Dsurefire.reportsDirectory="$REPORT_DIR" \
  -Djacoco.append=true
