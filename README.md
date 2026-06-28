# Maranatha Store V2 — Backend corregido

Backend de microservicios con Spring Boot 3.1.5, Java 17, PostgreSQL, JWT, Swagger/OpenAPI y API Gateway.

## Funciones terminadas

- Inicio de sesión JWT con usuario inicial `admin` y contraseña `admin123`.
- Productos: registrar, guardar en PostgreSQL, listar, editar, eliminar y controlar stock.
- Clientes: registrar personas o empresas, validar DNI/RUC, listar, editar y eliminar.
- Ventas: validar cliente y productos, impedir ventas sin stock, recalcular precios y total en el servidor, descontar stock y guardar el detalle.
- Comprobantes internos: boleta `B001`, factura `F001` y nota de pedido `NP01`, con número correlativo basado en el ID de la venta.
- Anulación de venta con reposición de stock y conservación del historial.
- Swagger unificado en el Gateway y colección Postman incluida.

> La emisión implementada es facturación interna del sistema. No envía XML ni comprobantes a SUNAT. La facturación electrónica SUNAT requiere RUC real, certificado digital y credenciales SOL/OSE/PSE.

## Puertos

| Servicio | Puerto |
|---|---:|
| API Gateway | 8080 |
| Autenticación | 8081 |
| Productos | 8082 |
| Clientes | 8083 |
| Ventas | 8084 |
| PostgreSQL | 5432 |

Eureka y Config Server quedan como módulos auxiliares, pero no son necesarios para ejecutar la aplicación local: el Gateway usa URLs directas y configurables.

## Opción A: ejecutar el backend desde IntelliJ IDEA

### 1. Requisitos

- JDK 17.
- PostgreSQL 15 o Docker Desktop.
- IntelliJ IDEA con soporte Maven.

### 2. Preparar PostgreSQL

La forma más sencilla es abrir PowerShell en esta carpeta y ejecutar:

```powershell
docker compose up -d postgres
```

Esto crea automáticamente estas bases:

- `db_auth`
- `db_producto`
- `db_cliente`
- `db_ventas`

Usuario: `postgres`  
Contraseña: `admin123`

Si ya existe una instalación anterior con tablas incompatibles, ejecute una sola vez:

```powershell
.\reset-postgres-docker.ps1
```

Esto elimina el volumen anterior y crea bases limpias. Se perderán los datos antiguos.

Para PostgreSQL instalado directamente en Windows, ejecute `crear-bases-postgresql.sql` con `psql` y verifique que la contraseña configurada en los cuatro archivos `application.properties` sea correcta.

### 3. Importar el proyecto

1. Abra IntelliJ IDEA.
2. Seleccione **Open** y abra esta carpeta o el archivo `pom.xml` principal.
3. Configure el Project SDK en Java 17.
4. Espere a que Maven descargue las dependencias.

### 4. Iniciar las aplicaciones

Ejecute estas clases, cada una en una ventana de ejecución distinta:

1. `MsAuthApplication`
2. `MsProductoApplication`
3. `MsClienteApplication`
4. `MsVentasApplication`
5. `ApiGatewayApplication`

También puede abrir PowerShell como usuario normal y ejecutar:

```powershell
.\iniciar-backend.ps1
```

### 5. Verificar

- Gateway: <http://localhost:8080>
- Swagger completo: <http://localhost:8080/swagger-ui.html>
- Login: `admin` / `admin123`

## Opción B: ejecutar todo el backend con Docker

```powershell
docker compose up --build
```

Para detener:

```powershell
docker compose down
```

Para borrar también las bases y comenzar de cero:

```powershell
docker compose down -v
```

## Swagger

Abra <http://localhost:8080/swagger-ui.html>. En la parte superior podrá elegir:

- Autenticación
- Productos
- Clientes
- Ventas

Primero ejecute `POST /auth/login`, copie el token y luego pulse **Authorize** en los módulos protegidos. Pegue solamente el token JWT.

Swagger directo por servicio:

- <http://localhost:8081/swagger-ui.html>
- <http://localhost:8082/swagger-ui.html>
- <http://localhost:8083/swagger-ui.html>
- <http://localhost:8084/swagger-ui.html>

## Postman

Importe los dos archivos de la carpeta `postman`:

1. `Maranatha-Store-V2.postman_collection.json`
2. `Maranatha-Store-Local.postman_environment.json`

Ejecute primero **1. Autenticación → Iniciar sesión**. La prueba guarda el token automáticamente. Después ejecute productos, clientes y ventas en ese orden.

## Compilar

```powershell
.\mvnw.cmd clean package -DskipTests
```

O use:

```powershell
.\compilar-todo.ps1
```

## Errores corregidos respecto al proyecto recibido

- La clave JWT del servicio de autenticación no coincidía con la del Gateway.
- Productos se almacenaban en una lista de memoria y nunca se guardaban en PostgreSQL.
- Las tablas de los scripts SQL no coincidían con los nombres de las entidades.
- El Gateway mezclaba rutas directas con Eureka y podía devolver servicio no disponible.
- Ventas aceptaba el total enviado por el navegador y no descontaba stock.
- La relación venta–detalle duplicaba la columna `venta_id`.
- Cliente no guardaba tipo ni número de documento.
- La impresión del frontend vaciaba los detalles antes de abrir el comprobante.
- El `docker-compose` del backend intentaba construir una carpeta frontend inexistente.
