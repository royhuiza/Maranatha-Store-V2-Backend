Write-Host "=== DIAGNÓSTICO MARANATHA STORE V2 ===" -ForegroundColor Cyan
$ports = @(5432,8080,8081,8082,8083,8084)
foreach ($port in $ports) {
    $ok = Test-NetConnection -ComputerName localhost -Port $port -InformationLevel Quiet -WarningAction SilentlyContinue
    if ($ok) { Write-Host "Puerto $port: ABIERTO" -ForegroundColor Green }
    else { Write-Host "Puerto $port: CERRADO" -ForegroundColor Red }
}
try {
    $gateway = Invoke-RestMethod -Uri 'http://localhost:8080/health' -TimeoutSec 5
    Write-Host "Gateway: $($gateway.status)" -ForegroundColor Green
} catch {
    Write-Host 'Gateway no responde en http://localhost:8080' -ForegroundColor Red
}
Write-Host 'Swagger: http://localhost:8080/swagger-ui.html' -ForegroundColor Yellow
