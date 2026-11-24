-- Ejecutar únicamente en db_auth, después de respaldo.
BEGIN;
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_rol_check;
UPDATE usuarios SET rol = 'VENDEDOR' WHERE rol = 'USER';
ALTER TABLE usuarios ADD CONSTRAINT usuarios_rol_check CHECK (rol IN ('ADMIN','VENDEDOR'));
COMMIT;
