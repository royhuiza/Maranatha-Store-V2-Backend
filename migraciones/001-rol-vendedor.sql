-- Ejecutar en db_auth tras respaldo y revisar restricciones CHECK existentes.
BEGIN;
UPDATE usuarios SET rol = 'VENDEDOR' WHERE rol = 'USER';
COMMIT;
