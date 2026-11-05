# Maranatha Store V2: ajustes y pruebas

Revisión del 2 de noviembre de 2026. Los cambios están guardados en los proyectos originales, en las ramas `feature/sprint1-frontend-contreras` y `feature/sprint1-backend-contreras`. No se hicieron commits ni push.

## 1. Qué cambió y qué se comprobó

- Se usa el PNG transparente proporcionado por el usuario en login, navegación, recuperación y comprobantes.
- Recuperación tiene título centrado y fondo de esferas animadas; respeta la preferencia de reducir movimiento.
- Inventario tiene botones visibles para Todos, Disponibles, Stock bajo y Sin stock. Conserva el selector anterior y las funciones de importar/exportar.
- La búsqueda sin resultados tiene texto y fondo con contraste en ambos temas.
- El carrito muestra la cantidad como texto, sin flechas ni edición. La cantidad se elige antes de agregar; para reemplazar una cantidad, quitar y agregar de nuevo.
- Los cuadros de ventas, descarga y stock se abren en la capa superior del navegador, centrados en la ventana. El fondo no se desplaza mientras están abiertos.
- Se guarda el método de pago previsto: Yape, Plin, BCP, tarjeta, efectivo u otra transferencia. La venta permanece con pago PENDIENTE; no se confirmó ni realizó un cobro.
- Solo queda la acción visible Eliminar en el historial. Internamente se conserva la compensación del stock antes de borrar un registro interno; no se permite borrar comprobantes fiscales ni pagos confirmados.
- PDF A4 y ticket contienen un QR generado localmente. El ticket usa 80 mm y texto más grande.
- El PDF A4 fue descargado y revisado visualmente. El ticket fue generado desde tema oscuro y comprobado con fondo blanco. Los QR de ambos archivos se decodificaron y coincidieron con su número y total.
- El administrador tiene menú de Usuarios y Reportes; el vendedor tiene su espacio de ventas, sin acciones administrativas. Se probaron con MockMvc los bloqueos del backend, incluyendo 403 al intentar listar usuarios o crear administradores como vendedor.
- Angular compiló correctamente; pasaron 34 pruebas del backend.

La página de Clientes no se modificó en esta revisión. No se crearon ventas ni se eliminaron productos/comprobantes reales durante las pruebas. Se solicitó un enlace para la cuenta local existente, sin cambiar su contraseña.

## 2. Abrir la versión actualizada

1. Mantener Docker Desktop abierto y el frontend iniciado como antes.
2. Abrir http://localhost:4200 y pulsar Ctrl + F5.
3. Cerrar sesión y volver a ingresar para refrescar el rol.
4. En Productos, pulsar Stock bajo: aparecen artículos con 1 a 5 unidades. Sin stock muestra los de 0. Disponibles muestra los de 6 o más.
5. En Ventas, elegir cantidad antes de Agregar al carrito. El número de la fila ya no se edita. Quitar permite rehacer la selección sin registrar una venta.
6. En un comprobante existente, Descargar > PDF A4 o PDF ticket > Guardar PDF.

## 3. Recuperar contraseña: cuál correo usar

Hay dos direcciones distintas:

- **Correo del usuario:** el que el administrador guardó en Usuarios y roles al crear su cuenta. Ese es el que se escribe en Olvidaste tu contraseña.
- **Correo remitente SMTP:** la cuenta que envía los enlaces. Se configura en el servidor; no es necesariamente el mismo correo del usuario.

El flujo actual utiliza un **enlace con token**, no un código de seis cifras. El enlace vence en 15 minutos y se utiliza una sola vez. El backend guarda un hash del token y una contraseña cifrada con BCrypt; Postman no muestra la contraseña guardada.

### Prueba local disponible ahora, sin Gmail ni claves SMTP

Se preparó Mailpit: recibe los correos dentro del equipo, sin enviarlos a Internet. Su documentación oficial describe los puertos de buzón y SMTP: https://mailpit.axllent.org/docs/install/docker/

1. Abrir http://localhost:4200/recuperar-contrasena.
2. Para la cuenta inicial `admin`, usar `admin@maranatha.local` si ese sigue siendo el correo mostrado en Usuarios y roles. Para un vendedor, usar el correo guardado al crearlo.
3. Pulsar Enviar enlace. La respuesta es genérica para no revelar qué cuentas existen.
4. Abrir http://localhost:8025. El mensaje aparece en ese buzón local, no en Gmail/Outlook.
5. Abrir el mensaje de recuperación y seguir su enlace.
6. Elegir personalmente una contraseña de 10 a 72 caracteres y confirmarla.
7. Volver a iniciar sesión con la nueva contraseña. La anterior debe fallar.

Si el mensaje no llega: comprobar que el correo coincide con un usuario guardado y que no hay un error de envío. Un correo inventado sin cuenta registrada no genera mensaje. Esperar un minuto entre solicitudes a la misma dirección. Un enlace vencido o usado devuelve error; solicitar otro.

