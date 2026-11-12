# Infraestructura Docker unificada

Todo el backend vive en un solo archivo (`docker-compose.yml`), bajo el proyecto de
Docker **`maranatha-backend`** y en la red compartida **`maranatha-net`**.

## Levantar el stack completo

```powershell
docker compose up -d --build
```

Verificar que todos los contenedores pertenecen al mismo grupo:

```powershell
docker compose ps
```

Deben aparecer 12 servicios en estado `Up` (más `maranatha-whatsapp-init`, que es un
job de una sola ejecución y termina en `Exited (0)`).

## Servicios

| Servicio Compose | Contenedor | Puerto | Rol |
|---|---|---:|---|
| `api-gateway` | `maranatha-gateway` | 8080 | Punto de entrada HTTP |
| `ms-auth` | `maranatha-auth` | 8081 | JWT y usuarios |
| `ms-producto` | `maranatha-productos` | 8082 | Catálogo y stock |
| `ms-cliente` | `maranatha-clientes` | 8083 | Clientes y RUC/DNI |
| `ms-ventas` | `maranatha-ventas` | 8084 | Ventas e historial |
| `ms-facturacion` | `maranatha-facturacion` | 8085 | Facturación SUNAT |
| `ms-chat` | `maranatha-chat` | 8087 | Chat (n8n + WhatsApp) |
| `maranatha-whatsapp` | `maranatha-whatsapp` | 8086 | Evolution API |
| `maranatha-n8n` | `maranatha-n8n` | 5678 | Workflows del chatbot |
| `maranatha-postgres` | `maranatha-postgres` | 5432 | Bases de datos |
| `maranatha-redis` | `maranatha-redis` | 6379 | Caché de Evolution API |
| `maranatha-whatsapp-init` | `maranatha-whatsapp-init` | — | Registra la instancia de WhatsApp (idempotente) |

## Bases de datos

`docker/init-multiple-dbs.sql` crea al vuelo una base por microservicio:

- `db_auth`
- `db_producto`
- `db_cliente`
- `db_ventas`

> Evolution API no usa una base propia: usa el schema `evolution` dentro de `postgres`.
> Prisma crea ese schema solo en su primer arranque, por lo que el servicio no
> depende de que el volumen de datos ya exista. Aislar el schema evita que las
> tablas de Prisma choquen con las de los microservicios.

## Orden de arranque

- `maranatha-postgres` y `maranatha-redis` arrancan primero y exponen *healthchecks*.
- `maranatha-whatsapp` **espera a ambos en estado healthy** (`depends_on` con
  `condition: service_healthy`). Sin esta espera, Evolution API entra en un bucle de
  reconexión contra Redis y se queda sin sesión.
- `maranatha-whatsapp-init` espera a Evolution API y registra la instancia
  `maranatha-store`. Es idempotente: si ya existe, no hace nada.
- `ms-chat` espera a `maranatha-whatsapp-init` (`service_completed_successfully`).
- `api-gateway` espera a todos los microservicios del dominio.

## Endpoints de chat

- `GET /api/v1/chat/whatsapp/qr` — QR de WhatsApp en Base64, consultado a Evolution API.
- `GET /api/v1/chat/logs` — bitácora reciente de la actividad del chatbot.

Ambos se consumen con token JWT, igual que el resto del Gateway:

```powershell
$token = (Invoke-RestMethod -Uri http://localhost:8080/auth/login -Method Post `
  -ContentType "application/json" -Body '{"username":"admin","password":"admin123"}').token
Invoke-RestMethod -Uri http://localhost:8080/api/v1/chat/whatsapp/qr -Headers @{ Authorization = "Bearer $token" }
```

## Notas de Evolution API

- Se fija la versión `evoapicloud/evolution-api:v2.3.7`. La línea 2.4.x exige
  activación de licencia y devuelve `503` hasta registrarla.
- El registro de la instancia debe enviar **`instanceName`**, no `name`. Con `name`,
  Evolution API crea la instancia sin nombre (`undefined`) y Prisma falla con
  `Setting_instanceId_fkey`.
- El QR se obtiene con `GET /instance/connect/{instanceName}`.

### Por qué el contenedor "parece apagado"

Hay dos contenedores con nombres parecidos:

| Contenedor | Qué es | Estado esperado |
|---|---|---|
| `maranatha-whatsapp` | Evolution API (el que emite el QR) | `Up` |
| `maranatha-whatsapp-init` | Job que registra la instancia | `Exited (0)` |

`maranatha-whatsapp-init` **debe** terminar así: corre una vez, crea la instancia
y sale. Verlo en `Exited (0)` es correcto, no es una falla. Para confirmar que el
servidor real está vivo:

```powershell
docker compose ps maranatha-whatsapp
docker logs maranatha-whatsapp --tail 20
```

### El QR rota cada ~45 segundos

Baileys regenera el código con frecuencia. `GET /api/v1/chat/whatsapp/qr` siempre
devuelve el QR **vigente** (se verificó que el base64 cambia junto con el contador
de Evolution API). Escanear una captura antigua falla: hay que escanear el QR
que se está mostrando en ese momento.

Para confirmar que la sesión quedó emparejada:

```powershell
docker exec maranatha-whatsapp wget -qO- --header="apikey: maranatha_secret_key_2026" \
  http://localhost:8080/instance/fetchInstances
```

Si `connectionStatus` es `open` y `ownerJid` trae un número, ya está emparejada.
Si es `connecting`, todavía no se escaneó ningún QR.

### La sesión se guarda en Redis

`CACHE_REDIS_SAVE_INSTANCES=true` guarda las credenciales de Baileys en Redis
(db 6). Sin esa variable, cada reinicio del contenedor pierde la sesión
emparejada y hay que escanear un QR nuevo.

## Notas de n8n

- El workflow del chatbot (`My workflow.json`) se importa y publica solo, antes de
  que arranque n8n. En n8n 2.x el CLI no activa workflows en modo regular
  (`import:workflow --activeState=fromJson` solo funciona en modo queue o
  multi-main), por eso la publicación se hace sobre la base de n8n
  (`docker/publish-n8n-workflow.js`).
- Al nodo **Google Gemini Chat Model** hay que asignarle la credencial de la API
  desde la UI de n8n (<http://localhost:5678>). Sin ella el workflow se ejecuta
  pero responde "Error in workflow"; ms-chat devuelve el mensaje de fallback.

## Limpieza

```powershell
# Detener y borrar contenedores, red y volumen de datos
docker compose down -v

# Quitar contenedores huérfanos de otros proyectos
docker rm -f maranatha-whatsapp maranatha-redis
```

## Configuración

Las variables se leen de `.env` (ignorado por Git). Todos los valores tienen default
en `docker-compose.yml`, así que el stack arranca sin `.env`. Copie `.env.example`
si quiere sobrescribirlos.
