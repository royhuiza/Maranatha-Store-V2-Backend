# Maranatha Store V2 — Backend

<div align="center">

<img src="https://img.shields.io/badge/Java-17-007396?style=for-the-badge&logo=java&logoColor=white" />
<img src="https://img.shields.io/badge/Spring%20Boot-3.1.5-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" />
<img src="https://img.shields.io/badge/PostgreSQL-15-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" />
<img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white" />
<img src="https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black" />
<img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" />

</div>

---

##  Características principales

<div align="center">

<table>
  <tr>
    <td>
      <div style="background:#e8f5e9;border-left:5px solid #4caf50;padding:12px;border-radius:6px;margin:4px;">
        <strong style="color:#2e7d32;">🔐 Autenticación</strong><br>
        JWT con usuario `admin` / `admin123`. Tokens seguros y expirables.
      </div>
    </td>
    <td>
      <div style="background:#e3f2fd;border-left:5px solid #2196f3;padding:12px;border-radius:6px;margin:4px;">
        <strong style="color:#1565c0;">📦 Productos</strong><br>
        CRUD completo, control de stock en PostgreSQL y validación de disponibilidad.
      </div>
    </td>
    <td>
      <div style="background:#fff3e0;border-left:5px solid #ff9800;padding:12px;border-radius:6px;margin:4px;">
        <strong style="color:#e65100;">👥 Clientes</strong><br>
        Registro de personas (DNI) y empresas (RUC), validación de documentos y listado.
      </div>
    </td>
  </tr>
  <tr>
    <td>
      <div style="background:#fce4ec;border-left:5px solid #e91e63;padding:12px;border-radius:6px;margin:4px;">
        <strong style="color:#c2185b;">💰 Ventas</strong><br>
        Validación de stock, descuento automático, totales calculados en servidor y comprobantes (Boleta/Factura/Nota de pedido).
      </div>
    </td>
    <td>
      <div style="background:#e0f7fa;border-left:5px solid #00bcd4;padding:12px;border-radius:6px;margin:4px;">
        <strong style="color:#006064;">🧾 SUNAT / DNI</strong><br>
        Consulta de documentos vía Decolecta (DNI/RUC) desde ms-cliente y ms-facturacion.
      </div>
    </td>
    <td>
      <div style="background:#f3e5f5;border-left:5px solid #9c27b0;padding:12px;border-radius:6px;margin:4px;">
        <strong style="color:#6a1b9a;">🐳 Docker</strong><br>
        Stack completo con docker-compose, PostgreSQL, Redis, n8n, WhatsApp y Chat.
      </div>
    </td>
  </tr>
</table>

</div>

---

##  Arquitectura de microservicios

```
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway  :8080                     │
└────────────────┬────────────────────────────────────────────┘
                 │
   ┌─────────────┼─────────────┬─────────────┬─────────────┐
   ▼             ▼             ▼             ▼             ▼
┌───────┐   ┌───────┐   ┌───────┐   ┌───────┐   ┌───────┐
│ Auth  │   │ Producto│   │ Cliente│   │ Ventas │   │ Chat  │
│ :8081 │   │  :8082 │   │  :8083 │   │ :8084 │   │ :8087 │
└───────┘   └───────┘   └───────┘   └───────┘   └───────┘
   │             │             │             │
   ▼             ▼             ▼             ▼
┌───────┐   ┌───────┐   ┌───────┐   ┌───────┐
│ db_auth│  │db_producto│  │db_cliente│  │ db_ventas│
└───────┘   └───────┘   └───────┘   └───────┘
```

---

##  Puertos de servicio

| Servicio | Puerto | Descripción |
|:---|:---:|:---|
| API Gateway | 8080 | Punto de entrada único y Swagger unificado |
| Autenticación (ms-auth) | 8081 | Login, JWT y gestión de usuarios |
| Productos (ms-producto) | 8082 | Catálogo, stock y ajustes de inventario |
| Clientes (ms-cliente) | 8083 | Clientes, consulta DNI/RUC (Decolecta) |
| Ventas (ms-ventas) | 8084 | Registro, anulación y historial de ventas |
| Facturación (ms-facturacion) | 8085 | Consulta RUC hacia ms-cliente (interno) |
| Chat (ms-chat) | 8087 | Chatbot y QR de WhatsApp |
| PostgreSQL | 5432 | Bases de datos por microservicio |
| Redis | 6379 | Caché y sesiones |
| n8n | 5678 | Automatizaciones y flujos de trabajo |

---

##  Inicio rápido

### Opción A: Docker (recomendado, todo el stack)