Para volver a arrancar el modo de correo local, ejecutar desde la carpeta principal del backend:

```powershell
docker compose -f docker-compose.yml -f docker-compose.correo-pruebas.yml up -d --no-build --no-deps mailpit ms-auth
```

### Enviar a una dirección real

1. Usar el host, puerto y credenciales SMTP facilitados por el proveedor de correo.
2. Configurarlos solamente en `C:\Users\DIEGO\Downloads\Maranatha-Store-V2-Backend\.env`:

```dotenv
SMTP_HOST=servidor_indicado_por_tu_proveedor
SMTP_PORT=587
SMTP_USERNAME=tu_usuario_smtp
SMTP_PASSWORD=tu_credencial_smtp
SMTP_FROM=tu_remitente_autorizado
SMTP_AUTH=true
SMTP_STARTTLS=true
FRONTEND_URL=http://localhost:4200
```

3. El puerto y TLS deben coincidir con las instrucciones del proveedor; no copiar esos valores como si fueran universales.
4. Reiniciar usando el archivo principal, sin el archivo de Mailpit:

```powershell
docker compose up -d --no-build --no-deps --force-recreate ms-auth
```

5. Crear un usuario con un correo real propio y probar la recuperación. El envío a Gmail/Outlook no se ha comprobado: faltan las credenciales SMTP del usuario.

## 4. Recuperación y usuarios en Postman

Importar `Maranatha-pruebas-revision4.postman_collection.json` mediante Import > Files. Es una colección nueva; no reemplaza las anteriores. En Variables, guardar contraseñas y documentos únicamente de forma local. Las claves Decolecta nunca van en la colección.

### Crear vendedor y comprobar permisos

1. En Variables, completar `admin_username` y `admin_password` con su cuenta actual.
2. Ejecutar **1. Administrador y usuarios > 1. Iniciar sesión admin**. Debe devolver 200; el script guarda `token_admin` localmente.
3. Ejecutar Ver sesión admin y Listar usuarios: deben devolver 200.
4. Elegir `seller_username`, `seller_email` y `seller_password` (mínimo 10 caracteres). Ejecutar Crear vendedor una sola vez. Debe devolver 201. Si el nombre o correo ya existen, elegir otros; no sobrescribir cuentas para esta prueba.
5. Ejecutar Iniciar sesión vendedor. Debe devolver 200 y rol USER. El script guarda `token_seller`.
6. Ejecutar Vendedor NO lista usuarios y Vendedor NO crea usuarios: deben devolver **403**.
7. Ejecutar Vendedor SÍ consulta productos: debe devolver **200**.
8. En la web, cerrar sesión e ingresar con ese vendedor. No debe aparecer Usuarios/Reportes ni acciones de crear, editar, ajustar o eliminar productos. Puede consultar productos, administrar clientes y registrar ventas.
9. Volver a ingresar como administrador: aparecen las acciones administrativas.

Petición equivalente para crear un vendedor, con Authorization > Bearer Token del administrador:

```http
POST http://localhost:8080/auth/usuarios
Content-Type: application/json
```

```json
{
  "username": "vendedor_prueba",
  "email": "vendedor.prueba@example.invalid",
  "password": "elige_tu_clave_de_prueba",
  "rol": "USER"
}
```

El ejemplo usa un correo de prueba para Mailpit. Para recibir correos externos debe registrar una dirección real y configurar SMTP. El rol ADMIN se usa solo para cuentas que deban administrar el sistema.

### Cambiar contraseña y verificar el resultado

1. Configurar `recovery_email` con el correo del usuario elegido.
2. Ejecutar Solicitar enlace en la carpeta Recuperación. Debe devolver **202**. No necesita Bearer Token.
3. Abrir el mensaje en Mailpit (o correo real cuando SMTP esté configurado).
4. Copiar únicamente el valor después de `token=` del enlace a la variable local `recovery_token`.
5. Elegir `new_password` y ejecutar Cambiar password con token. Debe devolver **204**. Esta acción sí cambia la contraseña de esa cuenta.
6. Ejecutar Login del vendedor con `new_password` solamente si se recuperó la cuenta del vendedor. Si recuperó admin, usar admin como username en una petición de login.
7. Volver a enviar el token usado: debe devolver **400**. Login con la contraseña anterior debe devolver **401**.

No ejecutar automáticamente toda la colección en Runner: contiene creación de cuenta y cambio de contraseña que deben elegirse conscientemente.

## 5. Decolecta: por qué aparece el mensaje

“Falta configurar la clave de Decolecta en el servidor” significa que el backend no recibió una API Key. **No significa que el DNI/RUC sea falso**. En esa situación no se consulta al proveedor.

