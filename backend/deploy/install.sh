#!/usr/bin/env bash
# Run as root on the VPS, from inside the extracted kono-api folder:   sudo ./deploy/install.sh
# Safe to re-run for upgrades: keeps /etc/kono/kono-api.env and the uploads folder.
set -euo pipefail
[ "$(id -u)" -eq 0 ] || { echo "run as root (sudo)"; exit 1; }

SRC="$(cd "$(dirname "$0")/.." && pwd)"
APP=/opt/kono-api
DATA=/var/lib/kono
ETC=/etc/kono

id kono >/dev/null 2>&1 || useradd --system --home "$APP" --shell /usr/sbin/nologin kono
mkdir -p "$APP" "$DATA/uploads" "$ETC"

echo "→ copying app to $APP (uploads are preserved)"
if command -v rsync >/dev/null; then
  rsync -a --delete --exclude 'wwwroot/uploads' --exclude 'deploy' "$SRC"/ "$APP"/
else
  find "$APP" -mindepth 1 -maxdepth 1 ! -name wwwroot -exec rm -rf {} +
  cp -a "$SRC"/. "$APP"/ && rm -rf "$APP/deploy"
fi
# uploads live outside the app folder so upgrades never wipe them
rm -rf "$APP/wwwroot/uploads"
ln -sfn "$DATA/uploads" "$APP/wwwroot/uploads"
chmod +x "$APP/Kono.Api"
chown -R kono:kono "$APP" "$DATA"

if [ ! -f "$ETC/kono-api.env" ]; then
  echo "→ creating $ETC/kono-api.env (edit it before first start!)"
  cp "$SRC/deploy/kono-api.env.example" "$ETC/kono-api.env"
  KEY="$(openssl rand -base64 48 | tr -d '\n=+/' | cut -c1-64)"
  sed -i "s|^Jwt__Key=.*|Jwt__Key=$KEY|" "$ETC/kono-api.env"
  chmod 600 "$ETC/kono-api.env"; chown root:root "$ETC/kono-api.env"
  NEW_ENV=1
fi

install -m 644 "$SRC/deploy/kono-api.service" /etc/systemd/system/kono-api.service
systemctl daemon-reload
systemctl enable kono-api >/dev/null

if [ "${NEW_ENV:-0}" = 1 ]; then
  echo
  echo "✔ installed. NEXT:"
  echo "  1) nano $ETC/kono-api.env      # DB password, public URL, CORS origins"
  echo "  2) systemctl start kono-api && journalctl -u kono-api -f"
  echo "  3) curl http://127.0.0.1:5210/health"
else
  systemctl restart kono-api
  sleep 3
  echo "✔ upgraded and restarted:"; curl -s http://127.0.0.1:5210/health || true; echo
fi
