#!/usr/bin/env bash
# Prints the folder of a JDK 17 on this machine (macOS, Linux, WSL) so the pipeline needs no hardcoded path.
# Usage: find-jdk17.sh [explicit-path]   (an explicit path wins if it is a JDK 17)
set -u

is17() {
  [ -x "$1/bin/java" ] && "$1/bin/java" -version 2>&1 | head -1 | grep -q '"17\.'
}

candidates=()
[ -n "${1:-}" ] && candidates+=("$1")
[ -n "${JAVA_HOME_17:-}" ] && candidates+=("${JAVA_HOME_17}")
# macOS: the system registry of JDKs, then Homebrew on Apple Silicon and Intel
if [ -x /usr/libexec/java_home ]; then
  candidates+=("$(/usr/libexec/java_home -v 17 2>/dev/null || true)")
fi
candidates+=(
  /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
  /usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
  /usr/lib/jvm/java-17-openjdk-amd64
  /usr/lib/jvm/java-17-openjdk-arm64
  /usr/lib/jvm/java-17-openjdk
  /usr/lib/jvm/temurin-17-jdk-amd64
)
# Any other 17 under the usual JVM folders (sdkman, Temurin, Zulu...)
for d in /usr/lib/jvm/*17* "$HOME"/.sdkman/candidates/java/17* /Library/Java/JavaVirtualMachines/*17*/Contents/Home; do
  [ -d "$d" ] && candidates+=("$d")
done

for c in "${candidates[@]}"; do
  [ -n "$c" ] && is17 "$c" && { echo "$c"; exit 0; }
done

echo "No JDK 17 found. Install it (macOS: brew install openjdk@17; Ubuntu/WSL: sudo apt install openjdk-17-jdk) or set the build parameter JAVA_HOME_17." >&2
exit 1
