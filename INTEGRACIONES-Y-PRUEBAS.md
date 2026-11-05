# Integraciones y pruebas locales

Estado de la revisión: 2 de noviembre de 2026. Sin commits ni push.

## Decolecta

La clave se lee exclusivamente en ms-cliente, mediante DECOLECTA_API_KEY.
Implementación: ms-cliente/src/main/java/com/libreria/cliente/service/DecolectaService.java.
Rutas protegidas: GET /clientes/documentos/dni/{numero} y GET /clientes/documentos/ruc/{numero}.
Angular accede con el prefijo /api a través del gateway; no contiene la clave del proveedor.

En Docker, crear .env junto a docker-compose.yml y guardar DECOLECTA_API_KEY=su_clave.
.env está ignorado por Git y por Docker. No pegar claves en el chat.
Recrear solamente ms-cliente con el proyecto Docker backend para cargar la variable.
En IntelliJ: Run > Edit Configurations > ms-cliente > Environment variables.
Agregar DECOLECTA_API_KEY y reiniciar ese servicio. No ejecutar simultáneamente el contenedor y la configuración de IntelliJ en el mismo puerto.

Iniciar sesión en Angular, abrir Ventas, escribir un DNI de 8 dígitos o RUC de 11.
La consulta espera 600 ms y completa el nombre únicamente cuando el proveedor responde con datos válidos.
El cliente nuevo se guarda cuando se registra la venta; consultar no registra una venta.
Sin clave: error 503 de configuración. Cuota agotada: error de cuota, sin datos inventados.
No se ha verificado una consulta real ni el alcance de la cuenta de 100 consultas sin su clave.

En Postman: POST http://localhost:8080/auth/login con username/password de su cuenta.
Usar el token devuelto como Bearer en las dos rutas GET anteriores.
No guardar el token en colecciones compartidas.
Documentación: https://decolecta.gitbook.io/docs/servicios/integrations-2 y https://decolecta.gitbook.io/docs/servicios/integrations

## Recuperación

El formulario permanece abierto ante errores y no usa el token de una sesión anterior.
Para enviar mensajes reales faltan SMTP y una dirección remitente.
En ms-auth configurar SPRING_MAIL_HOST, SPRING_MAIL_PORT, SPRING_MAIL_USERNAME,
SPRING_MAIL_PASSWORD, RECOVERY_MAIL_FROM y FRONTEND_URL en el entorno del proceso.
Configurar TLS/autenticación según el proveedor mediante SPRING_MAIL_PROPERTIES_*.
En Docker estas variables también deben pasarse al servicio ms-auth; un .env por sí solo no las inyecta.
Para correos ficticios usar un buzón de pruebas local como Mailpit, nunca asumir que un correo inventado recibirá mensajes.
Sin SMTP el endpoint debe devolver 503. Con SMTP usa respuesta genérica, token de 15 minutos y un solo uso.

## Pagos y documentos fiscales

CulqiSandboxClient es un adaptador preparado y probado con respuestas controladas.
No hay una pasarela activada ni transacciones realizadas en un sandbox oficial.
Faltan claves de integración, tokenización/Checkout, webhook autenticado e idempotencia antes de habilitar cobros.
El adaptador solo admite claves sk_test_ y tokens tkn_test_.
Yape, Plin y BCP no se consideran activos. Las ventas quedan con pago pendiente.
XML fiscal y CDR requieren un flujo real de emisión, firma/envío y respuesta SUNAT/OSE.
No se generan CDR falsos. JSON descargado corresponde al registro interno.

## Verificación de interfaz

1. Abrir http://localhost:4200 y recargar. Cerrar sesión e ingresar de nuevo para actualizar el rol.
2. Revisar logo en login, navegación y comprobante; dashboard con datos reales.
3. Con administrador, revisar Usuarios, Importar plantilla Excel y acciones de productos.
4. Buscar por letras en Ventas: una lista con máximo 5 sugerencias y botón de lupa.
5. Agregar productos al carrito sin registrar para comprobar totales y disponibilidad.
6. Elegir un comprobante existente y descargar PDF A4/ticket. Verificar papel blanco, logo y formato correcto en ambos temas.
7. Eliminar registros internos requiere administrador y confirmación; primero se anulan y reponen stock. No probar con datos que se deban conservar.
8. Probar informes semanal, mensual y anual y exportaciones.

Se ejecutaron build de Angular y 28 pruebas de backend. Las pruebas con proveedor externo,
correo real, pagos oficiales y revisión visual del PDF requieren comprobación adicional.
