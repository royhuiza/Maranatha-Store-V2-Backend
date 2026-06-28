# Validación técnica realizada

Fecha: 28 de junio de 2026

## Comprobaciones aprobadas

- Compilación estática de todas las clases Java 17 de `api-gateway`, `ms-auth`, `ms-producto`, `ms-cliente` y `ms-ventas` contra las APIs de Spring/Jakarta: **correcta**.
- Comprobación TypeScript del frontend con `tsc --noEmit`: **correcta**.
- Compilación de componentes y plantillas Angular con `ngc`: **correcta**.
- Lectura y validación XML de todos los archivos `pom.xml`: **correcta**.
- Revisión de rutas entre Angular, Gateway y microservicios: **correcta**.
- Revisión de contratos JSON para productos, clientes, ventas, stock y comprobantes: **correcta**.

## Límite del entorno de validación

El entorno usado para reparar el proyecto no tiene Docker ni acceso a Internet para descargar Maven y dependencias nuevas. Por ello, la prueba integral levantando PostgreSQL y todos los procesos no pudo ejecutarse aquí. El código, las plantillas y los contratos sí fueron compilados/verificados con las dependencias disponibles del proyecto.

Al abrir el proyecto en una computadora con Java 17, Maven, Node 20/22 y PostgreSQL o Docker, sigue el `README.md` y ejecuta la colección de Postman incluida.
