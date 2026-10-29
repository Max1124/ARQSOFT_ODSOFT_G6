#!/usr/bin/env bash
# One-time VM set-up for the three environments. Run as root on the Ubuntu VM: sudo ./deploy/setup-vm.sh
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
  echo "Run as root (sudo)." >&2
  exit 1
fi

declare -A PORTS=([dev]=8081 [uat]=8082 [prod]=8083)

id lms >/dev/null 2>&1 || useradd --system --home /var/lib/lms --shell /usr/sbin/nologin lms
install -d -m 755 /etc/lms
# Jenkins writes the jars, the service user only reads them.
install -d -m 755 -o jenkins -g jenkins /opt/lms

for env in "${!PORTS[@]}"; do
  install -d -m 755 -o jenkins -g jenkins "/opt/lms/${env}"
  install -d -m 750 -o lms -g lms "/var/lib/lms/${env}" "/var/lib/lms/${env}/uploads"
  cat > "/etc/lms/${env}.env" <<EOF
SERVER_PORT=${PORTS[$env]}
SPRING_PROFILES_ACTIVE=${env},bootstrap
# No SMS gateway or SMTP server on the VM: keep SMS off (its credentials are mandatory when enabled) and the daily check disabled.
NOTIFICATION_CHANNELS=email,webhook
NOTIFICATION_CHECK_CRON=-
EOF
done

install -m 644 "$(dirname "$0")/lms@.service" /etc/systemd/system/lms@.service

# Jenkins may only restart and query the three services.
cat > /etc/sudoers.d/jenkins-lms <<'EOF'
jenkins ALL=(root) NOPASSWD: /usr/bin/systemctl restart lms@dev, /usr/bin/systemctl restart lms@uat, /usr/bin/systemctl restart lms@prod
jenkins ALL=(root) NOPASSWD: /usr/bin/systemctl is-active lms@dev, /usr/bin/systemctl is-active lms@uat, /usr/bin/systemctl is-active lms@prod
EOF
chmod 440 /etc/sudoers.d/jenkins-lms
visudo -cf /etc/sudoers.d/jenkins-lms

systemctl daemon-reload
for env in "${!PORTS[@]}"; do
  systemctl enable "lms@${env}"
done
echo "Done. Ports: dev 8081, uat 8082, prod 8083. The services start at the first deploy."