1. En IntelliJ, localizar `C:\Users\DIEGO\Downloads\Maranatha-Store-V2-Backend`, donde está `docker-compose.yml`.
2. Crear o abrir `.env` sin reemplazar otras variables existentes. Evitar que Windows lo guarde como `.env.txt`.
3. Añadir `DECOLECTA_API_KEY=su_clave_real` y guardar. No compartir ese archivo, no pegar la clave en Angular ni en el chat.
4. Desde la terminal del backend ejecutar:

```powershell
docker compose up -d --no-build --no-deps --force-recreate ms-cliente
```

5. Esperar el arranque y consultar de nuevo en Ventas: DNI de 8 dígitos para boleta; RUC de 11 para factura.
6. El nombre procede de la respuesta real de Decolecta. La consulta no crea una venta; si el cliente no estaba registrado, se asocia al guardar la venta.
7. En Postman, iniciar sesión y ejecutar `GET {{baseUrl}}/clientes/documentos/dni/{{dni}}` o `/ruc/{{ruc}}` con Bearer Token. La clave del proveedor la añade el servidor.

Implementación: `C:\Users\DIEGO\Downloads\Maranatha-Store-V2-Backend\ms-cliente\src\main\java\com\libreria\cliente\service\DecolectaService.java` y `DocumentoController.java` en el mismo módulo. Docker pasa la variable a ms-cliente; la clave no se incluye en la imagen ni en Git.

Decolecta es el proveedor privado utilizado para las consultas DNI/RUC; no se anuncia una conexión directa contratada con RENIEC/SUNAT. Documentación oficial: https://decolecta.gitbook.io/docs/servicios/integrations-2 y https://decolecta.gitbook.io/docs/servicios/integrations

La cuenta del usuario indica 100 peticiones mensuales. Sin su clave configurada no se pudo verificar qué servicios habilita esa cuenta ni ejecutar consultas reales. Cuota/permisos del proveedor pueden impedir una consulta aun con clave; se mostrarán errores, nunca datos inventados. La caché breve evita repetir algunas consultas idénticas.

## 6. Qué significan inventario y gráficos

En su captura, **32 disponibles + 5 por reponer = 37 tipos de productos activos**. Son artículos distintos, no la suma de unidades físicas. Un producto con 100 unidades cuenta como un tipo de producto. La regla actual considera vendible un artículo con más de 5 unidades; los de 0 a 5 requieren reposición. El filtro Stock bajo separa 1–5, y Sin stock separa 0.

Las barras del dashboard provienen de ventas reales registradas en el backend, excluyendo anuladas. Se consultan al abrir Inicio, cada **30 segundos** y al pulsar Actualizar. Es actualización periódica, no transmisión instantánea mediante WebSockets. Los importes registrados no prueban cobros bancarios.

## 7. Reportes Excel: ejemplo claro

1. Ingresar como administrador y abrir Reportes Excel.
2. Pulsar Actualizar datos.
3. Elegir Mensual y una fecha de noviembre de 2026. El informe incluye las ventas EMITIDAS de todo noviembre, no solo las del día elegido.
4. En la captura del usuario aparecen **3 ventas, S/ 53.50 y promedio S/ 17.83**. El promedio es 53.50 / 3, redondeado a dos decimales. La venta anulada de S/ 8.00 queda excluida.
5. Semanal usa lunes a domingo de la semana que contiene la fecha. Anual usa enero a diciembre del año elegido.
6. El dashboard usa últimos 7 días; ese rango no siempre coincide con una semana de lunes a domingo.
7. Exportar ventas del período guarda las ventas filtradas. Informe integral guarda hojas Productos, Clientes, Ventas y Detalles; Productos/Clientes corresponden a su estado actual y Ventas/Detalles al período.
8. Descargar Excel actualizado consulta el inventario actual al servidor. Los archivos descargados son una fotografía de los datos: no cambian solos después de descargarlos.

Los reportes se actualizan con su botón o al volver a cargar la página; no se presentan como un flujo bancario en tiempo real.

## 8. Pagos y QR

El selector de métodos guarda la intención del cliente, no una transferencia. Para un cobro automático faltan credenciales comerciales, Checkout/tokenización, confirmación verificable del proveedor e idempotencia. No se generaron QR bancarios ficticios ni se solicitaron datos de tarjetas. El adaptador de Culqi está preparado para pruebas, pero no hubo transacciones comprobadas en un sandbox oficial. Documentación: https://docs.culqi.com/es/documentacion/pagos-online/integracion-vs-produccion/

El QR del comprobante contiene texto con número, fecha, cliente, base, IGV, total, estado y detalle de la venta (o un resumen cuando el detalle es muy largo). Puede leerse con un lector QR compatible con texto, sin una página pública. Es una fotografía del registro al generar el PDF: un PDF anterior no cambia si luego se elimina o modifica una venta. No es un QR de pago ni validación fiscal SUNAT.

El diseño nuevo toma la distribución, tarjetas y jerarquía de las referencias del usuario, conservando su logo y funciones. No copia cifras de muestra del diseño como si fueran datos del negocio.
