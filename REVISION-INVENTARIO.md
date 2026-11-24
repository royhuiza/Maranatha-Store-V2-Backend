# Revisión coordinada de inventario
- Excel suma existencias por nombre/ID existente, conserva precio y descripción; registra reposiciones existentes en historial.
- Bloqueo de archivos idénticos mediante SHA-256; un archivo distinto puede representar otra entrega.
- Edición de catálogo no modifica stock.
- Código público provisional PRD-000001, estable; prefijos por categoría pendientes (no existe entidad categoría en este modelo).
- Plantilla usa Cantidad ingresada; no importar una exportación de existencias como si fuera una entrega.
- Pendientes: roles dinámicos, Yape real, comprobantes por correo/WhatsApp, menú y reportes definitivos, compilación y pruebas integrales.
