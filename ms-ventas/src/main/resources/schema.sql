-- Evidencia de firma digital local.

CREATE TABLE IF NOT EXISTS comprobante_firma_local (
                                                       venta_id BIGINT PRIMARY KEY REFERENCES ventas(id),
    serie VARCHAR(10) NOT NULL,
    numero VARCHAR(12) NOT NULL,
    xml_firmado TEXT NOT NULL,
    contenido_qr TEXT NOT NULL,
    hash VARCHAR(128) NOT NULL,
    fecha_firma TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (serie, numero)
    );

-- Numeracion independiente para SUNAT Beta.

CREATE SEQUENCE IF NOT EXISTS comprobante_beta_seq
    START WITH 18
    INCREMENT BY 1
    MAXVALUE 99999999
    NO CYCLE;

-- Vinculacion entre venta interna y comprobante Beta.

CREATE TABLE IF NOT EXISTS comprobante_beta (
                                                venta_id BIGINT PRIMARY KEY REFERENCES ventas(id),
    serie VARCHAR(10) NOT NULL,
    numero VARCHAR(12) NOT NULL,
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (serie, numero)
    );