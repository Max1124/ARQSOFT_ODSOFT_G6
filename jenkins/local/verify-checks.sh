#!/usr/bin/env bash
# Run the pipeline test selections without Jenkins (Java 17, Node and Mailpit required).
set -euo pipefail
cd "$(dirname "$0")/../.."
export JAVA_HOME="$(bash jenkins/local/find-jdk17.sh)"
export PATH="$JAVA_HOME/bin:$PATH"
bash mvnw -B clean test -Dtest='*Test' -Dsurefire.reportsDirectory=target/surefire-reports-unit
mkdir -p target/coverage-data
cp target/jacoco.exec target/coverage-data/unit.exec
rm -f target/jacoco.exec
bash mvnw -B test -Dtest='*IT,!**/bibliographicmanagement/**,!*Notification*IT,!ScheduledNotificationCheckIT,!LendingSystemIT' -Dsurefire.reportsDirectory=target/surefire-reports-existing-it
mkdir -p target/coverage-data
cp target/jacoco.exec target/coverage-data/existing-it.exec
bash mvnw -B -Pbibliographic test
mkdir -p target/coverage-data
cp target/jacoco-bibliographic.exec target/coverage-data/bibliographic-unit.exec
bash jenkins/run-bibliographic-its.sh
mkdir -p target/coverage-data
cp target/jacoco-bibliographic.exec target/coverage-data/bibliographic-it.exec
rm -f target/jacoco.exec
bash mvnw -B -Plending verify
mkdir -p target/coverage-data
cp target/jacoco.exec target/coverage-data/lending.exec
bash mvnw -B -Plending spotbugs:spotbugs
rm -f target/jacoco.exec
bash mvnw -B -Pnotification test
mkdir -p target/coverage-data
cp target/jacoco.exec target/coverage-data/notification.exec
bash jenkins/run-notification-its.sh
mkdir -p target/coverage-data
cp target/jacoco-notification-it.exec target/coverage-data/notification-it.exec
bash mvnw -B package -DskipTests

bash jenkins/publish-global-coverage.sh