```powershell
# 1. Clonar o actualizar la rama principal
git clone https://github.com/royhuiza/Maranatha-Store-V2-Backend.git
cd Maranatha-Store-V2-Backend
git checkout master
git pull origin master

# 2. Copiar el archivo de variables de entorno (editar con tus claves reales)
#    Copia .env.example a .env y completa DECOLECTA_API_KEY si la tienes
Copy-Item .env.example .env

# 3. Levantar todo el stack con Docker
docker compose up -d --build

# 4. Verificar que todos los contenedores estén arriba
docker compose ps
```

URLs importantes una vez levantado:

- Gateway + Swagger: http://localhost:8080/swagger-ui.html
- Login: `admin` / `admin123`
- API directa por servicio: 8081 (auth), 8082 (productos), 8083 (clientes), 8084 (ventas)

Para parar todo:

```powershell
docker compose down
```

Para borrar también las bases y comenzar de cero:

```powershell
docker compose down -v
```

### Opción B: IntelliJ IDEA (desarrollo local)

```powershell
# 1. Levantar solo PostgreSQL con Docker
docker compose up -d maranatha-postgres

# 2. Si las tablas están incompatibles, resetear una sola vez
.\reset-postgres-docker.ps1

# 3. Importar el pom.xml en IntelliJ con JDK 17
# 4. Ejecutar en este orden:
#    MsAuthApplication → MsProductoApplication → MsClienteApplication
#    MsVentasApplication → ApiGatewayApplication

# 5. Verificar
#    http://localhost:8080/swagger-ui.html
#    Login: admin / admin123
```

O usar el script:

```powershell
.\iniciar-backend.ps1
```

---

##  Swagger y Postman

**Swagger (Gateway unificado):**
- http://localhost:8080/swagger-ui.html

**Swagger directo por servicio:**
- Auth: http://localhost:8081/swagger-ui.html
- Productos: http://localhost:8082/swagger-ui.html
- Clientes: http://localhost:8083/swagger-ui.html
- Ventas: http://localhost:8084/swagger-ui.html

**Colección Postman:**
Importar desde la carpeta `postman`:
1. `Maranatha-Store-V2.postman_collection.json`
2. `Maranatha-Store-Local.postman_environment.json`

Ejecutar primero **Autenticación → Iniciar sesión** para obtener el token JWT.

---

##  Consulta de documentos (DNI / RUC)

Los endpoints de consulta de documentos están expuestos en ms-cliente:

```bash
# Consultar DNI (8 dígitos)
GET http://localhost:8083/clientes/documentos/dni/{numero}

# Consultar RUC (11 dígitos)
GET http://localhost:8083/clientes/documentos/ruc/{numero}
```

La consulta de RUC desde facturación se activa con:

```properties
consulta-ruc.enabled=true
```

La clave de Decolecta es obligatoria:

```properties
integrations.decolecta.api-key=SU_CLAVE_AQUI
```

> **Nota:** La facturación es interna del sistema. No envía XML ni comprobantes a SUNAT. Para facturación electrónica oficial se requiere RUC real, certificado digital y credenciales SOL/OSE/PSE.

---

##  Compilar

```powershell
.\mvnw.cmd clean package -DskipTests
```

O:

```powershell
.\compilar-todo.ps1
```

---

##  Variables de entorno recomendadas

| Variable | Valor por defecto | Descripción |
|:---|---|---|
| `JWT_SECRET` | `MaranathaStoreClaveSuperSeguraJWT2026Maranatha` | Clave de firma JWT |
| `DECOLECTA_API_KEY` | *(vacío)* | Clave para consulta DNI/RUC |
| `CONSULTA_RUC_ENABLED` | `false` | Habilita consulta RUC en facturación |
| `SPRING_DATASOURCE_PASSWORD` | `admin123` | Contraseña PostgreSQL |
| `INTERNAL_SERVICE_TOKEN` | *(vacío)* | Token entre microservicios |

---

##  Mejoras respecto al proyecto original

- Clave JWT unificada entre Auth y Gateway.
- Productos persistidos en PostgreSQL (ya no en memoria).
- Tablas SQL alineadas con nombres de entidades JPA.
- Gateway sin rutas conflictivas con Eureka.
- Ventas calculan total en servidor y descuentan stock real.
- Relación venta–detalle sin columna duplicada.
- Clientes guardan tipo y número de documento (DNI/RUC).
- Docker-compose ya no intenta construir frontend inexistente.

---

##  Flujo de trabajo sugerido

1. Trabaja en tu rama de feature.
2. Sincroniza con `qa/integracion-backend` antes de hacer PR.
3. Abre un Pull Request hacia `master` una vez revisado y probado.
4. Tras el merge, despliega a producción con `docker compose up -d --build`.
