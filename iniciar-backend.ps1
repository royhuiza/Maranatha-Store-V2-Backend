$root = $PSScriptRoot
$modules = @('ms-auth','ms-producto','ms-cliente','ms-ventas','api-gateway')
foreach ($module in $modules) {
    $command = "Set-Location '$root'; .\mvnw.cmd -pl $module spring-boot:run"
    Start-Process powershell -ArgumentList '-NoExit','-Command',$command
    Start-Sleep -Seconds 2
}
Write-Host 'Servicios iniciados en ventanas separadas. Swagger: http://localhost:8080/swagger-ui.html' -ForegroundColor Green
