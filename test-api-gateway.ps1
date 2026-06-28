$ErrorActionPreference = 'Stop'
$baseUrl = 'http://localhost:8080'
$stamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()

Write-Host '[1/6] Login...' -ForegroundColor Cyan
$login = Invoke-RestMethod -Method Post -Uri "$baseUrl/auth/login" -ContentType 'application/json' -Body (@{
    username='admin'; password='admin123'
} | ConvertTo-Json)
$headers = @{ Authorization = "Bearer $($login.token)" }

Write-Host '[2/6] Crear producto...' -ForegroundColor Cyan
$producto = Invoke-RestMethod -Method Post -Uri "$baseUrl/productos" -Headers $headers -ContentType 'application/json' -Body (@{
    nombre="Producto prueba $stamp"; descripcion='Creado por test-api-gateway.ps1'; precio=10.50; stock=10; activo=$true
} | ConvertTo-Json)

Write-Host '[3/6] Crear cliente...' -ForegroundColor Cyan
$cliente = Invoke-RestMethod -Method Post -Uri "$baseUrl/clientes" -Headers $headers -ContentType 'application/json' -Body (@{
    nombre='Cliente'; apellido="Prueba $stamp"; tipoDocumento='CE'; numeroDocumento="CE$stamp";
    email="cliente.$stamp@example.com"; telefono='999999999'; direccion='Tacna'; activo=$true
} | ConvertTo-Json)

Write-Host '[4/6] Registrar venta...' -ForegroundColor Cyan
$venta = Invoke-RestMethod -Method Post -Uri "$baseUrl/ventas" -Headers $headers -ContentType 'application/json' -Body (@{
    clienteId=$cliente.id; tipoComprobante='BOLETA'; detalles=@(@{ productoId=$producto.id; cantidad=2 })
} | ConvertTo-Json -Depth 4)

Write-Host '[5/6] Verificar stock...' -ForegroundColor Cyan
$productoActualizado = Invoke-RestMethod -Method Get -Uri "$baseUrl/productos/$($producto.id)" -Headers $headers
if ($productoActualizado.stock -ne 8) { throw "Stock incorrecto: $($productoActualizado.stock)" }

Write-Host '[6/6] Anular venta y verificar reposición...' -ForegroundColor Cyan
Invoke-RestMethod -Method Put -Uri "$baseUrl/ventas/$($venta.id)/anular" -Headers $headers -ContentType 'application/json' -Body '{}' | Out-Null
$productoRestaurado = Invoke-RestMethod -Method Get -Uri "$baseUrl/productos/$($producto.id)" -Headers $headers
if ($productoRestaurado.stock -ne 10) { throw "El stock no se repuso: $($productoRestaurado.stock)" }

Write-Host "PRUEBA COMPLETA OK: $($venta.serie)-$($venta.numero)" -ForegroundColor Green
