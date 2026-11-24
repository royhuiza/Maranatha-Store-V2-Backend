#!/bin/sh
# Registra la instancia de WhatsApp en Evolution API si todavia no existe.
#
# Evolution API v2.x espera el campo "instanceName" (no "name"). Si se envia
# "name", la instancia queda sin nombre ("undefined") y Prisma falla al crear
# la fila de Setting con el error de clave foranea Setting_instanceId_fkey.
#
# Es idempotente: si la instancia ya existe, no hace nada.

set -e

EVOLUTION_URL="${EVOLUTION_API_URL:-http://maranatha-whatsapp:8080}"
API_KEY="${EVOLUTION_API_KEY:-maranatha_secret_key_2026}"
INSTANCE_NAME="${EVOLUTION_API_INSTANCE:-maranatha-store}"
PAYLOAD="/tmp/payload.json"

printf '{"instanceName":"%s","integration":"WHATSAPP-BAILEYS"}' "${INSTANCE_NAME}" > "${PAYLOAD}"

echo "[init] esperando a Evolution API en ${EVOLUTION_URL}"
until wget -qO- --header="apikey: ${API_KEY}" "${EVOLUTION_URL}/instance/fetchInstances" >/dev/null 2>&1; do
  sleep 3
done

EXISTING=$(wget -qO- --header="apikey: ${API_KEY}" "${EVOLUTION_URL}/instance/fetchInstances" 2>/dev/null || echo "[]")
case "${EXISTING}" in
  *"\"${INSTANCE_NAME}\""*)
    echo "[init] la instancia '${INSTANCE_NAME}' ya existe"
    exit 0
    ;;
esac

echo "[init] creando la instancia '${INSTANCE_NAME}'"
wget -qO- --post-file="${PAYLOAD}" \
  --header="apikey: ${API_KEY}" \
  --header="Content-Type: application/json" \
  "${EVOLUTION_URL}/instance/create" >/dev/null

echo "[init] instancia '${INSTANCE_NAME}' registrada"
