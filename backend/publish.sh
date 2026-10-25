#!/usr/bin/env bash
# Builds a self-contained Linux package of the Kono API and bundles the deploy files.
#   ./publish.sh            → linux-x64 (default VPS)
#   ./publish.sh linux-arm64
set -euo pipefail
RID="${1:-linux-x64}"
cd "$(dirname "$0")"
OUT="deploy/dist/kono-api"
rm -rf "$OUT" "deploy/dist/kono-api-$RID.tar.gz"; mkdir -p "$OUT"

dotnet publish src/Kono.Api/Kono.Api.csproj -c Release -r "$RID" --self-contained true \
  -p:InvariantGlobalization=true -p:PublishTrimmed=false -p:DebugType=none -p:DebugSymbols=false -o "$OUT"

# never ship local test uploads or the dev-only settings
rm -rf "$OUT/wwwroot/uploads"; mkdir -p "$OUT/wwwroot/uploads"; touch "$OUT/wwwroot/uploads/.gitkeep"
rm -f "$OUT/appsettings.Development.json"

# the bundled appsettings.json must not carry the dev DB root login / dev JWT key —
# real values come from /etc/kono/kono-api.env
python3 - "$OUT/appsettings.json" <<'PY'
import json,sys
p=sys.argv[1]; d=json.load(open(p))
d["ConnectionStrings"]["Default"]="Server=127.0.0.1;Port=3306;Database=kono;User=kono;Password=SET_IN_ENV_FILE;TreatTinyAsBoolean=true;AllowUserVariables=true;UseAffectedRows=false"
d["Jwt"]["Key"]="SET_IN_ENV_FILE"
d["Cors"]["AllowedOrigins"]=[]
d["Logging"]["LogLevel"]["Default"]="Warning"
json.dump(d,open(p,"w"),indent=2)
PY

mkdir -p "$OUT/deploy"
cp deploy/kono-api.service deploy/kono-api.env.example deploy/nginx-kono.conf deploy/install.sh deploy/README-DEPLOY.md "$OUT/deploy/"
chmod +x "$OUT/Kono.Api" "$OUT/deploy/install.sh"

tar -C deploy/dist -czf "deploy/dist/kono-api-$RID.tar.gz" kono-api
ls -lh "deploy/dist/kono-api-$RID.tar.gz"
