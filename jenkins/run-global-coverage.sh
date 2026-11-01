#!/usr/bin/env bash
# Complete coverage without Jenkins (Java 17, Node and Mailpit required).
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME="$(bash jenkins/local/find-jdk17.sh)"
export PATH="$JAVA_HOME/bin:$PATH"
bash ./mvnw -B clean test \
  -Dtest='*Test,*IT,!*NotificationDeliveryIT,!ScheduledNotificationCheckIT' \
  -Dsurefire.reportsDirectory=target/surefire-reports-global \
  -Djacoco.append=false
mkdir -p target/coverage-data
cp target/jacoco.exec target/coverage-data/all-tests.exec
bash jenkins/run-notification-its.sh
cp target/jacoco-notification-it.exec target/coverage-data/notification-it.exec
bash jenkins/publish-global-coverage.sh
