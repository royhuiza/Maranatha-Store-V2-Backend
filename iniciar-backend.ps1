$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
Set-Location $root

# Cargar variables de entorno
if (Test-Path "$root\.env") {
Get-Content "$root\.env" | ForEach-Object {
    if ($_ -match '^\s*([^#=\s]+)\s*=(.*)$') {
        $nombre = $matches[1]
        $valor = $matches[2].Trim().Trim('"').Trim("'")
        [Environment]::SetEnvironmentVariable(
            $nombre, $valor, "Process"
        )
    }
}

}

if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET)) {
    throw "JWT_SECRET no configurado"
}
# PostgreSQL password for Spring Boot
if ([string]::IsNullOrWhiteSpace($env:POSTGRES_PASSWORD)) { throw "POSTGRES_PASSWORD no configurado" }
$env:SPRING_DATASOURCE_PASSWORD = $env:POSTGRES_PASSWORD
$env:SUNAT_BETA_ENABLED = "false"
if (-not $env:MS_CLIENTE_URL) { $env:MS_CLIENTE_URL = "http://localhost:8083" }
# Activar descubrimiento de microservicios
$env:EUREKA_ENABLED = "true"
$env:EUREKA_URL = "http://localhost:8761/eureka/"
$logs = Join-Path $root "logs"
New-Item -ItemType Directory -Path $logs -Force | Out-Null

$modules = @(
    "config-server",
    "eureka-server",
    "ms-auth",
    "ms-producto",
    "ms-cliente",
    "ms-ventas",
    "ms-facturacion",
    "ms-chat",
    "api-gateway"
)

docker start maranatha-postgres | Out-Null

if ($LASTEXITCODE -ne 0) {
    throw "Error al iniciar PostgreSQL"
}

foreach ($module in $modules) {

    $jar = Join-Path $root "$module\target\$module-2.0.0.jar"

    if (-not (Test-Path $jar)) {
        Write-Host "[ERROR] JAR no encontrado: $module" -ForegroundColor Red
        continue
    }

    $running = Get-CimInstance Win32_Process |
        Where-Object {
            $_.Name -eq "java.exe" -and
            ($_.CommandLine -like "*$module-2.0.0.jar*" -or
             $_.CommandLine -like "*\$module\target\classes*")
        }

    if ($running) {
        Write-Host "[ACTIVO] $module" -ForegroundColor Yellow
        continue
    }

    $fecha = Get-Date -Format "yyyyMMdd-HHmmss"
    $stdout = Join-Path $logs "$module-$fecha.log"
    $stderr = Join-Path $logs "$module-$fecha-error.log"

    $proceso = Start-Process `
        -FilePath "java.exe" `
        -ArgumentList @("-jar", "`"$jar`"") `
        -WorkingDirectory $root `
        -NoNewWindow `
        -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr `
        -PassThru

    Write-Host "[INICIANDO] $module PID: $($proceso.Id)" -ForegroundColor Cyan

    Start-Sleep -Seconds 10

    $proceso.Refresh()

    if ($proceso.HasExited) {
        Write-Host "[ERROR] $module - revisar logs" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Solicitudes de arranque finalizadas."
Write-Host "Logs: $logs"
Write-Host "Eureka: http://localhost:8761"
Write-Host "Gateway: http://localhost:8080"
