-- Maranatha Store V2
-- Migracion 001: clasificacion tributaria de productos

BEGIN;

ALTER TABLE public.productos
    ADD COLUMN IF NOT EXISTS afectacion_igv VARCHAR(20);

COMMIT;
