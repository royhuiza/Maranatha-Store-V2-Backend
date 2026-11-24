#!/bin/sh
# Importa y publica el workflow del chatbot en n8n antes de iniciar n8n.
#
# Sin este paso el webhook /webhook/maranatha-chat responde 404 y ms-chat
# devuelve el mensaje de fallback en vez de la respuesta del chatbot.
#
# n8n 2.x no permite activar workflows desde el CLI en modo regular
# ("import:workflow --activeState=fromJson" solo funciona en modo queue o
# multi-main). Por eso, tras importar, se publica la version directamente en
# la base de n8n, antes de arrancar el servidor, para que registre el webhook.
#
# Es idempotente: si el workflow ya esta importado y publicado, no se
# vuelve a importar (evita duplicados).
#
# Con "--start" (usado por docker-compose) ejecuta ademas el arranque de n8n.

set -e

WORKFLOW_FILE="${N8N_WORKFLOW_FILE:-/workflow/workflow.json}"
DB_FILE="${N8N_DB_FILE:-/home/node/.n8n/database.sqlite}"
CONFIG_FILE="${N8N_CONFIG_FILE:-/home/node/.n8n/config}"
PUBLISH_SCRIPT="${N8N_PUBLISH_SCRIPT:-/init/publish-n8n-workflow.js}"
CREDENTIALS_SCRIPT="${N8N_CREDENTIALS_SCRIPT:-/init/n8n-credentials.js}"

start_n8n() {
  echo "[n8n] arrancando n8n"
  exec n8n start
}

if [ ! -f "${WORKFLOW_FILE}" ]; then
  echo "[n8n] no se encontro ${WORKFLOW_FILE}; se omite la importacion"
  [ "$1" = "--start" ] && start_n8n
  exit 0
fi

# El workflow ya esta importado y publicado?
if [ -f "${DB_FILE}" ] && node "${PUBLISH_SCRIPT}" --check "${DB_FILE}" >/dev/null 2>&1; then
  echo "[n8n] el workflow ya esta importado y publicado"
  [ "$1" = "--start" ] && start_n8n
  exit 0
fi

echo "[n8n] importando el workflow del chatbot"
n8n import:workflow --input="${WORKFLOW_FILE}" 2>&1 | tail -n 3 || true

echo "[n8n] registrando las credenciales que usa el workflow"
node "${CREDENTIALS_SCRIPT}" "${DB_FILE}" "${CONFIG_FILE}" 2>&1 | tail -n 6 || true

echo "[n8n] publicando la version del workflow"
node "${PUBLISH_SCRIPT}" "${DB_FILE}" 2>&1 | tail -n 4 || true

echo "[n8n] workflow del chatbot listo"

[ "$1" = "--start" ] && start_n8n
exit 0
