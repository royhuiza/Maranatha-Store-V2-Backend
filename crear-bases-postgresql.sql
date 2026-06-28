SELECT 'CREATE DATABASE db_auth' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'db_auth')\gexec
SELECT 'CREATE DATABASE db_producto' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'db_producto')\gexec
SELECT 'CREATE DATABASE db_cliente' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'db_cliente')\gexec
SELECT 'CREATE DATABASE db_ventas' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'db_ventas')\gexec
