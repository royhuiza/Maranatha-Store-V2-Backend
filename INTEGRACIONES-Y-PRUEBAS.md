# Integraciones y pruebas locales

Estado de la revisión: 2 de noviembre de 2026. Sin commits ni push.


## Recuperación

El formulario permanece abierto ante errores y no usa el token de una sesión anterior.
Para enviar mensajes reales faltan SMTP y una dirección remitente.
En ms-auth configurar SPRING_MAIL_HOST, SPRING_MAIL_PORT, SPRING_MAIL_USERNAME,
SPRING_MAIL_PASSWORD, RECOVERY_MAIL_FROM y FRONTEND_URL en el entorno del proceso.
Configurar TLS/autenticación según el proveedor mediante SPRING_MAIL_PROPERTIES_*.
En Docker estas variables también deben pasarse al servicio ms-auth; un .env por sí solo no las inyecta.
Para correos ficticios usar un buzón de pruebas local como Mailpit, nunca asumir que un correo inventado recibirá mensajes.
Sin SMTP el endpoint debe devolver 503. Con SMTP usa respuesta genérica, token de 15 minutos y un solo uso.


## Verificación de interfaz

1. Abrir http://localhost:4200 y recargar. Cerrar sesión e ingresar de nuevo para actualizar el rol.
2. Revisar logo en login, navegación y comprobante; dashboard con datos reales.
3. Con administrador, revisar Usuarios, Importar plantilla Excel y acciones de productos.
4. Buscar por letras en Ventas: una lista con máximo 5 sugerencias y botón de lupa.
5. Agregar productos al carrito sin registrar para comprobar totales y disponibilidad.
6. Elegir un comprobante existente y descargar PDF A4/ticket. Verificar papel blanco, logo y formato correcto en ambos temas.
7. Eliminar registros internos requiere administrador y confirmación; primero se anulan y reponen stock. No probar con datos que se deban conservar.
8. Probar informes semanal, mensual y anual y exportaciones.

Se ejecutaron build de Angular y 28 pruebas de backend. El correo real y la revisión visual del PDF requieren comprobación adicional.
