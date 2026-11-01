#!/usr/bin/env bash
# Merge the current run's suite snapshots and report on all production classes.
set -euo pipefail
cd "$(dirname "$0")/.."
if [ -s target/coverage-data/all-tests.exec ]; then
  required=(all-tests notification-it)
else
  required=(unit existing-it lending bibliographic-unit bibliographic-it notification notification-it)
fi
for suite in "${required[@]}"; do
  if [ ! -s "target/coverage-data/$suite.exec" ]; then
    echo "Missing coverage snapshot: $suite. Run all test stages before publishing global coverage." >&2
    exit 1
  fi
done
bash ./mvnw -B jacoco:merge@global-merge jacoco:report@global-report

# Keep the conventional report path consistent with the final global report.
mkdir -p target/site/jacoco
cp -R target/site/jacoco-global/. target/site/jacoco/
