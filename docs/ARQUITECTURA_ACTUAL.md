# Arquitectura actual

```text
Angular :4200
      |
      | /api (proxy local, elimina /api)
      v
API Gateway :8080 -- JWT
   |       |        |        |
 :8081   :8082    :8083    :8084
 Auth   Productos Clientes  Ventas
   |       |        |        |
 db_auth db_producto db_cliente db_ventas
              PostgreSQL :5432
```

El servicio de ventas consulta directamente a productos y clientes. El precio, subtotal y total se calculan en el backend. Al emitir una venta se descuenta stock; al anularla se repone. Las URLs de servicios son configurables mediante variables de entorno, por lo que funciona tanto desde IntelliJ como en Docker.
