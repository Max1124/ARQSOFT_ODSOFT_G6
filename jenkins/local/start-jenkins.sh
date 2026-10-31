#!/usr/bin/env bash
# Starts a local Jenkins for pipeline 1 (WSL, Linux or macOS). No Docker needed; it runs the official jenkins.war.
# Usage: bash jenkins/local/start-jenkins.sh        (stop it with Ctrl+C; keep the terminal open while you work)
# Settings: JENKINS_HOME (data folder, default ~/jenkins-local), JENKINS_PORT (default 9090), JENKINS_JAVA_HOME (JDK 21+ for Jenkins).
set -euo pipefail

home="${JENKINS_HOME:-$HOME/jenkins-local}"
port="${JENKINS_PORT:-9090}"
war="${home}/jenkins.war"

# Jenkins itself needs Java 21 or newer (the project build needs 17; the job passes JDK 17 to the build separately).
# To use another JDK for Jenkins only, set JENKINS_JAVA_HOME to its folder before running this script.
if [ -n "${JENKINS_JAVA_HOME:-}" ]; then
  java_cmd="${JENKINS_JAVA_HOME}/bin/java"
else
  java_cmd="java"
fi
major="$("${java_cmd}" -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+).*/\1/')"
if [ "${major}" -lt 21 ]; then
  echo "Java ${major} found; Jenkins needs 21 or newer. Install one (WSL: sudo apt install -y openjdk-21-jdk) and set JENKINS_JAVA_HOME, see jenkins/README.md." >&2
  exit 1
fi

mkdir -p "${home}"
if [ ! -f "${war}" ]; then
  echo "Downloading jenkins.war (about 90 MB, once)..."
  curl -fL -o "${war}" https://get.jenkins.io/war-stable/latest/jenkins.war
fi

export JENKINS_HOME="${home}"
echo "Jenkins data: ${home}"
echo "Open http://localhost:${port} (first start takes about a minute)."
# ALLOW_LOCAL_CHECKOUT lets the job build the repository folder on this machine.
exec "${java_cmd}" -Dhudson.plugins.git.GitSCM.ALLOW_LOCAL_CHECKOUT=true -jar "${war}" --httpPort="${port}"
