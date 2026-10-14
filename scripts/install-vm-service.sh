#!/bin/bash
# Run this once on each VM to install the flash-sale systemd service.
# After this, CI can restart the app with: sudo systemctl restart flash-sale

set -e

PROJECT=gcp-flash-sale-project

echo "==> Writing /etc/flash-sale.env (secrets from Secret Manager)"
sudo bash -c "cat > /etc/flash-sale.env" <<EOF
REDIS_HOST=$(gcloud secrets versions access latest --secret=REDIS_HOST_MEMORYSTORE --project=$PROJECT)
REDIS_PORT=$(gcloud secrets versions access latest --secret=REDIS_PORT_MEMORYSTORE --project=$PROJECT)
ADMIN_API_KEY=$(gcloud secrets versions access latest --secret=ADMIN_API_KEY --project=$PROJECT)
EOF

sudo chmod 600 /etc/flash-sale.env

echo "==> Writing /etc/systemd/system/flash-sale.service"
sudo bash -c 'cat > /etc/systemd/system/flash-sale.service' <<'UNIT'
[Unit]
Description=Flash Sale API
After=network.target

[Service]
EnvironmentFile=/etc/flash-sale.env
ExecStart=/usr/bin/java \
  -Dspring.data.redis.host=${REDIS_HOST} \
  -Dspring.data.redis.port=${REDIS_PORT} \
  -DADMIN_API_KEY=${ADMIN_API_KEY} \
  -jar /tmp/app.jar
Restart=on-failure
RestartSec=5
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
UNIT

echo "==> Enabling and starting flash-sale service"
sudo systemctl daemon-reload
sudo systemctl enable flash-sale
sudo systemctl restart flash-sale

echo "==> Done. Check status with: sudo systemctl status flash-sale"
